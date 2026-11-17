package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.repository.UsuariosRepository;
import com.chickenbikerv2.security.UsuarioDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class UsuariosService {

    public static final int ESTADO_ACTIVO = UsuarioDetailsService.ESTADO_USUARIO_ACTIVO;
    public static final int ESTADO_INACTIVO = 2;

    /** BCrypt solo usa los primeros 72 bytes; se rechazan contraseñas más largas. */
    private static final int MAX_BYTES_CONTRASENA = 72;
    private static final int MIN_LONGITUD_CONTRASENA = 6;

    private final UsuariosRepository usuariosRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuariosService(UsuariosRepository usuariosRepository, PasswordEncoder passwordEncoder) {
        this.usuariosRepository = usuariosRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuarios> listar() {
        return usuariosRepository.findAll();
    }

    public List<Usuarios> listarActivos() {
        return usuariosRepository.findAll().stream()
                .filter(u -> u.getEstadosUsuariosIdEstadosUsuarios() == ESTADO_ACTIVO)
                .toList();
    }

    public Optional<Usuarios> buscarPorId(Integer id) {
        return usuariosRepository.findById(id);
    }

    public Optional<Usuarios> buscarPorCorreo(String correo) {
        return usuariosRepository.findByCorreoUsuario(correo);
    }

    public Optional<Usuarios> buscarPorDocumento(String documento) {
        return usuariosRepository.findByDocumentoUsuario(documento);
    }

    /** Registra un usuario nuevo guardando la contraseña como hash BCrypt. */
    @Transactional
    public Usuarios crear(Usuarios usuario) {
        validarContrasena(usuario.getPasswordUsuario());
        usuario.setPasswordUsuario(passwordEncoder.encode(usuario.getPasswordUsuario()));
        return usuariosRepository.save(usuario);
    }

    /**
     * Actualiza un usuario. Si la contraseña llega vacía se conserva el hash
     * almacenado tal cual (no se vuelve a codificar); si llega con valor, se
     * guarda el hash BCrypt de la nueva contraseña.
     */
    @Transactional
    public Usuarios actualizar(Usuarios datos) {
        Usuarios actual = usuariosRepository.findById(datos.getIdUsuario())
                .orElseThrow(() -> new IllegalArgumentException("El usuario que intentas actualizar no existe."));

        String nueva = datos.getPasswordUsuario();
        if (nueva == null || nueva.isBlank()) {
            datos.setPasswordUsuario(actual.getPasswordUsuario());
        } else {
            validarContrasena(nueva);
            datos.setPasswordUsuario(passwordEncoder.encode(nueva));
        }
        return usuariosRepository.save(datos);
    }

    @Transactional
    public void cambiarEstado(int idUsuario, int idEstado) {
        usuariosRepository.findById(idUsuario).ifPresent(u -> {
            u.setEstadosUsuariosIdEstadosUsuarios(idEstado);
            usuariosRepository.save(u);
        });
    }

    /** Registra la autorización de tratamiento de datos (columna usuarios.autorizacion_datos). */
    @Transactional
    public void registrarAutorizacionDatos(int idUsuario) {
        usuariosRepository.findById(idUsuario).ifPresent(u -> {
            if (u.getAutorizacionDatos() == null || u.getAutorizacionDatos() != 1) {
                u.setAutorizacionDatos((byte) 1);
                usuariosRepository.save(u);
            }
        });
    }

    public void eliminar(Integer id) {
        usuariosRepository.deleteById(id);
    }

    /** Copia sin contraseña para guardar en la sesión HTTP. */
    public Usuarios copiaParaSesion(Usuarios origen) {
        Usuarios copia = new Usuarios();
        copia.setIdUsuario(origen.getIdUsuario());
        copia.setNombreUsuario(origen.getNombreUsuario());
        copia.setApellidoUsuario(origen.getApellidoUsuario());
        copia.setDocumentoUsuario(origen.getDocumentoUsuario());
        copia.setCorreoUsuario(origen.getCorreoUsuario());
        copia.setTelefonoUsuario(origen.getTelefonoUsuario());
        copia.setRolesIdRoles(origen.getRolesIdRoles());
        copia.setTipoDocumentoIdTipoDocumento(origen.getTipoDocumentoIdTipoDocumento());
        copia.setEstadosUsuariosIdEstadosUsuarios(origen.getEstadosUsuariosIdEstadosUsuarios());
        copia.setAutorizacionDatos(origen.getAutorizacionDatos());
        return copia;
    }

    private void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
        if (contrasena.length() < MIN_LONGITUD_CONTRASENA) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + MIN_LONGITUD_CONTRASENA + " caracteres.");
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_CONTRASENA) {
            throw new IllegalArgumentException("La contraseña es demasiado larga (máximo 72 bytes).");
        }
    }
}
