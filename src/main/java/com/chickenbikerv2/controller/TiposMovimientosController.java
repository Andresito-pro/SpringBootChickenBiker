package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.TiposMovimientos;
import com.chickenbikerv2.service.TiposMovimientosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-movimientos")
public class TiposMovimientosController {

    private final TiposMovimientosService tiposMovimientosService;

    public TiposMovimientosController(TiposMovimientosService tiposMovimientosService) {
        this.tiposMovimientosService = tiposMovimientosService;
    }

    @GetMapping("/listar")
    public List<TiposMovimientos> listar() {
        return tiposMovimientosService.listar();
    }

    @PostMapping("/guardar")
    public TiposMovimientos guardar(@RequestBody TiposMovimientos tiposMovimientos) {
        return tiposMovimientosService.guardar(tiposMovimientos);
    }
}
