package com.chickenbikerv2.controller.vista;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Formulario de inicio de sesión. El POST /login y el POST /logout los procesa
 * Spring Security (ver SecurityConfig).
 */
@Controller
public class LoginVistaController {

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/dashboard";
    }
}
