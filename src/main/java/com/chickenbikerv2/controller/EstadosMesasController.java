package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.EstadosMesas;
import com.chickenbikerv2.service.EstadosMesasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estados-mesas")
public class EstadosMesasController {

    private final EstadosMesasService estadosMesasService;

    public EstadosMesasController(EstadosMesasService estadosMesasService) {
        this.estadosMesasService = estadosMesasService;
    }

    @GetMapping("/listar")
    public List<EstadosMesas> listar() {
        return estadosMesasService.listar();
    }

    @PostMapping("/guardar")
    public EstadosMesas guardar(@RequestBody EstadosMesas estadosMesas) {
        return estadosMesasService.guardar(estadosMesas);
    }
}