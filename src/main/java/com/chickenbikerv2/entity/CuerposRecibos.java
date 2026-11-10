package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "cuerpos_recibos")
@Getter @Setter
public class CuerposRecibos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuerpo_recibo")
    private int idCuerpoRecibo;

    @Column(name = "cantidad_")
    private int cantidad;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Column(name = "sub_total")
    private BigDecimal subTotal;

    @Column(name = "productos_id_productos")
    private int productosIdProductos;

    @Column(name = "cabecera_recibo_id_cabecera_recibo")
    private int cabeceraReciboIdCabeceraRecibo;
}