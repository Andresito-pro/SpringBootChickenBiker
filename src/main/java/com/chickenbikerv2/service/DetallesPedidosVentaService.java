package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.DetallesPedidosVenta;
import com.chickenbikerv2.repository.DetallesPedidosVentaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DetallesPedidosVentaService {

    private final DetallesPedidosVentaRepository detallesPedidosVentaRepository;

    public DetallesPedidosVentaService(DetallesPedidosVentaRepository detallesPedidosVentaRepository) {
        this.detallesPedidosVentaRepository = detallesPedidosVentaRepository;
    }

    public List<DetallesPedidosVenta> listar() {
        return detallesPedidosVentaRepository.findAll();
    }

    public Optional<DetallesPedidosVenta> buscarPorId(Integer id) {
        return detallesPedidosVentaRepository.findById(id);
    }

    public List<DetallesPedidosVenta> listarPorPedido(Integer idPedidoVenta) {
        return detallesPedidosVentaRepository.findByPedidoVentaIdPedidoVenta(idPedidoVenta);
    }

    public DetallesPedidosVenta guardar(DetallesPedidosVenta detallesPedidosVenta) {
        return detallesPedidosVentaRepository.save(detallesPedidosVenta);
    }

    public void eliminar(Integer id) {
        detallesPedidosVentaRepository.deleteById(id);
    }
}
