package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.CategoriasProductos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriasProductosRepository extends JpaRepository<CategoriasProductos, Integer> {
}