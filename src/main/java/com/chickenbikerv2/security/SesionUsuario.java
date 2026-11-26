package com.chickenbikerv2.security;

import com.chickenbikerv2.entity.Usuarios;
import jakarta.servlet.http.HttpSession;

/**
 * Nombres de los atributos de sesión (los mismos que usaba la versión NetBeans
 * y que ya leen las vistas Thymeleaf) y acceso tipado al usuario autenticado.
 */
public final class SesionUsuario {

    public static final String USUARIO = "usuarioSesion";
    public static final String PERMISOS = "permisosSesion";
    public static final String NOMBRE_ROL = "nombreRolSesion";
    public static final String ID_ROL = "rolUsuario";

    private SesionUsuario() {
    }

    /** Usuario autenticado guardado en sesión al iniciar sesión, o null. */
    public static Usuarios obtener(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object valor = session.getAttribute(USUARIO);
        return valor instanceof Usuarios u ? u : null;
    }

    /**
     * Id del rol ACTUAL del usuario en sesión (AutoridadesVigentesFilter lo
     * actualiza en cada petición desde la base de datos), o 0 si no hay sesión.
     */
    public static int idRol(HttpSession session) {
        Object valor = session == null ? null : session.getAttribute(ID_ROL);
        return valor instanceof Integer i ? i : 0;
    }
}
