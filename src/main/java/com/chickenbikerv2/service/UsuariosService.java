package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.repository.UsuariosRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuariosService {

    private final UsuariosRepository usuariosRepository;

    public UsuariosService(UsuariosRepository usuariosRepository) {
        this.usuariosRepository = usuariosRepository;
    }

    public List<Usuarios> listar() {
        return usuariosRepository.findAll();
    }

    public Optional<Usuarios> buscarPorId(Integer id) {
        return usuariosRepository.findById(id);
    }

    public Optional<Usuarios> buscarPorCorreo(String correo) {
        return usuariosRepository.findByCorreoUsuario(correo);
    }

    public Optional<Usuarios> buscarPorDocumento(String documento) {
        return usuariosRepository.findByDocumentoUsuario(documento);
    }

    public Usuarios guardar(Usuarios usuarios) {
        return usuariosRepository.save(usuarios);
    }

    public void eliminar(Integer id) {
        usuariosRepository.deleteById(id);
    }
}