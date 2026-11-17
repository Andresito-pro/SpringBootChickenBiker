package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.EstadosMesas;
import com.chickenbikerv2.service.EstadosMesasService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping({"/estados-mesas", "/estadosMesas"})
public class EstadosMesasVistaController {

    private final EstadosMesasService estadosMesasService;

    public EstadosMesasVistaController(EstadosMesasService estadosMesasService) {
        this.estadosMesasService = estadosMesasService;
    }

    @GetMapping({"", "/listar"})
    public String listar(Model model) {
        List<EstadosMesas> listaEstadosMesas = estadosMesasService.listar();
        model.addAttribute("listaEstadosMesas", listaEstadosMesas);
        model.addAttribute("totalEstadosMesas", listaEstadosMesas.size());
        model.addAttribute("menuActivo", "estados-mesas");
        return "estadosMesas/listarEstadosMesas";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        EstadosMesas estadoMesa = new EstadosMesas();
        estadoMesa.setActivoEstadoMesa((byte) 1);

        model.addAttribute("estadoMesa", estadoMesa);
        model.addAttribute("menuActivo", "estados-mesas");
        return "estadosMesas/nuevoEstadoMesa";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        EstadosMesas estadoMesa = estadosMesasService.buscarPorId(id).orElse(null);
        if (estadoMesa == null) {
            redirectAttributes.addFlashAttribute("error", "El estado de mesa seleccionado no existe.");
            return "redirect:/estados-mesas";
        }

        model.addAttribute("estadoMesa", estadoMesa);
        model.addAttribute("menuActivo", "estados-mesas");
        return "estadosMesas/editarEstadoMesa";
    }

    @PostMapping
    public String guardar(@ModelAttribute("estadoMesa") EstadosMesas estadoMesa,
                          RedirectAttributes redirectAttributes) {
        boolean esEdicion = estadoMesa.getIdEstadoMesa() > 0;
        String rutaError = esEdicion
                ? "redirect:/estados-mesas/editar/" + estadoMesa.getIdEstadoMesa()
                : "redirect:/estados-mesas/nuevo";

        if (estadoMesa.getNombreEstadoMesa() == null || estadoMesa.getNombreEstadoMesa().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "El nombre del estado de mesa es obligatorio.");
            return rutaError;
        }

        String nombreLimpio = estadoMesa.getNombreEstadoMesa().trim();
        estadoMesa.setNombreEstadoMesa(nombreLimpio);

        boolean duplicado = estadosMesasService.listar().stream()
                .anyMatch(em -> em.getIdEstadoMesa() != estadoMesa.getIdEstadoMesa()
                        && em.getNombreEstadoMesa() != null
                        && em.getNombreEstadoMesa().trim().equalsIgnoreCase(nombreLimpio));

        if (duplicado) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un estado de mesa registrado con ese nombre.");
            return rutaError;
        }

        if (estadoMesa.getActivoEstadoMesa() == null) {
            estadoMesa.setActivoEstadoMesa((byte) 0);
        }
        // descripcion_estado_mesa es NOT NULL en la base de datos
        estadoMesa.setDescripcionEstadoMesa(estadoMesa.getDescripcionEstadoMesa() == null
                ? "" : estadoMesa.getDescripcionEstadoMesa().trim());

        try {
            estadosMesasService.guardar(estadoMesa);
            redirectAttributes.addFlashAttribute("mensaje",
                    esEdicion ? "Estado de mesa actualizado correctamente." : "Estado de mesa registrado correctamente.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar el estado de mesa. Verifica que los datos sean válidos.");
            return rutaError;
        }

        return "redirect:/estados-mesas";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            estadosMesasService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensaje", "Estado de mesa eliminado correctamente.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar este estado porque está asignado a una o más mesas.");
        }
        return "redirect:/estados-mesas";
    }
}