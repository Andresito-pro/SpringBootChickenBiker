package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.Mesas;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MesasRepository extends JpaRepository<Mesas, Integer> {

    Optional<Mesas> findByNumeroMesa(Integer numeroMesa);

    List<Mesas> findAllByOrderByNumeroMesaAsc();

    long countByEstadoMesaIdEstadoMesa(int idEstadoMesa);

    /** Bloquea la mesa mientras se ocupa o se libera (dos meseros no toman la misma mesa). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Mesas m WHERE m.idMesa = :id")
    Optional<Mesas> bloquearPorId(@Param("id") Integer id);
}
