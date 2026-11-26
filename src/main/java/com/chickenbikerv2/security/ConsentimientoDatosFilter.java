package com.chickenbikerv2.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Protección de datos personales (Ley 1581 de 2012), igual que LoginServlet en
 * la versión NetBeans: el inicio de sesión exige que el usuario marque la
 * autorización de tratamiento de datos. Se valida en el servidor porque el
 * atributo "required" del navegador puede omitirse.
 */
public class ConsentimientoDatosFilter extends OncePerRequestFilter {

    private static final String URL_LOGIN = "/login";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        if ("POST".equalsIgnoreCase(request.getMethod()) && URL_LOGIN.equals(ruta)
                && !"si".equals(request.getParameter("autorizacionDatos"))) {
            response.sendRedirect(request.getContextPath() + URL_LOGIN + "?consentimiento");
            return;
        }
        chain.doFilter(request, response);
    }
}
