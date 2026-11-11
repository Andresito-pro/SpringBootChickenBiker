package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.MovimientosCaja;
import com.chickenbikerv2.service.MovimientosCajaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movimientos-caja")
public class MovimientosCajaController {

    private final MovimientosCajaService movimientosCajaService;

    public MovimientosCajaController(MovimientosCajaService movimientosCajaService) {
        this.movimientosCajaService = movimientosCajaService;
    }

    @GetMapping("/listar")
    public List<MovimientosCaja> listar() {
        return movimientosCajaService.listar();
    }

    @PostMapping("/guardar")
    public MovimientosCaja guardar(@RequestBody MovimientosCaja movimientosCaja) {
        return movimientosCajaService.guardar(movimientosCaja);
    }
}
