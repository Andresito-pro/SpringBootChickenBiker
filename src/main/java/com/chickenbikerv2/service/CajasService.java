package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.Cajas;
import com.chickenbikerv2.repository.CajasRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CajasService {

    private final CajasRepository cajasRepository;

    public CajasService(CajasRepository cajasRepository) {
        this.cajasRepository = cajasRepository;
    }

    public List<Cajas> listar() {
        return cajasRepository.findAll();
    }

    public Optional<Cajas> buscarPorId(Integer id) {
        return cajasRepository.findById(id);
    }

    public Cajas guardar(Cajas cajas) {
        return cajasRepository.save(cajas);
    }

    public void eliminar(Integer id) {
        cajasRepository.deleteById(id);
    }
}