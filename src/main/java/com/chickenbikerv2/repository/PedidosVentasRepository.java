package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.PedidosVentas;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidosVentasRepository extends JpaRepository<PedidosVentas, Integer> {

    List<PedidosVentas> findByUsuarioIdUsuario(Integer usuarioId);

    List<PedidosVentas> findByEstadoPedidoIdEstado(Integer estadoId);

    List<PedidosVentas> findAllByOrderByFechaPedidoDesc();

    /** Pedidos registrados por un usuario (vista restringida de los meseros). */
    List<PedidosVentas> findByUsuarioIdUsuarioOrderByFechaPedidoDesc(int usuarioId);

    /** Consulta individual restringida: solo devuelve el pedido si lo registró ese usuario. */
    Optional<PedidosVentas> findByIdPedidoVentaAndUsuarioIdUsuario(int idPedidoVenta, int usuarioId);

    long countByFechaPedidoBetween(LocalDateTime desde, LocalDateTime hasta);

    /** Pedidos del periodo sin contar los anulados (indicadores de los dashboards). */
    long countByFechaPedidoBetweenAndEstadoPedidoIdEstadoNot(LocalDateTime desde, LocalDateTime hasta, int estadoExcluido);

    /** ¿La mesa tiene otro pedido pendiente aparte del indicado? (regla: mesa ocupada mientras haya pendientes). */
    boolean existsByMesaIdMesaAndEstadoPedidoIdEstadoAndIdPedidoVentaNot(Integer mesaIdMesa, int estado, int idPedidoVenta);

    /** Bloquea el pedido mientras se anula (evita anulaciones o cobros simultáneos). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PedidosVentas p WHERE p.idPedidoVenta = :id")
    Optional<PedidosVentas> bloquearPorId(@Param("id") Integer id);

    /** Pedidos registrados desde una fecha por usuarios distintos al indicado (notificaciones de la campana). */
    @Query("SELECT p FROM PedidosVentas p WHERE p.fechaPedido >= :desde AND p.usuarioIdUsuario <> :idUsuario "
            + "AND p.estadoPedidoIdEstado <> :estadoExcluido ORDER BY p.fechaPedido DESC, p.idPedidoVenta DESC")
    List<PedidosVentas> listarPedidosDeOtrosUsuariosDesde(@Param("desde") LocalDateTime desde,
                                                          @Param("idUsuario") int idUsuario,
                                                          @Param("estadoExcluido") int estadoExcluido);

    /** Pedidos en mesa cuyo estado no está entre los indicados (p. ej. sin Entregado ni Anulado), del más antiguo al más reciente. */
    @Query("SELECT p FROM PedidosVentas p WHERE p.mesaIdMesa IS NOT NULL "
            + "AND p.estadoPedidoIdEstado NOT IN :excluidos ORDER BY p.fechaPedido ASC, p.idPedidoVenta ASC")
    List<PedidosVentas> listarPedidosEnMesaExcluyendoEstados(@Param("excluidos") Collection<Integer> excluidos);

    /** Ids de los pedidos en el estado indicado (p. ej. Anulado, para marcar sus recibos). */
    @Query("SELECT p.idPedidoVenta FROM PedidosVentas p WHERE p.estadoPedidoIdEstado = :estado")
    List<Integer> idsPorEstado(@Param("estado") int idEstado);
}
