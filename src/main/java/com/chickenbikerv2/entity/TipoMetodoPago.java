package com.chickenbikerv2.entity;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Clasificación de los medios de pago para el cuadre de caja.
 * Solo EFECTIVO es dinero físico que entra al cajón; TARJETA y DIGITAL
 * (Nequi, Daviplata, transferencias...) se registran pero no forman parte
 * del efectivo esperado.
 */
public enum TipoMetodoPago {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    DIGITAL("Plataformas digitales");

    private final String etiqueta;

    TipoMetodoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean esEfectivo() {
        return this == EFECTIVO;
    }

    /** Usa el valor de la columna tipo_metodo; si falta o no es válido, lo deduce del nombre. */
    public static TipoMetodoPago desde(String valorColumna, String nombreMetodo) {
        if (valorColumna != null) {
            try {
                return valueOf(valorColumna.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignorado) {
                // valor desconocido: se clasifica por nombre
            }
        }
        return clasificarPorNombre(nombreMetodo);
    }

    /** Clasificación por nombre (efectivo / tarjeta / resto digital) para filas sin tipo_metodo. */
    public static TipoMetodoPago clasificarPorNombre(String nombre) {
        String n = nombre == null ? "" : Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (n.contains("efectivo")) {
            return EFECTIVO;
        }
        if (n.contains("tarjeta") || n.contains("datafono") || n.contains("credito") || n.contains("debito")) {
            return TARJETA;
        }
        return DIGITAL;
    }
}
