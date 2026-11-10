package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.MovimientosCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MovimientosCajaRepository extends JpaRepository<MovimientosCaja, Integer> {

    List<MovimientosCaja> findByAperturaCierreCajaIdAperturaCierreCaja(Integer idAperturaCierre);
}
