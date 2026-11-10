package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.DetallesPedidosVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetallesPedidosVentaRepository extends JpaRepository<DetallesPedidosVenta, Integer> {

    List<DetallesPedidosVenta> findByPedidoVentaIdPedidoVenta(Integer idPedidoVenta);
}