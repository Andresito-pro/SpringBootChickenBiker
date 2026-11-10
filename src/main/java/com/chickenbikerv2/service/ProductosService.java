package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.Productos;
import com.chickenbikerv2.repository.ProductosRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductosService {

    private final ProductosRepository productosRepository;

    public ProductosService(ProductosRepository productosRepository) {
        this.productosRepository = productosRepository;
    }

    public List<Productos> listar() {
        return productosRepository.findAll();
    }

    public Optional<Productos> buscarPorId(Integer id) {
        return productosRepository.findById(id);
    }

    public List<Productos> listarActivos(Byte activo) {
        return productosRepository.findByActivoProducto(activo);
    }

    public Productos guardar(Productos productos) {
        return productosRepository.save(productos);
    }

    public void eliminar(Integer id) {
        productosRepository.deleteById(id);
    }
}