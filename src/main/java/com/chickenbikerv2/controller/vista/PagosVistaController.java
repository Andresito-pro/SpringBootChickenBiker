package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.CabecerasRecibos;
import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.service.PagosService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagosVistaController {

    private final PagosService pagosService;

    @GetMapping
    public String listarPendientesCobro(Model model) {
        model.addAttribute("pendientes", pagosService.listarRecibosPendientes());
        model.addAttribute("metodosPago", pagosService.listarMetodosPagoDisponibles());
        return "pagos/cobrarPedido";
    }

    @PostMapping("/cobrar")
    public String cobrarPedido(@RequestParam("idCabeceraRecibo") int idCabeceraRecibo,
                               @RequestParam("idMetodoPago") int idMetodoPago,
                               @RequestParam(value = "origen", defaultValue = "pagos") String origen,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/login";
        }

        String rutaDestino = "pedidos".equalsIgnoreCase(origen) ? "redirect:/pedidos" : "redirect:/pagos";

        try {
            CabecerasRecibos recibo = pagosService.cobrarRecibo(
                    idCabeceraRecibo,
                    idMetodoPago,
                    usuario.getIdUsuario()
            );
            redirectAttributes.addFlashAttribute("mensaje",
                    "Cobro registrado con éxito para el recibo #" + recibo.getNumeroRecibo() + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return rutaDestino;
    }
}