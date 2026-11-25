package com.chickenbikerv2.controller.api;

import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.UsuariosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {

    private final UsuariosService usuariosService;

    public UsuariosController(UsuariosService usuariosService) {
        this.usuariosService = usuariosService;
    }

    @GetMapping({"/listar", "/listarRoles"})
    public List<Usuarios> listar() {
        return usuariosService.listar();
    }

    /**
     * Crea (id 0) o actualiza un usuario. La contraseña se guarda siempre como
     * hash BCrypt y nunca se devuelve en la respuesta (WRITE_ONLY en la entidad).
     */
    @PostMapping("/guardar")
    public ResponseEntity<?> guardar(@RequestBody Usuarios usuarios, HttpSession session) {
        try {
            Usuarios actor = SesionUsuario.obtener(session);
            usuariosService.validarCambiosSobreSiMismo(usuarios, actor != null ? actor.getIdUsuario() : 0);
            Usuarios guardado = usuarios.getIdUsuario() > 0
                    ? usuariosService.actualizar(usuarios)
                    : usuariosService.crear(usuarios);
            return ResponseEntity.ok(guardado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
