package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.AperturasCierresCaja;
import com.chickenbikerv2.repository.AperturasCierresCajaRepository;
import com.chickenbikerv2.repository.CajasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AperturasCierresCajaService {

    public static final int CAJA_OPERATIVA_ID = 1;
    public static final String ESTADO_ABIERTA = "Abierta";
    public static final String ESTADO_CERRADA = "Cerrada";

    private final AperturasCierresCajaRepository aperturasCierresCajaRepository;
    private final CajasRepository cajasRepository;
    private final MovimientosCajaService movimientosCajaService;

    public AperturasCierresCajaService(AperturasCierresCajaRepository aperturasCierresCajaRepository,
                                       CajasRepository cajasRepository,
                                       MovimientosCajaService movimientosCajaService) {
        this.aperturasCierresCajaRepository = aperturasCierresCajaRepository;
        this.cajasRepository = cajasRepository;
        this.movimientosCajaService = movimientosCajaService;
    }

    public List<AperturasCierresCaja> listar() {
        return aperturasCierresCajaRepository.findAll();
    }

    public Optional<AperturasCierresCaja> buscarPorId(Integer id) {
        return aperturasCierresCajaRepository.findById(id);
    }

    public Optional<AperturasCierresCaja> consultarCajaAbierta() {
        return aperturasCierresCajaRepository
                .findFirstByCajaIdCajaAndEstadoCierreCajaIgnoreCaseOrderByIdAperturaCierreCajaDesc(
                        CAJA_OPERATIVA_ID, ESTADO_ABIERTA);
    }

    @Transactional
    public AperturasCierresCaja abrirCaja(BigDecimal montoInicial, int idUsuarioApertura) {
        if (montoInicial == null || montoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Monto inicial inválido.");
        }

        // Bloqueo pesimista sobre la caja operativa para evitar aperturas simultáneas
        cajasRepository.bloquearPorId(CAJA_OPERATIVA_ID);

        Optional<AperturasCierresCaja> existente = consultarCajaAbierta();
        if (existente.isPresent()) {
            throw new IllegalStateException("Ya existe una caja abierta (ID "
                    + existente.get().getIdAperturaCierreCaja() + "). No se puede abrir otra.");
        }

        AperturasCierresCaja nueva = new AperturasCierresCaja();
        nueva.setFechaApertura(LocalDateTime.now());
        nueva.setMontoInicial(montoInicial);
        nueva.setFechaCierre(null);
        nueva.setMontoFinalSistema(null);
        nueva.setMontoFinalReal(null);
        nueva.setDiferencia(BigDecimal.ZERO);
        nueva.setEstadoCierreCaja(ESTADO_ABIERTA);
        nueva.setCajaIdCaja(CAJA_OPERATIVA_ID);
        nueva.setUsuarioIdUsuario(idUsuarioApertura);
        nueva.setUsuarioIdUsuario1(idUsuarioApertura); // Placeholder hasta el cierre

        return aperturasCierresCajaRepository.save(nueva);
    }

    @Transactional
    public AperturasCierresCaja cerrarCaja(BigDecimal montoFinalReal, int idUsuarioCierre) {
        if (montoFinalReal == null || montoFinalReal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Monto final real inválido.");
        }

        // Bloqueo pesimista sobre la caja operativa
        cajasRepository.bloquearPorId(CAJA_OPERATIVA_ID);

        AperturasCierresCaja abierta = consultarCajaAbierta()
                .orElseThrow(() -> new IllegalStateException("NO_HAY_CAJA_ABIERTA"));

        BigDecimal ingresos = movimientosCajaService.sumarIngresosPorTurno(abierta.getIdAperturaCierreCaja());
        BigDecimal egresos = movimientosCajaService.sumarEgresosPorTurno(abierta.getIdAperturaCierreCaja());
        BigDecimal montoSistema = abierta.getMontoInicial().add(ingresos).subtract(egresos);

        abierta.setFechaCierre(LocalDateTime.now());
        abierta.setMontoFinalSistema(montoSistema);
        abierta.setMontoFinalReal(montoFinalReal);
        abierta.setDiferencia(montoFinalReal.subtract(montoSistema));
        abierta.setEstadoCierreCaja(ESTADO_CERRADA);
        abierta.setUsuarioIdUsuario1(idUsuarioCierre);

        return aperturasCierresCajaRepository.saveAndFlush(abierta);
    }
}