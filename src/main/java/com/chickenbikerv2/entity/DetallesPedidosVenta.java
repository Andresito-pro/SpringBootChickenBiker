package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "detalles_pedidos_venta")
@Getter @Setter
public class DetallesPedidosVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_pedido_venta")
    private int idDetallePedidoVenta;

    @Column(name = "cantidad")
    private int cantidad;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "pedido_venta_id_pedido_venta")
    private int pedidoVentaIdPedidoVenta;

    @Column(name = "productos_id_productos")
    private int productosIdProductos;
}