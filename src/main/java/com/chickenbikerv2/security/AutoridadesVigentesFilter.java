package com.chickenbikerv2.security;

import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.repository.UsuariosRepository;
import com.chickenbikerv2.service.UsuariosService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mantiene la autorización al día en CADA petición, sin depender de lo que se
 * guardó en la sesión al iniciar sesión:
 *
 *  1. Relee el usuario de la base de datos. Si ya no existe, fue inactivado o
 *     su cuenta venció, se cierra la sesión y se le envía al login
 *     (las peticiones /api/** reciben 401).
 *  2. Recalcula sus autoridades (permisos del rol + ROLE_rol) desde
 *     permisos_roles. Si cambiaron (el administrador asignó/revocó permisos o
 *     le cambió el rol), reemplaza la autenticación antes de que
 *     AuthorizationFilter decida, y actualiza los atributos de sesión que usan
 *     las vistas (sidebar, botones).
 *
 * Se registra antes de AuthorizationFilter (ver SecurityConfig). No se aplica a
 * los recursos estáticos.
 */
public class AutoridadesVigentesFilter extends OncePerRequestFilter {

    private static final List<String> PREFIJOS_ESTATICOS = List.of("/css/", "/js/", "/img/", "/favicon.ico");

    private final UsuariosRepository usuariosRepository;
    private final UsuarioDetailsService usuarioDetailsService;
    private final UsuariosService usuariosService;
    private final VigenciaUsuario vigenciaUsuario;
    private final SecurityContextRepository repositorioContexto = new HttpSessionSecurityContextRepository();

    public AutoridadesVigentesFilter(UsuariosRepository usuariosRepository,
                                     UsuarioDetailsService usuarioDetailsService,
                                     UsuariosService usuariosService,
                                     VigenciaUsuario vigenciaUsuario) {
        this.usuariosRepository = usuariosRepository;
        this.usuarioDetailsService = usuarioDetailsService;
        this.usuariosService = usuariosService;
        this.vigenciaUsuario = vigenciaUsuario;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return PREFIJOS_ESTATICOS.stream().anyMatch(ruta::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication actual = SecurityContextHolder.getContext().getAuthentication();
        if (actual == null || !actual.isAuthenticated() || actual instanceof AnonymousAuthenticationToken) {
            chain.doFilter(request, response);
            return;
        }

        Optional<Usuarios> optUsuario = usuariosRepository.findByCorreoUsuario(actual.getName());
        if (optUsuario.isEmpty() || !UsuarioDetailsService.estaActivo(optUsuario.get())) {
            expulsar(request, response, "inactivo");
            return;
        }
        Usuarios usuario = optUsuario.get();
        if (vigenciaUsuario.estaVencida(usuario)) {
            expulsar(request, response, "vencida");
            return;
        }

        List<GrantedAuthority> vigentes = usuarioDetailsService.autoridadesDe(usuario);
        if (!mismasAutoridades(actual, vigentes)) {
            UsernamePasswordAuthenticationToken nueva =
                    UsernamePasswordAuthenticationToken.authenticated(actual.getPrincipal(), null, vigentes);
            nueva.setDetails(actual.getDetails());
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(nueva);
            SecurityContextHolder.setContext(contexto);
            repositorioContexto.saveContext(contexto, request, response);
        }

        actualizarSesion(request.getSession(false), usuario, vigentes);
        chain.doFilter(request, response);
    }

    private static boolean mismasAutoridades(Authentication actual, List<GrantedAuthority> vigentes) {
        Set<String> a = actual.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        Set<String> b = vigentes.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        return a.equals(b);
    }

    /** Atributos que leen las vistas: siempre reflejan el rol y los permisos actuales. */
    private void actualizarSesion(HttpSession session, Usuarios usuario, List<GrantedAuthority> vigentes) {
        if (session == null) {
            return;
        }
        List<String> permisos = vigentes.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();
        session.setAttribute(SesionUsuario.PERMISOS, permisos);
        session.setAttribute(SesionUsuario.ID_ROL, usuario.getRolesIdRoles());
        session.setAttribute(SesionUsuario.NOMBRE_ROL, vigentes.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst().orElse(null));
        Usuarios enSesion = SesionUsuario.obtener(session);
        if (enSesion == null || enSesion.getRolesIdRoles() != usuario.getRolesIdRoles()) {
            session.setAttribute(SesionUsuario.USUARIO, usuariosService.copiaParaSesion(usuario));
        }
    }

    private void expulsar(HttpServletRequest request, HttpServletResponse response, String motivo) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        if (ruta.startsWith("/api/")) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
        } else {
            response.sendRedirect(request.getContextPath() + "/login?" + motivo);
        }
    }
}
