package com.chickenbikerv2;

import com.chickenbikerv2.entity.*;
import com.chickenbikerv2.repository.*;
import com.chickenbikerv2.service.AperturasCierresCajaService;
import com.chickenbikerv2.service.PagosService;
import com.chickenbikerv2.service.PedidosService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglas de negocio obligatorias: caja única (n.º 1), turno diario único, pedidos
 * solo con caja abierta, mesa ocupada hasta el cobro, pedidos para llevar sin
 * mesa y recibos internos con trazabilidad.
 */
@SpringBootTest
@Sql(scripts = "/datos-prueba.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FlujoNegocioTests {

    private static final int ADMIN = 1;
    private static final int CAJERO = 2;

    @Autowired private AperturasCierresCajaService cajaService;
    @Autowired private PedidosService pedidosService;
    @Autowired private PagosService pagosService;
    @Autowired private MesasRepository mesasRepository;
    @Autowired private CabecerasRecibosRepository cabecerasRecibosRepository;
    @Autowired private CuerposRecibosRepository cuerposRecibosRepository;
    @Autowired private DetallesPedidosVentaRepository detallesRepository;
    @Autowired private MovimientosCajaRepository movimientosRepository;
    @Autowired private PedidosVentasRepository pedidosRepository;

    @Test
    void noSePuedeRegistrarPedidoSinCajaAbierta() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                pedidosService.registrarPedido(PedidosService.TIPO_PARA_LLEVAR, null, List.of(1), List.of(1), ADMIN));
        assertTrue(e.getMessage().contains("caja abierta"));
        assertEquals(0, pedidosRepository.count());
    }

    @Test
    void laCajaEsUnicaYSeAbreUnaSolaVezPorDia() {
        AperturasCierresCaja turno = cajaService.abrirCaja(new BigDecimal("100000"), ADMIN);
        assertEquals(AperturasCierresCajaService.CAJA_OPERATIVA_ID, turno.getCajaIdCaja());
        assertEquals(AperturasCierresCajaService.ESTADO_ABIERTA, turno.getEstadoCierreCaja());

        // No se puede abrir otra mientras está abierta
        assertThrows(IllegalStateException.class, () -> cajaService.abrirCaja(BigDecimal.TEN, ADMIN));

        // Tras cerrarla tampoco se puede abrir otro turno el mismo día
        cajaService.cerrarCaja(new BigDecimal("100000"), ADMIN);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> cajaService.abrirCaja(BigDecimal.TEN, ADMIN));
        assertTrue(e.getMessage().contains("único turno diario"));
    }

    @Test
    void flujoCompletoPedidoEnMesaCobroYCierre() {
        AperturasCierresCaja turno = cajaService.abrirCaja(new BigDecimal("50000"), CAJERO);

        // 2 pollos (12.000) + 1 gaseosa (3.000) = 27.000; el producto repetido se consolida
        PedidosVentas pedido = pedidosService.registrarPedido(
                PedidosService.TIPO_MESA, 1, List.of(1, 2, 1), List.of(1, 1, 1), CAJERO);

        assertEquals(0, new BigDecimal("27000").compareTo(pedido.getTotalPedido()));
        assertEquals(1, pedido.getMesaIdMesa());
        assertEquals(PedidosService.ESTADO_PEDIDO_PENDIENTE, pedido.getEstadoPedidoIdEstado());
        assertEquals(PedidosService.ESTADO_MESA_OCUPADA, mesa(1).getEstadoMesaIdEstadoMesa());
        assertEquals(2, detallesRepository.findByPedidoVentaIdPedidoVenta(pedido.getIdPedidoVenta()).size());

        CabecerasRecibos recibo = cabecerasRecibosRepository.findByPedidoVentaIdPedidoVenta(pedido.getIdPedidoVenta())
                .orElseThrow();
        assertEquals("REC-" + pedido.getIdPedidoVenta(), recibo.getNumeroRecibo());
        assertEquals(PedidosService.ESTADO_RECIBO_GENERADO, recibo.getEstadoReciboIdEstadoRecibo());
        assertEquals(PedidosService.METODO_PAGO_PENDIENTE_ID, recibo.getMetodoPagoIdMetodoPago());
        assertEquals(turno.getIdAperturaCierreCaja(), recibo.getAperturaCierreCajaIdAperturaCierreCaja());
        assertEquals(2, cuerposRecibosRepository.findByCabeceraReciboIdCabeceraRecibo(recibo.getIdCabeceraRecibo()).size());

        // La mesa ocupada no admite otro pedido
        assertThrows(IllegalStateException.class, () -> pedidosService.registrarPedido(
                PedidosService.TIPO_MESA, 1, List.of(2), List.of(1), CAJERO));

        // No se puede cerrar la caja con pedidos pendientes de cobro
        IllegalStateException pendiente = assertThrows(IllegalStateException.class,
                () -> cajaService.cerrarCaja(new BigDecimal("77000"), CAJERO));
        assertEquals(AperturasCierresCajaService.ERROR_RECIBOS_PENDIENTES, pendiente.getMessage());

        // El placeholder "Pendiente de pago" no es un método válido
        assertThrows(IllegalArgumentException.class, () -> pagosService.cobrarRecibo(
                recibo.getIdCabeceraRecibo(), PedidosService.METODO_PAGO_PENDIENTE_ID, CAJERO));
        assertTrue(pagosService.listarMetodosPagoDisponibles().stream()
                .noneMatch(m -> m.getIdMetodoPago() == PedidosService.METODO_PAGO_PENDIENTE_ID));

        // Cobro
        assertEquals(1, pagosService.listarRecibosPendientes().size());
        pagosService.cobrarRecibo(recibo.getIdCabeceraRecibo(), 1, CAJERO);

        CabecerasRecibos pagado = cabecerasRecibosRepository.findById(recibo.getIdCabeceraRecibo()).orElseThrow();
        assertEquals(PedidosService.ESTADO_RECIBO_PAGADO, pagado.getEstadoReciboIdEstadoRecibo());
        assertEquals(1, pagado.getMetodoPagoIdMetodoPago());
        assertEquals(PedidosService.ESTADO_PEDIDO_ENTREGADO,
                pedidosRepository.findById(pedido.getIdPedidoVenta()).orElseThrow().getEstadoPedidoIdEstado());
        assertEquals(PedidosService.ESTADO_MESA_DISPONIBLE, mesa(1).getEstadoMesaIdEstadoMesa());
        assertTrue(pagosService.listarRecibosPendientes().isEmpty());

        List<MovimientosCaja> movimientos = movimientosRepository
                .findByAperturaCierreCajaIdAperturaCierreCaja(turno.getIdAperturaCierreCaja());
        assertEquals(1, movimientos.size());
        assertEquals(27000f, movimientos.get(0).getValorMovimientoCaja());

        // Un recibo no se cobra dos veces
        assertThrows(IllegalStateException.class,
                () -> pagosService.cobrarRecibo(recibo.getIdCabeceraRecibo(), 1, CAJERO));

        // Egreso manual y cierre: sistema = 50.000 + 27.000 - 2.000 = 75.000
        cajaService.registrarMovimientoManual(new BigDecimal("2000"), 2, "Compra de bolsas", CAJERO);
        AperturasCierresCaja cerrada = cajaService.cerrarCaja(new BigDecimal("74500"), ADMIN);
        assertEquals(AperturasCierresCajaService.ESTADO_CERRADA, cerrada.getEstadoCierreCaja());
        assertEquals(0, new BigDecimal("75000").compareTo(cerrada.getMontoFinalSistema()));
        assertEquals(0, new BigDecimal("-500").compareTo(cerrada.getDiferencia()));
        assertEquals(ADMIN, cerrada.getUsuarioIdUsuario1());
        assertFalse(cajaService.hayCajaAbierta());
    }

    @Test
    void pedidoParaLlevarNoRequiereNiModificaMesas() {
        cajaService.abrirCaja(BigDecimal.ZERO, CAJERO);

        // Aunque llegue un mesaId, un pedido para llevar lo ignora
        PedidosVentas pedido = pedidosService.registrarPedido(
                PedidosService.TIPO_PARA_LLEVAR, 1, List.of(2), List.of(2), CAJERO);

        assertNull(pedido.getMesaIdMesa());
        assertEquals(PedidosService.TIPO_PARA_LLEVAR, pedido.getTipoPedido());
        assertEquals(PedidosService.ESTADO_MESA_DISPONIBLE, mesa(1).getEstadoMesaIdEstadoMesa());

        CabecerasRecibos recibo = cabecerasRecibosRepository.findByPedidoVentaIdPedidoVenta(pedido.getIdPedidoVenta())
                .orElseThrow();
        pagosService.cobrarRecibo(recibo.getIdCabeceraRecibo(), 2, CAJERO);
        assertEquals(PedidosService.ESTADO_MESA_DISPONIBLE, mesa(1).getEstadoMesaIdEstadoMesa());
    }

    @Test
    void validacionesDelPedido() {
        cajaService.abrirCaja(BigDecimal.ZERO, CAJERO);

        // Pedido en mesa sin mesa
        assertThrows(IllegalArgumentException.class, () -> pedidosService.registrarPedido(
                PedidosService.TIPO_MESA, null, List.of(1), List.of(1), CAJERO));
        // Mesa reservada
        assertThrows(IllegalStateException.class, () -> pedidosService.registrarPedido(
                PedidosService.TIPO_MESA, 3, List.of(1), List.of(1), CAJERO));
        // Producto inactivo
        assertThrows(IllegalStateException.class, () -> pedidosService.registrarPedido(
                PedidosService.TIPO_PARA_LLEVAR, null, List.of(3), List.of(1), CAJERO));
        // Sin productos
        assertThrows(IllegalArgumentException.class, () -> pedidosService.registrarPedido(
                PedidosService.TIPO_PARA_LLEVAR, null, List.of(), List.of(), CAJERO));

        // Ningún intento fallido dejó datos a medias ni mesas ocupadas
        assertEquals(0, pedidosRepository.count());
        assertEquals(PedidosService.ESTADO_MESA_DISPONIBLE, mesa(1).getEstadoMesaIdEstadoMesa());
    }

    @Test
    void movimientosManualesRequierenCajaAbiertaYDatosValidos() {
        assertThrows(IllegalStateException.class,
                () -> cajaService.registrarMovimientoManual(BigDecimal.TEN, 1, "Sin caja", ADMIN));

        cajaService.abrirCaja(BigDecimal.ZERO, ADMIN);
        assertThrows(IllegalArgumentException.class,
                () -> cajaService.registrarMovimientoManual(BigDecimal.ZERO, 1, "Cero", ADMIN));
        assertThrows(IllegalArgumentException.class,
                () -> cajaService.registrarMovimientoManual(BigDecimal.TEN, 3, "Tipo no permitido", ADMIN));
        assertThrows(IllegalArgumentException.class,
                () -> cajaService.registrarMovimientoManual(BigDecimal.TEN, 1, "x".repeat(46), ADMIN));

        cajaService.registrarMovimientoManual(new BigDecimal("1500.50"), 1, "Base adicional", ADMIN);
        assertEquals(1, movimientosRepository.count());
    }

    private Mesas mesa(int id) {
        return mesasRepository.findById(id).orElseThrow();
    }
}
