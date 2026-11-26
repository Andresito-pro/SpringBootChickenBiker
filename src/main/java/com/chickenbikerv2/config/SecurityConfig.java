package com.chickenbikerv2.config;

import com.chickenbikerv2.repository.UsuariosRepository;
import com.chickenbikerv2.security.AutoridadesVigentesFilter;
import com.chickenbikerv2.security.ConsentimientoDatosFilter;
import com.chickenbikerv2.security.ContrasenaPasswordEncoder;
import com.chickenbikerv2.security.DashboardPorRol;
import com.chickenbikerv2.security.LoginExitosoHandler;
import com.chickenbikerv2.security.PermisosSistema;
import com.chickenbikerv2.security.UsuarioDetailsService;
import com.chickenbikerv2.security.VigenciaUsuario;
import com.chickenbikerv2.service.UsuariosService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * Autenticación y autorización de Chicken Biker V2.
 *
 * Traslada a Spring Security lo que en la versión NetBeans hacían
 * AutenticacionFilter (sesión obligatoria) y AutorizacionUtil (permisos por
 * rol leídos de permisos_roles). Las reglas por ruta replican las que cada
 * Servlet validaba con exigirPermiso / exigirRol.
 *
 * Los permisos son dinámicos: AutoridadesVigentesFilter recarga en cada
 * petición los permisos del rol (y verifica que la cuenta siga activa y
 * vigente) antes de que se evalúen estas reglas.
 */
@Configuration
public class SecurityConfig {

    public static final String ROL_ADMINISTRADOR = "Administrador";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new ContrasenaPasswordEncoder();
    }

    /**
     * Proveedor de autenticación con las comprobaciones de estado de la cuenta
     * (inactiva, vencida) DESPUÉS de validar la contraseña: así el formulario
     * no revela si una cuenta existe, está inactiva o vencida a quien no
     * conoce su contraseña.
     */
    @Bean
    public DaoAuthenticationProvider proveedorAutenticacion(UsuarioDetailsService usuarioDetailsService,
                                                           PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(usuarioDetailsService);
        proveedor.setPasswordEncoder(passwordEncoder);
        proveedor.setUserDetailsPasswordService(usuarioDetailsService);
        proveedor.setPreAuthenticationChecks(usuario -> { });
        proveedor.setPostAuthenticationChecks(new AccountStatusUserDetailsChecker());
        return proveedor;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   LoginExitosoHandler loginExitosoHandler,
                                                   DaoAuthenticationProvider proveedorAutenticacion,
                                                   UsuariosRepository usuariosRepository,
                                                   UsuarioDetailsService usuarioDetailsService,
                                                   UsuariosService usuariosService,
                                                   VigenciaUsuario vigenciaUsuario) throws Exception {
        http
            .authenticationProvider(proveedorAutenticacion)
            .authorizeHttpRequests(auth -> auth
                // Recursos públicos
                .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()

                // Dashboards independientes por rol: cada ruta solo la abre su dueño
                // (ver DashboardPorRol). /dashboard es solo un redireccionador.
                .requestMatchers(DashboardPorRol.RUTA_ADMINISTRADOR)
                    .access(DashboardPorRol.soloDueno(DashboardPorRol.RUTA_ADMINISTRADOR))
                .requestMatchers(DashboardPorRol.RUTA_CAJERO)
                    .access(DashboardPorRol.soloDueno(DashboardPorRol.RUTA_CAJERO))
                .requestMatchers(DashboardPorRol.RUTA_MESERO)
                    .access(DashboardPorRol.soloDueno(DashboardPorRol.RUTA_MESERO))
                .requestMatchers("/dashboard").authenticated()
                .requestMatchers("/dashboard/**").denyAll()

                // Estadísticas de ventas: exclusivas del dashboard del administrador
                .requestMatchers("/api/dashboard/**").hasRole(ROL_ADMINISTRADOR)

                // Estado de la caja para el indicador del topbar: cualquier usuario con sesión
                .requestMatchers("/api/caja/estado").authenticated()

                // Notificaciones de nuevos pedidos (Administrador y Cajero por defecto)
                .requestMatchers("/api/notificaciones/**").hasAuthority(PermisosSistema.RECIBIR_NOTIFICACIONES)

                // Solo Administrador (UsuarioServlet / TipoDocumentoServlet usaban exigirRol)
                .requestMatchers("/usuarios/**", "/tipos-documento/**").hasRole(ROL_ADMINISTRADOR)

                // Gestión de permisos por rol: pestaña exclusiva del Administrador
                .requestMatchers("/permisos/**").hasRole(ROL_ADMINISTRADOR)

                // Por permiso (exigirPermiso en los Servlets originales)
                .requestMatchers("/roles/**").hasAuthority(PermisosSistema.GESTIONAR_ROLES)
                .requestMatchers("/mesas/**", "/estados-mesas/**", "/estadosMesas/**")
                    .hasAuthority(PermisosSistema.GESTIONAR_MESAS)
                .requestMatchers("/productos/**", "/categorias/**").hasAuthority(PermisosSistema.GESTIONAR_PRODUCTOS)
                // Anulación: permiso propio además del acceso al módulo de pedidos
                .requestMatchers("/pedidos/anular-pagado/**").hasAuthority(PermisosSistema.ANULAR_PEDIDOS_PAGADOS)
                .requestMatchers("/pedidos/anular/**").hasAuthority(PermisosSistema.ANULAR_PEDIDOS)
                .requestMatchers("/pedidos/**").hasAuthority(PermisosSistema.GESTIONAR_PEDIDOS)
                .requestMatchers("/caja/**", "/movimientos/**", "/pagos/**",
                        "/api/aperturas-cierres-caja/**").hasAuthority(PermisosSistema.GESTIONAR_CAJA)
                .requestMatchers("/recibos/**").hasAuthority(PermisosSistema.CONSULTAR_REPORTES)

                // La API REST genérica permite escribir cualquier tabla: solo Administrador
                .requestMatchers("/api/**").hasRole(ROL_ADMINISTRADOR)

                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("correo")
                .passwordParameter("password")
                .successHandler(loginExitosoHandler)
                .failureHandler((request, response, exception) -> {
                    String motivo = exception instanceof DisabledException ? "inactivo"
                            : exception instanceof AccountExpiredException ? "vencida"
                            : "error";
                    response.sendRedirect(request.getContextPath() + "/login?" + motivo);
                })
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            // Las peticiones AJAX a /api/** sin sesión reciben 401 en lugar de una
            // redirección al HTML del login (que fetch() seguiría en silencio).
            // El resto de rutas conserva la redirección al formulario de login.
            .exceptionHandling(ex -> ex
                    .defaultAuthenticationEntryPointFor(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                            PathPatternRequestMatcher.pathPattern("/api/**"))
                    .defaultAuthenticationEntryPointFor(
                            new LoginUrlAuthenticationEntryPoint("/login"),
                            AnyRequestMatcher.INSTANCE))
            .addFilterBefore(new ConsentimientoDatosFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new AutoridadesVigentesFilter(usuariosRepository, usuarioDetailsService,
                    usuariosService, vigenciaUsuario), AuthorizationFilter.class);

        return http.build();
    }
}
