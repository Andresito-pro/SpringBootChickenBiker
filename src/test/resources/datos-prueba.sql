-- Datos mínimos de prueba (mismos IDs y catálogos que el script semilla real).

TRUNCATE TABLE movimientos_caja RESTART IDENTITY;
TRUNCATE TABLE cuerpos_recibos RESTART IDENTITY;
TRUNCATE TABLE cabeceras_recibos RESTART IDENTITY;
TRUNCATE TABLE detalles_pedidos_venta RESTART IDENTITY;
TRUNCATE TABLE pedidos_ventas RESTART IDENTITY;
TRUNCATE TABLE aperturas_cierres_caja RESTART IDENTITY;
TRUNCATE TABLE usuarios RESTART IDENTITY;
TRUNCATE TABLE permisos_roles;
TRUNCATE TABLE permisos RESTART IDENTITY;
TRUNCATE TABLE roles RESTART IDENTITY;
TRUNCATE TABLE tipos_documentos RESTART IDENTITY;
TRUNCATE TABLE estados_usuarios RESTART IDENTITY;
TRUNCATE TABLE productos RESTART IDENTITY;
TRUNCATE TABLE categorias_productos RESTART IDENTITY;
TRUNCATE TABLE mesas RESTART IDENTITY;
TRUNCATE TABLE estados_mesas RESTART IDENTITY;
TRUNCATE TABLE estados_pedidos RESTART IDENTITY;
TRUNCATE TABLE estados_recibos RESTART IDENTITY;
TRUNCATE TABLE metodos_pago RESTART IDENTITY;
TRUNCATE TABLE cajas RESTART IDENTITY;
TRUNCATE TABLE tipos_movimientos RESTART IDENTITY;

INSERT INTO roles (id_roles, nombre_rol, descripcion_rol) VALUES
(1, 'Administrador', 'Control total del sistema'),
(2, 'Cajero', 'Gestiona pagos y caja'),
(3, 'Mesero', 'Gestiona pedidos de venta');

INSERT INTO permisos (id_permisos, nombre_permiso) VALUES
(4, 'Gestionar productos'), (5, 'Gestionar pedidos'), (6, 'Gestionar caja'),
(7, 'Consultar reportes'), (8, 'Gestionar permisos'), (9, 'Gestionar mesas'), (10, 'Gestionar roles'),
(11, 'Ver todos los pedidos'), (12, 'Anular pedidos'), (13, 'Anular pedidos pagados'), (14, 'Recibir notificaciones');

INSERT INTO permisos_roles (permisos_id_permisos, roles_id_roles) VALUES
(4, 1), (5, 1), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1), (11, 1), (12, 1), (13, 1), (14, 1),
(5, 2), (6, 2), (7, 2), (11, 2), (12, 2), (14, 2),
(5, 3);

INSERT INTO tipos_documentos (id_tipo_documento, nombre_tipo_documento, descripcion_tipo_documento) VALUES
(1, 'Cédula de ciudadanía', 'Documento de ciudadanía colombiano');

INSERT INTO estados_usuarios (id_estados_usuarios, nombre_estado) VALUES (1, 'Activo'), (2, 'Inactivo');

-- Contraseñas heredadas en TEXTO PLANO, como en el script semilla real.
INSERT INTO usuarios (id_usuario, nombre_usuario, apellido_usuario, documento_usuario, correo_usuario,
                      password_usuario, telefono_usuario, fecha_nacimiento, direccion_usuario, fecha_vencimiento,
                      autorizacion_datos, roles_id_roles, tipo_documento_id_tipo_documento,
                      estados_usuarios_id_estados_usuarios) VALUES
(1, 'Andres', 'Admin', '1001', 'admin@chickenbiker.com', '123456', '300', '2000-01-01', 'Bogota', '2035-01-01', 0, 1, 1, 1),
(2, 'Carla', 'Cajera', '1002', 'cajero@chickenbiker.com', '123456', '301', '2000-01-01', 'Bogota', '2035-01-01', 1, 2, 1, 1),
(3, 'Mario', 'Mesero', '1003', 'mesero@chickenbiker.com', '123456', '302', '2000-01-01', 'Bogota', '2035-01-01', 1, 3, 1, 1),
(4, 'Ines', 'Inactiva', '1004', 'inactivo@chickenbiker.com', '123456', '303', '2000-01-01', 'Bogota', '2035-01-01', 1, 3, 1, 2),
-- Cuenta ACTIVA pero VENCIDA (2.3) y un segundo mesero para probar la visibilidad (2.2)
(5, 'Vera', 'Vencida', '1005', 'vencido@chickenbiker.com', '123456', '304', '2000-01-01', 'Bogota', '2020-01-01', 1, 3, 1, 1),
(6, 'Sara', 'Mesera', '1006', 'mesero2@chickenbiker.com', '123456', '305', '2000-01-01', 'Bogota', '2035-01-01', 1, 3, 1, 1);
ALTER TABLE usuarios ALTER COLUMN id_usuario RESTART WITH 100;

INSERT INTO categorias_productos (id_categoria_producto, nombre_categoria, descripcion_categoria) VALUES
(1, 'Pollo', 'Productos de pollo broaster');

INSERT INTO productos (id_productos, nombre_plato, descripcion_productos, precio_venta, activo_producto,
                       categoria_producto_id_categoria_producto) VALUES
(1, 'Pollo 2 piezas', 'Dos piezas', 12000.00, 1, 1),
(2, 'Gaseosa', 'Bebida', 3000.00, 1, 1),
(3, 'Producto inactivo', 'No disponible', 1000.00, 0, 1);

INSERT INTO estados_mesas (id_estado_mesa, nombre_estado_mesa, descripcion_estado_mesa, activo_estado_mesa) VALUES
(1, 'Disponible', 'Mesa disponible', 1), (2, 'Ocupada', 'Mesa con pedido', 1),
(3, 'Reservada', 'Mesa reservada', 1), (4, 'Inactiva', 'Fuera de servicio', 1);

INSERT INTO mesas (id_mesa, numero_mesa, estado_mesa_id_estado_mesa) VALUES (1, 1, 1), (2, 2, 1), (3, 3, 3);

INSERT INTO estados_pedidos (id_estado, nombre_estado_pedido, activo_estado_pedido) VALUES
(1, 'Pendiente', 1), (2, 'En preparación', 1), (3, 'Listo', 1), (4, 'Entregado', 1), (5, 'Cancelado', 1),
(6, 'Anulado', 1);

INSERT INTO estados_recibos (id_estado_recibo, nombre_estado_recibo, activo_estado_recibo) VALUES
(1, 'Generado', 1), (2, 'Pagado', 1), (3, 'Anulado', 1);

INSERT INTO metodos_pago (id_metodo_pago, nombre_metodo, activo_metodo, tipo_metodo) VALUES
-- tipo_metodo NULL, como quedan las filas existentes tras el script 04 (se deduce del nombre)
(1, 'Efectivo', 1, NULL), (2, 'Tarjeta', 1, NULL), (3, 'Nequi', 1, NULL),
(5, 'Pendiente de pago', 0, NULL);

INSERT INTO cajas (id_caja, nombre_caja, activo_caja) VALUES (1, 'Caja Principal', 1);

INSERT INTO tipos_movimientos (id_tipo_movimiento, nombre_tipo_movimiento) VALUES (1, 'Ingreso'), (2, 'Egreso');
