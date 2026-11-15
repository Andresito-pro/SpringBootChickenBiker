package com.chickenbikerv2.controller.api;

import com.chickenbikerv2.entity.Roles;
import com.chickenbikerv2.service.RolesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolesController {
    private final RolesService rolesService;

    public RolesController(RolesService rolesService) {
        this.rolesService = rolesService;
    }

    @GetMapping("/listarRoles")
    public List<Roles> listar(){
        return rolesService.listarRoles();
    }

    @PostMapping("/guardar")
    public Roles guardar(Roles roles){
        return rolesService.guardar(roles);
    }
}
