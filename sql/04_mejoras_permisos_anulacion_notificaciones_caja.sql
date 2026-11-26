-- =====================================================================
-- FASE 4 — Datos de catálogo para permisos dinámicos y estado ANULADO de
--          pedidos — Chicken Biker POS (MariaDB / XAMPP)
--
-- SOLO DATOS: no crea, altera ni elimina tablas o columnas. La aplicación usa
-- exclusivamente la estructura existente de la base `chicken_biker`.
-- Se recomienda un respaldo previo.
--
-- El script es NO destructivo e idempotente:
--   * No elimina filas ni modifica registros existentes (solo INSERT de filas
--     nuevas en los catálogos permisos, permisos_roles y estados_pedidos).
--   * Puede ejecutarse más de una vez sin duplicar datos
--     (INSERT ... SELECT ... WHERE NOT EXISTS / INSERT IGNORE).
-- =====================================================================

USE `chicken_biker`;

-- ---------------------------------------------------------------------
-- 1) PERMISOS NUEVOS (tabla existente `permisos`)
--    Se identifican por nombre, igual que el resto de permisos del sistema
--    (SecurityConfig usa hasAuthority('<nombre>')).
-- ---------------------------------------------------------------------
--  'Ver todos los pedidos'   : sin él, el usuario solo ve los pedidos que registró (meseros).
--  'Anular pedidos'          : anular pedidos PENDIENTES de cobro.
--  'Anular pedidos pagados'  : procedimiento diferenciado para pedidos ya cobrados.
--  'Recibir notificaciones'  : campana de notificaciones de nuevos pedidos.
INSERT INTO permisos (nombre_permiso)
SELECT 'Ver todos los pedidos' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permisos WHERE nombre_permiso = 'Ver todos los pedidos');

INSERT INTO permisos (nombre_permiso)
SELECT 'Anular pedidos' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permisos WHERE nombre_permiso = 'Anular pedidos');

INSERT INTO permisos (nombre_permiso)
SELECT 'Anular pedidos pagados' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permisos WHERE nombre_permiso = 'Anular pedidos pagados');

INSERT INTO permisos (nombre_permiso)
SELECT 'Recibir notificaciones' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permisos WHERE nombre_permiso = 'Recibir notificaciones');

-- 'Gestionar permisos' se asigna abajo al Administrador; se crea solo si no existe.
INSERT INTO permisos (nombre_permiso)
SELECT 'Gestionar permisos' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permisos WHERE nombre_permiso = 'Gestionar permisos');

-- Asignación inicial (después se gestiona desde la pestaña "Permisos" del administrador).
-- INSERT IGNORE respeta la PK compuesta (permisos_id_permisos, roles_id_roles): no duplica
-- ni modifica asignaciones existentes.
INSERT IGNORE INTO permisos_roles (permisos_id_permisos, roles_id_roles)
SELECT p.id_permisos, r.id_roles
FROM permisos p
JOIN roles r ON r.nombre_rol IN ('Administrador', 'Cajero')
WHERE p.nombre_permiso IN ('Ver todos los pedidos', 'Anular pedidos', 'Recibir notificaciones');

INSERT IGNORE INTO permisos_roles (permisos_id_permisos, roles_id_roles)
SELECT p.id_permisos, r.id_roles
FROM permisos p
JOIN roles r ON r.nombre_rol = 'Administrador'
WHERE p.nombre_permiso IN ('Anular pedidos pagados', 'Gestionar permisos');

-- ---------------------------------------------------------------------
-- 2) ESTADO DE PEDIDO "Anulado" (tabla existente `estados_pedidos`)
--    La anulación de un pedido consiste ÚNICAMENTE en cambiar
--    pedidos_ventas.estado_pedido_id_estado al id de este estado, usando la
--    relación existente con estados_pedidos. No se crean tablas ni columnas
--    para registrar la anulación.
--    Se inserta solo si no existe ya un estado con ese nombre (sin distinguir
--    mayúsculas ni espacios). Toma el siguiente id libre; la aplicación lo
--    localiza por nombre, no por un id fijo.
-- ---------------------------------------------------------------------
INSERT INTO estados_pedidos (nombre_estado_pedido, activo_estado_pedido)
SELECT 'Anulado', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM estados_pedidos
                  WHERE UPPER(TRIM(nombre_estado_pedido)) = 'ANULADO');


-- ---------------------------------------------------------------------
-- 3) Sin cambios de estructura
--    * metodos_pago.tipo_metodo ya existe en la base (VARCHAR(20) NULL). Si está
--      vacía, la aplicación clasifica el método por su nombre ("efectivo" ->
--      EFECTIVO; "tarjeta", "datáfono", "crédito", "débito" -> TARJETA; el resto
--      -> DIGITAL).
--    * Cobro -> movimiento de caja: se relacionan por la descripción que genera
--      cada cobro ("Cobro REC-<n> pedido #<id>"); no se agrega ninguna columna a
--      movimientos_caja.
--    * Notificaciones: se derivan de pedidos_ventas; no se crea ninguna tabla.
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- Verificación rápida (solo lectura)
-- ---------------------------------------------------------------------
SELECT r.nombre_rol, p.nombre_permiso
FROM permisos_roles pr
JOIN roles r ON r.id_roles = pr.roles_id_roles
JOIN permisos p ON p.id_permisos = pr.permisos_id_permisos
ORDER BY r.nombre_rol, p.nombre_permiso;

-- Debe existir exactamente un estado "Anulado"
SELECT id_estado, nombre_estado_pedido, activo_estado_pedido FROM estados_pedidos ORDER BY id_estado;

SELECT id_metodo_pago, nombre_metodo, tipo_metodo FROM metodos_pago;
