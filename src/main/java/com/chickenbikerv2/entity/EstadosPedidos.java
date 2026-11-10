package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "estados_pedidos")
@Getter @Setter
public class EstadosPedidos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private int idEstado;

    @Column(name = "nombre_estado_pedido")
    private String nombreEstadoPedido;

    @Column(name = "descripcion_estado_pedido")
    private String descripcionEstadoPedido;

    @Column(name = "activo_estado_pedido")
    private int activoEstadoPedido;
}