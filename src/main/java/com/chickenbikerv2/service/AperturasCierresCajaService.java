package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.AperturasCierresCaja;
import com.chickenbikerv2.repository.AperturasCierresCajaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AperturasCierresCajaService {

    private final AperturasCierresCajaRepository aperturasCierresCajaRepository;

    public AperturasCierresCajaService(AperturasCierresCajaRepository aperturasCierresCajaRepository) {
        this.aperturasCierresCajaRepository = aperturasCierresCajaRepository;
    }

    public List<AperturasCierresCaja> listar() {
        return aperturasCierresCajaRepository.findAll();
    }

    public Optional<AperturasCierresCaja> buscarPorId(Integer id) {
        return aperturasCierresCajaRepository.findById(id);
    }

    public List<AperturasCierresCaja> listarPorCaja(Integer cajaId) {
        return aperturasCierresCajaRepository.findByCajaIdCaja(cajaId);
    }

    public AperturasCierresCaja guardar(AperturasCierresCaja aperturasCierresCaja) {
        return aperturasCierresCajaRepository.save(aperturasCierresCaja);
    }

    public void eliminar(Integer id) {
        aperturasCierresCajaRepository.deleteById(id);
    }
}