package com.chickenbikerv2.controller.api;

import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.NotificacionesService;
import com.chickenbikerv2.service.NotificacionesService.EstadoLectura;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Campana de notificaciones. Requiere el permiso "Recibir notificaciones"
 * (SecurityConfig); un mesero sin ese permiso recibe 403. El destinatario es
 * SIEMPRE el usuario de la sesión: el cliente no puede indicar otro usuario.
 * El estado leída/no leída vive en la sesión (no hay tabla de notificaciones).
 * Los POST exigen el token CSRF (lo envía js/topbar.js).
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionesController {

    static final String ATRIBUTO_ESTADO_LECTURA = "notificacionesEstadoLectura";

    private final NotificacionesService notificacionesService;

    public NotificacionesController(NotificacionesService notificacionesService) {
        this.notificacionesService = notificacionesService;
    }

    @GetMapping
    public ResponseEntity<?> resumen(HttpSession session) {
        Usuarios usuario = SesionUsuario.obtener(session);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(notificacionesService.resumen(usuario.getIdUsuario(), estadoLectura(session)));
    }

    @PostMapping("/{id}/leida")
    public ResponseEntity<?> marcarLeida(@PathVariable("id") int idNotificacion, HttpSession session) {
        Usuarios usuario = SesionUsuario.obtener(session);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        EstadoLectura estado = estadoLectura(session);
        if (!notificacionesService.marcarLeida(idNotificacion, usuario.getIdUsuario(), estado)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Notificación no encontrada."));
        }
        guardar(session, estado);
        return ResponseEntity.ok(notificacionesService.resumen(usuario.getIdUsuario(), estado));
    }

    @PostMapping("/leidas")
    public ResponseEntity<?> marcarTodas(HttpSession session) {
        Usuarios usuario = SesionUsuario.obtener(session);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        EstadoLectura estado = estadoLectura(session);
        notificacionesService.marcarTodasLeidas(usuario.getIdUsuario(), estado);
        guardar(session, estado);
        return ResponseEntity.ok(notificacionesService.resumen(usuario.getIdUsuario(), estado));
    }

    private static EstadoLectura estadoLectura(HttpSession session) {
        Object actual = session.getAttribute(ATRIBUTO_ESTADO_LECTURA);
        if (actual instanceof EstadoLectura estado) {
            return estado;
        }
        EstadoLectura nuevo = new EstadoLectura();
        session.setAttribute(ATRIBUTO_ESTADO_LECTURA, nuevo);
        return nuevo;
    }

    /** Vuelve a fijar el atributo para que el cambio se replique si la sesión se serializa. */
    private static void guardar(HttpSession session, EstadoLectura estado) {
        session.setAttribute(ATRIBUTO_ESTADO_LECTURA, estado);
    }
}
