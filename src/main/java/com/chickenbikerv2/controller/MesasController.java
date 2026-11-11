package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.Mesas;
import com.chickenbikerv2.service.MesasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mesas")
public class MesasController {

    private final MesasService mesasService;

    public MesasController(MesasService mesasService) {
        this.mesasService = mesasService;
    }

    @GetMapping("/listar")
    public List<Mesas> listar() {
        return mesasService.listar();
    }

    @PostMapping("/guardar")
    public Mesas guardar(@RequestBody Mesas mesas) {
        return mesasService.guardar(mesas);
    }
}
