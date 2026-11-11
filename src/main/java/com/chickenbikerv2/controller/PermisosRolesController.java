package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.PermisosRoles;
import com.chickenbikerv2.service.PermisosRolesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permisos-roles")
public class PermisosRolesController {

    private final PermisosRolesService permisosRolesService;

    public PermisosRolesController(PermisosRolesService permisosRolesService) {
        this.permisosRolesService = permisosRolesService;
    }

    @GetMapping("/listar")
    public List<PermisosRoles> listar() {
        return permisosRolesService.listar();
    }

    @PostMapping("/guardar")
    public PermisosRoles guardar(@RequestBody PermisosRoles permisosRoles) {
        return permisosRolesService.guardar(permisosRoles);
    }
}