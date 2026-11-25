package com.chickenbikerv2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "metodos_pago")
@Getter @Setter
public class MetodosPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_metodo_pago")
    private int idMetodoPago;

    @Column(name = "nombre_metodo")
    private String nombreMetodo;

    @Column(name = "activo_metodo")
    private Byte activoMetodo;

    @Column(name = "tipo_metodo", length = 20)
    private String tipoMetodo;

    @PrePersist
    @PreUpdate
    void asegurarTipoMetodo() {
        if (tipoMetodo == null || tipoMetodo.isBlank()) {
            tipoMetodo = TipoMetodoPago.clasificarPorNombre(nombreMetodo).name();
        }
    }

    /** Tipo efectivo del método: la columna si es válida; si no, se deduce del nombre. */
    public TipoMetodoPago tipo() {
        return TipoMetodoPago.desde(tipoMetodo, nombreMetodo);
    }
}