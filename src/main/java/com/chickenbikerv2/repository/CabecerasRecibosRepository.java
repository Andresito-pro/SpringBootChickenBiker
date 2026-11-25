package com.chickenbikerv2.repository;

import com.chickenbikerv2.dto.VentaDiariaDTO;
import com.chickenbikerv2.entity.CabecerasRecibos;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CabecerasRecibosRepository extends JpaRepository<CabecerasRecibos, Integer> {

    Optional<CabecerasRecibos> findByNumeroRecibo(String numeroRecibo);

    Optional<CabecerasRecibos> findByPedidoVentaIdPedidoVenta(int idPedidoVenta);

    List<CabecerasRecibos> findByEstadoReciboIdEstadoReciboOrderByFechaEmisionDesc(int idEstadoRecibo);

    long countByEstadoReciboIdEstadoRecibo(int idEstadoRecibo);

    List<CabecerasRecibos> findAllByOrderByFechaEmisionDesc();

    /**
     * Recibos en el estado indicado cuyo pedido NO está en estado Anulado.
     * La anulación solo cambia el estado del pedido; el recibo conserva su estado
     * real, por eso los recibos de pedidos anulados se excluyen aquí.
     */
    @Query("SELECT r FROM CabecerasRecibos r WHERE r.estadoReciboIdEstadoRecibo = :estado " +
            "AND NOT EXISTS (SELECT p FROM PedidosVentas p WHERE p.idPedidoVenta = r.pedidoVentaIdPedidoVenta " +
            "AND p.estadoPedidoIdEstado = :estadoPedidoAnulado) " +
            "ORDER BY r.fechaEmision DESC")
    List<CabecerasRecibos> listarPorEstadoSinPedidosAnulados(@Param("estado") int idEstadoRecibo,
                                                             @Param("estadoPedidoAnulado") int idEstadoPedidoAnulado);

    @Query("SELECT COUNT(r) FROM CabecerasRecibos r WHERE r.estadoReciboIdEstadoRecibo = :estado " +
            "AND NOT EXISTS (SELECT p FROM PedidosVentas p WHERE p.idPedidoVenta = r.pedidoVentaIdPedidoVenta " +
            "AND p.estadoPedidoIdEstado = :estadoPedidoAnulado)")
    long contarPorEstadoSinPedidosAnulados(@Param("estado") int idEstadoRecibo,
                                           @Param("estadoPedidoAnulado") int idEstadoPedidoAnulado);


    /**
     * Ventas pagadas agrupadas por día de emisión del recibo, en [desde, hastaExclusivo).
     * Solo cuenta recibos en el estado indicado (Pagado): excluye los pendientes
     * (Generado) y los recibos de pedidos en estado Anulado. Cada pedido tiene un único recibo y se suma su
     * total_general (no el detalle), así una venta nunca se cuenta dos veces.
     * Consulta de solo lectura: no modifica registros históricos.
     */
    @Query("SELECT new com.chickenbikerv2.dto.VentaDiariaDTO(" +
            "CAST(r.fechaEmision AS LocalDate), SUM(r.totalGeneral), COUNT(r)) " +
            "FROM CabecerasRecibos r " +
            "WHERE r.estadoReciboIdEstadoRecibo = :estado " +
            "AND r.fechaEmision >= :desde AND r.fechaEmision < :hastaExclusivo " +
            "AND NOT EXISTS (SELECT p FROM PedidosVentas p WHERE p.idPedidoVenta = r.pedidoVentaIdPedidoVenta " +
            "AND p.estadoPedidoIdEstado = :estadoPedidoAnulado) " +
            "GROUP BY CAST(r.fechaEmision AS LocalDate) " +
            "ORDER BY CAST(r.fechaEmision AS LocalDate)")
    List<VentaDiariaDTO> sumarVentasPorDia(@Param("estado") int idEstadoRecibo,
                                           @Param("desde") LocalDateTime desde,
                                           @Param("hastaExclusivo") LocalDateTime hastaExclusivo,
                                           @Param("estadoPedidoAnulado") int idEstadoPedidoAnulado);

    /** Bloquea la fila del recibo durante el cobro para evitar cobros dobles concurrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CabecerasRecibos r WHERE r.idCabeceraRecibo = :id")
    Optional<CabecerasRecibos> bloquearPorId(@Param("id") Integer id);
}
