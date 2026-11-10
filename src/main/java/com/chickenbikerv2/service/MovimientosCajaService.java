package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.MovimientosCaja;
import com.chickenbikerv2.repository.MovimientosCajaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MovimientosCajaService {

    private final MovimientosCajaRepository movimientosCajaRepository;

    public MovimientosCajaService(MovimientosCajaRepository movimientosCajaRepository) {
        this.movimientosCajaRepository = movimientosCajaRepository;
    }

    public List<MovimientosCaja> listar() {
        return movimientosCajaRepository.findAll();
    }

    public Optional<MovimientosCaja> buscarPorId(Integer id) {
        return movimientosCajaRepository.findById(id);
    }

    public List<MovimientosCaja> listarPorAperturaCierre(Integer idAperturaCierre) {
        return movimientosCajaRepository.findByAperturaCierreCajaIdAperturaCierreCaja(idAperturaCierre);
    }

    public MovimientosCaja guardar(MovimientosCaja movimientosCaja) {
        return movimientosCajaRepository.save(movimientosCaja);
    }

    public void eliminar(Integer id) {
        movimientosCajaRepository.deleteById(id);
    }
}
