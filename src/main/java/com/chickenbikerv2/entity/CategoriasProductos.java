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
    @Column(name = "idCategoriaProducto")
    private int idCategoriaProducto;

    @Column(name = "nombre_categoria")
    private String nombreCategoria;

    @Column(name = "descripcion_categoria")
    private String descripcionCategoria;
}
