package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "estados_usuarios")
@Getter @Setter
public class EstadosUsuarios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estados_usuarios")
    private int idEstadosUsuarios;

    @Column(name = "nombre_estado")
    private String nombreEstado;
}