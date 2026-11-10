package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.MetodosPago;
import com.chickenbikerv2.repository.MetodosPagoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MetodosPagoService {

    private final MetodosPagoRepository metodosPagoRepository;

    public MetodosPagoService(MetodosPagoRepository metodosPagoRepository) {
        this.metodosPagoRepository = metodosPagoRepository;
    }

    public List<MetodosPago> listar() {
        return metodosPagoRepository.findAll();
    }

    public Optional<MetodosPago> buscarPorId(Integer id) {
        return metodosPagoRepository.findById(id);
    }

    public MetodosPago guardar(MetodosPago metodosPago) {
        return metodosPagoRepository.save(metodosPago);
    }

    public void eliminar(Integer id) {
        metodosPagoRepository.deleteById(id);
    }
}
