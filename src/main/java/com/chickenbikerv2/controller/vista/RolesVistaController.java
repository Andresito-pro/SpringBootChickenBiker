package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.Roles;
import com.chickenbikerv2.service.RolesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("roles")
public class RolesVistaController {
    private final RolesService rolesService;
    public RolesVistaController(RolesService rolesService) {
        this.rolesService = rolesService;
    }

    @GetMapping("/listarRol")
    public String listar(Model model) {
        model.addAttribute ("roles", rolesService.listarRoles());
        return "roles/listarRoles";

    }

    @GetMapping("/nuevoRol")
    public String nuevoRol(Model model) {
        model.addAttribute("rol", new Roles());
        return "roles/nuevoRol";
    }

    @PostMapping
    public String insertarRol(@ModelAttribute("rol") Roles rol) {
        rolesService.insertar(rol);
        return "redirect:/roles/listarRoles";
    }
}
