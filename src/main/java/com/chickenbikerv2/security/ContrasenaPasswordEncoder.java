package com.chickenbikerv2.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

/**
 * Codificador de contraseñas de Chicken Biker.
 *
 * - Toda contraseña nueva o modificada se guarda SIEMPRE como hash BCrypt
 *   (coste 12, el mismo que usaba la versión NetBeans con jBCrypt).
 * - Los hashes existentes $2a$/$2b$/$2y$ (generados por jBCrypt) se verifican
 *   con BCrypt.matches(), sin volver a codificarlos.
 * - Compatibilidad: el script semilla de la base de datos insertó usuarios con
 *   la contraseña en TEXTO PLANO. Esas filas se reconocen porque no tienen
 *   formato BCrypt; se comparan en tiempo constante y, tras un inicio de sesión
 *   correcto, Spring Security las vuelve a guardar como BCrypt
 *   (upgradeEncoding + UserDetailsPasswordService). Así ninguna contraseña se
 *   modifica sin haber comprobado antes su formato y su validez.
 */
public class ContrasenaPasswordEncoder implements PasswordEncoder {

    public static final int COSTE_BCRYPT = 12;

    private static final Pattern FORMATO_BCRYPT =
            Pattern.compile("^\\$2[aby]?\\$\\d{2}\\$[./0-9A-Za-z]{53}$");

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(COSTE_BCRYPT);

    public static boolean esHashBcrypt(String valor) {
        return valor != null && FORMATO_BCRYPT.matcher(valor).matches();
    }

    @Override
    public String encode(CharSequence contrasenaPlano) {
        return bcrypt.encode(contrasenaPlano);
    }

    @Override
    public boolean matches(CharSequence contrasenaPlano, String almacenada) {
        if (contrasenaPlano == null || almacenada == null || almacenada.isEmpty()) {
            return false;
        }
        if (esHashBcrypt(almacenada)) {
            return bcrypt.matches(contrasenaPlano, almacenada);
        }
        // Registro heredado en texto plano: comparación en tiempo constante.
        return MessageDigest.isEqual(
                contrasenaPlano.toString().getBytes(StandardCharsets.UTF_8),
                almacenada.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean upgradeEncoding(String almacenada) {
        return almacenada != null && !esHashBcrypt(almacenada);
    }
}
