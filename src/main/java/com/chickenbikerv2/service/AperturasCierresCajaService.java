package com.chickenbikerv2.service;

import com.chickenbikerv2.dto.ResumenCajaDTO;
import com.chickenbikerv2.entity.AperturasCierresCaja;
import com.chickenbikerv2.entity.CabecerasRecibos;
import com.chickenbikerv2.entity.MetodosPago;
import com.chickenbikerv2.entity.MovimientosCaja;
import com.chickenbikerv2.entity.TipoMetodoPago;
import com.chickenbikerv2.repository.AperturasCierresCajaRepository;
import com.chickenbikerv2.repository.CabecerasRecibosRepository;
import com.chickenbikerv2.repository.CajasRepository;
import com.chickenbikerv2.repository.MetodosPagoRepository;
import com.chickenbikerv2.repository.PedidosVentasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private final MetodosPagoRepository metodosPagoRepository;
    private final CatalogoEstadosService catalogoEstadosService;
    private final PedidosVentasRepository pedidosVentasRepository;

    /** Descripción que siempre generó PagosService para el ingreso de un cobro. */
    private static final Pattern COBRO_HISTORICO = Pattern.compile("^Cobro (REC-\\d+)\\b");

    public AperturasCierresCajaService(AperturasCierresCajaRepository aperturasCierresCajaRepository,
                                       CajasRepository cajasRepository,
                                       CabecerasRecibosRepository cabecerasRecibosRepository,
                                       MovimientosCajaService movimientosCajaService,
                                       MetodosPagoRepository metodosPagoRepository,
                                       CatalogoEstadosService catalogoEstadosService,
                                       PedidosVentasRepository pedidosVentasRepository) {
        this.aperturasCierresCajaRepository = aperturasCierresCajaRepository;
        this.cajasRepository = cajasRepository;
        this.cabecerasRecibosRepository = cabecerasRecibosRepository;
        this.movimientosCajaService = movimientosCajaService;
        this.metodosPagoRepository = metodosPagoRepository;
        this.catalogoEstadosService = catalogoEstadosService;
        this.pedidosVentasRepository = pedidosVentasRepository;
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

        // Los recibos de pedidos ANULADOS no bloquean el cierre (el recibo conserva su estado)
        long pendientes = cabecerasRecibosRepository.contarPorEstadoSinPedidosAnulados(
                PedidosService.ESTADO_RECIBO_GENERADO, catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir());
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

    /**
     * Efectivo esperado en caja (lo que el cajero debe contar físicamente):
     * monto inicial + cobros en EFECTIVO + ingresos manuales − egresos.
     * Los pagos con tarjeta y plataformas digitales NO se suman.
     */
    public BigDecimal calcularMontoSistema(AperturasCierresCaja turno) {
        return calcularResumen(turno).efectivoEsperado();
    }

    /**
     * Cuadre del turno con los ingresos diferenciados por medio de pago.
     *
     * Cada ingreso por cobro se clasifica por el método de pago de su recibo, que
     * se identifica por la descripción que siempre generó el cobro
     * ("Cobro REC-n pedido #m"). Los movimientos no se modifican.
     * Los demás ingresos son movimientos manuales de caja (dinero físico).
     * Solo lectura: no altera pagos, movimientos ni cierres.
     */
    @Transactional(readOnly = true)
    public ResumenCajaDTO calcularResumen(AperturasCierresCaja turno) {
        int idTurno = turno.getIdAperturaCierreCaja();
        Map<Integer, MetodosPago> metodos = metodosPagoRepository.findAll().stream()
                .collect(Collectors.toMap(MetodosPago::getIdMetodoPago, Function.identity()));
        int estadoPedidoAnulado = catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir();

        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal tarjeta = BigDecimal.ZERO;
        BigDecimal digital = BigDecimal.ZERO;
        BigDecimal manuales = BigDecimal.ZERO;
        BigDecimal egresos = BigDecimal.ZERO;
        BigDecimal anulados = BigDecimal.ZERO;
        Map<Integer, BigDecimal> totalPorMetodo = new LinkedHashMap<>();
        Map<Integer, Long> cantidadPorMetodo = new LinkedHashMap<>();

        for (MovimientosCaja mov : movimientosCajaService.listarPorTurno(idTurno)) {
            BigDecimal valor = new BigDecimal(Float.toString(mov.getValorMovimientoCaja()));
            if (mov.getTipoMovimientoIdTipoMovimiento() == MovimientosCajaService.TIPO_MOV_EGRESO) {
                egresos = egresos.add(valor);
                continue;
            }
            if (mov.getTipoMovimientoIdTipoMovimiento() != MovimientosCajaService.TIPO_MOV_INGRESO) {
                continue; // otros tipos (p. ej. "Ajuste") no se sumaban antes y siguen sin sumarse
            }
            CabecerasRecibos recibo = reciboDelCobro(mov);
            if (recibo == null) {
                manuales = manuales.add(valor);
                continue;
            }
            MetodosPago metodo = metodos.get(recibo.getMetodoPagoIdMetodoPago());
            TipoMetodoPago tipo = metodo != null ? metodo.tipo() : TipoMetodoPago.DIGITAL;
            switch (tipo) {
                case EFECTIVO -> efectivo = efectivo.add(valor);
                case TARJETA -> tarjeta = tarjeta.add(valor);
                default -> digital = digital.add(valor);
            }
            // Cobro de un pedido anulado después de pagado: el dinero sigue en caja (informativo)
            if (pedidosVentasRepository.findById(recibo.getPedidoVentaIdPedidoVenta())
                    .map(p -> p.getEstadoPedidoIdEstado() == estadoPedidoAnulado).orElse(false)) {
                anulados = anulados.add(valor);
            }
            totalPorMetodo.merge(recibo.getMetodoPagoIdMetodoPago(), valor, BigDecimal::add);
            cantidadPorMetodo.merge(recibo.getMetodoPagoIdMetodoPago(), 1L, Long::sum);
        }

        List<ResumenCajaDTO.DetalleMetodo> porMetodo = totalPorMetodo.entrySet().stream()
                .map(e -> {
                    MetodosPago m = metodos.get(e.getKey());
                    return new ResumenCajaDTO.DetalleMetodo(
                            m != null ? m.getNombreMetodo() : "Método #" + e.getKey(),
                            m != null ? m.tipo() : TipoMetodoPago.DIGITAL,
                            escala(e.getValue()), cantidadPorMetodo.getOrDefault(e.getKey(), 0L));
                })
                .sorted(Comparator.comparing((ResumenCajaDTO.DetalleMetodo d) -> d.tipo().ordinal())
                        .thenComparing(ResumenCajaDTO.DetalleMetodo::nombreMetodo))
                .toList();

        BigDecimal inicial = turno.getMontoInicial() != null ? turno.getMontoInicial() : BigDecimal.ZERO;
        BigDecimal esperado = inicial.add(efectivo).add(manuales).subtract(egresos);
        return new ResumenCajaDTO(idTurno, escala(inicial), escala(efectivo), escala(tarjeta), escala(digital),
                escala(manuales), escala(egresos), escala(anulados), porMetodo, escala(esperado));
    }

    /**
     * Recibo que originó un ingreso por cobro, o null si es un ingreso manual.
     * La tabla movimientos_caja no tiene relación con cabeceras_recibos, por eso
     * el recibo se identifica por la descripción que PagosService genera en cada
     * cobro ("Cobro REC-n pedido #m"); los movimientos manuales no pueden usar
     * ese prefijo (ver registrarMovimientoManual).
     */
    private CabecerasRecibos reciboDelCobro(MovimientosCaja mov) {
        String descripcion = mov.getDescripcionMovientoCaja();
        Matcher m = descripcion == null ? null : COBRO_HISTORICO.matcher(descripcion);
        if (m == null || !m.find()) {
            return null;
        }
        return cabecerasRecibosRepository.findByNumeroRecibo(m.group(1))
                .filter(r -> r.getEstadoReciboIdEstadoRecibo() != PedidosService.ESTADO_RECIBO_GENERADO)
                .orElse(null);
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
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
        if (COBRO_HISTORICO.matcher(descripcionLimpia).find()) {
            // Prefijo reservado: identifica los ingresos generados por el cobro de un recibo.
            throw new IllegalArgumentException(
                    "La descripción no puede empezar por \"Cobro REC-\": está reservada para los cobros de recibos.");
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
