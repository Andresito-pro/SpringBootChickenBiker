package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.AperturasCierresCaja;
import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.AperturasCierresCajaService;
import com.chickenbikerv2.service.MovimientosCajaService;
import com.chickenbikerv2.service.UsuariosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequestMapping("/caja")
public class CajaVistaController {

    private static final String SQLSTATE_REGLA_NEGOCIO = "45000";

    private final AperturasCierresCajaService aperturasCierresCajaService;
    private final MovimientosCajaService movimientosCajaService;
    private final UsuariosService usuariosService;

    public CajaVistaController(AperturasCierresCajaService aperturasCierresCajaService,
                               MovimientosCajaService movimientosCajaService,
                               UsuariosService usuariosService) {
        this.aperturasCierresCajaService = aperturasCierresCajaService;
        this.movimientosCajaService = movimientosCajaService;
        this.usuariosService = usuariosService;
    }

    @GetMapping({"", "/estado"})
    public String estado(Model model) {
        Optional<AperturasCierresCaja> optAbierta = aperturasCierresCajaService.consultarCajaAbierta();
        AperturasCierresCaja abierta = optAbierta.orElse(null);

        model.addAttribute("caja", abierta);
        if (abierta != null) {
            BigDecimal ingresos = movimientosCajaService.sumarIngresosPorTurno(abierta.getIdAperturaCierreCaja());
            BigDecimal egresos = movimientosCajaService.sumarEgresosPorTurno(abierta.getIdAperturaCierreCaja());
            model.addAttribute("totalIngresos", ingresos);
            model.addAttribute("totalEgresos", egresos);
        }

        model.addAttribute("menuActivo", "caja");
        return "caja/estadoCaja";
    }

    @GetMapping("/abrir")
    public String mostrarAbrir(Model model, RedirectAttributes redirectAttributes) {
        Optional<AperturasCierresCaja> optAbierta = aperturasCierresCajaService.consultarCajaAbierta();
        if (optAbierta.isPresent()) {
            redirectAttributes.addFlashAttribute("info",
                    "Ya existe una caja abierta (ID " + optAbierta.get().getIdAperturaCierreCaja()
                            + "). Debe cerrarla antes de abrir otra.");
            return "redirect:/caja";
        }

        model.addAttribute("menuActivo", "caja");
        return "caja/abrirCaja";
    }

    @PostMapping("/abrir")
    public String abrirCaja(@RequestParam("montoInicial") BigDecimal montoInicial,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        int idUsuarioApertura = obtenerIdUsuarioOperacion(session);
        if (idUsuarioApertura <= 0) {
            redirectAttributes.addFlashAttribute("error", "No se encontró un usuario válido en el sistema para abrir la caja.");
            return "redirect:/caja/abrir";
        }

        try {
            AperturasCierresCaja abierta = aperturasCierresCajaService.abrirCaja(montoInicial, idUsuarioApertura);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Caja abierta correctamente. ID #" + abierta.getIdAperturaCierreCaja());
            return "redirect:/caja";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/caja/abrir";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al abrir caja: " + e.getMessage());
            return "redirect:/caja/abrir";
        }
    }

    @GetMapping("/cerrar")
    public String mostrarCerrar(Model model, RedirectAttributes redirectAttributes) {
        Optional<AperturasCierresCaja> optAbierta = aperturasCierresCajaService.consultarCajaAbierta();
        if (optAbierta.isEmpty()) {
            redirectAttributes.addFlashAttribute("advertencia", advertenciaSinCajaAbierta());
            return "redirect:/caja/advertencia";
        }

        AperturasCierresCaja abierta = optAbierta.get();
        BigDecimal ingresos = movimientosCajaService.sumarIngresosPorTurno(abierta.getIdAperturaCierreCaja());
        BigDecimal egresos = movimientosCajaService.sumarEgresosPorTurno(abierta.getIdAperturaCierreCaja());
        BigDecimal montoSistema = aperturasCierresCajaService.calcularMontoSistema(abierta);

        model.addAttribute("caja", abierta);
        model.addAttribute("totalIngresos", ingresos);
        model.addAttribute("totalEgresos", egresos);
        model.addAttribute("montoSistema", montoSistema);
        model.addAttribute("usuarios", usuariosService.listarActivos());
        model.addAttribute("menuActivo", "caja");
        return "caja/cerrarCaja";
    }

    @PostMapping("/cerrar")
    public String cerrarCaja(@RequestParam("montoFinalReal") BigDecimal montoFinalReal,
                             @RequestParam("idUsuarioCierre") Integer idUsuarioCierre,
                             RedirectAttributes redirectAttributes) {
        if (idUsuarioCierre == null || idUsuarioCierre <= 0) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar un usuario válido para cerrar caja.");
            return "redirect:/caja/cerrar";
        }

        Optional<Usuarios> optUsuario = usuariosService.buscarPorId(idUsuarioCierre)
                .filter(u -> u.getEstadosUsuariosIdEstadosUsuarios() == UsuariosService.ESTADO_ACTIVO);
        if (optUsuario.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El usuario seleccionado para cerrar caja no es válido o está inactivo.");
            return "redirect:/caja/cerrar";
        }

        try {
            AperturasCierresCaja cerrada = aperturasCierresCajaService.cerrarCaja(montoFinalReal, idUsuarioCierre);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Caja cerrada correctamente por " + optUsuario.get().getNombreUsuario()
                            + ". Diferencia: $" + cerrada.getDiferencia());
            return "redirect:/caja";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/caja/cerrar";
        } catch (IllegalStateException e) {
            if (AperturasCierresCajaService.ERROR_NO_HAY_CAJA_ABIERTA.equals(e.getMessage())) {
                redirectAttributes.addFlashAttribute("advertencia", advertenciaSinCajaAbierta());
                return "redirect:/caja/advertencia";
            }
            if (AperturasCierresCajaService.ERROR_RECIBOS_PENDIENTES.equals(e.getMessage())) {
                redirectAttributes.addFlashAttribute("advertencia", advertenciaPorRegla("recibos pendientes de cobro"));
                return "redirect:/caja/advertencia";
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/caja/cerrar";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("advertencia", construirAdvertenciaCierre(e));
            return "redirect:/caja/advertencia";
        }
    }

    @GetMapping("/advertencia")
    public String mostrarAdvertencia(Model model) {
        if (!model.containsAttribute("advertencia")) {
            return "redirect:/caja";
        }
        model.addAttribute("menuActivo", "caja");
        return "caja/advertenciaCaja";
    }

    /** Usuario autenticado que realiza la operación (nunca un usuario "por defecto"). */
    private int obtenerIdUsuarioOperacion(HttpSession session) {
        Usuarios usuario = SesionUsuario.obtener(session);
        return usuario != null ? usuario.getIdUsuario() : 0;
    }

    // ------------------------------------------------------------------
    // ADVERTENCIAS DE NEGOCIO DEL CIERRE DE CAJA
    // ------------------------------------------------------------------
    public record AdvertenciaCaja(
            String tipo,
            String titulo,
            String motivo,
            String instruccion,
            String textoAccion,
            String urlAccion,
            String textoAccionSecundaria,
            String urlAccionSecundaria,
            String nota
    ) {}

    private AdvertenciaCaja advertenciaSinCajaAbierta() {
        return new AdvertenciaCaja(
                "advertencia",
                "No hay una caja abierta para cerrar",
                "No existe un turno de caja activo en este momento. Es posible que otro usuario ya haya realizado el cierre.",
                "Consulta el estado de la caja. Si necesitas seguir operando, abre un nuevo turno.",
                "Ver estado de caja", "/caja",
                null, null,
                null
        );
    }

    private AdvertenciaCaja construirAdvertenciaCierre(Throwable e) {
        SQLException regla = buscarReglaDeNegocio(e);
        if (regla != null) {
            return advertenciaPorRegla(regla.getMessage());
        }
        return new AdvertenciaCaja(
                "error",
                "No fue posible completar el cierre de caja",
                "Ocurrió un problema al guardar el cierre. No se realizó ningún cambio: la caja sigue abierta y sus montos no se modificaron.",
                "Intenta nuevamente en unos momentos. Si el problema continúa, comunícate con el administrador del sistema.",
                "Volver al cierre de caja", "/caja/cerrar",
                "Ver estado de caja", "/caja",
                "La caja sigue abierta y no se modificó ningún monto."
        );
    }

    private SQLException buscarReglaDeNegocio(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sqle) {
                for (SQLException s = sqle; s != null; s = s.getNextException()) {
                    if (SQLSTATE_REGLA_NEGOCIO.equals(s.getSQLState())) {
                        return s;
                    }
                }
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return null;
    }

    private AdvertenciaCaja advertenciaPorRegla(String mensajeRegla) {
        String texto = normalizar(mensajeRegla);
        String titulo = "No es posible cerrar la caja todavía";

        if (texto.contains("recibo") || texto.contains("cobr") || texto.contains("pago")
                || (texto.contains("pedido") && texto.contains("pendiente"))) {
            return new AdvertenciaCaja("advertencia", titulo,
                    "Existen pedidos pendientes de pago en el turno actual.",
                    "Realiza el cobro de los pedidos pendientes y vuelve a intentar el cierre de caja.",
                    "Ir a cobrar pedidos", "/pagos",
                    "Volver a caja", "/caja",
                    "La caja sigue abierta y no se modificó ningún monto.");
        }
        if (texto.contains("mesa")) {
            return new AdvertenciaCaja("advertencia", titulo,
                    "Todavía hay mesas ocupadas con pedidos sin cerrar.",
                    "Cobra los pedidos de las mesas ocupadas para liberarlas y vuelve a intentar el cierre de caja.",
                    "Ir a cobrar pedidos", "/pagos",
                    "Volver a caja", "/caja",
                    "La caja sigue abierta y no se modificó ningún monto.");
        }
        if (texto.contains("pedido")) {
            return new AdvertenciaCaja("advertencia", titulo,
                    "Existen pedidos del turno que aún no han sido finalizados.",
                    "Finaliza o cobra los pedidos abiertos y vuelve a intentar el cierre de caja.",
                    "Ir a cobrar pedidos", "/pagos",
                    "Volver a caja", "/caja",
                    "La caja sigue abierta y no se modificó ningún monto.");
        }
        if (texto.contains("movimiento")) {
            return new AdvertenciaCaja("advertencia", titulo,
                    "Hay movimientos de caja del turno que deben revisarse antes del cierre.",
                    "Revisa los movimientos registrados en el turno y vuelve a intentar el cierre de caja.",
                    "Ver movimientos de caja", "/movimientos",
                    "Volver a caja", "/caja",
                    "La caja sigue abierta y no se modificó ningún monto.");
        }

        String motivo = (mensajeRegla == null || mensajeRegla.trim().isEmpty() || mensajeRegla.length() > 300)
                ? "Existe una condición del turno actual que impide realizar el cierre."
                : mensajeRegla.trim();
        return new AdvertenciaCaja("advertencia", titulo, motivo,
                "Resuelve la situación indicada y vuelve a intentar el cierre de caja.",
                "Volver a caja", "/caja",
                null, null,
                "La caja sigue abierta y no se modificó ningún monto.");
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}