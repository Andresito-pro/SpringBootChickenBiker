package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permisos")
@Getter @Setter
public class Permisos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permisos")
    private int idPermisos;

    @Column(name = "nombre_permiso")
    private String nombrePermiso;

    @Column(name = "descripcion_permiso")
    private String descripcionPermiso;
}