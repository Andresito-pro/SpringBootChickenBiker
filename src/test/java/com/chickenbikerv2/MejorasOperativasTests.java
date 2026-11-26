package com.chickenbikerv2;

import com.chickenbikerv2.dto.ResumenCajaDTO;
import com.chickenbikerv2.dto.VentasPeriodoDTO;
import com.chickenbikerv2.entity.*;
import com.chickenbikerv2.repository.*;
import com.chickenbikerv2.security.VigenciaUsuario;
import com.chickenbikerv2.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mejoras 2.2 (visibilidad del mesero), 2.3 (vigencia), 2.4 (ANULADO),
 * 2.5 (notificaciones), 2.7 (efectivo esperado) y 2.1 (permisos por rol)
 * a nivel de servicio.
 */
@SpringBootTest
@Sql(scripts = "/datos-prueba.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MejorasOperativasTests {

    private static final int ADMIN = 1;
    private static final int CAJERO = 2;
    private static final int MESERO = 3;
    private static final int MESERO_2 = 6;
    private static final int ROL_ADMIN = 1;
    private static final int ROL_CAJERO = 2;
    private static final int ESTADO_ANULADO = 6;

    @Autowired private AperturasCierresCajaService cajaService;
    @Autowired private PedidosService pedidosService;
    @Autowired private PagosService pagosService;
    @Autowired private AnulacionPedidosService anulacionService;
    @Autowired private NotificacionesService notificacionesService;
    @Autowired private GestionPermisosService gestionPermisosService;
    @Autowired private DashboardService dashboardService;
    @Autowired private PedidosVentasRepository pedidosRepository;
    @Autowired private CabecerasRecibosRepository recibosRepository;
    @Autowired private MesasRepository mesasRepository;
    @Autowired private MovimientosCajaRepository movimientosRepository;
    @Autowired private DetallesPedidosVentaRepository detallesRepository;
    @Autowired private PermisosRolesRepository permisosRolesRepository;
    @Autowired private AperturasCierresCajaRepository aperturasRepository;

    private PedidosVentas pedidoEnMesa(int mesa, int idUsuario) {
        return pedidosService.registrarPedido(PedidosService.TIPO_MESA, mesa, List.of(1), List.of(1), idUsuario);
    }

    private CabecerasRecibos recibo(PedidosVentas pedido) {
        return recibosRepository.findByPedidoVentaIdPedidoVenta(pedido.getIdPedidoVenta()).orElseThrow();
    }

    // ------------------------------------------------------------------ 2.2

    @Test
    void elMeseroSoloVeLosPedidosQueRegistro() {
        cajaService.abrirCaja(new BigDecimal("10000"), CAJERO);
        PedidosVentas propio = pedidoEnMesa(1, MESERO);
        PedidosVentas ajeno = pedidoEnMesa(2, MESERO_2);

        List<PedidosVentas> delMesero = pedidosService.listarPedidos(MESERO, false);
        assertEquals(List.of(propio.getIdPedidoVenta()),
                delMesero.stream().map(PedidosVentas::getIdPedidoVenta).toList());
        assertEquals(2, pedidosService.listarPedidos(CAJERO, true).size(), "Con 'Ver todos los pedidos' ve todo");

        assertTrue(pedidosService.buscarPedidoVisible(propio.getIdPedidoVenta(), MESERO, false).isPresent());
        assertTrue(pedidosService.buscarPedidoVisible(ajeno.getIdPedidoVenta(), MESERO, false).isEmpty());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> pedidosService.obtenerDetallePedido(ajeno.getIdPedidoVenta(), MESERO, false));
        assertFalse(e.getMessage().contains("Sara"), "El error no revela datos del otro mesero");
    }

    // ------------------------------------------------------------------ 2.4

    @Test
    void anularPedidoPendienteSoloCambiaSuEstadoLiberaLaMesaYNoSeRepite() {
        cajaService.abrirCaja(new BigDecimal("10000"), CAJERO);
        PedidosVentas pedido = pedidoEnMesa(1, MESERO);
        long movimientosAntes = movimientosRepository.count();
        long detallesAntes = detallesRepository.count();

        PedidosVentas anulado = anulacionService.anularPedidoPendiente(pedido.getIdPedidoVenta(), CAJERO, true);
        assertEquals(ESTADO_ANULADO, anulado.getEstadoPedidoIdEstado());

        PedidosVentas recargado = pedidosRepository.findById(pedido.getIdPedidoVenta()).orElseThrow();
        assertEquals(ESTADO_ANULADO, recargado.getEstadoPedidoIdEstado(), "No se borra: queda en estado Anulado");
        assertEquals(0, pedido.getTotalPedido().compareTo(recargado.getTotalPedido()));
        assertEquals(detallesAntes, detallesRepository.count(), "El detalle del pedido se conserva");
        assertEquals(PedidosService.ESTADO_RECIBO_GENERADO, recibo(pedido).getEstadoReciboIdEstadoRecibo(),
                "El recibo no se modifica: la anulación es solo el estado del pedido");
        assertEquals(PedidosService.ESTADO_MESA_DISPONIBLE,
                mesasRepository.findById(1).orElseThrow().getEstadoMesaIdEstadoMesa(), "La mesa queda libre");
        assertEquals(movimientosAntes, movimientosRepository.count(), "No se crean movimientos de dinero");

        IllegalStateException doble = assertThrows(IllegalStateException.class,
                () -> anulacionService.anularPedidoPendiente(pedido.getIdPedidoVenta(), CAJERO, true));
        assertTrue(doble.getMessage().contains("ya fue anulado"));
        assertThrows(IllegalStateException.class,
                () -> anulacionService.anularPedidoPagado(pedido.getIdPedidoVenta(), true, ADMIN, true),
                "Tampoco por el procedimiento de pagados");

        // El recibo de un pedido anulado no se cobra, no figura como pendiente y no bloquea el cierre
        IllegalStateException cobro = assertThrows(IllegalStateException.class,
                () -> pagosService.cobrarRecibo(recibo(pedido).getIdCabeceraRecibo(), 1, CAJERO));
        assertTrue(cobro.getMessage().contains("anulado"));
        assertEquals(PedidosService.ESTADO_RECIBO_GENERADO, recibo(pedido).getEstadoReciboIdEstadoRecibo());
        assertTrue(pagosService.listarRecibosPendientes().isEmpty());
        AperturasCierresCaja cerrado = cajaService.cerrarCaja(new BigDecimal("10000"), CAJERO);
        assertEquals(0, new BigDecimal("10000").compareTo(cerrado.getMontoFinalSistema()));
    }

    @Test
    void elMeseroNoPuedeAnularPedidosAjenos() {
        cajaService.abrirCaja(new BigDecimal("10000"), CAJERO);
        PedidosVentas ajeno = pedidoEnMesa(1, MESERO_2);
        assertThrows(IllegalArgumentException.class,
                () -> anulacionService.anularPedidoPendiente(ajeno.getIdPedidoVenta(), MESERO, false));
        assertEquals(PedidosService.ESTADO_PEDIDO_PENDIENTE,
                pedidosRepository.findById(ajeno.getIdPedidoVenta()).orElseThrow().getEstadoPedidoIdEstado());
    }

    @Test
    void unPedidoPagadoSoloSeAnulaPorElProcedimientoEspecialYSinRevertirDinero() {
        cajaService.abrirCaja(new BigDecimal("10000"), CAJERO);
        PedidosVentas pedido = pedidoEnMesa(1, MESERO);
        pagosService.cobrarRecibo(recibo(pedido).getIdCabeceraRecibo(), 1, CAJERO);
        long movimientosAntes = movimientosRepository.count();
        BigDecimal esperadoAntes = cajaService.calcularResumen(cajaService.consultarCajaAbierta().orElseThrow())
                .efectivoEsperado();

        IllegalStateException comoPendiente = assertThrows(IllegalStateException.class,
                () -> anulacionService.anularPedidoPendiente(pedido.getIdPedidoVenta(), ADMIN, true));
        assertTrue(comoPendiente.getMessage().contains("ya fue pagado"));

        assertThrows(IllegalArgumentException.class,
                () -> anulacionService.anularPedidoPagado(pedido.getIdPedidoVenta(), false, ADMIN, true),
                "Exige la confirmación adicional");

        anulacionService.anularPedidoPagado(pedido.getIdPedidoVenta(), true, ADMIN, true);
        assertEquals(ESTADO_ANULADO, pedidosRepository.findById(pedido.getIdPedidoVenta()).orElseThrow()
                .getEstadoPedidoIdEstado());
        assertEquals(PedidosService.ESTADO_RECIBO_PAGADO, recibo(pedido).getEstadoReciboIdEstadoRecibo(),
                "El recibo pagado se conserva tal cual");
        assertEquals(movimientosAntes, movimientosRepository.count(), "Sin reversión financiera automática");

        ResumenCajaDTO despues = cajaService.calcularResumen(cajaService.consultarCajaAbierta().orElseThrow());
        assertEquals(0, esperadoAntes.compareTo(despues.efectivoEsperado()),
                "El dinero cobrado sigue físicamente en la caja");
        assertEquals(0, new BigDecimal("12000").compareTo(despues.cobrosAnulados()));

        assertThrows(IllegalStateException.class,
                () -> anulacionService.anularPedidoPagado(pedido.getIdPedidoVenta(), true, ADMIN, true),
                "No se anula dos veces");
    }

    @Test
    void unPedidoPagadoEnUnTurnoYaCerradoNoSePuedeAnular() {
        cajaService.abrirCaja(new BigDecimal("0"), CAJERO);
        PedidosVentas pedido = pedidosService.registrarPedido(PedidosService.TIPO_PARA_LLEVAR, null, List.of(1), List.of(1), CAJERO);
        pagosService.cobrarRecibo(recibo(pedido).getIdCabeceraRecibo(), 1, CAJERO);
        cajaService.cerrarCaja(new BigDecimal("12000"), CAJERO);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> anulacionService.anularPedidoPagado(pedido.getIdPedidoVenta(), true, ADMIN, true));
        assertTrue(e.getMessage().contains("caja"));
        assertEquals(PedidosService.ESTADO_PEDIDO_ENTREGADO,
                pedidosRepository.findById(pedido.getIdPedidoVenta()).orElseThrow().getEstadoPedidoIdEstado());
    }

    @Test
    void losPedidosAnuladosNoCuentanEnDashboardNiVentas() {
        cajaService.abrirCaja(new BigDecimal("10000"), CAJERO);
        PedidosVentas anuladoPendiente = pedidoEnMesa(1, MESERO);
        PedidosVentas vendido = pedidoEnMesa(2, MESERO);
        PedidosVentas anuladoPagado = pedidosService.registrarPedido(PedidosService.TIPO_PARA_LLEVAR, null, List.of(2), List.of(1), CAJERO);
        pagosService.cobrarRecibo(recibo(vendido).getIdCabeceraRecibo(), 1, CAJERO);
        pagosService.cobrarRecibo(recibo(anuladoPagado).getIdCabeceraRecibo(), 2, CAJERO);
        anulacionService.anularPedidoPendiente(anuladoPendiente.getIdPedidoVenta(), ADMIN, true);
        anulacionService.anularPedidoPagado(anuladoPagado.getIdPedidoVenta(), true, ADMIN, true);

        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        assertEquals(1, pedidosRepository.countByFechaPedidoBetweenAndEstadoPedidoIdEstadoNot(
                inicio, inicio.plusDays(1), ESTADO_ANULADO));

        VentasPeriodoDTO ventas = dashboardService.ventasDiarias(LocalDate.now(), LocalDate.now());
        assertEquals(1, ventas.cantidadRecibos(), "Solo cuenta el pedido vendido");
        assertEquals(0, new BigDecimal("12000").compareTo(ventas.totalPeriodo()));
        assertEquals(0L, dashboardService.obtenerResumen().get("recibosPendientes"));
    }

    // ------------------------------------------------------------------ 2.5

    @Test
    void lasNotificacionesSeDerivanDeLosPedidosSinTablaPropia() {
        cajaService.abrirCaja(new BigDecimal("10000"), CAJERO);
        NotificacionesService.EstadoLectura lecturaAdmin = new NotificacionesService.EstadoLectura();
        NotificacionesService.EstadoLectura lecturaCajero = new NotificacionesService.EstadoLectura();
        PedidosVentas pedido = pedidoEnMesa(1, MESERO);

        NotificacionesService.Resumen admin = notificacionesService.resumen(ADMIN, lecturaAdmin);
        assertEquals(1, admin.noLeidas());
        assertEquals("Nuevo pedido #" + pedido.getIdPedidoVenta(), admin.recientes().get(0).titulo());
        assertTrue(admin.recientes().get(0).mensaje().startsWith("Mesa 1"));
        assertEquals(1, notificacionesService.resumen(CAJERO, lecturaCajero).noLeidas());

        // El creador no se notifica a sí mismo
        PedidosVentas delCajero = pedidoEnMesa(2, CAJERO);
        assertEquals(2, notificacionesService.resumen(ADMIN, lecturaAdmin).noLeidas());
        assertEquals(1, notificacionesService.resumen(CAJERO, lecturaCajero).noLeidas());

        // Un usuario no puede marcar como notificación un pedido que no le corresponde (el suyo propio)
        assertFalse(notificacionesService.marcarLeida(delCajero.getIdPedidoVenta(), CAJERO, lecturaCajero));
        assertTrue(notificacionesService.marcarLeida(delCajero.getIdPedidoVenta(), ADMIN, lecturaAdmin));
        assertEquals(1, notificacionesService.resumen(ADMIN, lecturaAdmin).noLeidas());
        notificacionesService.marcarTodasLeidas(ADMIN, lecturaAdmin);
        assertEquals(0, notificacionesService.resumen(ADMIN, lecturaAdmin).noLeidas());
        assertEquals(1, notificacionesService.resumen(CAJERO, lecturaCajero).noLeidas(), "No afecta a otros usuarios");

        // Un pedido nuevo vuelve a aparecer como no leído; uno anulado deja de notificarse
        PedidosVentas otro = pedidosService.registrarPedido(PedidosService.TIPO_PARA_LLEVAR, null, List.of(2), List.of(1), MESERO);
        assertEquals(1, notificacionesService.resumen(ADMIN, lecturaAdmin).noLeidas());
        anulacionService.anularPedidoPendiente(otro.getIdPedidoVenta(), ADMIN, true);
        assertEquals(0, notificacionesService.resumen(ADMIN, lecturaAdmin).noLeidas());
    }

    // ------------------------------------------------------------------ 2.7

    @Test
    void elEfectivoEsperadoSoloSumaCobrosEnEfectivo() {
        AperturasCierresCaja turno = cajaService.abrirCaja(new BigDecimal("100000"), CAJERO);
        // efectivo: 1 pollo (12.000) · tarjeta: 2 pollos (24.000) · Nequi: 1 gaseosa (3.000)
        PedidosVentas efectivo = pedidosService.registrarPedido(PedidosService.TIPO_PARA_LLEVAR, null, List.of(1), List.of(1), CAJERO);
        PedidosVentas tarjeta = pedidosService.registrarPedido(PedidosService.TIPO_PARA_LLEVAR, null, List.of(1), List.of(2), CAJERO);
        PedidosVentas digital = pedidosService.registrarPedido(PedidosService.TIPO_PARA_LLEVAR, null, List.of(2), List.of(1), CAJERO);
        pagosService.cobrarRecibo(recibo(efectivo).getIdCabeceraRecibo(), 1, CAJERO);
        pagosService.cobrarRecibo(recibo(tarjeta).getIdCabeceraRecibo(), 2, CAJERO);
        pagosService.cobrarRecibo(recibo(digital).getIdCabeceraRecibo(), 3, CAJERO);

        ResumenCajaDTO resumen = cajaService.calcularResumen(turno);
        assertEquals(0, new BigDecimal("12000").compareTo(resumen.cobrosEfectivo()));
        assertEquals(0, new BigDecimal("24000").compareTo(resumen.cobrosTarjeta()));
        assertEquals(0, new BigDecimal("3000").compareTo(resumen.cobrosDigitales()));
        assertEquals(0, new BigDecimal("27000").compareTo(resumen.totalElectronicos()));
        assertEquals(0, new BigDecimal("39000").compareTo(resumen.totalCobros()));
        assertEquals(0, new BigDecimal("112000").compareTo(resumen.efectivoEsperado()),
                "Monto inicial + solo efectivo");

        AperturasCierresCaja cerrado = cajaService.cerrarCaja(new BigDecimal("112000"), CAJERO);
        assertEquals(0, new BigDecimal("112000").compareTo(cerrado.getMontoFinalSistema()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cerrado.getDiferencia()));
    }

    @Test
    void losCobrosSeClasificanPorLaDescripcionDelMovimiento() {
        AperturasCierresCaja turno = cajaService.abrirCaja(new BigDecimal("0"), CAJERO);
        PedidosVentas pedido = pedidosService.registrarPedido(PedidosService.TIPO_PARA_LLEVAR, null, List.of(1), List.of(1), CAJERO);
        pagosService.cobrarRecibo(recibo(pedido).getIdCabeceraRecibo(), 2, CAJERO);

        // movimientos_caja no tiene relación con el recibo: se identifica por "Cobro REC-n pedido #m"
        MovimientosCaja mov = movimientosRepository.findAll().get(0);
        assertTrue(mov.getDescripcionMovientoCaja().startsWith("Cobro " + recibo(pedido).getNumeroRecibo()));

        ResumenCajaDTO resumen = cajaService.calcularResumen(turno);
        assertEquals(0, new BigDecimal("12000").compareTo(resumen.cobrosTarjeta()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resumen.efectivoEsperado()), "La tarjeta no entra al efectivo");

        // Un movimiento manual no puede hacerse pasar por un cobro
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> cajaService.registrarMovimientoManual(new BigDecimal("5000"),
                        MovimientosCajaService.TIPO_MOV_INGRESO, "Cobro " + recibo(pedido).getNumeroRecibo(), CAJERO));
        assertTrue(e.getMessage().contains("reservada"));
    }

    // ------------------------------------------------------------------ 2.3

    @Test
    void reglasDeVigenciaConZonaHoraria() {
        ZoneId bogota = ZoneId.of("America/Bogota");
        // 24/11/2026 23:30 en Bogotá = 25/11/2026 04:30 UTC
        Clock reloj = Clock.fixed(Instant.parse("2026-11-25T04:30:00Z"), bogota);
        VigenciaUsuario vigencia = new VigenciaUsuario(reloj);

        assertEquals(LocalDate.of(2026, 11, 24), vigencia.hoy(), "Usa el día local, no el UTC");
        assertFalse(vigencia.estaVencida(LocalDate.of(2026, 11, 24)), "Vigente hasta el final del día");
        assertTrue(vigencia.estaVencida(LocalDate.of(2026, 11, 23)));
        assertTrue(vigencia.estaVencida((LocalDate) null), "Sin fecha se trata como vencida");

        assertEquals(LocalDate.of(2027, 11, 24), vigencia.fechaRenovacion(LocalDate.of(2020, 1, 1)),
                "Vencida: un año desde hoy");
        assertEquals(LocalDate.of(2027, 12, 31), vigencia.fechaRenovacion(LocalDate.of(2026, 12, 31)),
                "Aún vigente: un año desde su vencimiento");
    }

    // ------------------------------------------------------------------ 2.1

    @Test
    void permisosPorRolConProteccionDelRolPropioYDelAdministrador() {
        assertThrows(IllegalStateException.class,
                () -> gestionPermisosService.actualizarPermisos(ROL_ADMIN, List.of(4), ROL_CAJERO),
                "Nadie modifica el rol Administrador");
        assertThrows(IllegalStateException.class,
                () -> gestionPermisosService.actualizarPermisos(ROL_CAJERO, List.of(), ROL_CAJERO),
                "Nadie modifica su propio rol");
        assertThrows(IllegalArgumentException.class,
                () -> gestionPermisosService.actualizarPermisos(ROL_CAJERO, List.of(999), ROL_ADMIN));

        long antes = permisosRolesRepository.findByRolesIdRoles(ROL_CAJERO).size();
        GestionPermisosService.ResultadoCambio cambio =
                gestionPermisosService.actualizarPermisos(ROL_CAJERO, List.of(5, 6, 7, 11, 12, 14, 9), ROL_ADMIN);
        assertEquals(List.of("Gestionar mesas"), cambio.asignados());
        assertTrue(cambio.revocados().isEmpty());
        assertEquals(antes + 1, permisosRolesRepository.findByRolesIdRoles(ROL_CAJERO).size());

        cambio = gestionPermisosService.actualizarPermisos(ROL_CAJERO, List.of(5, 7, 11, 12, 14), ROL_ADMIN);
        assertEquals(2, cambio.revocados().size());
        assertTrue(cambio.revocados().containsAll(List.of("Gestionar caja", "Gestionar mesas")));
    }
}
