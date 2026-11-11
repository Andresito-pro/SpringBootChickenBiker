package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.TiposDocumentos;
import com.chickenbikerv2.service.TiposDocumentosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-documentos")
public class TiposDocumentosController {

    private final TiposDocumentosService tiposDocumentosService;

    public TiposDocumentosController(TiposDocumentosService tiposDocumentosService) {
        this.tiposDocumentosService = tiposDocumentosService;
    }

    @GetMapping("/listar")
    public List<TiposDocumentos> listar() {
        return tiposDocumentosService.listar();
    }

    @PostMapping("/guardar")
    public TiposDocumentos guardar(@RequestBody TiposDocumentos tiposDocumentos) {
        return tiposDocumentosService.guardar(tiposDocumentos);
    }
}