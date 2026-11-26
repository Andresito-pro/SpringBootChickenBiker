package com.chickenbikerv2.controller.api;

import com.chickenbikerv2.entity.AperturasCierresCaja;
import com.chickenbikerv2.service.AperturasCierresCajaService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Fuente ÚNICA del estado de la caja para el indicador del topbar de todas las
 * vistas (js/topbar.js). Consulta siempre la base de datos (sin caché) y no
 * expone montos: solo si la caja 1 tiene un turno abierto. Disponible para
 * cualquier usuario autenticado (SecurityConfig).
 */
@RestController
public class CajaEstadoController {

    private final AperturasCierresCajaService aperturasCierresCajaService;

    public CajaEstadoController(AperturasCierresCajaService aperturasCierresCajaService) {
        this.aperturasCierresCajaService = aperturasCierresCajaService;
    }

    @GetMapping("/api/caja/estado")
    public ResponseEntity<Map<String, Object>> estado() {
        Optional<AperturasCierresCaja> abierta = aperturasCierresCajaService.consultarCajaAbierta();
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("abierta", abierta.isPresent());
        cuerpo.put("idTurno", abierta.map(AperturasCierresCaja::getIdAperturaCierreCaja).orElse(null));
        cuerpo.put("fechaApertura", abierta.map(AperturasCierresCaja::getFechaApertura).orElse(null));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }
}
