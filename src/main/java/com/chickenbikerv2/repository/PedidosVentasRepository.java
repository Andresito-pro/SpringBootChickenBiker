package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.PedidosVentas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidosVentasRepository extends JpaRepository<PedidosVentas, Integer> {

    List<PedidosVentas> findByUsuarioIdUsuario(Integer usuarioId);
    List<PedidosVentas> findByEstadoPedidoIdEstado(Integer estadoId);
}