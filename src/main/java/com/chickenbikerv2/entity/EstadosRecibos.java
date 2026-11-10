package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "estados_recibos")
@Getter @Setter
public class EstadosRecibos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_recibo")
    private int idEstadoRecibo;

    @Column(name = "nombre_estado_recibo")
    private String nombreEstadoRecibo;

    @Column(name = "descripcion_estado_recibo")
    private String descripcionEstadoRecibo;

    @Column(name = "activo_estado_recibo")
    private int activoEstadoRecibo;
}