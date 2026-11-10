package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Entity
@Table(name = "permisos_roles")
@IdClass(PermisosRoles.PermisosRolesId.class)
@Getter @Setter
public class PermisosRoles {

    @Id
    @Column(name = "permisos_id_permisos")
    private int permisosIdPermisos;

    @Id
    @Column(name = "roles_id_roles")
    private int rolesIdRoles;

    @Getter @Setter
    @EqualsAndHashCode
    public static class PermisosRolesId implements Serializable {
        private int permisosIdPermisos;
        private int rolesIdRoles;
    }
}