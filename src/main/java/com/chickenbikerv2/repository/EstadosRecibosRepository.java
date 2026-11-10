package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.EstadosRecibos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadosRecibosRepository extends JpaRepository<EstadosRecibos, Integer> {
}