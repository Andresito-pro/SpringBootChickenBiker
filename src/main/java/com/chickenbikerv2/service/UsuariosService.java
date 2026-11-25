package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.repository.UsuariosRepository;
import com.chickenbikerv2.security.PoliticaContrasena;
import com.chickenbikerv2.security.UsuarioDetailsService;
import com.chickenbikerv2.security.VigenciaUsuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuariosService {

    public static final int ESTADO_ACTIVO = UsuarioDetailsService.ESTADO_USUARIO_ACTIVO;
    public static final int ESTADO_INACTIVO = 2;

    private final UsuariosRepository usuariosRepository;
    private final PasswordEncoder passwordEncoder;
    private final VigenciaUsuario vigenciaUsuario;

    public UsuariosService(UsuariosRepository usuariosRepository, PasswordEncoder passwordEncoder,
                           VigenciaUsuario vigenciaUsuario) {
        this.usuariosRepository = usuariosRepository;
        this.passwordEncoder = passwordEncoder;
        this.vigenciaUsuario = vigenciaUsuario;
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
        // Campo vacío = conservar la contraseña actual. Una contraseña de solo
        // espacios NO se trata como vacía: se valida y se rechaza.
        if (nueva == null || nueva.isEmpty()) {
            datos.setPasswordUsuario(actual.getPasswordUsuario());
        } else {
            validarContrasena(nueva);
            datos.setPasswordUsuario(passwordEncoder.encode(nueva));
        }
        return usuariosRepository.save(datos);
    }

    /**
     * Renovación anual de la vigencia, ejecutada por un administrador
     * (ruta /usuarios/** restringida a ROLE_Administrador). Es la única
     * operación que extiende la fecha de vencimiento; nunca ocurre sola.
     * No cambia el estado Activo/Inactivo.
     */
    @Transactional
    public Usuarios renovarVigencia(int idUsuario) {
        Usuarios usuario = usuariosRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario seleccionado no existe."));
        usuario.setFechaVencimiento(vigenciaUsuario.fechaRenovacion(usuario.getFechaVencimiento()));
        return usuariosRepository.save(usuario);
    }

    /**
     * Un usuario no puede cambiar su propio rol ni su propio estado (evita
     * escalar privilegios o bloquear por error el acceso administrativo).
     */
    public void validarCambiosSobreSiMismo(Usuarios datos, int idUsuarioActor) {
        if (datos.getIdUsuario() <= 0 || datos.getIdUsuario() != idUsuarioActor) {
            return;
        }
        Usuarios actual = usuariosRepository.findById(idUsuarioActor)
                .orElseThrow(() -> new IllegalArgumentException("El usuario que intentas actualizar no existe."));
        if (datos.getRolesIdRoles() != actual.getRolesIdRoles()) {
            throw new IllegalArgumentException("No puedes cambiar tu propio rol.");
        }
        if (datos.getEstadosUsuariosIdEstadosUsuarios() != 0
                && datos.getEstadosUsuariosIdEstadosUsuarios() != actual.getEstadosUsuariosIdEstadosUsuarios()) {
            throw new IllegalArgumentException("No puedes cambiar el estado de tu propia cuenta.");
        }
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

    /** Política de contraseñas seguras (ver PoliticaContrasena), aplicada en el servidor. */
    private void validarContrasena(String contrasena) {
        PoliticaContrasena.validar(contrasena);
    }
}
