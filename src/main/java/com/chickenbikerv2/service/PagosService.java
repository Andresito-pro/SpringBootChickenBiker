package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.*;
import com.chickenbikerv2.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Cobro de pedidos (equivalente a PagoServlet). En una sola transacción:
 *  - exige caja abierta y bloquea la caja 1 y el recibo (evita cobros dobles);
 *  - el recibo pasa de "Generado" a "Pagado" con el método de pago real;
 *  - el pedido pasa a "Entregado";
 *  - la mesa queda disponible si ya no tiene otros pedidos pendientes;
 *  - se registra el ingreso en movimientos_caja del turno abierto.
 */
@Service
@RequiredArgsConstructor
public class PagosService {

    private final CabecerasRecibosRepository cabecerasRecibosRepository;
    private final PedidosVentasRepository pedidosVentasRepository;
    private final MesasRepository mesasRepository;
    private final MetodosPagoRepository metodosPagoRepository;
    private final MovimientosCajaRepository movimientosCajaRepository;
    private final AperturasCierresCajaService aperturasCierresCajaService;

    @Transactional(readOnly = true)
    public List<CabecerasRecibos> listarRecibosPendientes() {
        return cabecerasRecibosRepository
                .findByEstadoReciboIdEstadoReciboOrderByFechaEmisionDesc(PedidosService.ESTADO_RECIBO_GENERADO);
    }

    /** Métodos de pago activos, sin el placeholder "Pendiente de pago". */
    @Transactional(readOnly = true)
    public List<MetodosPago> listarMetodosPagoDisponibles() {
        return metodosPagoRepository.findAll().stream()
                .filter(this::esMetodoSeleccionable)
                .toList();
    }

    @Transactional
    public CabecerasRecibos cobrarRecibo(int idCabeceraRecibo, int idMetodoPago, int idUsuario) {
        AperturasCierresCaja cajaAbierta = aperturasCierresCajaService.bloquearYConsultarCajaAbierta()
                .orElseThrow(() -> new IllegalStateException("No hay una caja abierta para registrar el cobro."));

        MetodosPago metodo = metodosPagoRepository.findById(idMetodoPago)
                .filter(this::esMetodoSeleccionable)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un método de pago válido."));

        CabecerasRecibos recibo = cabecerasRecibosRepository.bloquearPorId(idCabeceraRecibo)
                .orElseThrow(() -> new IllegalArgumentException("El recibo seleccionado no existe."));

        if (recibo.getEstadoReciboIdEstadoRecibo() != PedidosService.ESTADO_RECIBO_GENERADO) {
            throw new IllegalStateException("El recibo #" + recibo.getNumeroRecibo() + " ya fue cobrado o anulado.");
        }

        recibo.setMetodoPagoIdMetodoPago(metodo.getIdMetodoPago());
        recibo.setEstadoReciboIdEstadoRecibo(PedidosService.ESTADO_RECIBO_PAGADO);
        recibo = cabecerasRecibosRepository.save(recibo);

        PedidosVentas pedido = pedidosVentasRepository.findById(recibo.getPedidoVentaIdPedidoVenta())
                .orElseThrow(() -> new IllegalStateException("No se encontró el pedido asociado al recibo."));
        pedido.setEstadoPedidoIdEstado(PedidosService.ESTADO_PEDIDO_ENTREGADO);
        pedidosVentasRepository.save(pedido);

        // Liberar la mesa (los pedidos para llevar no tienen mesa)
        Integer idMesa = pedido.getMesaIdMesa();
        if (idMesa != null && idMesa > 0) {
            boolean otrosPendientes = pedidosVentasRepository.existsByMesaIdMesaAndEstadoPedidoIdEstadoAndIdPedidoVentaNot(
                    idMesa, PedidosService.ESTADO_PEDIDO_PENDIENTE, pedido.getIdPedidoVenta());
            if (!otrosPendientes) {
                mesasRepository.bloquearPorId(idMesa).ifPresent(mesa -> {
                    if (mesa.getEstadoMesaIdEstadoMesa() == PedidosService.ESTADO_MESA_OCUPADA) {
                        mesa.setEstadoMesaIdEstadoMesa(PedidosService.ESTADO_MESA_DISPONIBLE);
                        mesasRepository.save(mesa);
                    }
                });
            }
        }

        MovimientosCaja movimiento = new MovimientosCaja();
        movimiento.setValorMovimientoCaja(recibo.getTotalGeneral().floatValue());
        movimiento.setDescripcionMovientoCaja(recortar(
                "Cobro " + recibo.getNumeroRecibo() + " pedido #" + pedido.getIdPedidoVenta()));
        movimiento.setFechaMovimientoCaja(LocalDate.now());
        movimiento.setTipoMovimientoIdTipoMovimiento(PedidosService.TIPO_MOVIMIENTO_INGRESO_ID);
        movimiento.setUsuarioIdUsuario(idUsuario);
        movimiento.setAperturaCierreCajaIdAperturaCierreCaja(cajaAbierta.getIdAperturaCierreCaja());
        movimientosCajaRepository.save(movimiento);

        return recibo;
    }

    private boolean esMetodoSeleccionable(MetodosPago m) {
        return m.getIdMetodoPago() != PedidosService.METODO_PAGO_PENDIENTE_ID
                && m.getActivoMetodo() != null && m.getActivoMetodo() == 1;
    }

    private static String recortar(String texto) {
        int max = AperturasCierresCajaService.MAX_DESCRIPCION_MOVIMIENTO;
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
