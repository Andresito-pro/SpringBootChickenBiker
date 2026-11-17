package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.AperturasCierresCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AperturasCierresCajaRepository extends JpaRepository<AperturasCierresCaja, Integer> {

    Optional<AperturasCierresCaja> findFirstByCajaIdCajaAndEstadoCierreCajaInOrderByIdAperturaCierreCajaDesc(
            int cajaIdCaja, Collection<String> estadosAbierta);

    boolean existsByCajaIdCajaAndFechaAperturaBetween(int cajaIdCaja, LocalDateTime desde, LocalDateTime hasta);

    List<AperturasCierresCaja> findAllByOrderByFechaAperturaDesc();
}
