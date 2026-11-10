package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.EstadosPedidos;
import com.chickenbikerv2.repository.EstadosPedidosRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EstadosPedidosService {

    private final EstadosPedidosRepository estadosPedidosRepository;

    public EstadosPedidosService(EstadosPedidosRepository estadosPedidosRepository) {
        this.estadosPedidosRepository = estadosPedidosRepository;
    }

    public List<EstadosPedidos> listar() {
        return estadosPedidosRepository.findAll();
    }

    public Optional<EstadosPedidos> buscarPorId(Integer id) {
        return estadosPedidosRepository.findById(id);
    }

    public EstadosPedidos guardar(EstadosPedidos estadosPedidos) {
        return meGuardar(estadosPedidos);
    }

    private EstadosPedidos meGuardar(EstadosPedidos estadosPedidos) {
        return estadosPedidosRepository.save(estadosPedidos);
    }

    public void eliminar(Integer id) {
        estadosPedidosRepository.deleteById(id);
    }
}