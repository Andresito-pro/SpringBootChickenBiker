package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.EstadosUsuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadosUsuariosRepository extends JpaRepository<EstadosUsuarios, Integer> {
}