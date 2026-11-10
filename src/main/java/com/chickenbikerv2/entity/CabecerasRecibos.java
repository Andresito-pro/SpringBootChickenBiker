package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cabeceras_recibos")
@Getter @Setter
public class CabecerasRecibos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cabecera_recibo")
    private int idCabeceraRecibo;

    @Column(name = "numero_recibo")
    private String numeroRecibo;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    @Column(name = "total_general")
    private BigDecimal totalGeneral;

    @Column(name = "pedido_venta_id_pedido_venta")
    private int pedidoVentaIdPedidoVenta;

    @Column(name = "estado_recibo_id_estado_recibo")
    private int estadoReciboIdEstadoRecibo;

    @Column(name = "metodo_pago_id_metodo_pago")
    private int metodoPagoIdMetodoPago;

    @Column(name = "apertura_cierre_caja_id_apertura_cierre_caja")
    private int aperturaCierreCajaIdAperturaCierreCaja;
}