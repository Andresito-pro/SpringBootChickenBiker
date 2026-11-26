package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.CabecerasRecibos;
import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.security.PermisosSistema;
import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.CabecerasRecibosService;
import com.chickenbikerv2.service.PedidosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consulta de recibos internos (control y trazabilidad). No son facturas
 * electrónicas DIAN: no se genera CUFE, XML ni se envía nada a terceros.
 *
 * Los recibos exponen datos de los pedidos, así que siguen la misma regla de
 * visibilidad: sin "Ver todos los pedidos" solo se ven los recibos de los
 * pedidos que registró el propio usuario.
 */
@Controller
@RequestMapping("/recibos")
public class RecibosVistaController {

    private final CabecerasRecibosService cabecerasRecibosService;
    private final PedidosService pedidosService;

    public RecibosVistaController(CabecerasRecibosService cabecerasRecibosService, PedidosService pedidosService) {
        this.cabecerasRecibosService = cabecerasRecibosService;
        this.pedidosService = pedidosService;
    }

    @GetMapping
    public String listar(HttpSession session, Authentication authentication, Model model) {
        Usuarios usuario = SesionUsuario.obtener(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        List<CabecerasRecibos> recibos = cabecerasRecibosService.listarRecientes();
        if (!PermisosSistema.tiene(authentication, PermisosSistema.VER_TODOS_LOS_PEDIDOS)) {
            Set<Integer> propios = pedidosService.listarPedidos(usuario.getIdUsuario(), false).stream()
                    .map(p -> p.getIdPedidoVenta()).collect(Collectors.toSet());
            recibos = recibos.stream().filter(r -> propios.contains(r.getPedidoVentaIdPedidoVenta())).toList();
        }
        model.addAttribute("recibos", recibos);
        model.addAttribute("mapaMetodos", cabecerasRecibosService.mapaMetodosPago());
        model.addAttribute("mapaEstados", cabecerasRecibosService.mapaEstadosRecibo());
        model.addAttribute("pedidosAnulados", pedidosService.idsPedidosAnulados());
        model.addAttribute("menuActivo", "recibos");
        return "recibos/listarRecibos";
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable("id") int id, HttpSession session, Authentication authentication,
                      Model model, RedirectAttributes redirectAttributes) {
        Usuarios usuario = SesionUsuario.obtener(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        boolean verTodos = PermisosSistema.tiene(authentication, PermisosSistema.VER_TODOS_LOS_PEDIDOS);
        boolean visible = cabecerasRecibosService.buscarPorId(id)
                .map(r -> pedidosService.buscarPedidoVisible(r.getPedidoVentaIdPedidoVenta(),
                        usuario.getIdUsuario(), verTodos).isPresent())
                .orElse(false);
        if (!visible) {
            redirectAttributes.addFlashAttribute("error", "El recibo #" + id + " no existe.");
            return "redirect:/recibos";
        }
        Map<String, Object> detalle = cabecerasRecibosService.obtenerDetalleRecibo(id);
        model.addAllAttributes(detalle);
        Object recibo = detalle.get("recibo");
        model.addAttribute("pedidoAnulado", recibo instanceof CabecerasRecibos r
                && pedidosService.idsPedidosAnulados().contains(r.getPedidoVentaIdPedidoVenta()));
        model.addAttribute("menuActivo", "recibos");
        return "recibos/verRecibo";
    }
}
