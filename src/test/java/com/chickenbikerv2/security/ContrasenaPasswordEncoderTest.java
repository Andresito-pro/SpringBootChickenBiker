package com.chickenbikerv2.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class ContrasenaPasswordEncoderTest {

    private final ContrasenaPasswordEncoder encoder = new ContrasenaPasswordEncoder();

    @Test
    void codificaConBcryptYNuncaEnTextoPlano() {
        String hash = encoder.encode("secreta1");
        assertNotEquals("secreta1", hash);
        assertTrue(ContrasenaPasswordEncoder.esHashBcrypt(hash));
        assertTrue(hash.startsWith("$2a$12$"), "Debe usar coste 12, igual que la versión NetBeans");
        assertTrue(encoder.matches("secreta1", hash));
        assertFalse(encoder.matches("otra", hash));
        assertFalse(encoder.upgradeEncoding(hash));
    }

    @Test
    void verificaHashesExistentesDeJbcryptSinRecodificarlos() {
        // Hash $2a$ como los que generaba jBCrypt (BCrypt.hashpw) en la versión NetBeans
        String hashJbcrypt = new BCryptPasswordEncoder(10).encode("clave123");
        assertTrue(encoder.matches("clave123", hashJbcrypt));
        assertFalse(encoder.matches("clave124", hashJbcrypt));
        assertFalse(encoder.upgradeEncoding(hashJbcrypt));
    }

    @Test
    void reconoceContrasenaHeredadaEnTextoPlanoYSolicitaMigrarla() {
        assertTrue(encoder.matches("123456", "123456"));
        assertFalse(encoder.matches("1234567", "123456"));
        assertTrue(encoder.upgradeEncoding("123456"));
    }

    @Test
    void rechazaValoresVacios() {
        assertFalse(encoder.matches("x", null));
        assertFalse(encoder.matches("x", ""));
        assertFalse(encoder.matches(null, "x"));
    }
}
