package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "productos")
@Getter @Setter
public class Productos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_productos")
    private int idProductos;

    @Column(name = "nombre_plato")
    private String nombrePlato;

    @Column(name = "descripcion_productos")
    private String descripcionProductos;

    @Column(name = "precio_venta")
    private BigDecimal precioVenta;

    @Column(name = "activo_producto")
    private int activoProducto;

    @Column(name = "categoria_producto_id_categoria_producto")
    private int categoriaProductoIdCategoriaProducto;
}