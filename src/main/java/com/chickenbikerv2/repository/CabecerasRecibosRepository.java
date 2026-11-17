package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.CabecerasRecibos;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CabecerasRecibosRepository extends JpaRepository<CabecerasRecibos, Integer> {

    Optional<CabecerasRecibos> findByNumeroRecibo(String numeroRecibo);

    Optional<CabecerasRecibos> findByPedidoVentaIdPedidoVenta(int idPedidoVenta);

    /** Recibos según su estado (1 = Generado / pendiente de cobro, 2 = Pagado, 3 = Anulado). */
    List<CabecerasRecibos> findByEstadoReciboIdEstadoReciboOrderByFechaEmisionDesc(int idEstadoRecibo);

    long countByEstadoReciboIdEstadoRecibo(int idEstadoRecibo);

    List<CabecerasRecibos> findAllByOrderByFechaEmisionDesc();

    /** Bloquea la fila del recibo durante el cobro para evitar cobros dobles concurrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CabecerasRecibos r WHERE r.idCabeceraRecibo = :id")
    Optional<CabecerasRecibos> bloquearPorId(@Param("id") Integer id);
}
