package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.CategoriasProductos;
import com.chickenbikerv2.entity.Productos;
import com.chickenbikerv2.service.CategoriasProductosService;
import com.chickenbikerv2.service.ProductosService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/productos")
public class ProductosVistaController {

    private final ProductosService productosService;
    private final CategoriasProductosService categoriasProductosService;

    public ProductosVistaController(ProductosService productosService,
                                    CategoriasProductosService categoriasProductosService) {
        this.productosService = productosService;
        this.categoriasProductosService = categoriasProductosService;
    }

    @GetMapping({"", "/listar"})
    public String listar(Model model) {
        List<Productos> listaProductos = productosService.listar();
        List<CategoriasProductos> listaCategorias = categoriasProductosService.listar();

        Map<Integer, String> mapaCategorias = listaCategorias.stream()
                .collect(Collectors.toMap(
                        CategoriasProductos::getIdCategoriaProducto,
                        CategoriasProductos::getNombreCategoria,
                        (existente, reemplazo) -> existente
                ));

        model.addAttribute("listaProductos", listaProductos);
        model.addAttribute("totalProductos", listaProductos.size());
        model.addAttribute("listaCategorias", listaCategorias);
        model.addAttribute("mapaCategorias", mapaCategorias);
        model.addAttribute("menuActivo", "productos");
        return "productos/listarProductos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("producto", new Productos());
        model.addAttribute("listaCategorias", categoriasProductosService.listar());
        model.addAttribute("menuActivo", "productos");
        return "productos/nuevoProducto";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        Productos producto = productosService.buscarPorId(id).orElse(null);
        if (producto == null) {
            redirectAttributes.addFlashAttribute("error", "El producto seleccionado no existe.");
            return "redirect:/productos";
        }

        model.addAttribute("producto", producto);
        model.addAttribute("listaCategorias", categoriasProductosService.listar());
        model.addAttribute("menuActivo", "productos");
        return "productos/editarProducto";
    }

    @PostMapping
    public String guardar(@ModelAttribute("producto") Productos producto,
                          RedirectAttributes redirectAttributes) {
        boolean esEdicion = producto.getIdProductos() > 0;
        String rutaError = esEdicion
                ? "redirect:/productos/editar/" + producto.getIdProductos()
                : "redirect:/productos/nuevo";

        if (producto.getNombrePlato() == null || producto.getNombrePlato().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El nombre del producto es obligatorio.");
            return rutaError;
        }

        if (producto.getPrecioVenta() == null || producto.getPrecioVenta().compareTo(BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("error", "El precio de venta debe ser mayor o igual a 0.");
            return rutaError;
        }

        if (producto.getCategoriaProductoIdCategoriaProducto() <= 0) {
            redirectAttributes.addFlashAttribute("error", "Debes seleccionar una categoría válida.");
            return rutaError;
        }

        producto.setNombrePlato(producto.getNombrePlato().trim());
        if (producto.getDescripcionProductos() != null) {
            producto.setDescripcionProductos(producto.getDescripcionProductos().trim());
        }

        if (esEdicion) {
            Optional<Productos> actual = productosService.buscarPorId(producto.getIdProductos());
            if (actual.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El producto que intentas actualizar no existe.");
                return "redirect:/productos";
            }
            if (producto.getActivoProducto() == null) {
                producto.setActivoProducto(actual.get().getActivoProducto());
            }
        } else {
            producto.setActivoProducto((byte) 1);
        }

        try {
            productosService.guardar(producto);
            redirectAttributes.addFlashAttribute("mensaje",
                    esEdicion ? "Producto actualizado correctamente." : "Producto registrado correctamente.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar el producto. Verifica la categoría seleccionada.");
            return rutaError;
        }

        return "redirect:/productos";
    }

    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        Optional<Productos> optProducto = productosService.buscarPorId(id);
        if (optProducto.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El producto seleccionado no existe.");
            return "redirect:/productos";
        }

        Productos producto = optProducto.get();
        boolean estaActivo = producto.getActivoProducto() != null && producto.getActivoProducto() == 1;
        producto.setActivoProducto(estaActivo ? (byte) 0 : (byte) 1);
        productosService.guardar(producto);

        redirectAttributes.addFlashAttribute("mensaje",
                estaActivo ? "Producto desactivado." : "Producto activado.");
        return "redirect:/productos";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            productosService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensaje", "Producto eliminado correctamente.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error",
                    "No se puede eliminar este producto porque está asociado a pedidos registrados. Puedes desactivarlo desde su interruptor.");
        }
        return "redirect:/productos";
    }
}