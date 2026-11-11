package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.EstadosUsuarios;
import com.chickenbikerv2.service.EstadosUsuariosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estados-usuarios")
public class EstadosUsuariosController {

    private final EstadosUsuariosService estadosUsuariosService;

    public EstadosUsuariosController(EstadosUsuariosService estadosUsuariosService) {
        this.estadosUsuariosService = estadosUsuariosService;
    }

    @GetMapping("/listar")
    public List<EstadosUsuarios> listar() {
        return estadosUsuariosService.listar();
    }

    @PostMapping("/guardar")
    public EstadosUsuarios guardar(@RequestBody EstadosUsuarios estadosUsuarios) {
        return estadosUsuariosService.guardar(estadosUsuarios);
    }
}