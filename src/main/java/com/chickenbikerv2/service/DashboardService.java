package com.chickenbikerv2.service;

import com.chickenbikerv2.dto.ResumenCajaDTO;
import com.chickenbikerv2.dto.VentaDiariaDTO;
import com.chickenbikerv2.dto.VentasPeriodoDTO;
import com.chickenbikerv2.entity.AperturasCierresCaja;
import com.chickenbikerv2.entity.MovimientosCaja;
import com.chickenbikerv2.repository.CabecerasRecibosRepository;
import com.chickenbikerv2.repository.MesasRepository;
import com.chickenbikerv2.repository.PedidosVentasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Indicadores de los dashboards por rol (administrador, cajero y mesero) y
 * estadísticas de ventas del dashboard del administrador. Solo lectura.
 */
@Service
public class DashboardService {

    /** Rango máximo que admite la consulta de ventas (un año + 1 día bisiesto). */
    public static final int MAX_DIAS_RANGO = 366;
    /** Filas de movimientos del turno que muestra el dashboard del cajero. */
    public static final int MAX_MOVIMIENTOS_DASHBOARD = 10;
    public static final String MONEDA = "COP";

    private final AperturasCierresCajaService aperturasCierresCajaService;
    private final MovimientosCajaService movimientosCajaService;
    private final PagosService pagosService;
    private final PedidosVentasRepository pedidosVentasRepository;
    private final CabecerasRecibosRepository cabecerasRecibosRepository;
    private final MesasRepository mesasRepository;
    private final CatalogoEstadosService catalogoEstadosService;

    public DashboardService(AperturasCierresCajaService aperturasCierresCajaService,
                            MovimientosCajaService movimientosCajaService,
                            PagosService pagosService,
                            PedidosVentasRepository pedidosVentasRepository,
                            CabecerasRecibosRepository cabecerasRecibosRepository,
                            MesasRepository mesasRepository,
                            CatalogoEstadosService catalogoEstadosService) {
        this.aperturasCierresCajaService = aperturasCierresCajaService;
        this.movimientosCajaService = movimientosCajaService;
        this.pagosService = pagosService;
        this.pedidosVentasRepository = pedidosVentasRepository;
        this.cabecerasRecibosRepository = cabecerasRecibosRepository;
        this.mesasRepository = mesasRepository;
        this.catalogoEstadosService = catalogoEstadosService;
    }

    /** Dashboard del administrador: indicadores generales de la operación. */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumen() {
        Map<String, Object> resumen = new LinkedHashMap<>();
        Optional<AperturasCierresCaja> caja = aperturasCierresCajaService.consultarCajaAbierta();
        LocalDate hoy = LocalDate.now();

        resumen.put("caja", caja.orElse(null));
        ResumenCajaDTO cuadre = caja.map(aperturasCierresCajaService::calcularResumen).orElse(null);
        resumen.put("resumenCaja", cuadre);
        resumen.put("ingresosTurno", cuadre != null ? cuadre.totalIngresos() : BigDecimal.ZERO);
        // Los pedidos anulados no cuentan como pedidos del día
        resumen.put("pedidosHoy", pedidosVentasRepository.countByFechaPedidoBetweenAndEstadoPedidoIdEstadoNot(
                hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX), catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir()));
        resumen.put("recibosPendientes", cabecerasRecibosRepository.contarPorEstadoSinPedidosAnulados(
                PedidosService.ESTADO_RECIBO_GENERADO, catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir()));
        resumen.putAll(resumenMesas());
        return resumen;
    }

    /** Dashboard del cajero: turno de caja abierto, cobros pendientes y movimientos. */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumenCajero() {
        Map<String, Object> resumen = new LinkedHashMap<>();
        AperturasCierresCaja caja = aperturasCierresCajaService.consultarCajaAbierta().orElse(null);
        resumen.put("caja", caja);

        if (caja != null) {
            int idTurno = caja.getIdAperturaCierreCaja();
            ResumenCajaDTO cuadre = aperturasCierresCajaService.calcularResumen(caja);
            resumen.put("resumenCaja", cuadre);
            resumen.put("montoInicial", cuadre.montoInicial());
            resumen.put("ingresosTurno", cuadre.totalIngresos());
            resumen.put("egresosTurno", cuadre.egresos());
            // Solo dinero físico: los pagos con tarjeta / plataformas digitales no se suman
            resumen.put("montoEsperado", cuadre.efectivoEsperado());
            resumen.put("movimientosTurno", movimientosCajaService.listarPorTurno(idTurno).stream()
                    .sorted(Comparator.comparingInt(MovimientosCaja::getIdMovimientoCaja).reversed())
                    .limit(MAX_MOVIMIENTOS_DASHBOARD)
                    .toList());
        } else {
            resumen.put("movimientosTurno", List.of());
        }

        resumen.put("pendientes", pagosService.listarRecibosPendientes());
        resumen.put("metodosPago", pagosService.listarMetodosPagoDisponibles());
        return resumen;
    }

    /** Dashboard del mesero: disponibilidad de mesas y estado de la caja. */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumenMesero() {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("caja", aperturasCierresCajaService.consultarCajaAbierta().orElse(null));
        resumen.putAll(resumenMesas());
        return resumen;
    }

    /**
     * Ventas pagadas por día entre {@code desde} y {@code hasta} (ambos incluidos).
     *
     * Regla de negocio: una venta es un recibo (cabeceras_recibos) en estado
     * "Pagado". Se contabiliza en la fecha de emisión del recibo; el recibo se
     * cobra dentro del mismo turno diario de caja, que no se puede cerrar con
     * recibos pendientes (ver AperturasCierresCajaService), así que esa fecha
     * coincide con el día del turno en que ingresó el dinero.
     * Los días sin ventas se devuelven con total 0.
     */
    @Transactional(readOnly = true)
    public VentasPeriodoDTO ventasDiarias(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Debes indicar la fecha inicial y la fecha final.");
        }
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial.");
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (dias > MAX_DIAS_RANGO) {
            throw new IllegalArgumentException("El rango máximo de consulta es de " + MAX_DIAS_RANGO + " días.");
        }

        Map<LocalDate, VentaDiariaDTO> porFecha = cabecerasRecibosRepository
                .sumarVentasPorDia(PedidosService.ESTADO_RECIBO_PAGADO,
                        desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(),
                        catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir())
                .stream()
                .collect(Collectors.toMap(VentaDiariaDTO::fecha, Function.identity()));

        List<VentaDiariaDTO> serie = new ArrayList<>((int) dias);
        BigDecimal total = BigDecimal.ZERO;
        long recibos = 0;
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            VentaDiariaDTO venta = porFecha.getOrDefault(dia, VentaDiariaDTO.sinVentas(dia));
            serie.add(venta);
            total = total.add(venta.total());
            recibos += venta.cantidadRecibos();
        }
        return new VentasPeriodoDTO(desde, hasta, MONEDA, total, recibos, serie);
    }

    private Map<String, Object> resumenMesas() {
        Map<String, Object> mesas = new LinkedHashMap<>();
        mesas.put("mesasOcupadas", mesasRepository.countByEstadoMesaIdEstadoMesa(PedidosService.ESTADO_MESA_OCUPADA));
        mesas.put("mesasDisponibles", mesasRepository.countByEstadoMesaIdEstadoMesa(PedidosService.ESTADO_MESA_DISPONIBLE));
        mesas.put("totalMesas", mesasRepository.count());
        return mesas;
    }
}
