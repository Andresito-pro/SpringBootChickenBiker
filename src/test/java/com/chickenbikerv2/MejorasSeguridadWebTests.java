package com.chickenbikerv2;

import com.chickenbikerv2.entity.PedidosVentas;
import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.repository.UsuariosRepository;
import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.AperturasCierresCajaService;
import com.chickenbikerv2.service.GestionPermisosService;
import com.chickenbikerv2.service.PedidosService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controles de acceso de las mejoras: vigencia en login y en sesión activa,
 * permisos dinámicos aplicados en la siguiente petición, visibilidad de pedidos,
 * notificaciones restringidas, pestaña de permisos y estado de caja.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/datos-prueba.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MejorasSeguridadWebTests {

    private static final int ROL_ADMIN = 1;
    private static final int ROL_CAJERO = 2;

    @Autowired private MockMvc mvc;
    @Autowired private UsuariosRepository usuariosRepository;
    @Autowired private GestionPermisosService gestionPermisosService;
    @Autowired private AperturasCierresCajaService cajaService;
    @Autowired private PedidosService pedidosService;
    @Autowired private com.chickenbikerv2.service.PagosService pagosService;
    @Autowired private com.chickenbikerv2.service.AnulacionPedidosService anulacionService;
    @Autowired private com.chickenbikerv2.repository.CabecerasRecibosRepository recibosRepository;

    private MockHttpSession login(String correo) throws Exception {
        MvcResult resultado = mvc.perform(post("/login")
                        .param("correo", correo).param("password", "123456")
                        .param("autorizacionDatos", "si").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/dashboard/*"))
                .andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    @Test
    void unaCuentaVencidaNoPuedeIniciarSesionYSeLeExplicaElMotivo() throws Exception {
        mvc.perform(post("/login").param("correo", "vencido@chickenbiker.com").param("password", "123456")
                        .param("autorizacionDatos", "si").with(csrf()))
                .andExpect(redirectedUrl("/login?vencida"));
        // Con contraseña errónea no se revela que la cuenta está vencida
        mvc.perform(post("/login").param("correo", "vencido@chickenbiker.com").param("password", "mala")
                        .param("autorizacionDatos", "si").with(csrf()))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/login").param("vencida", ""))
                .andExpect(content().string(containsString("Solicita la renovación al administrador")));
    }

    @Test
    void siLaCuentaVenceDuranteLaSesionSeCierraEnLaSiguientePeticion() throws Exception {
        MockHttpSession sesion = login("mesero@chickenbiker.com");
        mvc.perform(get("/pedidos").session(sesion)).andExpect(status().isOk());

        Usuarios mesero = usuariosRepository.findById(3).orElseThrow();
        mesero.setFechaVencimiento(LocalDate.now().minusDays(2));
        usuariosRepository.save(mesero);

        mvc.perform(get("/pedidos").session(sesion)).andExpect(redirectedUrl("/login?vencida"));
    }

    @Test
    void elAdministradorRenuevaLaVigenciaUnAnio() throws Exception {
        MockHttpSession admin = login("admin@chickenbiker.com");
        mvc.perform(post("/usuarios/renovar/5").session(admin).with(csrf()))
                .andExpect(redirectedUrl("/usuarios"));
        assertEquals(LocalDate.now(java.time.ZoneId.of("America/Bogota")).plusYears(1),
                usuariosRepository.findById(5).orElseThrow().getFechaVencimiento());
        login("vencido@chickenbiker.com"); // ya puede ingresar

        MockHttpSession cajero = login("cajero@chickenbiker.com");
        mvc.perform(post("/usuarios/renovar/5").session(cajero).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void losCambiosDePermisosSeAplicanEnLaSiguientePeticionSinCerrarSesion() throws Exception {
        MockHttpSession cajero = login("cajero@chickenbiker.com");
        mvc.perform(get("/caja").session(cajero)).andExpect(status().isOk());

        gestionPermisosService.actualizarPermisos(ROL_CAJERO, List.of(5, 7, 11, 12, 14), ROL_ADMIN); // quita "Gestionar caja"
        mvc.perform(get("/caja").session(cajero)).andExpect(status().isForbidden());
        assertFalse(((List<?>) cajero.getAttribute(SesionUsuario.PERMISOS)).contains("Gestionar caja"),
                "El menú (permisos en sesión) también se actualiza");

        gestionPermisosService.actualizarPermisos(ROL_CAJERO, List.of(5, 6, 7, 11, 12, 14), ROL_ADMIN);
        mvc.perform(get("/caja").session(cajero)).andExpect(status().isOk());
    }

    @Test
    void laPestanaDePermisosEsSoloDelAdministradorYProtegeSuRol() throws Exception {
        MockHttpSession cajero = login("cajero@chickenbiker.com");
        mvc.perform(get("/permisos").session(cajero)).andExpect(status().isForbidden());

        MockHttpSession admin = login("admin@chickenbiker.com");
        mvc.perform(get("/permisos").session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Gestión de permisos")));

        // Intentar quitarse permisos a sí mismo (rol Administrador): rechazado
        mvc.perform(post("/permisos/" + ROL_ADMIN).session(admin).with(csrf()).param("permisos", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
        mvc.perform(get("/usuarios").session(admin)).andExpect(status().isOk());

        // El administrador no puede cambiarse su propio rol desde la API
        mvc.perform(post("/api/usuarios/guardar").session(admin).with(csrf())
                        .contentType("application/json")
                        .content("{\"idUsuario\":1,\"rolesIdRoles\":3,\"estadosUsuariosIdEstadosUsuarios\":1}"))
                .andExpect(status().is4xxClientError());
        assertEquals(ROL_ADMIN, usuariosRepository.findById(1).orElseThrow().getRolesIdRoles());
    }

    @Test
    void elMeseroNoVeNiAbrePedidosDeOtrosMeseros() throws Exception {
        cajaService.abrirCaja(new BigDecimal("10000"), 2);
        PedidosVentas propio = pedidosService.registrarPedido(PedidosService.TIPO_MESA, 1, List.of(1), List.of(1), 3);
        PedidosVentas ajeno = pedidosService.registrarPedido(PedidosService.TIPO_MESA, 2, List.of(1), List.of(1), 6);

        MockHttpSession mesero = login("mesero@chickenbiker.com");
        mvc.perform(get("/pedidos/detalle/" + propio.getIdPedidoVenta()).session(mesero)).andExpect(status().isOk());
        mvc.perform(get("/pedidos/detalle/" + ajeno.getIdPedidoVenta()).session(mesero))
                .andExpect(redirectedUrl("/pedidos"));
        mvc.perform(get("/pedidos").session(mesero))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/pedidos/detalle/" + propio.getIdPedidoVenta())))
                .andExpect(content().string(not(containsString("/pedidos/detalle/" + ajeno.getIdPedidoVenta()))));

        // Tampoco puede anular (no tiene el permiso)
        mvc.perform(post("/pedidos/anular/" + ajeno.getIdPedidoVenta()).session(mesero).with(csrf())
                        )
                .andExpect(status().isForbidden());
    }

    @Test
    void lasNotificacionesSoloSonParaAdministradorYCajero() throws Exception {
        cajaService.abrirCaja(new BigDecimal("10000"), 2);
        pedidosService.registrarPedido(PedidosService.TIPO_MESA, 1, List.of(1), List.of(1), 3);

        MockHttpSession mesero = login("mesero@chickenbiker.com");
        mvc.perform(get("/api/notificaciones").session(mesero)).andExpect(status().isForbidden());
        mvc.perform(get("/pedidos").session(mesero))
                .andExpect(content().string(not(containsString("data-notificaciones"))));

        mvc.perform(get("/api/notificaciones")).andExpect(status().isUnauthorized());

        MockHttpSession cajero = login("cajero@chickenbiker.com");
        mvc.perform(get("/api/notificaciones").session(cajero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidas").value(1))
                .andExpect(jsonPath("$.recientes[0].titulo").exists());
        mvc.perform(post("/api/notificaciones/leidas").session(cajero)).andExpect(status().isForbidden()); // sin CSRF
        mvc.perform(post("/api/notificaciones/leidas").session(cajero).with(csrf()))
                .andExpect(jsonPath("$.noLeidas").value(0));
        mvc.perform(get("/api/notificaciones").session(cajero)) // el estado de lectura se conserva en la sesión
                .andExpect(jsonPath("$.noLeidas").value(0));
        mvc.perform(post("/api/notificaciones/99999/leida").session(cajero).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void elEstadoDeLaCajaEsElMismoEnTodasLasPantallas() throws Exception {
        MockHttpSession admin = login("admin@chickenbiker.com");
        mvc.perform(get("/api/caja/estado").session(admin))
                .andExpect(jsonPath("$.abierta").value(false));
        // Antes estas vistas tenían "Caja Abierta" fijo en el HTML
        mvc.perform(get("/categorias").session(admin))
                .andExpect(content().string(containsString("Caja Cerrada")))
                .andExpect(content().string(not(containsString("Caja Abierta"))));

        cajaService.abrirCaja(new BigDecimal("10000"), 1);
        mvc.perform(get("/api/caja/estado").session(admin)).andExpect(jsonPath("$.abierta").value(true));
        for (String ruta : List.of("/categorias", "/productos", "/roles", "/mesas", "/pedidos", "/dashboard/administrador")) {
            mvc.perform(get(ruta).session(admin))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Caja Abierta")));
        }
    }

    @Test
    void lasVistasModificadasSeRenderizanConDatosReales() throws Exception {
        cajaService.abrirCaja(new BigDecimal("50000"), 2);
        PedidosVentas pendiente = pedidosService.registrarPedido(PedidosService.TIPO_MESA, 1, List.of(1), List.of(1), 3);
        PedidosVentas pagado = pedidosService.registrarPedido(PedidosService.TIPO_PARA_LLEVAR, null, List.of(2), List.of(1), 3);
        PedidosVentas anulado = pedidosService.registrarPedido(PedidosService.TIPO_MESA, 2, List.of(1), List.of(1), 3);
        pagosService.cobrarRecibo(recibosRepository.findByPedidoVentaIdPedidoVenta(pagado.getIdPedidoVenta())
                .orElseThrow().getIdCabeceraRecibo(), 2, 2);
        anulacionService.anularPedidoPendiente(anulado.getIdPedidoVenta(), 1, true);

        MockHttpSession admin = login("admin@chickenbiker.com");
        mvc.perform(get("/pedidos").session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("modalAnular" + pendiente.getIdPedidoVenta())))
                .andExpect(content().string(containsString("modalAnularPagado" + pagado.getIdPedidoVenta())))
                .andExpect(content().string(not(containsString("modalAnular" + anulado.getIdPedidoVenta() + "\""))))
                .andExpect(content().string(containsString("data-filter-value=\"anulado\"")));
        mvc.perform(get("/pedidos/detalle/" + anulado.getIdPedidoVenta()).session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Motivo"))))
                .andExpect(content().string(containsString("Pedido anulado")));
        mvc.perform(get("/usuarios").session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vencida")))
                .andExpect(content().string(containsString("/usuarios/renovar/5")));
        mvc.perform(get("/movimientos").session(admin)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Efectivo esperado")));
        for (String ruta : List.of("/caja", "/caja/cerrar")) {
            mvc.perform(get(ruta).session(admin)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("Cobros con tarjeta")));
        }
        mvc.perform(get("/recibos").session(admin)).andExpect(status().isOk());
        mvc.perform(get("/permisos").param("rol", "2").session(admin)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Anular pedidos pagados")));
        mvc.perform(get("/dashboard/administrador").session(admin)).andExpect(status().isOk());

        MockHttpSession cajero = login("cajero@chickenbiker.com");
        mvc.perform(get("/dashboard/cajero").session(cajero)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Efectivo esperado en caja")))
                .andExpect(content().string(containsString("data-notificaciones")));
        // El cajero puede anular pendientes pero no tiene el procedimiento de pagados
        mvc.perform(get("/pedidos").session(cajero)).andExpect(status().isOk())
                .andExpect(content().string(containsString("modalAnular" + pendiente.getIdPedidoVenta())))
                .andExpect(content().string(not(containsString("modalAnularPagado"))));
        mvc.perform(post("/pedidos/anular-pagado/" + pagado.getIdPedidoVenta()).session(cajero).with(csrf())
                        .param("confirmaProcedimiento", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anulacionPorPostCambiaElEstadoYNoSePuedeRepetir() throws Exception {
        cajaService.abrirCaja(new BigDecimal("10000"), 2);
        PedidosVentas pedido = pedidosService.registrarPedido(PedidosService.TIPO_MESA, 1, List.of(1), List.of(1), 3);

        MockHttpSession cajero = login("cajero@chickenbiker.com");
        mvc.perform(post("/pedidos/anular/" + pedido.getIdPedidoVenta()).session(cajero))
                .andExpect(status().isForbidden()); // sin CSRF
        mvc.perform(post("/pedidos/anular/" + pedido.getIdPedidoVenta()).session(cajero).with(csrf()))
                .andExpect(redirectedUrl("/pedidos"))
                .andExpect(flash().attributeExists("mensaje"));
        mvc.perform(post("/pedidos/anular/" + pedido.getIdPedidoVenta()).session(cajero).with(csrf()))
                .andExpect(redirectedUrl("/pedidos"))
                .andExpect(flash().attribute("error", containsString("ya fue anulado")));

        mvc.perform(get("/recibos").session(cajero))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pedido anulado")));
        int idRecibo = recibosRepository.findByPedidoVentaIdPedidoVenta(pedido.getIdPedidoVenta())
                .orElseThrow().getIdCabeceraRecibo();
        mvc.perform(get("/recibos/" + idRecibo).session(cajero))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pedido anulado")));
        mvc.perform(get("/pagos").session(cajero)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("REC-" + pedido.getIdPedidoVenta() + "<"))));
    }
}
