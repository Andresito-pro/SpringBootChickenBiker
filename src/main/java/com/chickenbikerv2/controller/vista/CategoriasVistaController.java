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
    public String guardar(@ModelAttribute CategoriasProductos categoria) {
        categoriasService.guardar(categoria);
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