package com.chickenbikerv2.controller.api;

import com.chickenbikerv2.dto.VentasPeriodoDTO;
import com.chickenbikerv2.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Set<Integer> DIAS_PERMITIDOS = Set.of(7, 30, 90);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/ventas-diarias")
    public ResponseEntity<?> ventasDiarias(
            @RequestParam(value = "dias", required = false) Integer dias,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        try {
            VentasPeriodoDTO ventas;
            if (desde != null || hasta != null) {
                ventas = dashboardService.ventasDiarias(desde, hasta);
            } else {
                int n = dias == null ? 7 : dias;
                if (!DIAS_PERMITIDOS.contains(n)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Periodo no válido. Usa 7, 30 o 90 días."));
                }
                ventas = dashboardService.ventasDiarias(hoy.minusDays(n - 1L), hoy);
            }
            return ResponseEntity.ok(ventas);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
