package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tipos_documentos")
@Getter @Setter
public class TiposDocumentos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_documento")
    private int idTipoDocumento;

    @Column(name = "nombre_tipo_documento")
    private String nombreTipoDocumento;

    @Column(name = "descripcion_tipo_documento")
    private String descripcionTipoDocumento;
}