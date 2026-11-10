package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.EstadosUsuarios;
import com.chickenbikerv2.repository.EstadosUsuariosRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EstadosUsuariosService {

    private final EstadosUsuariosRepository estadosUsuariosRepository;

    public EstadosUsuariosService(EstadosUsuariosRepository estadosUsuariosRepository) {
        this.estadosUsuariosRepository = estadosUsuariosRepository;
    }

    public List<EstadosUsuarios> listar() {
        return estadosUsuariosRepository.findAll();
    }

    public Optional<EstadosUsuarios> buscarPorId(Integer id) {
        return estadosUsuariosRepository.findById(id);
    }

    public EstadosUsuarios guardar(EstadosUsuarios estadosUsuarios) {
        return estadosUsuariosRepository.save(estadosUsuarios);
    }

    public void eliminar(Integer id) {
        estadosUsuariosRepository.deleteById(id);
    }
}