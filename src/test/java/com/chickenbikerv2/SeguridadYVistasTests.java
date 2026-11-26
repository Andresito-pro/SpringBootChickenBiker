package com.chickenbikerv2;

import com.chickenbikerv2.entity.Usuarios;
import com.chickenbikerv2.repository.UsuariosRepository;
import com.chickenbikerv2.security.ContrasenaPasswordEncoder;
import com.chickenbikerv2.security.SesionUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/datos-prueba.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SeguridadYVistasTests {

    @Autowired private MockMvc mvc;
    @Autowired private UsuariosRepository usuariosRepository;

    private MockHttpSession login(String correo, String password) throws Exception {
        MvcResult resultado = mvc.perform(post("/login")
                        .param("correo", correo)
                        .param("password", password)
                        .param("autorizacionDatos", "si")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                // Cada rol entra a su propio dashboard (DashboardPorRol)
                .andExpect(redirectedUrlPattern("/dashboard/*"))
                .andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    @Test
    void sinSesionSeRedirigeAlLogin() throws Exception {
        mvc.perform(get("/pedidos")).andExpect(status().is3xxRedirection())
                .andExpect(result -> assertTrue(
                        result.getResponse().getRedirectedUrl().endsWith("/login")));
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void loginConContrasenaHeredadaEnTextoPlanoLaMigraABcrypt() throws Exception {
        MockHttpSession sesion = login("admin@chickenbiker.com", "123456");

        Usuarios admin = usuariosRepository.findById(1).orElseThrow();
        assertTrue(ContrasenaPasswordEncoder.esHashBcrypt(admin.getPasswordUsuario()),
                "La contraseña en texto plano debe quedar guardada como BCrypt tras el login");
        assertEquals((byte) 1, admin.getAutorizacionDatos(), "Se registra la autorización de datos");

        // Datos de sesión que esperan vistas y controladores
        Usuarios enSesion = (Usuarios) sesion.getAttribute(SesionUsuario.USUARIO);
        assertNotNull(enSesion);
        assertNull(enSesion.getPasswordUsuario(), "La sesión no guarda el hash");
        assertEquals("Administrador", sesion.getAttribute(SesionUsuario.NOMBRE_ROL));
        assertTrue(((List<?>) sesion.getAttribute(SesionUsuario.PERMISOS)).contains("Gestionar caja"));

        // Con el hash BCrypt ya guardado se sigue pudiendo entrar
        login("admin@chickenbiker.com", "123456");
    }

    @Test
    void credencialesInvalidasUsuarioInactivoYSinConsentimiento() throws Exception {
        mvc.perform(post("/login").param("correo", "admin@chickenbiker.com").param("password", "mala")
                        .param("autorizacionDatos", "si").with(csrf()))
                .andExpect(redirectedUrl("/login?error"));
        assertEquals("123456", usuariosRepository.findById(1).orElseThrow().getPasswordUsuario(),
                "Un intento fallido no modifica la contraseña almacenada");

        mvc.perform(post("/login").param("correo", "inactivo@chickenbiker.com").param("password", "123456")
                        .param("autorizacionDatos", "si").with(csrf()))
                .andExpect(redirectedUrl("/login?inactivo"));

        mvc.perform(post("/login").param("correo", "admin@chickenbiker.com").param("password", "123456")
                        .with(csrf()))
                .andExpect(redirectedUrl("/login?consentimiento"));

        // Sin token CSRF el formulario se rechaza
        mvc.perform(post("/login").param("correo", "admin@chickenbiker.com").param("password", "123456")
                        .param("autorizacionDatos", "si"))
                .andExpect(status().isForbidden());
    }

    @Test
    void permisosPorRol() throws Exception {
        MockHttpSession mesero = login("mesero@chickenbiker.com", "123456");
        mvc.perform(get("/pedidos").session(mesero)).andExpect(status().isOk());
        mvc.perform(get("/pedidos/tomar").session(mesero)).andExpect(status().isOk());
        mvc.perform(get("/caja").session(mesero)).andExpect(status().isForbidden());
        mvc.perform(get("/pagos").session(mesero)).andExpect(status().isForbidden());
        mvc.perform(get("/usuarios").session(mesero)).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/listar").session(mesero)).andExpect(status().isForbidden());

        MockHttpSession cajero = login("cajero@chickenbiker.com", "123456");
        mvc.perform(get("/caja").session(cajero)).andExpect(status().isOk());
        mvc.perform(get("/recibos").session(cajero)).andExpect(status().isOk());
        mvc.perform(get("/usuarios").session(cajero)).andExpect(status().isForbidden());
        mvc.perform(get("/productos").session(cajero)).andExpect(status().isForbidden());
    }

    @Test
    void altaYEdicionDeUsuarioGuardanBcrypt() throws Exception {
        MockHttpSession admin = login("admin@chickenbiker.com", "123456");

        mvc.perform(post("/usuarios").session(admin).with(csrf())
                        .param("nombreUsuario", "Nuevo").param("apellidoUsuario", "Usuario")
                        .param("tipoDocumentoIdTipoDocumento", "1").param("documentoUsuario", "2001")
                        .param("rolesIdRoles", "3").param("correoUsuario", "nuevo@chickenbiker.com")
                        .param("passwordUsuario", "ClaveSegura1!").param("telefonoUsuario", "310")
                        .param("fechaNacimiento", "1999-05-10").param("direccionUsuario", "Calle 1"))
                .andExpect(redirectedUrl("/usuarios"));

        Usuarios nuevo = usuariosRepository.findByCorreoUsuario("nuevo@chickenbiker.com").orElseThrow();
        String hashInicial = nuevo.getPasswordUsuario();
        assertTrue(ContrasenaPasswordEncoder.esHashBcrypt(hashInicial));
        login("nuevo@chickenbiker.com", "ClaveSegura1!");

        // Edición sin contraseña: se conserva el hash exacto (no se recodifica)
        mvc.perform(post("/usuarios").session(admin).with(csrf())
                        .param("idUsuario", String.valueOf(nuevo.getIdUsuario()))
                        .param("nombreUsuario", "Nuevo Editado").param("apellidoUsuario", "Usuario")
                        .param("tipoDocumentoIdTipoDocumento", "1").param("documentoUsuario", "2001")
                        .param("rolesIdRoles", "3").param("correoUsuario", "nuevo@chickenbiker.com")
                        .param("passwordUsuario", "").param("telefonoUsuario", "310")
                        .param("fechaNacimiento", "1999-05-10").param("direccionUsuario", "Calle 1")
                        .param("fechaVencimiento", "2030-01-01").param("estadosUsuariosIdEstadosUsuarios", "1"))
                .andExpect(redirectedUrl("/usuarios"));
        assertEquals(hashInicial, usuariosRepository.findById(nuevo.getIdUsuario()).orElseThrow().getPasswordUsuario());

        // La API nunca devuelve contraseñas
        String json = mvc.perform(get("/api/usuarios/listar").session(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(json.contains("passwordUsuario"));
        assertFalse(json.contains("$2a$"));
    }

    @Test
    void todasLasVistasPrincipalesSeRenderizan() throws Exception {
        MockHttpSession admin = login("admin@chickenbiker.com", "123456");

        // Con caja cerrada
        for (String ruta : List.of("/dashboard/administrador", "/caja", "/caja/abrir", "/movimientos", "/pedidos/tomar")) {
            mvc.perform(get(ruta).session(admin)).andExpect(status().isOk());
        }

        // Abrir caja y crear un pedido desde los formularios
        mvc.perform(post("/caja/abrir").session(admin).with(csrf()).param("montoInicial", "10000"))
                .andExpect(redirectedUrl("/caja"));
        mvc.perform(post("/pedidos/guardar").session(admin).with(csrf())
                        .param("tipoPedido", "Mesa").param("mesaId", "2")
                        .param("productoId", "1").param("cantidad", "2"))
                .andExpect(redirectedUrl("/pedidos"));
        mvc.perform(post("/movimientos").session(admin).with(csrf())
                        .param("valor", "500").param("tipoMovimiento", "2").param("descripcion", "Hielo"))
                .andExpect(redirectedUrl("/movimientos"));

        List<String> rutas = List.of(
                "/dashboard/administrador", "/mesas", "/mesas/nuevo", "/mesas/editar/1",
                "/estados-mesas", "/estados-mesas/nuevo", "/estados-mesas/editar/1",
                "/productos", "/productos/nuevo", "/productos/editar/1",
                "/categorias", "/categorias/editar/1",
                "/roles", "/roles/nuevoRol", "/roles/editar/1",
                "/tipos-documento", "/tipos-documento/nuevo", "/tipos-documento/editar/1",
                "/usuarios", "/usuarios/nuevo", "/usuarios/editar/2",
                "/pedidos", "/pedidos/tomar", "/pedidos/nuevo", "/pedidos/detalle/1",
                "/pagos", "/caja", "/caja/cerrar", "/movimientos",
                "/recibos", "/recibos/1");
        for (String ruta : rutas) {
            mvc.perform(get(ruta).session(admin))
                    .andExpect(result -> assertEquals(200, result.getResponse().getStatus(),
                            "La vista " + ruta + " no se renderizó correctamente"));
        }

        // Cobro desde la vista y cierre de caja
        mvc.perform(post("/pagos/cobrar").session(admin).with(csrf())
                        .param("idCabeceraRecibo", "1").param("idMetodoPago", "1"))
                .andExpect(redirectedUrl("/pagos"))
                .andExpect(flash().attributeExists("mensaje"));
        mvc.perform(post("/caja/cerrar").session(admin).with(csrf())
                        .param("montoFinalReal", "33500").param("idUsuarioCierre", "1"))
                .andExpect(redirectedUrl("/caja"))
                .andExpect(flash().attributeExists("mensaje"));
    }

    @Test
    void crudConDescripcionObligatoria() throws Exception {
        MockHttpSession admin = login("admin@chickenbiker.com", "123456");
        mvc.perform(post("/categorias").session(admin).with(csrf()).param("nombreCategoria", "Bebidas"))
                .andExpect(redirectedUrl("/categorias")).andExpect(flash().attributeExists("mensaje"));
        mvc.perform(post("/roles").session(admin).with(csrf()).param("nombreRol", "Gerente"))
                .andExpect(redirectedUrl("/roles/listarRol")).andExpect(flash().attributeExists("mensaje"));
        mvc.perform(post("/tipos-documento").session(admin).with(csrf()).param("nombreTipoDocumento", "Pasaporte"))
                .andExpect(redirectedUrl("/tipos-documento"));
        mvc.perform(post("/estados-mesas").session(admin).with(csrf()).param("nombreEstadoMesa", "Limpieza"))
                .andExpect(redirectedUrl("/estados-mesas"));
        mvc.perform(post("/productos/cambiar-estado/1").session(admin).with(csrf()))
                .andExpect(redirectedUrl("/productos"));
    }

    @Test
    void logoutCierraLaSesion() throws Exception {
        MockHttpSession admin = login("admin@chickenbiker.com", "123456");
        mvc.perform(post("/logout").session(admin).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
        assertTrue(admin.isInvalid());
    }
}
