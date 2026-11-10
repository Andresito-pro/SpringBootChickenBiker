package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.Mesas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MesasRepository extends JpaRepository<Mesas, Integer> {

    Optional<Mesas> findByNumeroMesa(Integer numeroMesa);
}