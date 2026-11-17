package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.PedidosVentas;
import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.PagosService;
import com.chickenbikerv2.service.PedidosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidosVistaController {

    private final PedidosService pedidosService;
    private final PagosService pagosService;

    @GetMapping
    public String listarPedidos(Model model) {
        model.addAttribute("pedidos", pedidosService.listarPedidos());
        model.addAttribute("recibosPorPedido", pedidosService.obtenerMapaRecibosPorPedido());
        model.addAttribute("numerosMesa", pedidosService.obtenerMapaNumerosMesa());
        model.addAttribute("metodosPago", pagosService.listarMetodosPagoDisponibles());
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
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> detalle = pedidosService.obtenerDetallePedido(idPedido);
            model.addAllAttributes(detalle);
            model.addAttribute("menuActivo", "pedidos");
            return "pedidos/verDetallePedido";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/pedidos";
        }
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
