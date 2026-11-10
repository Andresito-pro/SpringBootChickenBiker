package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.EstadosRecibos;
import com.chickenbikerv2.repository.EstadosRecibosRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EstadosRecibosService {

    private final EstadosRecibosRepository estadosRecibosRepository;

    public EstadosRecibosService(EstadosRecibosRepository estadosRecibosRepository) {
        this.estadosRecibosRepository = estadosRecibosRepository;
    }

    public List<EstadosRecibos> listar() {
        return estadosRecibosRepository.findAll();
    }

    public Optional<EstadosRecibos> buscarPorId(Integer id) {
        return estadosRecibosRepository.findById(id);
    }

    public EstadosRecibos guardar(EstadosRecibos estadosRecibos) {
        return estadosRecibosRepository.save(estadosRecibos);
    }

    public void eliminar(Integer id) {
        estadosRecibosRepository.deleteById(id);
    }
}