package com.chickenbikerv2.controller;

import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.service.UsuariosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {

    private final UsuariosService usuariosService;

    public UsuariosController(UsuariosService usuariosService) {
        this.usuariosService = usuariosService;
    }

    @GetMapping("/listar")
    public List<Usuarios> listar() {
        return usuariosService.listar();
    }

    @PostMapping("/guardar")
    public Usuarios guardar(@RequestBody Usuarios usuarios) {
        return usuariosService.guardar(usuarios);
    }
}
