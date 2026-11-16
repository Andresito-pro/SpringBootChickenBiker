package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.Cajas;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CajasRepository extends JpaRepository<Cajas, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cajas c WHERE c.idCaja = :id")
    Optional<Cajas> bloquearPorId(@Param("id") Integer id);
}