package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.PedidosVentas;
import com.chickenbikerv2.service.PedidosVentasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos-ventas")
public class PedidosVentasController {

    private final PedidosVentasService pedidosVentasService;

    public PedidosVentasController(PedidosVentasService pedidosVentasService) {
        this.pedidosVentasService = pedidosVentasService;
    }

    @GetMapping("/listar")
    public List<PedidosVentas> listar() {
        return pedidosVentasService.listar();
    }

    @PostMapping("/guardar")
    public PedidosVentas guardar(@RequestBody PedidosVentas pedidosVentas) {
        return pedidosVentasService.guardar(pedidosVentas);
    }
}