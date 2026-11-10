package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.CategoriasProductos;
import com.chickenbikerv2.repository.CategoriasProductosRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriasProductosService {

    private final CategoriasProductosRepository categoriasProductosRepository;

    public CategoriasProductosService(CategoriasProductosRepository categoriasProductosRepository) {
        this.categoriasProductosRepository = categoriasProductosRepository;
    }

    public List<CategoriasProductos> listar() {
        return categoriasProductosRepository.findAll();
    }

    public Optional<CategoriasProductos> buscarPorId(Integer id) {
        return categoriasProductosRepository.findById(id);
    }

    public CategoriasProductos guardar(CategoriasProductos categoriasProductos) {
        return categoriasProductosRepository.save(categoriasProductos);
    }

    public void eliminar(Integer id) {
        categoriasProductosRepository.deleteById(id);
    }
}