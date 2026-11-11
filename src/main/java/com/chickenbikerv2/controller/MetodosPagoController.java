package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.MetodosPago;
import com.chickenbikerv2.service.MetodosPagoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/metodos-pago")
public class MetodosPagoController {

    private final MetodosPagoService metodosPagoService;

    public MetodosPagoController(MetodosPagoService metodosPagoService) {
        this.metodosPagoService = metodosPagoService;
    }

    @GetMapping("/listar")
    public List<MetodosPago> listar() {
        return metodosPagoService.listar();
    }

    @PostMapping("/guardar")
    public MetodosPago guardar(@RequestBody MetodosPago metodosPago) {
        return metodosPagoService.guardar(metodosPago);
    }
}
