package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.Roles;
import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.RolesService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/roles")
public class RolesVistaController {

    private final RolesService rolesService;

    public RolesVistaController(RolesService rolesService) {
        this.rolesService = rolesService;
    }

    @GetMapping({"", "/listarRol"})
    public String listar(Model model) {
        model.addAttribute("roles", rolesService.listarRoles());
        model.addAttribute("menuActivo", "roles");
        return "roles/listarRoles";
    }

    @GetMapping("/nuevoRol")
    public String nuevoRol(Model model) {
        model.addAttribute("rol", new Roles());
        model.addAttribute("menuActivo", "roles");
        return "roles/nuevoRol";
    }

    @GetMapping("/editar/{id}")
    public String editarRol(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        Roles rol = rolesService.buscarPorId(id).orElse(null);
        if (rol == null) {
            redirectAttributes.addFlashAttribute("error", "El rol seleccionado no existe.");
            return "redirect:/roles/listarRol";
        }
        model.addAttribute("rol", rol);
        model.addAttribute("menuActivo", "roles");
        return "roles/nuevoRol";
    }

    @PostMapping
    public String guardarRol(@ModelAttribute("rol") Roles rol, HttpSession session,
                             RedirectAttributes redirectAttributes) {
        boolean esEdicion = rol.getIdRoles() != null && rol.getIdRoles() > 0;
        String rutaError = esEdicion ? "redirect:/roles/editar/" + rol.getIdRoles() : "redirect:/roles/nuevoRol";

        if (rol.getNombreRol() == null || rol.getNombreRol().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "El nombre del rol es obligatorio.");
            return rutaError;
        }
        rol.setNombreRol(rol.getNombreRol().trim());
        try {
            // Evita roles duplicados y el escalamiento de privilegios por nombre de rol
            rolesService.validarGuardado(rol, SesionUsuario.idRol(session));
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return rutaError;
        }
        // descripcion_rol es NOT NULL en la base de datos
        rol.setDescripcionRol(rol.getDescripcionRol() == null ? "" : rol.getDescripcionRol().trim());

        try {
            rolesService.guardar(rol);
            redirectAttributes.addFlashAttribute("mensaje",
                    esEdicion ? "Rol actualizado correctamente." : "Rol registrado correctamente.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar el rol. Verifica los datos.");
            return rutaError;
        }
        return "redirect:/roles/listarRol";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarRol(@PathVariable("id") Integer id, HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            rolesService.validarEliminacion(id, SesionUsuario.idRol(session));
            rolesService.eliminar(id);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar este rol porque tiene usuarios o permisos asociados.");
        }
        return "redirect:/roles/listarRol";
    }
}