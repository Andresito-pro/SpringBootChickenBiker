package com.chickenbikerv2.security;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nombres de los permisos (tabla permisos) que la aplicación verifica y su
 * descripción para la pestaña "Permisos" del administrador.
 *
 * La autorización es dinámica: SecurityConfig y los servicios comprueban la
 * autoridad con el NOMBRE del permiso, y AutoridadesVigentesFilter recarga en
 * cada petición los permisos del rol desde permisos_roles. Asignar o revocar
 * un permiso desde la interfaz se refleja en la siguiente petición del usuario.
 */
public final class PermisosSistema {

    public static final String GESTIONAR_PEDIDOS = "Gestionar pedidos";
    public static final String VER_TODOS_LOS_PEDIDOS = "Ver todos los pedidos";
    public static final String ANULAR_PEDIDOS = "Anular pedidos";
    public static final String ANULAR_PEDIDOS_PAGADOS = "Anular pedidos pagados";
    public static final String RECIBIR_NOTIFICACIONES = "Recibir notificaciones";
    public static final String GESTIONAR_CAJA = "Gestionar caja";
    public static final String CONSULTAR_REPORTES = "Consultar reportes";
    public static final String GESTIONAR_MESAS = "Gestionar mesas";
    public static final String GESTIONAR_PRODUCTOS = "Gestionar productos";
    public static final String GESTIONAR_ROLES = "Gestionar roles";
    public static final String GESTIONAR_PERMISOS = "Gestionar permisos";

    /**
     * @param modulo      agrupación en la pantalla de permisos
     * @param descripcion qué habilita el permiso
     * @param aplicado    true si la aplicación lo verifica; false si solo está
     *                    registrado en la base de datos (informativo)
     */
    public record Descripcion(String modulo, String descripcion, boolean aplicado) {
    }

    private static final Map<String, Descripcion> CATALOGO = new LinkedHashMap<>();

    static {
        CATALOGO.put(GESTIONAR_PEDIDOS, new Descripcion("Pedidos",
                "Tomar pedidos y consultar el apartado de pedidos.", true));
        CATALOGO.put(VER_TODOS_LOS_PEDIDOS, new Descripcion("Pedidos",
                "Ver los pedidos de todos los usuarios. Sin este permiso solo se ven los pedidos propios (meseros).", true));
        CATALOGO.put(ANULAR_PEDIDOS, new Descripcion("Pedidos",
                "Anular pedidos pendientes de cobro (cambia su estado a Anulado, con confirmación).", true));
        CATALOGO.put(ANULAR_PEDIDOS_PAGADOS, new Descripcion("Pedidos",
                "Procedimiento diferenciado para anular pedidos ya cobrados del turno abierto. No revierte dinero automáticamente.", true));
        CATALOGO.put(RECIBIR_NOTIFICACIONES, new Descripcion("Notificaciones",
                "Campana de notificaciones: aviso de cada nuevo pedido registrado.", true));
        CATALOGO.put(GESTIONAR_CAJA, new Descripcion("Caja",
                "Abrir y cerrar la caja, cobrar pedidos y registrar movimientos.", true));
        CATALOGO.put(CONSULTAR_REPORTES, new Descripcion("Reportes",
                "Consultar los recibos internos.", true));
        CATALOGO.put(GESTIONAR_MESAS, new Descripcion("Mesas",
                "Crear, editar y cambiar el estado de las mesas y sus estados.", true));
        CATALOGO.put(GESTIONAR_PRODUCTOS, new Descripcion("Productos",
                "Gestionar productos y categorías del menú.", true));
        CATALOGO.put(GESTIONAR_ROLES, new Descripcion("Administración",
                "Crear y editar roles.", true));
        CATALOGO.put(GESTIONAR_PERMISOS, new Descripcion("Administración",
                "Informativo: la pestaña Permisos es exclusiva del rol Administrador.", false));
        CATALOGO.put("Crear usuarios", new Descripcion("Administración",
                "Informativo: el módulo Usuarios es exclusivo del rol Administrador.", false));
        CATALOGO.put("Editar usuarios", new Descripcion("Administración",
                "Informativo: el módulo Usuarios es exclusivo del rol Administrador.", false));
        CATALOGO.put("Consultar usuarios", new Descripcion("Administración",
                "Informativo: el módulo Usuarios es exclusivo del rol Administrador.", false));
    }

    private PermisosSistema() {
    }

    /**
     * ¿La autenticación ACTUAL (recalculada en cada petición por
     * AutoridadesVigentesFilter) incluye el permiso?
     */
    public static boolean tiene(org.springframework.security.core.Authentication autenticacion, String permiso) {
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(a -> permiso.equals(a.getAuthority()));
    }

    /** Descripción de un permiso; los permisos creados fuera de la aplicación se muestran como informativos. */
    public static Descripcion describir(String nombrePermiso) {
        Descripcion d = CATALOGO.get(nombrePermiso);
        return d != null ? d
                : new Descripcion("Otros", "Permiso registrado en la base de datos sin uso en la aplicación.", false);
    }
}
