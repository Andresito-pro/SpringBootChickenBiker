package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.CabecerasRecibos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CabecerasRecibosRepository extends JpaRepository<CabecerasRecibos, Integer> {

    Optional<CabecerasRecibos> findByNumeroRecibo(String numeroRecibo);
}