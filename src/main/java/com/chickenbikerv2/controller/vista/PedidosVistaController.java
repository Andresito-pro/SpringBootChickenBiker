package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.PedidosVentas;
import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.security.PermisosSistema;
import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.AnulacionPedidosService;
import com.chickenbikerv2.service.CatalogoEstadosService;
import com.chickenbikerv2.service.PagosService;
import com.chickenbikerv2.service.PedidosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pedidos. La visibilidad se decide en el servidor con el usuario de la sesión
 * y el permiso "Ver todos los pedidos": un mesero solo lista, consulta y
 * (si tiene permiso) anula los pedidos que él registró, aunque manipule la URL.
 */
@Controller
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidosVistaController {

    private final PedidosService pedidosService;
    private final PagosService pagosService;
    private final AnulacionPedidosService anulacionPedidosService;
    private final CatalogoEstadosService catalogoEstadosService;

    @GetMapping
    public String listarPedidos(HttpSession session, Authentication authentication, Model model) {
        Usuarios usuario = SesionUsuario.obtener(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        boolean verTodos = PermisosSistema.tiene(authentication, PermisosSistema.VER_TODOS_LOS_PEDIDOS);
        List<PedidosVentas> pedidos = pedidosService.listarPedidos(usuario.getIdUsuario(), verTodos);

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("recibosPorPedido", pedidosService.obtenerMapaRecibosPorPedido(pedidos));
        model.addAttribute("numerosMesa", pedidosService.obtenerMapaNumerosMesa());
        model.addAttribute("metodosPago", pagosService.listarMetodosPagoDisponibles());
        model.addAttribute("idEstadoAnulado", catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir());
        model.addAttribute("verTodosLosPedidos", verTodos);
        model.addAttribute("puedeAnular", PermisosSistema.tiene(authentication, PermisosSistema.ANULAR_PEDIDOS));
        model.addAttribute("puedeAnularPagados",
                PermisosSistema.tiene(authentication, PermisosSistema.ANULAR_PEDIDOS_PAGADOS));
        model.addAttribute("menuActivo", "pedidos");
        return "pedidos/pedidos";
    }

    @GetMapping({"/tomar", "/nuevo"})
    public String mostrarFormularioTomarPedido(
            @RequestParam(value = "tipoPedido", required = false) String tipoPedido,
            Model model) {

        boolean cajaAbierta = pedidosService.hayCajaAbierta();
        model.addAttribute("cajaAbierta", cajaAbierta);
        if (!cajaAbierta) {
            model.addAttribute("avisoCaja", "La caja se encuentra cerrada.");
        }

        model.addAttribute("mesas", pedidosService.listarMesas());
        model.addAttribute("productos", pedidosService.listarProductosActivos());
        model.addAttribute("TIPO_MESA", PedidosService.TIPO_MESA);
        model.addAttribute("TIPO_PARA_LLEVAR", PedidosService.TIPO_PARA_LLEVAR);
        model.addAttribute("menuActivo", "tomarPedido");

        if (!model.containsAttribute("tipoPedidoSeleccionado")) {
            model.addAttribute("tipoPedidoSeleccionado",
                    tipoPedido != null ? tipoPedido : PedidosService.TIPO_MESA);
        }

        return "pedidos/tomarPedido";
    }

    @PostMapping("/guardar")
    public String guardarPedido(@RequestParam(value = "tipoPedido", defaultValue = "Mesa") String tipoPedido,
                                @RequestParam(value = "mesaId", required = false) Integer mesaId,
                                HttpServletRequest request,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Usuarios usuario = SesionUsuario.obtener(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        try {
            List<Integer> productosIds = extraerEnteros(request, "productoId", "productosId", "idProducto");
            List<Integer> cantidades = extraerEnteros(request, "cantidad", "cantidades");

            PedidosVentas pedido = pedidosService.registrarPedido(
                    tipoPedido,
                    mesaId,
                    productosIds,
                    cantidades,
                    usuario.getIdUsuario()
            );

            String detalle = pedido.getMesaIdMesa() == null
                    ? " (para llevar). Queda pendiente de cobro."
                    : ". La mesa quedó ocupada hasta el cobro.";
            redirectAttributes.addFlashAttribute("mensaje",
                    "Pedido #" + pedido.getIdPedidoVenta() + " registrado correctamente" + detalle);
            return "redirect:/pedidos";

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("tipoPedidoSeleccionado", tipoPedido);
            return "redirect:/pedidos/tomar";
        }
    }

    @GetMapping("/detalle/{idPedido}")
    public String verDetallePedido(@PathVariable("idPedido") int idPedido,
                                   HttpSession session,
                                   Authentication authentication,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        Usuarios usuario = SesionUsuario.obtener(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        try {
            Map<String, Object> detalle = pedidosService.obtenerDetallePedido(idPedido, usuario.getIdUsuario(),
                    PermisosSistema.tiene(authentication, PermisosSistema.VER_TODOS_LOS_PEDIDOS));
            model.addAllAttributes(detalle);
            model.addAttribute("menuActivo", "pedidos");
            return "pedidos/verDetallePedido";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/pedidos";
        }
    }

    /** Anulación de un pedido PENDIENTE (permiso "Anular pedidos", ver SecurityConfig). */
    @PostMapping("/anular/{idPedido}")
    public String anularPedido(@PathVariable("idPedido") int idPedido,
                               HttpSession session,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        Usuarios usuario = SesionUsuario.obtener(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        try {
            anulacionPedidosService.anularPedidoPendiente(idPedido, usuario.getIdUsuario(),
                    PermisosSistema.tiene(authentication, PermisosSistema.VER_TODOS_LOS_PEDIDOS));
            redirectAttributes.addFlashAttribute("mensaje",
                    "Pedido #" + idPedido + " anulado. Su mesa (si tenía) quedó disponible y no se contabiliza como venta.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos";
    }

    /** Procedimiento diferenciado para pedidos YA PAGADOS (permiso "Anular pedidos pagados"). */
    @PostMapping("/anular-pagado/{idPedido}")
    public String anularPedidoPagado(@PathVariable("idPedido") int idPedido,
                                     @RequestParam(value = "confirmaProcedimiento", defaultValue = "false")
                                     boolean confirmaProcedimiento,
                                     HttpSession session,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        Usuarios usuario = SesionUsuario.obtener(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        try {
            anulacionPedidosService.anularPedidoPagado(idPedido, confirmaProcedimiento,
                    usuario.getIdUsuario(),
                    PermisosSistema.tiene(authentication, PermisosSistema.VER_TODOS_LOS_PEDIDOS));
            redirectAttributes.addFlashAttribute("mensaje",
                    "Pedido pagado #" + idPedido + " anulado. No se hizo ninguna reversión automática: si se "
                            + "devolvió dinero al cliente, regístralo como egreso en Movimientos de caja.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos";
    }

    private List<Integer> extraerEnteros(HttpServletRequest request, String... nombresParametro) {
        for (String nombre : nombresParametro) {
            String[] valores = request.getParameterValues(nombre);
            if (valores != null && valores.length > 0) {
                List<Integer> lista = new ArrayList<>(valores.length);
                for (String val : valores) {
                    lista.add(Integer.parseInt(val.trim()));
                }
                return lista;
            }
        }
        return List.of();
    }
}
