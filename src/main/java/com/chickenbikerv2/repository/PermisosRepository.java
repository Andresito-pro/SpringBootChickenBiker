package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.Permisos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermisosRepository extends JpaRepository<Permisos, Integer> {

    List<Permisos> findAllByOrderByNombrePermisoAsc();
}
