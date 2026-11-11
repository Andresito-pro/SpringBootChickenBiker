package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.Productos;
import com.chickenbikerv2.service.ProductosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductosController {

    private final ProductosService productosService;

    public ProductosController(ProductosService productosService) {
        this.productosService = productosService;
    }

    @GetMapping("/listar")
    public List<Productos> listar() {
        return productosService.listar();
    }

    @PostMapping("/guardar")
    public Productos guardar(@RequestBody Productos productos) {
        return productosService.guardar(productos);
    }
}