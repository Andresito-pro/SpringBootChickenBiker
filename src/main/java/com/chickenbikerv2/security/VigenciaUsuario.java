package com.chickenbikerv2.security;

import com.chickenbikerv2.entity.Usuarios;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Regla de vigencia de las cuentas (columna usuarios.fecha_vencimiento, tipo DATE).
 *
 * - La cuenta es válida HASTA el final del día de vencimiento, inclusive:
 *   vence cuando "hoy" es posterior a fecha_vencimiento.
 * - "Hoy" se calcula en la zona horaria del negocio (por defecto
 *   America/Bogota, propiedad chickenbiker.zona-horaria), no en la del
 *   servidor ni en la de la conexión JDBC (serverTimezone=UTC): así el cambio
 *   de día no se adelanta ni se retrasa 5 horas.
 * - La vigencia es independiente del estado Activo/Inactivo: una cuenta activa
 *   pero vencida tampoco puede entrar.
 * - Solo un administrador cambia la fecha (renovación); nunca se modifica sola.
 */
@Component
public class VigenciaUsuario {

    public static final int DIAS_AVISO_VENCIMIENTO = 30;
    public static final int MESES_RENOVACION = 12;

    private final Clock reloj;

    @Autowired
    public VigenciaUsuario(@Value("${chickenbiker.zona-horaria:America/Bogota}") String zona) {
        this.reloj = Clock.system(ZoneId.of(zona));
    }

    /** Constructor para pruebas con un reloj fijo. */
    public VigenciaUsuario(Clock reloj) {
        this.reloj = reloj;
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    public boolean estaVencida(Usuarios usuario) {
        return estaVencida(usuario.getFechaVencimiento());
    }

    /** Sin fecha registrada se considera vencida (la columna es NOT NULL; se falla de forma segura). */
    public boolean estaVencida(LocalDate fechaVencimiento) {
        return fechaVencimiento == null || hoy().isAfter(fechaVencimiento);
    }

    /** Días que faltan para vencer (negativo si ya venció). */
    public long diasRestantes(LocalDate fechaVencimiento) {
        return fechaVencimiento == null ? -1 : ChronoUnit.DAYS.between(hoy(), fechaVencimiento);
    }

    public boolean venceProximamente(LocalDate fechaVencimiento) {
        long dias = diasRestantes(fechaVencimiento);
        return dias >= 0 && dias <= DIAS_AVISO_VENCIMIENTO;
    }

    /**
     * Nueva fecha tras una renovación anual: un año a partir de hoy o, si la
     * cuenta sigue vigente, un año a partir de su vencimiento actual (renovar
     * antes de tiempo no recorta el periodo ya concedido).
     */
    public LocalDate fechaRenovacion(LocalDate vencimientoActual) {
        LocalDate base = (vencimientoActual == null || vencimientoActual.isBefore(hoy())) ? hoy() : vencimientoActual;
        return base.plusMonths(MESES_RENOVACION);
    }

    /** Vencimiento por defecto de una cuenta nueva: un año desde hoy. */
    public LocalDate vencimientoInicial() {
        return hoy().plusMonths(MESES_RENOVACION);
    }
}
