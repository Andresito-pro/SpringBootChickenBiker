package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.EstadosUsuarios;
import com.chickenbikerv2.entity.Roles;
import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.security.VigenciaUsuario;
import com.chickenbikerv2.service.EstadosUsuariosService;
import com.chickenbikerv2.service.RolesService;
import com.chickenbikerv2.service.TiposDocumentosService;
import com.chickenbikerv2.service.UsuariosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/usuarios")
public class UsuariosVistaController {

    private final UsuariosService usuariosService;
    private final RolesService rolesService;
    private final TiposDocumentosService tiposDocumentosService;
    private final EstadosUsuariosService estadosUsuariosService;
    private final VigenciaUsuario vigenciaUsuario;

    public UsuariosVistaController(UsuariosService usuariosService,
                                   RolesService rolesService,
                                   TiposDocumentosService tiposDocumentosService,
                                   EstadosUsuariosService estadosUsuariosService,
                                   VigenciaUsuario vigenciaUsuario) {
        this.usuariosService = usuariosService;
        this.rolesService = rolesService;
        this.tiposDocumentosService = tiposDocumentosService;
        this.estadosUsuariosService = estadosUsuariosService;
        this.vigenciaUsuario = vigenciaUsuario;
    }

    @GetMapping({"", "/listar"})
    public String listar(Model model) {
        List<Usuarios> listaUsuarios = usuariosService.listar();
        List<Roles> listaRoles = rolesService.listar();
        List<EstadosUsuarios> listaEstados = estadosUsuariosService.listar();

        Map<Integer, String> mapaRoles = listaRoles.stream()
                .collect(Collectors.toMap(
                        Roles::getIdRoles,
                        Roles::getNombreRol,
                        (existente, reemplazo) -> existente
                ));

        Map<Integer, String> mapaEstados = listaEstados.stream()
                .collect(Collectors.toMap(
                        EstadosUsuarios::getIdEstadosUsuarios,
                        EstadosUsuarios::getNombreEstado,
                        (existente, reemplazo) -> existente
                ));

        // Estado de vigencia de cada cuenta: vencida / proxima (≤ 30 días) / vigente
        Map<Integer, String> vigencias = listaUsuarios.stream().collect(Collectors.toMap(
                Usuarios::getIdUsuario,
                u -> vigenciaUsuario.estaVencida(u) ? "vencida"
                        : vigenciaUsuario.venceProximamente(u.getFechaVencimiento()) ? "proxima" : "vigente",
                (a, b) -> a));
        Map<Integer, Long> diasRestantes = listaUsuarios.stream().collect(Collectors.toMap(
                Usuarios::getIdUsuario, u -> vigenciaUsuario.diasRestantes(u.getFechaVencimiento()), (a, b) -> a));

        model.addAttribute("vigencias", vigencias);
        model.addAttribute("diasRestantes", diasRestantes);
        model.addAttribute("listaUsuarios", listaUsuarios);
        model.addAttribute("listaRoles", listaRoles);
        model.addAttribute("mapaRoles", mapaRoles);
        model.addAttribute("mapaEstados", mapaEstados);
        model.addAttribute("menuActivo", "usuarios");
        return "usuarios/listarUsuarios";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        Usuarios usuario = new Usuarios();
        usuario.setEstadosUsuariosIdEstadosUsuarios(1);
        usuario.setAutorizacionDatos((byte) 0);

        model.addAttribute("usuario", usuario);
        model.addAttribute("listaTiposDoc", tiposDocumentosService.listar());
        model.addAttribute("listaRoles", rolesService.listar());
        model.addAttribute("menuActivo", "usuarios");
        return "usuarios/nuevoUsuario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        Usuarios usuario = usuariosService.buscarPorId(id).orElse(null);
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "El usuario seleccionado no existe.");
            return "redirect:/usuarios";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("listaTiposDoc", tiposDocumentosService.listar());
        model.addAttribute("listaRoles", rolesService.listar());
        model.addAttribute("listaEstados", estadosUsuariosService.listar());
        model.addAttribute("menuActivo", "usuarios");
        return "usuarios/editarUsuario";
    }

    @PostMapping
    public String guardar(@ModelAttribute("usuario") Usuarios usuario,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        boolean esEdicion = usuario.getIdUsuario() > 0;
        String rutaError = esEdicion ? "redirect:/usuarios/editar/" + usuario.getIdUsuario() : "redirect:/usuarios/nuevo";

        String faltante = validarObligatorios(usuario);
        if (faltante != null) {
            redirectAttributes.addFlashAttribute("error", "El campo \"" + faltante + "\" es obligatorio.");
            return rutaError;
        }
        usuario.setCorreoUsuario(usuario.getCorreoUsuario().trim());
        usuario.setDocumentoUsuario(usuario.getDocumentoUsuario().trim());

        Optional<Usuarios> existenteCorreo = usuariosService.buscarPorCorreo(usuario.getCorreoUsuario());
        if (existenteCorreo.isPresent() && existenteCorreo.get().getIdUsuario() != usuario.getIdUsuario()) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un usuario registrado con ese correo electrónico.");
            return rutaError;
        }

        Optional<Usuarios> existenteDoc = usuariosService.buscarPorDocumento(usuario.getDocumentoUsuario());
        if (existenteDoc.isPresent() && existenteDoc.get().getIdUsuario() != usuario.getIdUsuario()) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un usuario registrado con ese número de documento.");
            return rutaError;
        }

        if (!esEdicion && usuario.getEstadosUsuariosIdEstadosUsuarios() == 0) {
            usuario.setEstadosUsuariosIdEstadosUsuarios(UsuariosService.ESTADO_ACTIVO);
        }
        if (usuario.getFechaVencimiento() == null) {
            if (esEdicion) {
                // Editar NO renueva la vigencia: se conserva la fecha almacenada.
                // La renovación es una acción explícita (botón "Renovar vigencia").
                usuario.setFechaVencimiento(usuariosService.buscarPorId(usuario.getIdUsuario())
                        .map(Usuarios::getFechaVencimiento).orElse(null));
            }
            if (usuario.getFechaVencimiento() == null) {
                usuario.setFechaVencimiento(vigenciaUsuario.vencimientoInicial());
            }
        }

        Usuarios actor = SesionUsuario.obtener(session);
        try {
            usuariosService.validarCambiosSobreSiMismo(usuario, actor != null ? actor.getIdUsuario() : 0);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return rutaError;
        }
        if (usuario.getAutorizacionDatos() == null) {
            usuario.setAutorizacionDatos((byte) 0);
        }

        try {
            // La contraseña se guarda siempre como hash BCrypt (ver UsuariosService)
            if (esEdicion) {
                usuariosService.actualizar(usuario);
            } else {
                usuariosService.crear(usuario);
            }
            redirectAttributes.addFlashAttribute("mensaje",
                    esEdicion ? "Usuario actualizado correctamente." : "Usuario registrado correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return rutaError;
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar el usuario. Verifica que los datos sean válidos y no estén duplicados.");
            return rutaError;
        }

        return "redirect:/usuarios";
    }

    /**
     * Renovación anual de la vigencia (solo Administrador: /usuarios/**).
     * Un año desde hoy o, si la cuenta sigue vigente, desde su vencimiento actual.
     */
    @PostMapping("/renovar/{id}")
    public String renovar(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            Usuarios renovado = usuariosService.renovarVigencia(id);
            redirectAttributes.addFlashAttribute("mensaje", "Vigencia de " + renovado.getNombreUsuario() + " "
                    + renovado.getApellidoUsuario() + " renovada hasta el "
                    + renovado.getFechaVencimiento().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + ".");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/activar/{id}")
    public String activar(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        usuariosService.cambiarEstado(id, UsuariosService.ESTADO_ACTIVO);
        redirectAttributes.addFlashAttribute("mensaje", "Usuario activado correctamente.");
        return "redirect:/usuarios";
    }

    @PostMapping("/inactivar/{id}")
    public String inactivar(@PathVariable("id") Integer id, HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Usuarios actual = SesionUsuario.obtener(session);
        if (actual != null && actual.getIdUsuario() == id) {
            redirectAttributes.addFlashAttribute("error", "No puedes inactivar tu propia cuenta.");
            return "redirect:/usuarios";
        }
        usuariosService.cambiarEstado(id, UsuariosService.ESTADO_INACTIVO);
        redirectAttributes.addFlashAttribute("mensaje", "Usuario inactivado correctamente.");
        return "redirect:/usuarios";
    }

    /** Columnas NOT NULL de la tabla usuarios que llegan desde el formulario. */
    private static String validarObligatorios(Usuarios u) {
        if (vacio(u.getNombreUsuario())) return "Nombre";
        if (vacio(u.getApellidoUsuario())) return "Apellido";
        if (u.getTipoDocumentoIdTipoDocumento() <= 0) return "Tipo de documento";
        if (vacio(u.getDocumentoUsuario())) return "Documento";
        if (u.getRolesIdRoles() <= 0) return "Rol";
        if (vacio(u.getCorreoUsuario())) return "Correo";
        if (vacio(u.getTelefonoUsuario())) return "Teléfono";
        if (u.getFechaNacimiento() == null) return "Fecha de nacimiento";
        if (vacio(u.getDireccionUsuario())) return "Dirección";
        return null;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
