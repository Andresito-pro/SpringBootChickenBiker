package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.security.DashboardPorRol;
import com.chickenbikerv2.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Dashboards independientes por rol. El acceso a cada ruta lo restringe
 * SecurityConfig con DashboardPorRol (un rol no puede abrir el dashboard de
 * otro escribiendo la URL). /dashboard se conserva como punto de entrada
 * común (sidebar, página 403, raíz "/") y redirige al dashboard que corresponda.
 */
@Controller
public class DashboardVistaController {

    private final DashboardService dashboardService;

    public DashboardVistaController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        String ruta = DashboardPorRol.rutaPara(authentication);
        return ruta != null ? "redirect:" + ruta : "redirect:/login";
    }

    @GetMapping(DashboardPorRol.RUTA_ADMINISTRADOR)
    public String dashboardAdministrador(Model model) {
        model.addAllAttributes(dashboardService.obtenerResumen());
        model.addAttribute("menuActivo", "dashboard");
        return "dashboards/dashboard";
    }

    @GetMapping(DashboardPorRol.RUTA_CAJERO)
    public String dashboardCajero(Model model) {
        model.addAllAttributes(dashboardService.obtenerResumenCajero());
        model.addAttribute("menuActivo", "dashboard");
        return "dashboards/dashboardCajero";
    }

    @GetMapping(DashboardPorRol.RUTA_MESERO)
    public String dashboardMesero(Model model) {
        model.addAllAttributes(dashboardService.obtenerResumenMesero());
        model.addAttribute("menuActivo", "dashboard");
        return "dashboards/dashboardMesero";
    }
}
