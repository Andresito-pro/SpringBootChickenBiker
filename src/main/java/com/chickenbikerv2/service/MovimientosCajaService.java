package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.MovimientosCaja;
import com.chickenbikerv2.repository.MovimientosCajaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class MovimientosCajaService {

    public static final int TIPO_MOV_INGRESO = 1;
    public static final int TIPO_MOV_EGRESO = 2;

    private final MovimientosCajaRepository movimientosCajaRepository;

    public MovimientosCajaService(MovimientosCajaRepository movimientosCajaRepository) {
        this.movimientosCajaRepository = movimientosCajaRepository;
    }

    public List<MovimientosCaja> listar() {
        return movimientosCajaRepository.findAll();
    }

    public List<MovimientosCaja> listarPorTurno(int idAperturaCierreCaja) {
        return movimientosCajaRepository.findByAperturaCierreCajaIdAperturaCierreCaja(idAperturaCierreCaja);
    }

    public Optional<MovimientosCaja> buscarPorId(Integer id) {
        return movimientosCajaRepository.findById(id);
    }

    public BigDecimal sumarIngresosPorTurno(int idAperturaCierreCaja) {
        return sumarPorTurnoYTipo(idAperturaCierreCaja, TIPO_MOV_INGRESO);
    }

    public BigDecimal sumarEgresosPorTurno(int idAperturaCierreCaja) {
        return sumarPorTurnoYTipo(idAperturaCierreCaja, TIPO_MOV_EGRESO);
    }

    public BigDecimal sumarPorTurnoYTipo(int idAperturaCierreCaja, int idTipoMovimiento) {
        Double suma = movimientosCajaRepository.sumarPorTurnoYTipo(idAperturaCierreCaja, idTipoMovimiento);
        return BigDecimal.valueOf(suma != null ? suma : 0.0).setScale(2, RoundingMode.HALF_UP);
    }

    public MovimientosCaja guardar(MovimientosCaja movimiento) {
        return movimientosCajaRepository.save(movimiento);
    }

    public void eliminar(Integer id) {
        movimientosCajaRepository.deleteById(id);
    }
}