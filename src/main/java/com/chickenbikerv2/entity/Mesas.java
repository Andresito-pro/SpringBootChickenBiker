package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mesas")
@Getter @Setter
public class Mesas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mesa")
    private int idMesa;

    @Column(name = "numero_mesa")
    private int numeroMesa;

    @Column(name = "estado_mesa_id_estado_mesa")
    private int estadoMesaIdEstadoMesa;
}