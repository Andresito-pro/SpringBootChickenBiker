package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.CuerposRecibos;
import com.chickenbikerv2.repository.CuerposRecibosRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CuerposRecibosService {

    private final CuerposRecibosRepository cuerposRecibosRepository;

    public CuerposRecibosService(CuerposRecibosRepository cuerposRecibosRepository) {
        this.cuerposRecibosRepository = cuerposRecibosRepository;
    }

    public List<CuerposRecibos> listar() {
        return cuerposRecibosRepository.findAll();
    }

    public Optional<CuerposRecibos> buscarPorId(Integer id) {
        return cuerposRecibosRepository.findById(id);
    }

    public List<CuerposRecibos> listarPorCabecera(Integer idCabeceraRecibo) {
        return cuerposRecibosRepository.findByCabeceraReciboIdCabeceraRecibo(idCabeceraRecibo);
    }

    public CuerposRecibos guardar(CuerposRecibos cuerposRecibos) {
        return cuerposRecibosRepository.save(cuerposRecibos);
    }

    public void eliminar(Integer id) {
        cuerposRecibosRepository.deleteById(id);
    }
}