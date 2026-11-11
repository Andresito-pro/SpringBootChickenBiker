package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.EstadosRecibos;
import com.chickenbikerv2.service.EstadosRecibosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estados-recibos")
public class EstadosRecibosController {

    private final EstadosRecibosService estadosRecibosService;

    public EstadosRecibosController(EstadosRecibosService estadosRecibosService) {
        this.estadosRecibosService = estadosRecibosService;
    }

    @GetMapping("/listar")
    public List<EstadosRecibos> listar() {
        return estadosRecibosService.listar();
    }

    @PostMapping("/guardar")
    public EstadosRecibos guardar(@RequestBody EstadosRecibos estadosRecibos) {
        return estadosRecibosService.guardar(estadosRecibos);
    }
}
