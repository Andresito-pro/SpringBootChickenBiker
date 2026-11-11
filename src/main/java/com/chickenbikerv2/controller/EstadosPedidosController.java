package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.EstadosPedidos;
import com.chickenbikerv2.service.EstadosPedidosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estados-pedidos")
public class EstadosPedidosController {

    private final EstadosPedidosService estadosPedidosService;

    public EstadosPedidosController(EstadosPedidosService estadosPedidosService) {
        this.estadosPedidosService = estadosPedidosService;
    }

    @GetMapping("/listar")
    public List<EstadosPedidos> listar() {
        return estadosPedidosService.listar();
    }

    @PostMapping("/guardar")
    public EstadosPedidos guardar(@RequestBody EstadosPedidos estadosPedidos) {
        return estadosPedidosService.guardar(estadosPedidos);
    }
}