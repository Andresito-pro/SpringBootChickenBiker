package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.EstadosMesas;
import com.chickenbikerv2.repository.EstadosMesasRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EstadosMesasService {

    private final EstadosMesasRepository estadosMesasRepository;

    public EstadosMesasService(EstadosMesasRepository estadosMesasRepository) {
        this.estadosMesasRepository = estadosMesasRepository;
    }

    public List<EstadosMesas> listar() {
        return estadosMesasRepository.findAll();
    }

    public Optional<EstadosMesas> buscarPorId(Integer id) {
        return estadosMesasRepository.findById(id);
    }

    public EstadosMesas guardar(EstadosMesas estadosMesas) {
        return estadosMesasRepository.save(estadosMesas);
    }

    public void eliminar(Integer id) {
        estadosMesasRepository.deleteById(id);
    }
}