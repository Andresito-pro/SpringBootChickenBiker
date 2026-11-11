package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.CategoriasProductos;
import com.chickenbikerv2.service.CategoriasProductosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias-productos")
public class CategoriasProductosController {

    private final CategoriasProductosService categoriasProductosService;

    public CategoriasProductosController(CategoriasProductosService categoriasProductosService) {
        this.categoriasProductosService = categoriasProductosService;
    }

    @GetMapping("/listar")
    public List<CategoriasProductos> listar() {
        return categoriasProductosService.listar();
    }

    @PostMapping("/guardar")
    public CategoriasProductos guardar(@RequestBody CategoriasProductos categoriasProductos) {
        return categoriasProductosService.guardar(categoriasProductos);
    }
}