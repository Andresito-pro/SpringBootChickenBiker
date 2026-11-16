package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.AperturasCierresCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AperturasCierresCajaRepository extends JpaRepository<AperturasCierresCaja, Integer> {

    Optional<AperturasCierresCaja> findFirstByCajaIdCajaAndEstadoCierreCajaIgnoreCaseOrderByIdAperturaCierreCajaDesc(
            int cajaIdCaja, String estadoCierreCaja);

    Optional<AperturasCierresCaja> findFirstByEstadoCierreCajaIgnoreCaseOrderByFechaAperturaDesc(String estadoCierreCaja);
}