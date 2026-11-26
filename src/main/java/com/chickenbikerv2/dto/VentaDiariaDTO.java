package com.chickenbikerv2.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ventas pagadas de un día: suma de total_general de los recibos en estado
 * "Pagado" y cuántos recibos la componen. Lo construye directamente la
 * consulta JPQL de CabecerasRecibosRepository (agrupada en la base de datos;
 * COUNT devuelve Long, por eso cantidadRecibos es Long).
 */
public record VentaDiariaDTO(LocalDate fecha, BigDecimal total, Long cantidadRecibos) {

    public static VentaDiariaDTO sinVentas(LocalDate fecha) {
        return new VentaDiariaDTO(fecha, BigDecimal.ZERO, 0L);
    }
}
