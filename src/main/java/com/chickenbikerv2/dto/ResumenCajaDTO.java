package com.chickenbikerv2.dto;

import com.chickenbikerv2.entity.TipoMetodoPago;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cuadre de un turno de caja, separando el dinero físico de los pagos electrónicos.
 *
 * Efectivo esperado = monto inicial + cobros en EFECTIVO + ingresos manuales − egresos.
 * Los cobros con tarjeta y por plataformas digitales se informan aparte y
 * NUNCA se suman al efectivo que el cajero debe contar.
 *
 * @param montoInicial      base con la que se abrió el turno
 * @param cobrosEfectivo    cobros de pedidos pagados en efectivo
 * @param cobrosTarjeta     cobros con tarjeta (débito/crédito/datáfono)
 * @param cobrosDigitales   cobros por plataformas digitales (Nequi, Daviplata...)
 * @param ingresosManuales  ingresos manuales de caja (dinero físico)
 * @param egresos           egresos manuales de caja (dinero físico que sale)
 * @param cobrosAnulados    parte de los cobros que corresponde a pedidos anulados
 *                          después de pagados (informativo; si se devolvió dinero,
 *                          se registra como egreso)
 * @param porMetodo         desglose de cobros por cada medio de pago
 * @param efectivoEsperado  dinero físico que debe haber en caja
 */
public record ResumenCajaDTO(int idTurno,
                             BigDecimal montoInicial,
                             BigDecimal cobrosEfectivo,
                             BigDecimal cobrosTarjeta,
                             BigDecimal cobrosDigitales,
                             BigDecimal ingresosManuales,
                             BigDecimal egresos,
                             BigDecimal cobrosAnulados,
                             List<DetalleMetodo> porMetodo,
                             BigDecimal efectivoEsperado) {

    public record DetalleMetodo(String nombreMetodo, TipoMetodoPago tipo, BigDecimal total, long cantidad) {
    }

    /** Tarjeta + plataformas digitales. */
    public BigDecimal totalElectronicos() {
        return cobrosTarjeta.add(cobrosDigitales);
    }

    /** Todos los cobros de pedidos, sin importar el medio de pago. */
    public BigDecimal totalCobros() {
        return cobrosEfectivo.add(totalElectronicos());
    }

    /** Todos los ingresos registrados en el turno (cobros + ingresos manuales). */
    public BigDecimal totalIngresos() {
        return totalCobros().add(ingresosManuales);
    }
}
