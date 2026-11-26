package com.chickenbikerv2;

import com.chickenbikerv2.dto.ResumenCajaDTO;
import com.chickenbikerv2.entity.AperturasCierresCaja;
import com.chickenbikerv2.entity.CabecerasRecibos;
import com.chickenbikerv2.entity.PedidosVentas;
import com.chickenbikerv2.repository.CabecerasRecibosRepository;
import com.chickenbikerv2.repository.MovimientosCajaRepository;
import com.chickenbikerv2.service.AnulacionPedidosService;
import com.chickenbikerv2.service.AperturasCierresCajaService;
import com.chickenbikerv2.service.DashboardService;
import com.chickenbikerv2.service.NotificacionesService;
import com.chickenbikerv2.service.PagosService;
import com.chickenbikerv2.service.PedidosService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integración con la estructura REAL de la base de datos.
 *
 * El resto de pruebas genera las tablas a partir de las entidades (create-drop),
 * por lo que nunca detectaría una columna o tabla que exista en el código pero no
 * en MariaDB. Aquí H2 carga la copia del script real
 * (src/test/resources/esquema/chicken_biker_esquema_real.sql, con sus claves
 * foráneas) y Hibernate arranca con ddl-auto=validate, igual que en producción:
 * si una entidad no coincide con una tabla o columna real, el contexto no inicia.
 * Después ejecuta el flujo principal (inserciones, actualizaciones y consultas)
 * con las claves foráneas activas.
 *
 * Limitación: H2 en modo MySQL no es MariaDB; esto no prueba la conexión real.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:chicken_biker_esquema_real;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                + "NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'classpath:esquema/chicken_biker_esquema_real.sql'",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Sql(scripts = {"/esquema/desactivar-integridad.sql", "/datos-prueba.sql", "/esquema/activar-integridad.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EsquemaRealMariaDbTests {

    private static final int ADMIN = 1;
    private static final int CAJERO = 2;
    private static final int MESERO = 3;

    @Autowired private List<JpaRepository<?, ?>> repositorios;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AperturasCierresCajaService cajaService;
    @Autowired private PedidosService pedidosService;
    @Autowired private PagosService pagosService;
    @Autowired private AnulacionPedidosService anulacionService;
    @Autowired private NotificacionesService notificacionesService;
    @Autowired private DashboardService dashboardService;
    @Autowired private CabecerasRecibosRepository recibosRepository;
    @Autowired private MovimientosCajaRepository movimientosRepository;

    @Test
    void lasEntidadesSeLeenSobreLasTablasReales() {
        // El contexto ya arrancó con ddl-auto=validate; además, cada repositorio consulta su tabla.
        assertFalse(repositorios.isEmpty());
        repositorios.forEach(JpaRepository::findAll);

        // No existen estructuras ajenas al modelo real
        Integer extras = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_name IN ('notificaciones', 'anulaciones_pedidos')", Integer.class);
        assertEquals(0, extras);
        Integer columnaExtra = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_name = 'movimientos_caja' AND column_name = 'cabecera_recibo_id_cabecera_recibo'",
                Integer.class);
        assertEquals(0, columnaExtra);
    }

    @Test
    void elFlujoPrincipalFuncionaConLasClavesForaneasReales() {
        AperturasCierresCaja turno = cajaService.abrirCaja(new BigDecimal("50000"), CAJERO);

        PedidosVentas enMesa = pedidosService.registrarPedido(PedidosService.TIPO_MESA, 1, List.of(1), List.of(1), MESERO);
        PedidosVentas paraLlevar = pedidosService.registrarPedido(PedidosService.TIPO_PARA_LLEVAR, null,
                List.of(1, 2), List.of(1, 1), MESERO);
        PedidosVentas aAnular = pedidosService.registrarPedido(PedidosService.TIPO_MESA, 2, List.of(2), List.of(1), MESERO);

        // Notificaciones derivadas de pedidos_ventas
        assertEquals(3, notificacionesService.resumen(CAJERO, new NotificacionesService.EstadoLectura()).noLeidas());

        // Cobros (efectivo y tarjeta) y anulación por cambio de estado
        pagosService.cobrarRecibo(recibo(enMesa).getIdCabeceraRecibo(), 1, CAJERO);
        pagosService.cobrarRecibo(recibo(paraLlevar).getIdCabeceraRecibo(), 2, CAJERO);
        anulacionService.anularPedidoPendiente(aAnular.getIdPedidoVenta(), ADMIN, true);
        assertEquals(2, movimientosRepository.count());

        ResumenCajaDTO resumen = cajaService.calcularResumen(turno);
        assertEquals(0, new BigDecimal("12000").compareTo(resumen.cobrosEfectivo()));
        assertEquals(0, new BigDecimal("15000").compareTo(resumen.cobrosTarjeta()));
        assertEquals(0, new BigDecimal("62000").compareTo(resumen.efectivoEsperado()));

        assertEquals(2, dashboardService.ventasDiarias(LocalDate.now(), LocalDate.now()).cantidadRecibos());
        assertEquals(0L, dashboardService.obtenerResumen().get("recibosPendientes"));

        AperturasCierresCaja cerrado = cajaService.cerrarCaja(new BigDecimal("62000"), CAJERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(cerrado.getDiferencia()));
    }

    private CabecerasRecibos recibo(PedidosVentas pedido) {
        return recibosRepository.findByPedidoVentaIdPedidoVenta(pedido.getIdPedidoVenta()).orElseThrow();
    }
}
