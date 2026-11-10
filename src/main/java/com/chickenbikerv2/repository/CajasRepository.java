package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.Cajas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CajasRepository extends JpaRepository<Cajas, Integer> {
}