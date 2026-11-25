package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.PermisosRoles;
import com.chickenbikerv2.entity.PermisosRoles.PermisosRolesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermisosRolesRepository extends JpaRepository<PermisosRoles, PermisosRolesId> {

    /** Nombres de los permisos asignados a un rol (equivale a PermisosRolesDAO.listarNombresPermisosPorRol). */
    @Query("SELECT p.nombrePermiso FROM PermisosRoles pr, Permisos p "
            + "WHERE pr.permisosIdPermisos = p.idPermisos AND pr.rolesIdRoles = :idRol")
    List<String> listarNombresPermisosPorRol(@Param("idRol") int idRol);

    List<PermisosRoles> findByRolesIdRoles(int idRol);
}
