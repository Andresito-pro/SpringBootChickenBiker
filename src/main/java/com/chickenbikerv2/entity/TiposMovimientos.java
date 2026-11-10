package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tipos_movimientos")
@Getter @Setter
public class TiposMovimientos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_movimiento")
    private int idTipoMovimiento;

    @Column(name = "nombre_tipo_movimiento")
    private String nombreTipoMovimiento;

    @Column(name = "descripcion_tipo_movimiento")
    private String descripcionTipoMovimiento;
}