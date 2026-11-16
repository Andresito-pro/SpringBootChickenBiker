package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.MovimientosCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientosCajaRepository extends JpaRepository<MovimientosCaja, Integer> {

    List<MovimientosCaja> findByAperturaCierreCajaIdAperturaCierreCaja(int idAperturaCierreCaja);

    @Query("SELECT COALESCE(SUM(m.valorMovimientoCaja), 0) " +
            "FROM MovimientosCaja m " +
            "WHERE m.aperturaCierreCajaIdAperturaCierreCaja = :idTurno " +
            "AND m.tipoMovimientoIdTipoMovimiento = :idTipo")
    Double sumarPorTurnoYTipo(@Param("idTurno") int idTurno, @Param("idTipo") int idTipo);
}