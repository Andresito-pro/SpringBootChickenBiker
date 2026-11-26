package com.chickenbikerv2;

import com.chickenbikerv2.dto.VentaDiariaDTO;
import com.chickenbikerv2.dto.VentasPeriodoDTO;
import com.chickenbikerv2.entity.CabecerasRecibos;
import com.chickenbikerv2.entity.PedidosVentas;
import com.chickenbikerv2.repository.CabecerasRecibosRepository;
import com.chickenbikerv2.repository.UsuariosRepository;
import com.chickenbikerv2.security.ContrasenaPasswordEncoder;
import com.chickenbikerv2.security.DashboardPorRol;
import com.chickenbikerv2.security.PoliticaContrasena;
import com.chickenbikerv2.service.AperturasCierresCajaService;
import com.chickenbikerv2.service.DashboardService;
import com.chickenbikerv2.service.PagosService;
import com.chickenbikerv2.service.PedidosService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Dashboards independientes por rol, estadísticas de ventas del administrador
 * y política de contraseñas seguras.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/datos-prueba.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DashboardsYContrasenasTests {

    private static final int CAJERO = 2;
    private static final String API_VENTAS = "/api/dashboard/ventas-diarias";

    @Autowired private MockMvc mvc;
    @Autowired private UsuariosRepository usuariosRepository;
    @Autowired private CabecerasRecibosRepository cabecerasRecibosRepository;
    @Autowired private AperturasCierresCajaService cajaService;
    @Autowired private PedidosService pedidosService;
    @Autowired private PagosService pagosService;
    @Autowired private DashboardService dashboardService;

    private MockHttpSession login(String correo, String destinoEsperado) throws Exception {
        MvcResult resultado = mvc.perform(post("/login")
                        .param("correo", correo).param("password", "123456")
                        .param("autorizacionDatos", "si").with(csrf()))
                .andExpect(redirectedUrl(destinoEsperado))
                .andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    // ------------------------------------------------------------------
    // Dashboards por rol
    // ------------------------------------------------------------------

    @Test
    void cadaRolEntraASuDashboardYNoPuedeAbrirLosDeOtros() throws Exception {
        MockHttpSession admin = login("admin@chickenbiker.com", DashboardPorRol.RUTA_ADMINISTRADOR);
        MockHttpSession cajero = login("cajero@chickenbiker.com", DashboardPorRol.RUTA_CAJERO);
        MockHttpSession mesero = login("mesero@chickenbiker.com", DashboardPorRol.RUTA_MESERO);

        // Su propio dashboard se renderiza
        mvc.perform(get(DashboardPorRol.RUTA_ADMINISTRADOR).session(admin)).andExpect(status().isOk())
                .andExpect(view().name("dashboards/dashboard"));
        mvc.perform(get(DashboardPorRol.RUTA_CAJERO).session(cajero)).andExpect(status().isOk())
                .andExpect(view().name("dashboards/dashboardCajero"));
        mvc.perform(get(DashboardPorRol.RUTA_MESERO).session(mesero)).andExpect(status().isOk())
                .andExpect(view().name("dashboards/dashboardMesero"));

        // /dashboard (enlace del sidebar) lleva al dashboard que corresponde
        mvc.perform(get("/dashboard").session(admin)).andExpect(redirectedUrl(DashboardPorRol.RUTA_ADMINISTRADOR));
        mvc.perform(get("/dashboard").session(cajero)).andExpect(redirectedUrl(DashboardPorRol.RUTA_CAJERO));
        mvc.perform(get("/dashboard").session(mesero)).andExpect(redirectedUrl(DashboardPorRol.RUTA_MESERO));
        mvc.perform(get("/").session(mesero)).andExpect(redirectedUrl("/dashboard"));

        // Acceso cruzado modificando la URL: 403
        mvc.perform(get(DashboardPorRol.RUTA_CAJERO).session(admin)).andExpect(status().isForbidden());
        mvc.perform(get(DashboardPorRol.RUTA_MESERO).session(admin)).andExpect(status().isForbidden());
        mvc.perform(get(DashboardPorRol.RUTA_ADMINISTRADOR).session(cajero)).andExpect(status().isForbidden());
        mvc.perform(get(DashboardPorRol.RUTA_MESERO).session(cajero)).andExpect(status().isForbidden());
        mvc.perform(get(DashboardPorRol.RUTA_ADMINISTRADOR).session(mesero)).andExpect(status().isForbidden());
        mvc.perform(get(DashboardPorRol.RUTA_CAJERO).session(mesero)).andExpect(status().isForbidden());
        mvc.perform(get("/dashboard/otro").session(admin)).andExpect(status().isForbidden());

        // Sin sesión: al login
        for (String ruta : List.of("/dashboard", DashboardPorRol.RUTA_ADMINISTRADOR,
                DashboardPorRol.RUTA_CAJERO, DashboardPorRol.RUTA_MESERO)) {
            mvc.perform(get(ruta)).andExpect(status().is3xxRedirection())
                    .andExpect(result -> assertTrue(result.getResponse().getRedirectedUrl().endsWith("/login")));
        }

        // Si ya hay sesión, /login lleva al dashboard
        mvc.perform(get("/login").session(cajero)).andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void dashboardCajeroMuestraDatosRealesYCobraDesdeElDashboard() throws Exception {
        MockHttpSession cajero = login("cajero@chickenbiker.com", DashboardPorRol.RUTA_CAJERO);

        // Caja cerrada
        mvc.perform(get(DashboardPorRol.RUTA_CAJERO).session(cajero))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("La caja está cerrada")));

        cajaService.abrirCaja(new BigDecimal("50000"), CAJERO);
        PedidosVentas pedido = pedidosService.registrarPedido(
                PedidosService.TIPO_PARA_LLEVAR, null, List.of(1), List.of(1), CAJERO);
        CabecerasRecibos recibo = cabecerasRecibosRepository
                .findByPedidoVentaIdPedidoVenta(pedido.getIdPedidoVenta()).orElseThrow();

        mvc.perform(get(DashboardPorRol.RUTA_CAJERO).session(cajero))
                .andExpect(status().isOk())
                .andExpect(model().attribute("montoEsperado", org.hamcrest.Matchers.comparesEqualTo(new BigDecimal("50000"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(recibo.getNumeroRecibo())));

        mvc.perform(post("/pagos/cobrar").session(cajero).with(csrf())
                        .param("idCabeceraRecibo", String.valueOf(recibo.getIdCabeceraRecibo()))
                        .param("idMetodoPago", "1").param("origen", "dashboard"))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attributeExists("mensaje"));

        mvc.perform(get(DashboardPorRol.RUTA_CAJERO).session(cajero))
                .andExpect(model().attribute("montoEsperado", org.hamcrest.Matchers.comparesEqualTo(new BigDecimal("62000"))));
    }

    @Test
    void logoutInvalidaLaSesionYBloqueaLosDashboards() throws Exception {
        MockHttpSession admin = login("admin@chickenbiker.com", DashboardPorRol.RUTA_ADMINISTRADOR);
        mvc.perform(post("/logout").session(admin).with(csrf())).andExpect(redirectedUrl("/login?logout"));
        assertTrue(admin.isInvalid());

        // Una petición nueva sin sesión válida ya no accede a nada protegido
        mvc.perform(get(DashboardPorRol.RUTA_ADMINISTRADOR)).andExpect(status().is3xxRedirection());
        mvc.perform(get(API_VENTAS)).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Estadísticas de ventas
    // ------------------------------------------------------------------

    @Test
    void endpointDeVentasSoloParaAdministrador() throws Exception {
        MockHttpSession admin = login("admin@chickenbiker.com", DashboardPorRol.RUTA_ADMINISTRADOR);
        MockHttpSession cajero = login("cajero@chickenbiker.com", DashboardPorRol.RUTA_CAJERO);
        MockHttpSession mesero = login("mesero@chickenbiker.com", DashboardPorRol.RUTA_MESERO);

        mvc.perform(get(API_VENTAS)).andExpect(status().isUnauthorized());
        mvc.perform(get(API_VENTAS).session(cajero)).andExpect(status().isForbidden());
        mvc.perform(get(API_VENTAS).session(mesero)).andExpect(status().isForbidden());

        LocalDate hoy = LocalDate.now();
        mvc.perform(get(API_VENTAS).param("dias", "30").session(admin).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moneda").value("COP"))
                .andExpect(jsonPath("$.desde").value(hoy.minusDays(29).toString()))
                .andExpect(jsonPath("$.hasta").value(hoy.toString()))
                .andExpect(jsonPath("$.dias.length()").value(30))
                .andExpect(jsonPath("$.dias[0].total").value(0))
                .andExpect(jsonPath("$.cantidadRecibos").value(0));

        mvc.perform(get(API_VENTAS).session(admin)).andExpect(jsonPath("$.dias.length()").value(7));
        mvc.perform(get(API_VENTAS).param("dias", "90").session(admin)).andExpect(jsonPath("$.dias.length()").value(90));

        // Parámetros inválidos
        mvc.perform(get(API_VENTAS).param("dias", "15").session(admin)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(get(API_VENTAS).param("desde", "2026-05-10").param("hasta", "2026-05-01").session(admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get(API_VENTAS).param("desde", "2024-01-01").param("hasta", "2026-01-01").session(admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get(API_VENTAS).param("desde", "2026-05-01").session(admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get(API_VENTAS).param("desde", "no-es-fecha").param("hasta", "2026-05-01").session(admin))
                .andExpect(status().isBadRequest());

        // Rango personalizado
        mvc.perform(get(API_VENTAS).param("desde", "2026-05-01").param("hasta", "2026-05-10").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias.length()").value(10))
                .andExpect(jsonPath("$.dias[0].fecha").value("2026-05-01"));
    }

    @Test
    void ventasDiariasSoloCuentanRecibosPagadosSinDuplicarYConDiasEnCero() {
        cajaService.abrirCaja(BigDecimal.ZERO, CAJERO);
        // Pagado: 12.000
        CabecerasRecibos pagado1 = reciboDe(pedidosService.registrarPedido(
                PedidosService.TIPO_PARA_LLEVAR, null, List.of(1), List.of(1), CAJERO));
        // Pagado: 2 x 3.000 = 6.000
        CabecerasRecibos pagado2 = reciboDe(pedidosService.registrarPedido(
                PedidosService.TIPO_PARA_LLEVAR, null, List.of(2), List.of(2), CAJERO));
        // Pendiente (Generado): 24.000 -> no cuenta
        reciboDe(pedidosService.registrarPedido(
                PedidosService.TIPO_PARA_LLEVAR, null, List.of(1), List.of(2), CAJERO));
        // Anulado: 3.000 -> no cuenta
        CabecerasRecibos anulado = reciboDe(pedidosService.registrarPedido(
                PedidosService.TIPO_PARA_LLEVAR, null, List.of(2), List.of(1), CAJERO));
        anulado.setEstadoReciboIdEstadoRecibo(3);
        cabecerasRecibosRepository.save(anulado);

        pagosService.cobrarRecibo(pagado1.getIdCabeceraRecibo(), 1, CAJERO);
        pagosService.cobrarRecibo(pagado2.getIdCabeceraRecibo(), 2, CAJERO);

        // Simula una venta pagada de hace 3 días (solo en la BD de pruebas H2)
        LocalDate hoy = LocalDate.now();
        CabecerasRecibos antiguo = cabecerasRecibosRepository.findById(pagado2.getIdCabeceraRecibo()).orElseThrow();
        antiguo.setFechaEmision(hoy.minusDays(3).atTime(13, 30));
        cabecerasRecibosRepository.save(antiguo);

        VentasPeriodoDTO semana = dashboardService.ventasDiarias(hoy.minusDays(6), hoy);
        assertEquals(7, semana.dias().size());
        assertEquals(0, new BigDecimal("18000").compareTo(semana.totalPeriodo()));
        assertEquals(2, semana.cantidadRecibos());

        Map<LocalDate, VentaDiariaDTO> porDia = semana.dias().stream()
                .collect(java.util.stream.Collectors.toMap(VentaDiariaDTO::fecha, d -> d));
        assertEquals(0, new BigDecimal("12000").compareTo(porDia.get(hoy).total()));
        assertEquals(1L, porDia.get(hoy).cantidadRecibos());
        assertEquals(0, new BigDecimal("6000").compareTo(porDia.get(hoy.minusDays(3)).total()));
        assertEquals(0, BigDecimal.ZERO.compareTo(porDia.get(hoy.minusDays(1)).total()), "Día sin ventas = 0");
        assertEquals(0L, porDia.get(hoy.minusDays(1)).cantidadRecibos());

        // Rango que excluye la venta antigua
        VentasPeriodoDTO soloHoy = dashboardService.ventasDiarias(hoy, hoy);
        assertEquals(0, new BigDecimal("12000").compareTo(soloHoy.totalPeriodo()));

        // La consulta no modificó ningún recibo
        assertEquals(1, pagosService.listarRecibosPendientes().size());
        assertEquals(3, cabecerasRecibosRepository.findById(anulado.getIdCabeceraRecibo())
                .orElseThrow().getEstadoReciboIdEstadoRecibo());
    }

    private CabecerasRecibos reciboDe(PedidosVentas pedido) {
        return cabecerasRecibosRepository.findByPedidoVentaIdPedidoVenta(pedido.getIdPedidoVenta()).orElseThrow();
    }

    // ------------------------------------------------------------------
    // Contraseñas seguras
    // ------------------------------------------------------------------

    @Test
    void politicaDeContrasenas() {
        assertTrue(PoliticaContrasena.esValida("Segura#2026"));
        assertTrue(PoliticaContrasena.esValida("Ñandú.2026"), "Admite letras con tilde y ñ");
        assertFalse(PoliticaContrasena.esValida(null));
        assertFalse(PoliticaContrasena.esValida(""));
        assertFalse(PoliticaContrasena.esValida("        "), "Solo espacios");
        assertFalse(PoliticaContrasena.esValida("Ab1!xyz"), "7 caracteres");
        assertFalse(PoliticaContrasena.esValida("segura#2026"), "Sin mayúscula");
        assertFalse(PoliticaContrasena.esValida("SEGURA#2026"), "Sin minúscula");
        assertFalse(PoliticaContrasena.esValida("Segura#Clave"), "Sin número");
        assertFalse(PoliticaContrasena.esValida("Segura2026"), "Sin carácter especial");
        assertFalse(PoliticaContrasena.esValida("Segura 2026"), "El espacio no cuenta como especial");
        assertFalse(PoliticaContrasena.esValida("Aa1!" + "x".repeat(70)), "Más de 72 bytes");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PoliticaContrasena.validar("segura"));
        assertTrue(e.getMessage().contains("mayúscula"));
        assertTrue(e.getMessage().contains("número"));
        assertTrue(e.getMessage().contains("carácter especial"));
        assertTrue(e.getMessage().contains("mínimo 8"));
    }

    @Test
    void registroYEdicionRechazanContrasenasDebilesYAceptanLasSeguras() throws Exception {
        MockHttpSession admin = login("admin@chickenbiker.com", DashboardPorRol.RUTA_ADMINISTRADOR);

        for (String debil : List.of("claveSegura1", "CLAVE#2026", "Cl#1", "        ", "")) {
            mvc.perform(post("/usuarios").session(admin).with(csrf())
                            .param("nombreUsuario", "Debil").param("apellidoUsuario", "Usuario")
                            .param("tipoDocumentoIdTipoDocumento", "1").param("documentoUsuario", "3001")
                            .param("rolesIdRoles", "3").param("correoUsuario", "debil@chickenbiker.com")
                            .param("passwordUsuario", debil).param("telefonoUsuario", "310")
                            .param("fechaNacimiento", "1999-05-10").param("direccionUsuario", "Calle 1"))
                    .andExpect(redirectedUrl("/usuarios/nuevo"))
                    .andExpect(flash().attributeExists("error"));
        }
        assertTrue(usuariosRepository.findByCorreoUsuario("debil@chickenbiker.com").isEmpty());

        // Petición directa a la API, saltándose el formulario
        mvc.perform(post("/api/usuarios/guardar").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"Api","apellidoUsuario":"Debil","documentoUsuario":"3002",
                                 "correoUsuario":"api@chickenbiker.com","passwordUsuario":"12345678",
                                 "telefonoUsuario":"311","fechaNacimiento":"1999-05-10","direccionUsuario":"Calle 2",
                                 "fechaVencimiento":"2030-01-01","autorizacionDatos":0,"rolesIdRoles":3,
                                 "tipoDocumentoIdTipoDocumento":1,"estadosUsuariosIdEstadosUsuarios":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        assertTrue(usuariosRepository.findByCorreoUsuario("api@chickenbiker.com").isEmpty());

        // Contraseña válida: se guarda como BCrypt y la respuesta no expone el hash
        mvc.perform(post("/usuarios").session(admin).with(csrf())
                        .param("nombreUsuario", "Fuerte").param("apellidoUsuario", "Usuario")
                        .param("tipoDocumentoIdTipoDocumento", "1").param("documentoUsuario", "3003")
                        .param("rolesIdRoles", "3").param("correoUsuario", "fuerte@chickenbiker.com")
                        .param("passwordUsuario", "Fuerte#2026").param("telefonoUsuario", "312")
                        .param("fechaNacimiento", "1999-05-10").param("direccionUsuario", "Calle 3"))
                .andExpect(redirectedUrl("/usuarios"));
        var fuerte = usuariosRepository.findByCorreoUsuario("fuerte@chickenbiker.com").orElseThrow();
        String hash = fuerte.getPasswordUsuario();
        assertTrue(ContrasenaPasswordEncoder.esHashBcrypt(hash));
        assertNotEquals("Fuerte#2026", hash);

        // Edición: solo espacios o contraseña débil se rechazan sin tocar el hash
        for (String debil : List.of("   ", "debil")) {
            mvc.perform(post("/usuarios").session(admin).with(csrf())
                            .param("idUsuario", String.valueOf(fuerte.getIdUsuario()))
                            .param("nombreUsuario", "Fuerte").param("apellidoUsuario", "Usuario")
                            .param("tipoDocumentoIdTipoDocumento", "1").param("documentoUsuario", "3003")
                            .param("rolesIdRoles", "3").param("correoUsuario", "fuerte@chickenbiker.com")
                            .param("passwordUsuario", debil).param("telefonoUsuario", "312")
                            .param("fechaNacimiento", "1999-05-10").param("direccionUsuario", "Calle 3")
                            .param("fechaVencimiento", "2030-01-01").param("estadosUsuariosIdEstadosUsuarios", "1"))
                    .andExpect(redirectedUrl("/usuarios/editar/" + fuerte.getIdUsuario()))
                    .andExpect(flash().attributeExists("error"));
        }
        assertEquals(hash, usuariosRepository.findById(fuerte.getIdUsuario()).orElseThrow().getPasswordUsuario());

        // Los usuarios existentes con contraseñas antiguas siguen pudiendo iniciar sesión
        login("mesero@chickenbiker.com", DashboardPorRol.RUTA_MESERO);
    }
}
