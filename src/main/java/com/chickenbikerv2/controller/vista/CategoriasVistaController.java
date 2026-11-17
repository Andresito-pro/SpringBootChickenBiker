package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.CategoriasProductos;
import com.chickenbikerv2.service.CategoriasProductosService;
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
@RequestMapping("/categorias")
public class CategoriasVistaController {

    private final CategoriasProductosService categoriasService;

    public CategoriasVistaController(CategoriasProductosService categoriasService) {
        this.categoriasService = categoriasService;
    }

    @GetMapping({"", "/listarCategorias"})
    public String listar(Model model) {
        model.addAttribute("categorias", categoriasService.listar());
        model.addAttribute("categoriaNueva", new CategoriasProductos());
        model.addAttribute("menuActivo", "categorias");
        return "categorias/listarCategorias";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        CategoriasProductos categoria = categoriasService.buscarPorId(id).orElse(null);
        if (categoria == null) {
            redirectAttributes.addFlashAttribute("error", "La categoría seleccionada no existe.");
            return "redirect:/categorias";
        }
        model.addAttribute("categoria", categoria);
        model.addAttribute("menuActivo", "categorias");
        return "categorias/editarCategoria";
    }

    @PostMapping
    public String guardar(@ModelAttribute CategoriasProductos categoria, RedirectAttributes redirectAttributes) {
        boolean esEdicion = categoria.getIdCategoriaProducto() > 0;
        String rutaError = esEdicion ? "redirect:/categorias/editar/" + categoria.getIdCategoriaProducto()
                : "redirect:/categorias";

        if (categoria.getNombreCategoria() == null || categoria.getNombreCategoria().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "El nombre de la categoría es obligatorio.");
            return rutaError;
        }
        categoria.setNombreCategoria(categoria.getNombreCategoria().trim());
        // descripcion_categoria es NOT NULL en la base de datos
        categoria.setDescripcionCategoria(categoria.getDescripcionCategoria() == null
                ? "" : categoria.getDescripcionCategoria().trim());

        try {
            categoriasService.guardar(categoria);
            redirectAttributes.addFlashAttribute("mensaje",
                    esEdicion ? "Categoría actualizada correctamente." : "Categoría registrada correctamente.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar la categoría. Verifica los datos.");
            return rutaError;
        }
        return "redirect:/categorias";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoriasService.eliminar(id);
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar esta categoría porque tiene productos asociados.");
        }
        return "redirect:/categorias";
    }
}