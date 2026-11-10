package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.EstadosMesas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadosMesasRepository extends JpaRepository<EstadosMesas, Integer> {
}