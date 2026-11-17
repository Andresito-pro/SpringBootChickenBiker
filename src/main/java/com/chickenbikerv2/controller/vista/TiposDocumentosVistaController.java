package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.TiposDocumentos;
import com.chickenbikerv2.service.TiposDocumentosService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tipos-documento")
public class TiposDocumentosVistaController {

    private final TiposDocumentosService tiposDocumentosService;

    public TiposDocumentosVistaController(TiposDocumentosService tiposDocumentosService) {
        this.tiposDocumentosService = tiposDocumentosService;
    }

    @GetMapping({"", "/listar"})
    public String listar(Model model) {
        model.addAttribute("listaTiposDocumento", tiposDocumentosService.listar());
        model.addAttribute("menuActivo", "tiposDocumento");
        return "tiposDocumento/listarTiposDocumentos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("tipoDocumento", new TiposDocumentos());
        model.addAttribute("menuActivo", "tiposDocumento");
        return "tiposDocumento/nuevoTipoDocumento";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        TiposDocumentos tipoDocumento = tiposDocumentosService.buscarPorId(id).orElse(null);
        if (tipoDocumento == null) {
            redirectAttributes.addFlashAttribute("error", "El tipo de documento seleccionado no existe.");
            return "redirect:/tipos-documento";
        }
        model.addAttribute("tipoDocumento", tipoDocumento);
        model.addAttribute("menuActivo", "tiposDocumento");
        return "tiposDocumento/nuevoTipoDocumento";
    }

    @PostMapping
    public String guardar(@ModelAttribute("tipoDocumento") TiposDocumentos tipoDocumento,
                          RedirectAttributes redirectAttributes) {
        boolean esEdicion = tipoDocumento.getIdTipoDocumento() > 0;
        String rutaError = esEdicion
                ? "redirect:/tipos-documento/editar/" + tipoDocumento.getIdTipoDocumento()
                : "redirect:/tipos-documento/nuevo";

        if (tipoDocumento.getNombreTipoDocumento() == null || tipoDocumento.getNombreTipoDocumento().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "El nombre del tipo de documento es obligatorio.");
            return rutaError;
        }
        tipoDocumento.setNombreTipoDocumento(tipoDocumento.getNombreTipoDocumento().trim());
        // descripcion_tipo_documento es NOT NULL en la base de datos
        tipoDocumento.setDescripcionTipoDocumento(tipoDocumento.getDescripcionTipoDocumento() == null
                ? "" : tipoDocumento.getDescripcionTipoDocumento().trim());

        try {
            tiposDocumentosService.guardar(tipoDocumento);
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar el tipo de documento. Verifica los datos.");
            return rutaError;
        }
        redirectAttributes.addFlashAttribute("resultado", esEdicion ? "actualizado" : "creado");
        return "redirect:/tipos-documento";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            tiposDocumentosService.eliminar(id);
            redirectAttributes.addFlashAttribute("resultado", "eliminado");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar este tipo de documento porque tiene usuarios asociados.");
        }
        return "redirect:/tipos-documento";
    }
}