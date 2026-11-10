package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cajas")
@Getter @Setter
public class Cajas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caja")
    private int idCaja;

    @Column(name = "nombre_caja")
    private String nombreCaja;

    @Column(name = "descripcion_caja")
    private String descripcionCaja;

    @Column(name = "activo_caja")
    private int activoCaja;
}