package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.CuerposRecibos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CuerposRecibosRepository extends JpaRepository<CuerposRecibos, Integer> {

    List<CuerposRecibos> findByCabeceraReciboIdCabeceraRecibo(Integer idCabeceraRecibo);
}