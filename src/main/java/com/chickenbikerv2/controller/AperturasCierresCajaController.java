package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.AperturasCierresCaja;
import com.chickenbikerv2.service.AperturasCierresCajaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/aperturas-cierres-caja")
public class AperturasCierresCajaController {

    private final AperturasCierresCajaService aperturasCierresCajaService;

    public AperturasCierresCajaController(AperturasCierresCajaService aperturasCierresCajaService) {
        this.aperturasCierresCajaService = aperturasCierresCajaService;
    }

    @GetMapping("/listar")
    public List<AperturasCierresCaja> listar() {
        return aperturasCierresCajaService.listar();
    }

    @PostMapping("/guardar")
    public AperturasCierresCaja guardar(@RequestBody AperturasCierresCaja aperturasCierresCaja) {
        return aperturasCierresCajaService.guardar(aperturasCierresCaja);
    }
}