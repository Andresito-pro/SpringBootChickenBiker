package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="categorias_productos")
@Getter @Setter

public class CategoriasProductos {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria_producto")
    private int idCategoriaProducto;

    @Column(name = "nombre_categoria")
    private String nombreCategoria;

    // Columna NOT NULL existente en la tabla (la usaba el DAO original).
    @Column(name = "descripcion_categoria")
    private String descripcionCategoria;

}
