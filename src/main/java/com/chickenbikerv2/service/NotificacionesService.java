package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.Mesas;
import com.chickenbikerv2.entity.PedidosVentas;
import com.chickenbikerv2.repository.MesasRepository;
import com.chickenbikerv2.repository.PedidosVentasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sistema ÚNICO de notificaciones (campana del topbar).
 *
 * La base de datos no tiene una tabla de notificaciones, así que se DERIVAN de
 * la tabla existente pedidos_ventas: cada pedido registrado hoy por OTRO usuario
 * (y que no esté anulado) es una notificación de "Nuevo pedido". El acceso a la
 * campana lo controla el permiso "Recibir notificaciones" (SecurityConfig): por
 * defecto Administrador y Cajero; el Mesero no lo tiene.
 *
 * El estado leída/no leída se guarda en la sesión HTTP del usuario
 * ({@link EstadoLectura}), porque no hay dónde persistirlo sin crear tablas:
 * al iniciar una nueva sesión, los pedidos del día vuelven a aparecer como no leídos.
 * El id de cada notificación es el id del pedido; el destinatario es siempre el
 * usuario de la sesión (nunca llega del cliente).
 */
@Service
public class NotificacionesService {

    public static final int MAX_RECIENTES = 15;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    /** fecha ya formateada (dd/MM/yyyy hh:mm a) para mostrarla tal cual en la campana. */
    public record NotificacionVista(int id, String titulo, String mensaje, String fecha,
                                    boolean leida, Integer idPedido) {
    }

    public record Resumen(long noLeidas, List<NotificacionVista> recientes) {
    }

    /** Estado de lectura de un usuario, guardado en su sesión HTTP. */
    public static final class EstadoLectura implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** Todos los pedidos con id menor o igual a este se consideran leídos. */
        private int leidasHasta;
        /** Pedidos marcados como leídos uno a uno (posteriores a leidasHasta). */
        private final Set<Integer> leidas = new HashSet<>();

        boolean estaLeida(int idPedido) {
            return idPedido <= leidasHasta || leidas.contains(idPedido);
        }
    }

    private final PedidosVentasRepository pedidosVentasRepository;
    private final MesasRepository mesasRepository;
    private final CatalogoEstadosService catalogoEstadosService;

    public NotificacionesService(PedidosVentasRepository pedidosVentasRepository,
                                 MesasRepository mesasRepository,
                                 CatalogoEstadosService catalogoEstadosService) {
        this.pedidosVentasRepository = pedidosVentasRepository;
        this.mesasRepository = mesasRepository;
        this.catalogoEstadosService = catalogoEstadosService;
    }

    @Transactional(readOnly = true)
    public Resumen resumen(int idUsuario, EstadoLectura estado) {
        List<PedidosVentas> pedidos = pedidosNotificables(idUsuario);
        long noLeidas = pedidos.stream().filter(p -> !estado.estaLeida(p.getIdPedidoVenta())).count();

        List<PedidosVentas> recientes = pedidos.stream().limit(MAX_RECIENTES).toList();
        Set<Integer> idsMesas = recientes.stream().map(PedidosVentas::getMesaIdMesa)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, Integer> numeroMesa = mesasRepository.findAllById(idsMesas).stream()
                .collect(Collectors.toMap(Mesas::getIdMesa, Mesas::getNumeroMesa, (a, b) -> a));

        List<NotificacionVista> vistas = recientes.stream()
                .map(p -> vista(p, numeroMesa.get(p.getMesaIdMesa()), estado.estaLeida(p.getIdPedidoVenta())))
                .toList();
        return new Resumen(noLeidas, vistas);
    }

    /** Marca como leída la notificación de un pedido. false si ese pedido no es una notificación del usuario. */
    @Transactional(readOnly = true)
    public boolean marcarLeida(int idPedido, int idUsuario, EstadoLectura estado) {
        boolean visible = pedidosNotificables(idUsuario).stream().anyMatch(p -> p.getIdPedidoVenta() == idPedido);
        if (visible) {
            estado.leidas.add(idPedido);
        }
        return visible;
    }

    @Transactional(readOnly = true)
    public int marcarTodasLeidas(int idUsuario, EstadoLectura estado) {
        List<PedidosVentas> pedidos = pedidosNotificables(idUsuario);
        int marcadas = (int) pedidos.stream().filter(p -> !estado.estaLeida(p.getIdPedidoVenta())).count();
        pedidos.stream().mapToInt(PedidosVentas::getIdPedidoVenta).max()
                .ifPresent(max -> estado.leidasHasta = Math.max(estado.leidasHasta, max));
        estado.leidas.removeIf(id -> id <= estado.leidasHasta);
        return marcadas;
    }

    /** Pedidos de hoy registrados por otros usuarios y no anulados, del más reciente al más antiguo. */
    private List<PedidosVentas> pedidosNotificables(int idUsuario) {
        return pedidosVentasRepository.listarPedidosDeOtrosUsuariosDesde(
                LocalDate.now().atStartOfDay(), idUsuario, catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir());
    }

    private static NotificacionVista vista(PedidosVentas pedido, Integer numeroMesa, boolean leida) {
        String modalidad = numeroMesa != null ? "Mesa " + numeroMesa : "Para llevar";
        String hora = pedido.getFechaPedido() != null ? pedido.getFechaPedido().format(HORA) : "";
        String total = pedido.getTotalPedido() != null
                ? String.format(Locale.US, "%,.0f", pedido.getTotalPedido()).replace(',', '.') : "0";
        return new NotificacionVista(pedido.getIdPedidoVenta(),
                "Nuevo pedido #" + pedido.getIdPedidoVenta(),
                modalidad + " · " + hora + " · Total $ " + total,
                pedido.getFechaPedido() != null ? pedido.getFechaPedido().format(FECHA_HORA) : "",
                leida,
                pedido.getIdPedidoVenta());
    }
}
