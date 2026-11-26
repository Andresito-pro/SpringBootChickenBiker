package com.chickenbikerv2.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta de GET /api/dashboard/ventas-diarias: un elemento por cada día
 * del rango [desde, hasta] (los días sin ventas llegan con total 0) y los
 * totales del periodo. Los montos están en pesos colombianos (COP).
 */
public record VentasPeriodoDTO(
        LocalDate desde,
        LocalDate hasta,
        String moneda,
        BigDecimal totalPeriodo,
        long cantidadRecibos,
        List<VentaDiariaDTO> dias) {
}
