package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.AperturasCierresCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AperturasCierresCajaRepository extends JpaRepository<AperturasCierresCaja, Integer> {

    List<AperturasCierresCaja> findByCajaIdCaja(Integer cajaId);
}