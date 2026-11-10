package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.PermisosRoles;
import com.chickenbikerv2.entity.PermisosRoles.PermisosRolesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermisosRolesRepository extends JpaRepository<PermisosRoles, PermisosRolesId> {
}
