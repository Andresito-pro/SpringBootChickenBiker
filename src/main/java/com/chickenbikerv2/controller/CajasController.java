package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.Cajas;
import com.chickenbikerv2.service.CajasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cajas")
public class CajasController {

    private final CajasService cajasService;

    public CajasController(CajasService cajasService) {
        this.cajasService = cajasService;
    }

    @GetMapping("/listar")
    public List<Cajas> listar() {
        return cajasService.listar();
    }

    @PostMapping("/guardar")
    public Cajas guardar(@RequestBody Cajas cajas) {
        return cajasService.guardar(cajas);
    }
}