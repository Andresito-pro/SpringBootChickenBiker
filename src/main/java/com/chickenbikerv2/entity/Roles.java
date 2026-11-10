package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="roles")
@Getter
@Setter

public class Roles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_roles")
    private int idRoles;

    @Column(name="nombre_rol")
    private String nombreRol;

    @Column(name="descripcion_rol")
    private String descripcionRol;
}

