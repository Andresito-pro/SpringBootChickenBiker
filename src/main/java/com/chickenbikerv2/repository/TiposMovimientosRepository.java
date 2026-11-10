package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.TiposMovimientos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TiposMovimientosRepository extends JpaRepository<TiposMovimientos, Integer> {
}