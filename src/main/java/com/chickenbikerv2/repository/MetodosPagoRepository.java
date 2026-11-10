package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.MetodosPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetodosPagoRepository extends JpaRepository<MetodosPago, Integer> {
}