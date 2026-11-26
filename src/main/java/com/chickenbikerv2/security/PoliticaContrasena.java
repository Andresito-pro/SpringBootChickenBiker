package com.chickenbikerv2.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Requisitos obligatorios para toda contraseña NUEVA o MODIFICADA (registro de
 * usuarios, edición con cambio de contraseña y API /api/usuarios/guardar).
 * Las mismas reglas las muestra validacionContrasena.js mientras se escribe,
 * pero quien decide es esta clase, en el servidor.
 *
 * No se usa como anotación de Bean Validation sobre la entidad Usuarios
 * porque ese campo guarda el HASH (y algunos registros heredados aún no
 * migrados): Hibernate Validator lo evaluaría en cada save() de la entidad,
 * por ejemplo al registrar la autorización de datos en el login, y rompería
 * a usuarios existentes. La política solo aplica a la contraseña en claro,
 * antes de codificarla. Las contraseñas existentes no se modifican.
 */
public final class PoliticaContrasena {

    public static final int LONGITUD_MINIMA = 8;
    /** BCrypt solo considera los primeros 72 bytes. */
    public static final int MAX_BYTES = 72;

    private static final Pattern MAYUSCULA = Pattern.compile("\\p{Lu}");
    private static final Pattern MINUSCULA = Pattern.compile("\\p{Ll}");
    private static final Pattern NUMERO = Pattern.compile("\\p{Nd}");
    /** Cualquier carácter que no sea letra, número ni espacio (p. ej. ! @ # $ % . _ -). */
    private static final Pattern ESPECIAL = Pattern.compile("[^\\p{L}\\p{N}\\s]");

    private PoliticaContrasena() {
    }

    /** Mensajes de los requisitos que la contraseña NO cumple (vacía si es válida). */
    public static List<String> requisitosIncumplidos(String contrasena) {
        List<String> faltantes = new ArrayList<>();
        if (contrasena == null || contrasena.isBlank()) {
            faltantes.add("La contraseña es obligatoria y no puede contener solo espacios.");
            return faltantes;
        }
        if (contrasena.codePointCount(0, contrasena.length()) < LONGITUD_MINIMA) {
            faltantes.add("mínimo " + LONGITUD_MINIMA + " caracteres");
        }
        if (!MAYUSCULA.matcher(contrasena).find()) {
            faltantes.add("al menos una letra mayúscula");
        }
        if (!MINUSCULA.matcher(contrasena).find()) {
            faltantes.add("al menos una letra minúscula");
        }
        if (!NUMERO.matcher(contrasena).find()) {
            faltantes.add("al menos un número");
        }
        if (!ESPECIAL.matcher(contrasena).find()) {
            faltantes.add("al menos un carácter especial");
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            faltantes.add("máximo " + MAX_BYTES + " bytes");
        }
        return faltantes;
    }

    public static boolean esValida(String contrasena) {
        return requisitosIncumplidos(contrasena).isEmpty();
    }

    /** Lanza IllegalArgumentException con un mensaje claro si la contraseña no es segura. */
    public static void validar(String contrasena) {
        List<String> faltantes = requisitosIncumplidos(contrasena);
        if (faltantes.isEmpty()) {
            return;
        }
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException(faltantes.get(0));
        }
        throw new IllegalArgumentException("La contraseña no es segura. Debe tener: " + String.join(", ", faltantes) + ".");
    }
}
