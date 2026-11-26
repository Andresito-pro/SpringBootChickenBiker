package com.chickenbikerv2.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Única fuente de verdad sobre qué dashboard corresponde a cada usuario.
 *
 * La usan tres piezas, para que nunca discrepen:
 *  - LoginExitosoHandler: a dónde redirigir tras iniciar sesión.
 *  - DashboardVistaController: a dónde lleva el enlace genérico /dashboard.
 *  - SecurityConfig: cada ruta de dashboard solo autoriza al usuario cuyo
 *    dashboard es exactamente esa ruta (no hay acceso cruzado por URL).
 *
 * Reglas (a partir de las autoridades que ya concede UsuarioDetailsService):
 *  1. ROLE_Administrador           -> dashboard del administrador.
 *  2. ROLE_Cajero                  -> dashboard del cajero.
 *  3. ROLE_Mesero                  -> dashboard del mesero.
 *  4. Rol creado desde el módulo Roles (sin nombre conocido):
 *     con permiso "Gestionar caja" -> dashboard del cajero; en otro caso,
 *     dashboard del mesero (solo indicadores de mesas). El dashboard del
 *     administrador, que expone las ventas, nunca se asigna por permisos.
 */
public final class DashboardPorRol {

    public static final String RUTA_ADMINISTRADOR = "/dashboard/administrador";
    public static final String RUTA_CAJERO = "/dashboard/cajero";
    public static final String RUTA_MESERO = "/dashboard/mesero";

    public static final String ROL_ADMINISTRADOR = "Administrador";
    public static final String ROL_CAJERO = "Cajero";
    public static final String ROL_MESERO = "Mesero";

    private static final String PERMISO_CAJA = "Gestionar caja";

    private DashboardPorRol() {
    }

    /** Ruta del dashboard del usuario autenticado, o null si no hay sesión real. */
    public static String rutaPara(Authentication autenticacion) {
        if (!estaAutenticado(autenticacion)) {
            return null;
        }
        Set<String> autoridades = autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (autoridades.contains("ROLE_" + ROL_ADMINISTRADOR)) {
            return RUTA_ADMINISTRADOR;
        }
        if (autoridades.contains("ROLE_" + ROL_CAJERO)) {
            return RUTA_CAJERO;
        }
        if (autoridades.contains("ROLE_" + ROL_MESERO)) {
            return RUTA_MESERO;
        }
        return autoridades.contains(PERMISO_CAJA) ? RUTA_CAJERO : RUTA_MESERO;
    }

    /**
     * Autoriza la petición solo si la ruta del dashboard del usuario es
     * exactamente {@code ruta}. Un anónimo recibe "denegado" y Spring Security
     * lo envía al login; un usuario de otro rol recibe 403.
     */
    public static AuthorizationManager<RequestAuthorizationContext> soloDueno(String ruta) {
        return (autenticacion, contexto) -> new AuthorizationDecision(ruta.equals(rutaPara(autenticacion.get())));
    }

    private static boolean estaAutenticado(Authentication autenticacion) {
        return autenticacion != null && autenticacion.isAuthenticated()
                && !(autenticacion instanceof AnonymousAuthenticationToken);
    }
}
