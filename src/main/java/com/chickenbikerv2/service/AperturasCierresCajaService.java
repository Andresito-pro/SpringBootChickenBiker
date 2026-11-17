package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.AperturasCierresCaja;
import com.chickenbikerv2.entity.MovimientosCaja;
import com.chickenbikerv2.repository.AperturasCierresCajaRepository;
import com.chickenbikerv2.repository.CabecerasRecibosRepository;
import com.chickenbikerv2.repository.CajasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Apertura y cierre de la caja.
 *
 * Reglas de negocio:
 *  - Existe una única caja operativa: la caja número 1.
 *  - Se trabaja con un único turno diario: la caja solo se puede abrir una vez por día.
 *  - No se puede cerrar la caja mientras existan recibos pendientes de cobro
 *    (las mesas de esos pedidos siguen ocupadas).
 *  - Los movimientos manuales solo se registran sobre el turno abierto.
 */
@Service
public class AperturasCierresCajaService {

    public static final int CAJA_OPERATIVA_ID = 1;
    public static final String ESTADO_ABIERTA = "Abierta";
    public static final String ESTADO_CERRADA = "Cerrada";

    /** "1" es el valor que guardaba la versión NetBeans para una caja abierta. */
    private static final List<String> VALORES_ESTADO_ABIERTA = List.of(ESTADO_ABIERTA, "1");

    /** Código usado por el controlador de vistas para mostrar la advertencia de cierre. */
    public static final String ERROR_NO_HAY_CAJA_ABIERTA = "NO_HAY_CAJA_ABIERTA";
    public static final String ERROR_RECIBOS_PENDIENTES = "RECIBOS_PENDIENTES";

    /** movimientos_caja.descripcion_moviento_caja es VARCHAR(45). */
    public static final int MAX_DESCRIPCION_MOVIMIENTO = 45;

    private final AperturasCierresCajaRepository aperturasCierresCajaRepository;
    private final CajasRepository cajasRepository;
    private final CabecerasRecibosRepository cabecerasRecibosRepository;
    private final MovimientosCajaService movimientosCajaService;

    public AperturasCierresCajaService(AperturasCierresCajaRepository aperturasCierresCajaRepository,
                                       CajasRepository cajasRepository,
                                       CabecerasRecibosRepository cabecerasRecibosRepository,
                                       MovimientosCajaService movimientosCajaService) {
        this.aperturasCierresCajaRepository = aperturasCierresCajaRepository;
        this.cajasRepository = cajasRepository;
        this.cabecerasRecibosRepository = cabecerasRecibosRepository;
        this.movimientosCajaService = movimientosCajaService;
    }

    public List<AperturasCierresCaja> listar() {
        return aperturasCierresCajaRepository.findAllByOrderByFechaAperturaDesc();
    }

    public Optional<AperturasCierresCaja> buscarPorId(Integer id) {
        return aperturasCierresCajaRepository.findById(id);
    }

    /** Turno abierto de la caja operativa (caja número 1), si existe. */
    public Optional<AperturasCierresCaja> consultarCajaAbierta() {
        return aperturasCierresCajaRepository
                .findFirstByCajaIdCajaAndEstadoCierreCajaInOrderByIdAperturaCierreCajaDesc(
                        CAJA_OPERATIVA_ID, VALORES_ESTADO_ABIERTA);
    }

    public boolean hayCajaAbierta() {
        return consultarCajaAbierta().isPresent();
    }

    /** Bloquea la fila de la caja 1 (SELECT ... FOR UPDATE) y devuelve el turno abierto. */
    @Transactional
    public Optional<AperturasCierresCaja> bloquearYConsultarCajaAbierta() {
        cajasRepository.bloquearPorId(CAJA_OPERATIVA_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe la caja operativa número " + CAJA_OPERATIVA_ID + " en la base de datos."));
        return consultarCajaAbierta();
    }

    @Transactional
    public AperturasCierresCaja abrirCaja(BigDecimal montoInicial, int idUsuarioApertura) {
        if (montoInicial == null || montoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Monto inicial inválido.");
        }

        // Bloqueo pesimista sobre la caja operativa para evitar aperturas simultáneas
        Optional<AperturasCierresCaja> existente = bloquearYConsultarCajaAbierta();
        if (existente.isPresent()) {
            throw new IllegalStateException("Ya existe una caja abierta (ID "
                    + existente.get().getIdAperturaCierreCaja() + "). No se puede abrir otra.");
        }

        LocalDate hoy = LocalDate.now();
        boolean turnoDeHoyRegistrado = aperturasCierresCajaRepository.existsByCajaIdCajaAndFechaAperturaBetween(
                CAJA_OPERATIVA_ID, hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX));
        if (turnoDeHoyRegistrado) {
            throw new IllegalStateException(
                    "El turno de caja de hoy ya fue abierto y cerrado. El sistema maneja un único turno diario.");
        }

        AperturasCierresCaja nueva = new AperturasCierresCaja();
        nueva.setFechaApertura(LocalDateTime.now());
        nueva.setMontoInicial(montoInicial);
        nueva.setFechaCierre(null);
        nueva.setMontoFinalSistema(null);
        nueva.setMontoFinalReal(null);
        nueva.setDiferencia(BigDecimal.ZERO);
        nueva.setEstadoCierreCaja(ESTADO_ABIERTA);
        nueva.setCajaIdCaja(CAJA_OPERATIVA_ID);
        nueva.setUsuarioIdUsuario(idUsuarioApertura);
        nueva.setUsuarioIdUsuario1(idUsuarioApertura); // Placeholder hasta el cierre (columna NOT NULL)

        return aperturasCierresCajaRepository.save(nueva);
    }

    @Transactional
    public AperturasCierresCaja cerrarCaja(BigDecimal montoFinalReal, int idUsuarioCierre) {
        if (montoFinalReal == null || montoFinalReal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Monto final real inválido.");
        }

        // Bloqueo pesimista sobre la caja operativa
        AperturasCierresCaja abierta = bloquearYConsultarCajaAbierta()
                .orElseThrow(() -> new IllegalStateException(ERROR_NO_HAY_CAJA_ABIERTA));

        long pendientes = cabecerasRecibosRepository.countByEstadoReciboIdEstadoRecibo(
                PedidosService.ESTADO_RECIBO_GENERADO);
        if (pendientes > 0) {
            throw new IllegalStateException(ERROR_RECIBOS_PENDIENTES);
        }

        BigDecimal montoSistema = calcularMontoSistema(abierta);

        abierta.setFechaCierre(LocalDateTime.now());
        abierta.setMontoFinalSistema(montoSistema);
        abierta.setMontoFinalReal(montoFinalReal);
        abierta.setDiferencia(montoFinalReal.subtract(montoSistema));
        abierta.setEstadoCierreCaja(ESTADO_CERRADA);
        abierta.setUsuarioIdUsuario1(idUsuarioCierre);

        return aperturasCierresCajaRepository.saveAndFlush(abierta);
    }

    /** Monto esperado en caja: monto inicial + ingresos - egresos del turno. */
    public BigDecimal calcularMontoSistema(AperturasCierresCaja turno) {
        BigDecimal ingresos = movimientosCajaService.sumarIngresosPorTurno(turno.getIdAperturaCierreCaja());
        BigDecimal egresos = movimientosCajaService.sumarEgresosPorTurno(turno.getIdAperturaCierreCaja());
        BigDecimal inicial = turno.getMontoInicial() != null ? turno.getMontoInicial() : BigDecimal.ZERO;
        return inicial.add(ingresos).subtract(egresos);
    }

    /**
     * Registra un ingreso o egreso manual sobre el turno abierto
     * (equivalente a MovimientoServlet de la versión NetBeans).
     */
    @Transactional
    public MovimientosCaja registrarMovimientoManual(BigDecimal valor, int idTipoMovimiento,
                                                     String descripcion, int idUsuario) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del movimiento debe ser mayor a cero.");
        }
        if (idTipoMovimiento != MovimientosCajaService.TIPO_MOV_INGRESO
                && idTipoMovimiento != MovimientosCajaService.TIPO_MOV_EGRESO) {
            throw new IllegalArgumentException("Selecciona un tipo de movimiento válido (ingreso o egreso).");
        }
        String descripcionLimpia = descripcion == null ? "" : descripcion.trim();
        if (descripcionLimpia.isEmpty()) {
            throw new IllegalArgumentException("La descripción del movimiento es obligatoria.");
        }
        if (descripcionLimpia.length() > MAX_DESCRIPCION_MOVIMIENTO) {
            throw new IllegalArgumentException(
                    "La descripción admite como máximo " + MAX_DESCRIPCION_MOVIMIENTO + " caracteres.");
        }

        AperturasCierresCaja abierta = bloquearYConsultarCajaAbierta()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay una caja abierta. Abre la caja antes de registrar movimientos."));

        MovimientosCaja movimiento = new MovimientosCaja();
        movimiento.setValorMovimientoCaja(valor.floatValue());
        movimiento.setDescripcionMovientoCaja(descripcionLimpia);
        movimiento.setFechaMovimientoCaja(LocalDate.now());
        movimiento.setTipoMovimientoIdTipoMovimiento(idTipoMovimiento);
        movimiento.setUsuarioIdUsuario(idUsuario);
        movimiento.setAperturaCierreCajaIdAperturaCierreCaja(abierta.getIdAperturaCierreCaja());
        return movimientosCajaService.guardar(movimiento);
    }
}
