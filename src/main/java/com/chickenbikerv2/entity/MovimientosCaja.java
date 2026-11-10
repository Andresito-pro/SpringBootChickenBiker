package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "movimientos_caja")
@Getter @Setter
public class MovimientosCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento_caja")
    private int idMovimientoCaja;

    @Column(name = "valor_movimiento_caja")
    private float valorMovimientoCaja;

    @Column(name = "descripcion_moviento_caja")
    private String descripcionMovientoCaja;

    @Column(name = "fecha_movimiento_caja")
    private LocalDate fechaMovimientoCaja;

    @Column(name = "tipo_movimiento_id_tipo_movimiento")
    private int tipoMovimientoIdTipoMovimiento;

    @Column(name = "usuario_id_usuario")
    private int usuarioIdUsuario;

    @Column(name = "apertura_cierre_caja_id_apertura_cierre_caja")
    private int aperturaCierreCajaIdAperturaCierreCaja;
}