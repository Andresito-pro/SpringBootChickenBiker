package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.EstadosPedidos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadosPedidosRepository extends JpaRepository<EstadosPedidos, Integer> {

    /** Localiza un estado por nombre (p. ej. "Anulado", agregado por el script 04). */
    Optional<EstadosPedidos> findFirstByNombreEstadoPedidoIgnoreCase(String nombre);
}
