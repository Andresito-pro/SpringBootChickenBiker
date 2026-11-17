package com.chickenbikerv2.controller.api;

import com.chickenbikerv2.entity.AperturasCierresCaja;
import com.chickenbikerv2.service.AperturasCierresCajaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aperturas-cierres-caja")
public class AperturasCierresCajaController {

    private final AperturasCierresCajaService aperturasCierresCajaService;

    public AperturasCierresCajaController(AperturasCierresCajaService aperturasCierresCajaService) {
        this.aperturasCierresCajaService = aperturasCierresCajaService;
    }

    @GetMapping("/listar")
    public List<AperturasCierresCaja> listar() {
        return aperturasCierresCajaService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AperturasCierresCaja> buscarPorId(@PathVariable Integer id) {
        return aperturasCierresCajaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/abierta")
    public ResponseEntity<AperturasCierresCaja> consultarCajaAbierta() {
        return aperturasCierresCajaService.consultarCajaAbierta()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/abrir")
    public ResponseEntity<?> abrirCaja(@RequestBody AperturaRequest request) {
        try {
            AperturasCierresCaja abierta = aperturasCierresCajaService.abrirCaja(
                    request.montoInicial(),
                    request.idUsuarioApertura()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(abierta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cerrar")
    public ResponseEntity<?> cerrarCaja(@RequestBody CierreRequest request) {
        try {
            AperturasCierresCaja cerrada = aperturasCierresCajaService.cerrarCaja(
                    request.montoFinalReal(),
                    request.idUsuarioCierre()
            );
            return ResponseEntity.ok(cerrada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    public record AperturaRequest(BigDecimal montoInicial, int idUsuarioApertura) {}
    public record CierreRequest(BigDecimal montoFinalReal, int idUsuarioCierre) {}
}