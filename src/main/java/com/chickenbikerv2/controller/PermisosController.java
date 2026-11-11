package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.Permisos;
import com.chickenbikerv2.service.PermisosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permisos")
public class PermisosController {

    private final PermisosService permisosService;

    public PermisosController(PermisosService permisosService) {
        this.permisosService = permisosService;
    }

    @GetMapping("/listar")
    public List<Permisos> listar() {
        return permisosService.listar();
    }

    @PostMapping("/guardar")
    public Permisos guardar(@RequestBody Permisos permisos) {
        return permisosService.guardar(permisos);
    }
}