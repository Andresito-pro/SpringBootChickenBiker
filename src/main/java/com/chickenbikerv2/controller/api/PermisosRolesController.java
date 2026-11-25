package com.chickenbikerv2.controller.api;

import com.chickenbikerv2.entity.PermisosRoles;
import com.chickenbikerv2.repository.PermisosRepository;
import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.GestionPermisosService;
import com.chickenbikerv2.service.PermisosRolesService;
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
@RequestMapping("/api/permisos-roles")
public class PermisosRolesController {

    private final PermisosRolesService permisosRolesService;
    private final GestionPermisosService gestionPermisosService;
    private final PermisosRepository permisosRepository;

    public PermisosRolesController(PermisosRolesService permisosRolesService,
                                   GestionPermisosService gestionPermisosService,
                                   PermisosRepository permisosRepository) {
        this.permisosRolesService = permisosRolesService;
        this.gestionPermisosService = gestionPermisosService;
        this.permisosRepository = permisosRepository;
    }

    @GetMapping({"/listar", "/listarRoles"})
    public List<PermisosRoles> listar() {
        return permisosRolesService.listar();
    }

    /**
     * Asigna un permiso a un rol. Aplica las mismas protecciones que la pestaña
     * Permisos: no se modifica el propio rol ni el rol Administrador, y el
     * permiso debe existir.
     */
    @PostMapping("/guardar")
    public ResponseEntity<?> guardar(@RequestBody PermisosRoles permisosRoles, HttpSession session) {
        try {
            gestionPermisosService.verificarRolEditable(permisosRoles.getRolesIdRoles(), SesionUsuario.idRol(session));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        if (!permisosRepository.existsById(permisosRoles.getPermisosIdPermisos())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El permiso indicado no existe."));
        }
        return ResponseEntity.ok(permisosRolesService.guardar(permisosRoles));
    }
}
