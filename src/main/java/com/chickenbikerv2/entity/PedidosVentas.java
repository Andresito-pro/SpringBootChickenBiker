package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos_ventas")
@Getter @Setter
public class PedidosVentas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido_venta")
    private int idPedidoVenta;

    @Column(name = "fecha_pedido")
    private LocalDateTime fechaPedido;

    @Column(name = "tipo_pedido")
    private String tipoPedido;

    @Column(name = "total_pedido")
    private BigDecimal totalPedido;

    @Column(name = "usuario_id_usuario")
    private int usuarioIdUsuario;

    @Column(name = "mesa_id_mesa")
    private Integer mesaIdMesa;

    @Column(name = "estado_pedido_id_estado")
    private int estadoPedidoIdEstado;
}