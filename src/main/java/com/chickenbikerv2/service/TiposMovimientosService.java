package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.TiposMovimientos;
import com.chickenbikerv2.repository.TiposMovimientosRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TiposMovimientosService {

    private final TiposMovimientosRepository tiposMovimientosRepository;

    public TiposMovimientosService(TiposMovimientosRepository tiposMovimientosRepository) {
        this.tiposMovimientosRepository = tiposMovimientosRepository;
    }

    public List<TiposMovimientos> listar() {
        return tiposMovimientosRepository.findAll();
    }

    public Optional<TiposMovimientos> buscarPorId(Integer id) {
        return tiposMovimientosRepository.findById(id);
    }

    public TiposMovimientos guardar(TiposMovimientos tiposMovimientos) {
        return tiposMovimientosRepository.save(tiposMovimientos);
    }

    public void eliminar(Integer id) {
        tiposMovimientosRepository.deleteById(id);
    }
}
