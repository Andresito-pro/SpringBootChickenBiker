package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.CuerposRecibos;
import com.chickenbikerv2.service.CuerposRecibosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cuerpos-recibos")
public class CuerposRecibosController {

    private final CuerposRecibosService cuerposRecibosService;

    public CuerposRecibosController(CuerposRecibosService cuerposRecibosService) {
        this.cuerposRecibosService = cuerposRecibosService;
    }

    @GetMapping("/listar")
    public List<CuerposRecibos> listar() {
        return cuerposRecibosService.listar();
    }

    @PostMapping("/guardar")
    public CuerposRecibos guardar(@RequestBody CuerposRecibos cuerposRecibos) {
        return cuerposRecibosService.guardar(cuerposRecibos);
    }
}