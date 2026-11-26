package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.GestionPermisosService;
import com.chickenbikerv2.service.GestionPermisosService.PermisoVista;
import com.chickenbikerv2.service.GestionPermisosService.ResultadoCambio;
import com.chickenbikerv2.service.GestionPermisosService.RolVista;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pestaña "Permisos" del administrador: consultar los roles, los permisos
 * (módulos/funcionalidades) disponibles y asignar o revocar permisos a un rol.
 * Acceso restringido a ROLE_Administrador en SecurityConfig; las reglas de
 * protección (rol propio, rol Administrador) las aplica GestionPermisosService.
 */
@Controller
@RequestMapping("/permisos")
public class PermisosVistaController {

    private final GestionPermisosService gestionPermisosService;

    public PermisosVistaController(GestionPermisosService gestionPermisosService) {
        this.gestionPermisosService = gestionPermisosService;
    }

    @GetMapping
    public String ver(@RequestParam(value = "rol", required = false) Integer idRol,
                      HttpSession session, Model model) {
        int idRolActor = SesionUsuario.idRol(session);
        List<RolVista> roles = gestionPermisosService.listarRoles(idRolActor);

        RolVista seleccionado = roles.stream()
                .filter(r -> idRol != null && r.idRol() == idRol)
                .findFirst()
                // Por defecto, el primer rol editable (o el primero de la lista)
                .orElseGet(() -> roles.stream().filter(r -> !r.protegido()).findFirst()
                        .orElse(roles.isEmpty() ? null : roles.get(0)));

        model.addAttribute("roles", roles);
        model.addAttribute("rolSeleccionado", seleccionado);
        if (seleccionado != null) {
            List<PermisoVista> permisos = gestionPermisosService.permisosDelRol(seleccionado.idRol());
            Map<String, List<PermisoVista>> porModulo = permisos.stream()
                    .collect(Collectors.groupingBy(PermisoVista::modulo, LinkedHashMap::new, Collectors.toList()));
            model.addAttribute("permisosPorModulo", porModulo);
            model.addAttribute("totalAsignados", permisos.stream().filter(PermisoVista::asignado).count());
            model.addAttribute("totalPermisos", permisos.size());
        }
        model.addAttribute("menuActivo", "permisos");
        return "permisos/gestionPermisos";
    }

    @PostMapping("/{idRol}")
    public String guardar(@PathVariable("idRol") int idRol,
                          @RequestParam(value = "permisos", required = false) List<Integer> permisos,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        try {
            ResultadoCambio resultado = gestionPermisosService.actualizarPermisos(
                    idRol, permisos, SesionUsuario.idRol(session));
            if (resultado.sinCambios()) {
                redirectAttributes.addFlashAttribute("info",
                        "No había cambios por guardar en el rol " + resultado.nombreRol() + ".");
            } else {
                redirectAttributes.addFlashAttribute("mensaje",
                        "Permisos del rol " + resultado.nombreRol() + " actualizados. Los cambios se aplican "
                                + "en la siguiente acción de cada usuario con ese rol.");
                redirectAttributes.addFlashAttribute("permisosAsignados", resultado.asignados());
                redirectAttributes.addFlashAttribute("permisosRevocados", resultado.revocados());
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/permisos?rol=" + idRol;
    }
}
