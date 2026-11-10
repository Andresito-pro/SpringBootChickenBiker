package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.CabecerasRecibos;
import com.chickenbikerv2.repository.CabecerasRecibosRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CabecerasRecibosService {

    private final CabecerasRecibosRepository cabecerasRecibosRepository;

    public CabecerasRecibosService(CabecerasRecibosRepository cabecerasRecibosRepository) {
        this.cabecerasRecibosRepository = cabecerasRecibosRepository;
    }

    public List<CabecerasRecibos> listar() {
        return cabecerasRecibosRepository.findAll();
    }

    public Optional<CabecerasRecibos> buscarPorId(Integer id) {
        return cabecerasRecibosRepository.findById(id);
    }

    public Optional<CabecerasRecibos> buscarPorNumeroRecibo(String numeroRecibo) {
        return cabecerasRecibosRepository.findByNumeroRecibo(numeroRecibo);
    }

    public CabecerasRecibos guardar(CabecerasRecibos cabecerasRecibos) {
        return cabecerasRecibosRepository.save(cabecerasRecibos);
    }

    public void eliminar(Integer id) {
        cabecerasRecibosRepository.deleteById(id);
    }
}
