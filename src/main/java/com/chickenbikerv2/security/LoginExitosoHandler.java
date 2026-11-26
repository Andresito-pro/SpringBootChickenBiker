package com.chickenbikerv2.security;

import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.service.UsuariosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Tras autenticar: registra la autorización de tratamiento de datos (si aún no
 * estaba registrada) y guarda en sesión el usuario, su rol y sus permisos, que
 * es lo que las vistas y los controladores existentes ya esperan encontrar.
 * Spring Security ya regeneró el id de sesión (protección contra fijación).
 */
@Component
public class LoginExitosoHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UsuariosService usuariosService;
    private final UsuarioDetailsService usuarioDetailsService;

    public LoginExitosoHandler(UsuariosService usuariosService, UsuarioDetailsService usuarioDetailsService) {
        super("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
        this.usuariosService = usuariosService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, jakarta.servlet.ServletException {
        Usuarios usuario = usuariosService.buscarPorCorreo(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));

        usuariosService.registrarAutorizacionDatos(usuario.getIdUsuario());

        HttpSession session = request.getSession();
        session.setAttribute(SesionUsuario.USUARIO, usuariosService.copiaParaSesion(usuario));
        session.setAttribute(SesionUsuario.ID_ROL, usuario.getRolesIdRoles());
        session.setAttribute(SesionUsuario.NOMBRE_ROL, usuarioDetailsService.nombreRol(usuario.getRolesIdRoles()));
        session.setAttribute(SesionUsuario.PERMISOS, usuarioDetailsService.permisosDelRol(usuario.getRolesIdRoles()));

        super.onAuthenticationSuccess(request, response, authentication);
    }

    /** Cada rol entra directamente a su propio dashboard (ver DashboardPorRol). */
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        String ruta = DashboardPorRol.rutaPara(authentication);
        return ruta != null ? ruta : super.determineTargetUrl(request, response, authentication);
    }
}
