package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "aperturas_cierres_caja")
@Getter @Setter
public class AperturasCierresCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_apertura_cierre_caja")
    private int idAperturaCierreCaja;

    @Column(name = "fecha_apertura")
    private LocalDateTime fechaApertura;

    @Column(name = "monto_inicial")
    private BigDecimal montoInicial;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_final_sistema")
    private BigDecimal montoFinalSistema;

    @Column(name = "monto_final_real")
    private BigDecimal montoFinalReal;

    @Column(name = "diferencia")
    private BigDecimal diferencia;

    @Column(name = "estado_cierre_caja")
    private String estadoCierreCaja;

    @Column(name = "caja_id_caja")
    private int cajaIdCaja;

    @Column(name = "usuario_id_usuario")
    private int usuarioIdUsuario;

    @Column(name = "usuario_id_usuario1")
    private int usuarioIdUsuario1;
}