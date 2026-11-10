package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.EstadosPedidos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadosPedidosRepository extends JpaRepository<EstadosPedidos, Integer> {
}