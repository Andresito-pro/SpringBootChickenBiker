package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.Productos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductosRepository extends JpaRepository<Productos, Integer> {

    List<Productos> findByActivoProducto(Byte activoProducto);
}