package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.AperturasCierresCaja;
import com.chickenbikerv2.entity.CabecerasRecibos;
import com.chickenbikerv2.entity.PedidosVentas;
import com.chickenbikerv2.repository.CabecerasRecibosRepository;
import com.chickenbikerv2.repository.MesasRepository;
import com.chickenbikerv2.repository.PedidosVentasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Anulación de pedidos. La anulación consiste ÚNICAMENTE en asociar el pedido al
 * estado "Anulado" del catálogo estados_pedidos (pedidos_ventas.estado_pedido_id_estado).
 * No se elimina el pedido, su detalle, su recibo ni ningún movimiento de caja, y no
 * se registra información adicional de la anulación.
 *
 * El recibo conserva su estado real (Generado / Pagado): las consultas de ventas,
 * cobros pendientes y cierre de caja excluyen los pedidos en estado Anulado.
 *
 * Dos procedimientos:
 *  1. {@link #anularPedidoPendiente}: pedido aún no cobrado (permiso "Anular pedidos").
 *     La mesa queda disponible.
 *  2. {@link #anularPedidoPagado}: pedido ya cobrado (permiso "Anular pedidos pagados",
 *     solo Administrador). Solo durante el turno de caja abierto en que se cobró y con
 *     confirmación adicional. NO revierte dinero automáticamente: el cobro se conserva
 *     en movimientos_caja y una devolución se registra como egreso manual.
 *
 * Concurrencia: se bloquean, en el mismo orden que el cobro (caja 1 -> recibo -> pedido
 * -> mesa), así un cobro y una anulación simultáneos no se cruzan y el bloqueo del
 * pedido impide anularlo dos veces.
 */
@Service
public class AnulacionPedidosService {

    private final PedidosVentasRepository pedidosVentasRepository;
    private final CabecerasRecibosRepository cabecerasRecibosRepository;
    private final MesasRepository mesasRepository;
    private final AperturasCierresCajaService aperturasCierresCajaService;
    private final CatalogoEstadosService catalogoEstadosService;

    public AnulacionPedidosService(PedidosVentasRepository pedidosVentasRepository,
                                   CabecerasRecibosRepository cabecerasRecibosRepository,
                                   MesasRepository mesasRepository,
                                   AperturasCierresCajaService aperturasCierresCajaService,
                                   CatalogoEstadosService catalogoEstadosService) {
        this.pedidosVentasRepository = pedidosVentasRepository;
        this.cabecerasRecibosRepository = cabecerasRecibosRepository;
        this.mesasRepository = mesasRepository;
        this.aperturasCierresCajaService = aperturasCierresCajaService;
        this.catalogoEstadosService = catalogoEstadosService;
    }

    /**
     * @param idUsuario usuario autenticado (de la sesión del servidor)
     * @param verTodos  si el usuario tiene "Ver todos los pedidos"; si no, solo
     *                  puede anular pedidos que él mismo registró
     */
    @Transactional
    public PedidosVentas anularPedidoPendiente(int idPedido, int idUsuario, boolean verTodos) {
        int estadoAnulado = catalogoEstadosService.idEstadoPedidoAnulado();

        aperturasCierresCajaService.bloquearYConsultarCajaAbierta();
        Optional<CabecerasRecibos> recibo = bloquearRecibo(idPedido);
        PedidosVentas pedido = bloquearPedidoVisible(idPedido, idUsuario, verTodos);

        verificarNoAnulado(pedido, estadoAnulado);
        boolean pagado = pedido.getEstadoPedidoIdEstado() == PedidosService.ESTADO_PEDIDO_ENTREGADO
                || recibo.map(r -> r.getEstadoReciboIdEstadoRecibo() == PedidosService.ESTADO_RECIBO_PAGADO).orElse(false);
        if (pagado) {
            throw new IllegalStateException("El pedido #" + idPedido + " ya fue pagado y no puede anularse como "
                    + "un pedido pendiente. Debe usarse el procedimiento de anulación de pedidos pagados.");
        }
        if (pedido.getEstadoPedidoIdEstado() != PedidosService.ESTADO_PEDIDO_PENDIENTE
                && recibo.map(r -> r.getEstadoReciboIdEstadoRecibo() != PedidosService.ESTADO_RECIBO_GENERADO).orElse(true)) {
            throw new IllegalStateException("El pedido #" + idPedido + " no está pendiente y no se puede anular.");
        }

        return cambiarAAnulado(pedido, estadoAnulado);
    }

    /**
     * Procedimiento diferenciado para pedidos ya cobrados.
     *
     * @param confirmaProcedimiento el usuario declaró conocer que no hay reversión
     *                              automática y que una devolución se registra aparte
     */
    @Transactional
    public PedidosVentas anularPedidoPagado(int idPedido, boolean confirmaProcedimiento,
                                            int idUsuario, boolean verTodos) {
        if (!confirmaProcedimiento) {
            throw new IllegalArgumentException("Debes confirmar que conoces el procedimiento de anulación de pedidos pagados.");
        }
        int estadoAnulado = catalogoEstadosService.idEstadoPedidoAnulado();

        AperturasCierresCaja turnoAbierto = aperturasCierresCajaService.bloquearYConsultarCajaAbierta()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay una caja abierta. Un pedido pagado solo puede anularse durante el turno en que se cobró."));
        CabecerasRecibos recibo = bloquearRecibo(idPedido).orElse(null);
        PedidosVentas pedido = bloquearPedidoVisible(idPedido, idUsuario, verTodos);

        verificarNoAnulado(pedido, estadoAnulado);
        if (recibo == null || recibo.getEstadoReciboIdEstadoRecibo() != PedidosService.ESTADO_RECIBO_PAGADO) {
            throw new IllegalStateException("El pedido #" + idPedido + " no está pagado. Si está pendiente, "
                    + "anúlalo con la opción de anulación normal.");
        }
        if (recibo.getAperturaCierreCajaIdAperturaCierreCaja() != turnoAbierto.getIdAperturaCierreCaja()) {
            throw new IllegalStateException("El pedido #" + idPedido + " se cobró en un turno de caja ya cerrado. "
                    + "Los cierres anteriores no se modifican.");
        }

        return cambiarAAnulado(pedido, estadoAnulado);
    }

    /** Única escritura de la anulación: el estado del pedido (y liberar su mesa). */
    private PedidosVentas cambiarAAnulado(PedidosVentas pedido, int estadoAnulado) {
        pedido.setEstadoPedidoIdEstado(estadoAnulado);
        PedidosVentas guardado = pedidosVentasRepository.save(pedido);
        liberarMesa(guardado);
        return guardado;
    }

    private Optional<CabecerasRecibos> bloquearRecibo(int idPedido) {
        return cabecerasRecibosRepository.findByPedidoVentaIdPedidoVenta(idPedido)
                .flatMap(r -> cabecerasRecibosRepository.bloquearPorId(r.getIdCabeceraRecibo()));
    }

    private PedidosVentas bloquearPedidoVisible(int idPedido, int idUsuario, boolean verTodos) {
        PedidosVentas pedido = pedidosVentasRepository.bloquearPorId(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("El pedido #" + idPedido + " no existe."));
        if (!verTodos && pedido.getUsuarioIdUsuario() != idUsuario) {
            // Misma respuesta que si no existiera: no se revelan pedidos de otros usuarios.
            throw new IllegalArgumentException("El pedido #" + idPedido + " no existe.");
        }
        return pedido;
    }

    private static void verificarNoAnulado(PedidosVentas pedido, int estadoAnulado) {
        if (pedido.getEstadoPedidoIdEstado() == estadoAnulado) {
            throw new IllegalStateException("El pedido #" + pedido.getIdPedidoVenta() + " ya fue anulado.");
        }
    }

    /** Un pedido anulado no ocupa mesa: se libera si no hay otros pedidos pendientes en ella. */
    private void liberarMesa(PedidosVentas pedido) {
        Integer idMesa = pedido.getMesaIdMesa();
        if (idMesa == null || idMesa <= 0) {
            return;
        }
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
}
