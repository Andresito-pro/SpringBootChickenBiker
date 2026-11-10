package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.Permisos;
import com.chickenbikerv2.repository.PermisosRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PermisosService {

    private final PermisosRepository permisosRepository;

    public PermisosService(PermisosRepository permisosRepository) {
        this.permisosRepository = permisosRepository;
    }

    public List<Permisos> listar() {
        return permisosRepository.findAll();
    }

    public Optional<Permisos> buscarPorId(Integer id) {
        return permisosRepository.findById(id);
    }

    public Permisos guardar(Permisos permisos) {
        return permisosRepository.save(permisos);
    }

    public void eliminar(Integer id) {
        permisosRepository.deleteById(id);
    }
}