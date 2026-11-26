package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.security.SesionUsuario;
import com.chickenbikerv2.service.AperturasCierresCajaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Expone a todas las vistas el rol y los permisos del usuario en sesión, para
 * que el sidebar muestre solo los módulos permitidos (igual que hacía
 * AutenticacionFilter con atributos de request). Es solo presentación: la
 * autorización real la aplica SecurityConfig en el servidor.
 *
 * También expone el estado ACTUAL de la caja para el indicador común del topbar
 * (fragments/topbar). Se consulta en cada renderizado, nunca desde la sesión;
 * js/topbar.js lo vuelve a consultar en segundo plano (/api/caja/estado).
 */
@ControllerAdvice(basePackages = "com.chickenbikerv2.controller.vista")
public class SesionModelAdvice {

    private final AperturasCierresCajaService aperturasCierresCajaService;

    public SesionModelAdvice(AperturasCierresCajaService aperturasCierresCajaService) {
        this.aperturasCierresCajaService = aperturasCierresCajaService;
    }

    @ModelAttribute("permisosSesion")
    public List<?> permisosSesion(HttpSession session) {
        Object permisos = session.getAttribute(SesionUsuario.PERMISOS);
        return permisos instanceof List<?> lista ? lista : null;
    }

    @ModelAttribute("nombreRolSesion")
    public String nombreRolSesion(HttpSession session) {
        Object rol = session.getAttribute(SesionUsuario.NOMBRE_ROL);
        return rol instanceof String s ? s : null;
    }

    @ModelAttribute("cajaAbiertaGlobal")
    public Boolean cajaAbiertaGlobal(HttpSession session) {
        if (SesionUsuario.obtener(session) == null) {
            return null; // páginas públicas (login): no se consulta
        }
        return aperturasCierresCajaService.hayCajaAbierta();
    }
}
