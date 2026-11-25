package com.chickenbikerv2.controller.api;

import com.chickenbikerv2.entity.Roles;
import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.RolesService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RolesController {
    private final RolesService rolesService;

    public RolesController(RolesService rolesService) {
        this.rolesService = rolesService;
    }

    @GetMapping({"/listar", "/listarRoles"})
    public List<Roles> listar(){
        return rolesService.listarRoles();
    }

    /** Mismas reglas que el módulo Roles: sin duplicados ni escalamiento por nombre de rol. */
    @PostMapping("/guardar")
    public ResponseEntity<?> guardar(@RequestBody Roles roles, HttpSession session){
        if (roles.getNombreRol() == null || roles.getNombreRol().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre del rol es obligatorio."));
        }
        roles.setNombreRol(roles.getNombreRol().trim());
        try {
            rolesService.validarGuardado(roles, SesionUsuario.idRol(session));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(rolesService.guardar(roles));
    }
}
