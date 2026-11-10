package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "estados_mesas")
@Getter @Setter
public class EstadosMesas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_mesa")
    private int idEstadoMesa;

    @Column(name = "nombre_estado_mesa")
    private String nombreEstadoMesa;

    @Column(name = "descripcion_estado_mesa")
    private String descripcionEstadoMesa;

    @Column(name = "activo_estado_mesa")
    private int activoEstadoMesa;
}