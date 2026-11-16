package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.Roles;
import com.chickenbikerv2.service.RolesService;
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
    public String guardarRol(@ModelAttribute("rol") Roles rol) {
        rolesService.guardar(rol);
        return "redirect:/roles/listarRol";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarRol(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            rolesService.eliminar(id);
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar este rol porque tiene usuarios o permisos asociados.");
        }
        return "redirect:/roles/listarRol";
    }
}