package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.*;
import com.chickenbikerv2.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagosService {

    private final CabecerasRecibosRepository cabecerasRecibosRepository;
    private final PedidosVentasRepository pedidosVentasRepository;
    private final MesasRepository mesasRepository;
    private final MetodosPagoRepository metodosPagoRepository;
    private final MovimientosCajaRepository movimientosCajaRepository;
    private final PedidosService pedidosService;

    @Transactional(readOnly = true)
    public List<CabecerasRecibos> listarRecibosPendientes() {
        return cabecerasRecibosRepository
                .findByMetodoPagoIdMetodoPagoOrderByFechaEmisionDesc(PedidosService.METODO_PAGO_PENDIENTE_ID);
    }

    @Transactional(readOnly = true)
    public List<MetodosPago> listarMetodosPagoDisponibles() {
        return metodosPagoRepository.findAll().stream()
                .filter(m -> m.getIdMetodoPago() != PedidosService.METODO_PAGO_PENDIENTE_ID)
                .collect(Collectors.toList());
    }

    @Transactional
    public CabecerasRecibos cobrarRecibo(int idCabeceraRecibo, int idMetodoPago, int idUsuario) {
        AperturasCierresCaja cajaAbierta = pedidosService.obtenerCajaAbierta()
                .orElseThrow(() -> new IllegalStateException("No hay una caja abierta para registrar el cobro."));

        if (idMetodoPago == PedidosService.METODO_PAGO_PENDIENTE_ID || !metodosPagoRepository.existsById(idMetodoPago)) {
            throw new IllegalArgumentException("Selecciona un método de pago válido.");
        }

        CabecerasRecibos recibo = cabecerasRecibosRepository.findById(idCabeceraRecibo)
                .orElseThrow(() -> new IllegalArgumentException("El recibo seleccionado no existe."));

        if (recibo.getMetodoPagoIdMetodoPago() != PedidosService.METODO_PAGO_PENDIENTE_ID) {
            throw new IllegalStateException("El recibo #" + recibo.getNumeroRecibo() + " ya fue cobrado.");
        }

        recibo.setMetodoPagoIdMetodoPago(idMetodoPago);
        recibo = cabecerasRecibosRepository.save(recibo);

        PedidosVentas pedido = pedidosVentasRepository.findById(recibo.getPedidoVentaIdPedidoVenta())
                .orElseThrow(() -> new IllegalStateException("No se encontró el pedido asociado al recibo."));

        pedido.setEstadoPedidoIdEstado(PedidosService.ESTADO_PEDIDO_ENTREGADO);
        pedidosVentasRepository.save(pedido);

        if (pedido.getMesaIdMesa() != null && pedido.getMesaIdMesa() > 0) {
            mesasRepository.findById(pedido.getMesaIdMesa()).ifPresent(mesa -> {
                mesa.setEstadoMesaIdEstadoMesa(PedidosService.ESTADO_MESA_DISPONIBLE);
                mesasRepository.save(mesa);
            });
        }

        MovimientosCaja movimiento = new MovimientosCaja();
        movimiento.setValorMovimientoCaja(recibo.getTotalGeneral().floatValue());
        movimiento.setDescripcionMovientoCaja(
                "Cobro recibo " + recibo.getNumeroRecibo() + " (Pedido #" + pedido.getIdPedidoVenta() + ")"
        );
        movimiento.setFechaMovimientoCaja(LocalDate.now());
        movimiento.setTipoMovimientoIdTipoMovimiento(PedidosService.TIPO_MOVIMIENTO_INGRESO_ID);
        movimiento.setUsuarioIdUsuario(idUsuario);
        movimiento.setAperturaCierreCajaIdAperturaCierreCaja(cajaAbierta.getIdAperturaCierreCaja());
        movimientosCajaRepository.save(movimiento);

        return recibo;
    }
}