package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.AperturasCierresCaja;
import com.chickenbikerv2.entity.TiposMovimientos;
import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.AperturasCierresCajaService;
import com.chickenbikerv2.service.MovimientosCajaService;
import com.chickenbikerv2.service.TiposMovimientosService;
import com.chickenbikerv2.service.UsuariosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Movimientos de caja del turno abierto (equivalente a MovimientoServlet). */
@Controller
@RequestMapping("/movimientos")
public class MovimientosVistaController {

    private final AperturasCierresCajaService aperturasCierresCajaService;
    private final MovimientosCajaService movimientosCajaService;
    private final TiposMovimientosService tiposMovimientosService;
    private final UsuariosService usuariosService;

    public MovimientosVistaController(AperturasCierresCajaService aperturasCierresCajaService,
                                      MovimientosCajaService movimientosCajaService,
                                      TiposMovimientosService tiposMovimientosService,
                                      UsuariosService usuariosService) {
        this.aperturasCierresCajaService = aperturasCierresCajaService;
        this.movimientosCajaService = movimientosCajaService;
        this.tiposMovimientosService = tiposMovimientosService;
        this.usuariosService = usuariosService;
    }

    @GetMapping
    public String listar(Model model) {
        Optional<AperturasCierresCaja> abierta = aperturasCierresCajaService.consultarCajaAbierta();
        model.addAttribute("caja", abierta.orElse(null));

        abierta.ifPresent(turno -> {
            int idTurno = turno.getIdAperturaCierreCaja();
            model.addAttribute("movimientos", movimientosCajaService.listarPorTurno(idTurno).stream()
                    .sorted(Comparator.comparingInt(m -> -m.getIdMovimientoCaja()))
                    .toList());
            var resumen = aperturasCierresCajaService.calcularResumen(turno);
            model.addAttribute("resumen", resumen);
            model.addAttribute("totalIngresos", resumen.totalIngresos());
            model.addAttribute("totalEgresos", resumen.egresos());
            // Efectivo esperado: solo dinero físico (no incluye tarjeta ni plataformas digitales)
            model.addAttribute("montoSistema", resumen.efectivoEsperado());
        });

        Map<Integer, String> mapaTipos = tiposMovimientosService.listar().stream()
                .collect(Collectors.toMap(TiposMovimientos::getIdTipoMovimiento,
                        TiposMovimientos::getNombreTipoMovimiento, (a, b) -> a));
        Map<Integer, String> mapaUsuarios = usuariosService.listar().stream()
                .collect(Collectors.toMap(Usuarios::getIdUsuario,
                        u -> u.getNombreUsuario() + " " + u.getApellidoUsuario(), (a, b) -> a));

        model.addAttribute("mapaTipos", mapaTipos);
        model.addAttribute("mapaUsuarios", mapaUsuarios);
        model.addAttribute("TIPO_INGRESO", MovimientosCajaService.TIPO_MOV_INGRESO);
        model.addAttribute("TIPO_EGRESO", MovimientosCajaService.TIPO_MOV_EGRESO);
        model.addAttribute("maxDescripcion", AperturasCierresCajaService.MAX_DESCRIPCION_MOVIMIENTO);
        model.addAttribute("menuActivo", "movimientos");
        return "movimientos/listarMovimientos";
    }

    @PostMapping
    public String registrar(@RequestParam("valor") BigDecimal valor,
                            @RequestParam("tipoMovimiento") int tipoMovimiento,
                            @RequestParam("descripcion") String descripcion,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Usuarios usuario = SesionUsuario.obtener(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        try {
            aperturasCierresCajaService.registrarMovimientoManual(valor, tipoMovimiento, descripcion,
                    usuario.getIdUsuario());
            redirectAttributes.addFlashAttribute("mensaje", "Movimiento registrado correctamente.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/movimientos";
    }
}
