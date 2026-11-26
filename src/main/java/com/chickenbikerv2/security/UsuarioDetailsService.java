package com.chickenbikerv2.security;

import com.chickenbikerv2.entity.Roles;
import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.repository.PermisosRolesRepository;
import com.chickenbikerv2.repository.RolesRepository;
import com.chickenbikerv2.repository.UsuariosRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Carga el usuario para Spring Security a partir de las tablas existentes
 * (usuarios, roles, permisos, permisos_roles).
 *
 * Autoridades concedidas:
 *  - Cada nombre de permiso del rol (p. ej. "Gestionar caja"), igual que
 *    AutorizacionUtil.tienePermiso() en la versión NetBeans.
 *  - "ROLE_" + nombre del rol (p. ej. ROLE_Administrador), equivalente a
 *    AutorizacionUtil.tieneRol().
 *
 * Estado de la cuenta: inactiva -> DisabledException; vencida (fecha de
 * vencimiento anterior a hoy) -> AccountExpiredException. Ambas se verifican
 * DESPUÉS de comprobar la contraseña (ver SecurityConfig), para no revelar el
 * estado de una cuenta a quien no conoce su contraseña.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService, UserDetailsPasswordService {

    public static final int ESTADO_USUARIO_ACTIVO = 1;

    private final UsuariosRepository usuariosRepository;
    private final RolesRepository rolesRepository;
    private final PermisosRolesRepository permisosRolesRepository;
    private final VigenciaUsuario vigenciaUsuario;

    public UsuarioDetailsService(UsuariosRepository usuariosRepository,
                                 RolesRepository rolesRepository,
                                 PermisosRolesRepository permisosRolesRepository,
                                 VigenciaUsuario vigenciaUsuario) {
        this.usuariosRepository = usuariosRepository;
        this.rolesRepository = rolesRepository;
        this.permisosRolesRepository = permisosRolesRepository;
        this.vigenciaUsuario = vigenciaUsuario;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        String correoLimpio = correo == null ? "" : correo.trim();
        Usuarios usuario = usuariosRepository.findByCorreoUsuario(correoLimpio)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));

        return User.withUsername(usuario.getCorreoUsuario())
                .password(usuario.getPasswordUsuario())
                .authorities(autoridadesDe(usuario))
                // Un usuario inactivo no puede iniciar sesión (DisabledException).
                .disabled(!estaActivo(usuario))
                // Una cuenta vencida tampoco, aunque esté activa (AccountExpiredException).
                .accountExpired(vigenciaUsuario.estaVencida(usuario))
                .build();
    }

    public static boolean estaActivo(Usuarios usuario) {
        return usuario.getEstadosUsuariosIdEstadosUsuarios() == ESTADO_USUARIO_ACTIVO;
    }

    public List<String> permisosDelRol(int idRol) {
        return permisosRolesRepository.listarNombresPermisosPorRol(idRol);
    }

    public String nombreRol(int idRol) {
        return rolesRepository.findById(idRol).map(Roles::getNombreRol).orElse(null);
    }

    /** Autoridades actuales del usuario según su rol y los permisos vigentes en la base de datos. */
    public List<GrantedAuthority> autoridadesDe(Usuarios usuario) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        for (String permiso : permisosDelRol(usuario.getRolesIdRoles())) {
            autoridades.add(new SimpleGrantedAuthority(permiso));
        }
        String rol = nombreRol(usuario.getRolesIdRoles());
        if (rol != null) {
            autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol));
        }
        return autoridades;
    }

    /**
     * Invocado por Spring Security tras un inicio de sesión correcto cuando la
     * contraseña almacenada no es BCrypt (registro heredado en texto plano):
     * guarda el nuevo hash BCrypt. Nunca se llama para hashes BCrypt válidos.
     */
    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, String nuevoHash) {
        usuariosRepository.findByCorreoUsuario(user.getUsername()).ifPresent(u -> {
            u.setPasswordUsuario(nuevoHash);
            usuariosRepository.save(u);
        });
        return User.withUserDetails(user).password(nuevoHash).build();
    }
}
