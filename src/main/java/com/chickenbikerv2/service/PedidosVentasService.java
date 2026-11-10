package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.PedidosVentas;
import com.chickenbikerv2.repository.PedidosVentasRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PedidosVentasService {

    private final PedidosVentasRepository pedidosVentasRepository;

    public PedidosVentasService(PedidosVentasRepository pedidosVentasRepository) {
        this.pedidosVentasRepository = pedidosVentasRepository;
    }

    public List<PedidosVentas> listar() {
        return pedidosVentasRepository.findAll();
    }

    public Optional<PedidosVentas> buscarPorId(Integer id) {
        return pedidosVentasRepository.findById(id);
    }

    public List<PedidosVentas> listarPorUsuario(Integer usuarioId) {
        return pedidosVentasRepository.findByUsuarioIdUsuario(usuarioId);
    }

    public List<PedidosVentas> listarPorEstado(Integer estadoId) {
        return pedidosVentasRepository.findByEstadoPedidoIdEstado(estadoId);
    }

    public PedidosVentas guardar(PedidosVentas pedidosVentas) {
        return pedidosVentasRepository.save(pedidosVentas);
    }

    public void eliminar(Integer id) {
        pedidosVentasRepository.deleteById(id);
    }
}
