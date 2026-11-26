-- Copia de la estructura REAL de la base chicken_biker (MariaDB), usada SOLO en
-- pruebas (EsquemaRealMariaDbTests) para validar que las entidades JPA coinciden
-- con ella. Igual al script original, salvo lo que H2 no interpreta:
-- sin las líneas SET @OLD_..., sin CREATE SCHEMA / USE y sin el prefijo `chicken_biker`.
-- No se ejecuta nunca contra MariaDB.

CREATE TABLE IF NOT EXISTS `categorias_productos` (
  `id_categoria_producto` INT NOT NULL AUTO_INCREMENT,
  `nombre_categoria` VARCHAR(45) NOT NULL,
  `descripcion_categoria` VARCHAR(100),
  PRIMARY KEY (`id_categoria_producto`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `roles` (
  `id_roles` INT NOT NULL AUTO_INCREMENT,
  `nombre_rol` VARCHAR(45) NOT NULL,
  `descripcion_rol` VARCHAR(100),
  PRIMARY KEY (`id_roles`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `tipos_documentos` (
  `id_tipo_documento` INT NOT NULL AUTO_INCREMENT,
  `nombre_tipo_documento` VARCHAR(45) NOT NULL,
  `descripcion_tipo_documento` VARCHAR(100),
  PRIMARY KEY (`id_tipo_documento`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `estados_usuarios` (
  `id_estados_usuarios` INT NOT NULL AUTO_INCREMENT,
  `nombre_estado` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`id_estados_usuarios`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `usuarios` (
  `id_usuario` INT NOT NULL AUTO_INCREMENT,
  `nombre_usuario` VARCHAR(45) NOT NULL,
  `apellido_usuario` VARCHAR(45) NOT NULL,
  `documento_usuario` VARCHAR(45) NOT NULL,
  `correo_usuario` VARCHAR(255) NOT NULL,
  `password_usuario` VARCHAR(255) NOT NULL,
  `telefono_usuario` VARCHAR(50) NOT NULL,
  `fecha_nacimiento` DATE NOT NULL,
  `direccion_usuario` VARCHAR(45) NOT NULL,
  `fecha_vencimiento` DATE NOT NULL,
  `autorizacion_datos` TINYINT NOT NULL,
  `roles_id_roles` INT NOT NULL,
  `tipo_documento_id_tipo_documento` INT NOT NULL,
  `estados_usuarios_id_estados_usuarios` INT NOT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE INDEX `documentoUsuario_UNIQUE` (`documento_usuario`),
  UNIQUE INDEX `correoUsuario_UNIQUE` (`correo_usuario`),
  INDEX `fk_usuario_roles1_idx` (`roles_id_roles`),
  INDEX `fk_usuario_tipo_documento1_idx` (`tipo_documento_id_tipo_documento`),
  INDEX `fk_usuarios_estados_usuarios1_idx` (`estados_usuarios_id_estados_usuarios`),
  CONSTRAINT `fk_usuario_roles1`
    FOREIGN KEY (`roles_id_roles`)
    REFERENCES `roles` (`id_roles`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_usuario_tipo_documento1`
    FOREIGN KEY (`tipo_documento_id_tipo_documento`)
    REFERENCES `tipos_documentos` (`id_tipo_documento`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_usuarios_estados_usuarios1`
    FOREIGN KEY (`estados_usuarios_id_estados_usuarios`)
    REFERENCES `estados_usuarios` (`id_estados_usuarios`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `productos` (
  `id_productos` INT NOT NULL AUTO_INCREMENT,
  `nombre_plato` VARCHAR(45) NOT NULL,
  `descripcion_productos` VARCHAR(45) NOT NULL,
  `precio_venta` DECIMAL(10,2) NOT NULL,
  `activo_producto` TINYINT NOT NULL,
  `categoria_producto_id_categoria_producto` INT NOT NULL,
  PRIMARY KEY (`id_productos`),
  INDEX `fk_Producto_categoria_producto1_idx` (`categoria_producto_id_categoria_producto`),
  CONSTRAINT `fk_Producto_categoria_producto1`
    FOREIGN KEY (`categoria_producto_id_categoria_producto`)
    REFERENCES `categorias_productos` (`id_categoria_producto`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `estados_mesas` (
  `id_estado_mesa` INT NOT NULL AUTO_INCREMENT,
  `nombre_estado_mesa` VARCHAR(45) NOT NULL,
  `descripcion_estado_mesa` VARCHAR(100),
  `activo_estado_mesa` TINYINT NOT NULL,
  PRIMARY KEY (`id_estado_mesa`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `mesas` (
  `id_mesa` INT NOT NULL AUTO_INCREMENT,
  `numero_mesa` INT NOT NULL,
  `estado_mesa_id_estado_mesa` INT NOT NULL,
  PRIMARY KEY (`id_mesa`),
  INDEX `fk_mesa_estado_mesa1_idx` (`estado_mesa_id_estado_mesa`),
  CONSTRAINT `fk_mesa_estado_mesa1`
    FOREIGN KEY (`estado_mesa_id_estado_mesa`)
    REFERENCES `estados_mesas` (`id_estado_mesa`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `estados_pedidos` (
  `id_estado` INT NOT NULL AUTO_INCREMENT,
  `nombre_estado_pedido` VARCHAR(45) NOT NULL,
  `activo_estado_pedido` TINYINT NOT NULL,
  PRIMARY KEY (`id_estado`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `pedidos_ventas` (
  `id_pedido_venta` INT NOT NULL AUTO_INCREMENT,
  `fecha_pedido` DATETIME NOT NULL,
  `tipo_pedido` VARCHAR(45) NOT NULL,
  `total_pedido` DECIMAL(10,2) NOT NULL,
  `usuario_id_usuario` INT NOT NULL,
  `mesa_id_mesa` INT NULL,
  `estado_pedido_id_estado` INT NOT NULL,
  PRIMARY KEY (`id_pedido_venta`),
  INDEX `fk_PedidoVenta_usuario1_idx` (`usuario_id_usuario`),
  INDEX `fk_PedidoVenta_mesa1_idx` (`mesa_id_mesa`),
  INDEX `fk_PedidoVenta_estado_pedido1_idx` (`estado_pedido_id_estado`),
  CONSTRAINT `fk_PedidoVenta_usuario1`
    FOREIGN KEY (`usuario_id_usuario`)
    REFERENCES `usuarios` (`id_usuario`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_PedidoVenta_mesa1`
    FOREIGN KEY (`mesa_id_mesa`)
    REFERENCES `mesas` (`id_mesa`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_PedidoVenta_estado_pedido1`
    FOREIGN KEY (`estado_pedido_id_estado`)
    REFERENCES `estados_pedidos` (`id_estado`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `detalles_pedidos_venta` (
  `id_detalle_pedido_venta` INT NOT NULL AUTO_INCREMENT,
  `cantidad` INT NOT NULL,
  `precio_unitario` DECIMAL(10,2) NOT NULL,
  `subtotal` DECIMAL(10,2) NOT NULL,
  `pedido_venta_id_pedido_venta` INT NOT NULL,
  `productos_id_productos` INT NOT NULL,
  PRIMARY KEY (`id_detalle_pedido_venta`),
  INDEX `fk_detalle_pedido_venta_pedido_venta1_idx` (`pedido_venta_id_pedido_venta`),
  INDEX `fk_detalle_pedido_venta_productos1_idx` (`productos_id_productos`),
  CONSTRAINT `fk_detalle_pedido_venta_pedido_venta1`
    FOREIGN KEY (`pedido_venta_id_pedido_venta`)
    REFERENCES `pedidos_ventas` (`id_pedido_venta`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_detalle_pedido_venta_productos1`
    FOREIGN KEY (`productos_id_productos`)
    REFERENCES `productos` (`id_productos`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `estados_recibos` (
  `id_estado_recibo` INT NOT NULL AUTO_INCREMENT,
  `nombre_estado_recibo` VARCHAR(45) NOT NULL,
  `activo_estado_recibo` TINYINT NOT NULL,
  PRIMARY KEY (`id_estado_recibo`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `metodos_pago` (
  `id_metodo_pago` INT NOT NULL AUTO_INCREMENT,
  `nombre_metodo` VARCHAR(45) NOT NULL,
  `activo_metodo` TINYINT NOT NULL,
  `tipo_metodo` VARCHAR(20) NULL,
  PRIMARY KEY (`id_metodo_pago`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `cajas` (
  `id_caja` INT NOT NULL AUTO_INCREMENT,
  `nombre_caja` VARCHAR(45) NOT NULL,
  `activo_caja` TINYINT NOT NULL,
  PRIMARY KEY (`id_caja`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `aperturas_cierres_caja` (
  `id_apertura_cierre_caja` INT NOT NULL AUTO_INCREMENT,
  `fecha_apertura` DATETIME NOT NULL,
  `monto_inicial` DECIMAL(10,2) NOT NULL,
  `fecha_cierre` DATETIME NULL,
  `monto_final_sistema` DECIMAL(10,2) NULL,
  `monto_final_real` DECIMAL(10,2) NULL,
  `diferencia` DECIMAL(10,2) NULL,
  `estado_cierre_caja` VARCHAR(45) NOT NULL,
  `caja_id_caja` INT NOT NULL,
  `usuario_id_usuario` INT NOT NULL,
  `usuario_id_usuario1` INT NOT NULL,
  PRIMARY KEY (`id_apertura_cierre_caja`),
  INDEX `fk_apertura_cierre_caja_caja1_idx` (`caja_id_caja`),
  INDEX `fk_apertura_cierre_caja_usuario1_idx` (`usuario_id_usuario`),
  INDEX `fk_apertura_cierre_caja_usuario2_idx` (`usuario_id_usuario1`),
  CONSTRAINT `fk_apertura_cierre_caja_caja1`
    FOREIGN KEY (`caja_id_caja`)
    REFERENCES `cajas` (`id_caja`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_apertura_cierre_caja_usuario1`
    FOREIGN KEY (`usuario_id_usuario`)
    REFERENCES `usuarios` (`id_usuario`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_apertura_cierre_caja_usuario2`
    FOREIGN KEY (`usuario_id_usuario1`)
    REFERENCES `usuarios` (`id_usuario`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `cabeceras_recibos` (
  `id_cabecera_recibo` INT NOT NULL AUTO_INCREMENT,
  `numero_recibo` VARCHAR(30) NOT NULL,
  `fecha_emision` DATETIME NOT NULL,
  `total_general` DECIMAL(10,2) NOT NULL,
  `pedido_venta_id_pedido_venta` INT NOT NULL,
  `estado_recibo_id_estado_recibo` INT NOT NULL,
  `metodo_pago_id_metodo_pago` INT NOT NULL,
  `apertura_cierre_caja_id_apertura_cierre_caja` INT NOT NULL,
  PRIMARY KEY (`id_cabecera_recibo`),
  INDEX `fk_cabecera_recibo_pedido_venta1_idx` (`pedido_venta_id_pedido_venta`),
  INDEX `fk_cabecera_recibo_estado_recibo1_idx` (`estado_recibo_id_estado_recibo`),
  INDEX `fk_cabecera_recibo_metodo_pago1_idx` (`metodo_pago_id_metodo_pago`),
  INDEX `fk_cabecera_recibo_apertura_cierre_caja1_idx` (`apertura_cierre_caja_id_apertura_cierre_caja`),
  CONSTRAINT `fk_cabecera_recibo_pedido_venta1`
    FOREIGN KEY (`pedido_venta_id_pedido_venta`)
    REFERENCES `pedidos_ventas` (`id_pedido_venta`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_cabecera_recibo_estado_recibo1`
    FOREIGN KEY (`estado_recibo_id_estado_recibo`)
    REFERENCES `estados_recibos` (`id_estado_recibo`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_cabecera_recibo_metodo_pago1`
    FOREIGN KEY (`metodo_pago_id_metodo_pago`)
    REFERENCES `metodos_pago` (`id_metodo_pago`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_cabecera_recibo_apertura_cierre_caja1`
    FOREIGN KEY (`apertura_cierre_caja_id_apertura_cierre_caja`)
    REFERENCES `aperturas_cierres_caja` (`id_apertura_cierre_caja`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `cuerpos_recibos` (
  `id_cuerpo_recibo` INT NOT NULL AUTO_INCREMENT,
  `cantidad_` INT NOT NULL,
  `precio_unitario` DECIMAL(10,2) NOT NULL,
  `sub_total` DECIMAL(10,2) NOT NULL,
  `productos_id_productos` INT NOT NULL,
  `cabecera_recibo_id_cabecera_recibo` INT NOT NULL,
  PRIMARY KEY (`id_cuerpo_recibo`),
  INDEX `fk_cuerpo_recibo_productos1_idx` (`productos_id_productos`),
  INDEX `fk_cuerpo_recibo_cabecera_recibo1_idx` (`cabecera_recibo_id_cabecera_recibo`),
  CONSTRAINT `fk_cuerpo_recibo_productos1`
    FOREIGN KEY (`productos_id_productos`)
    REFERENCES `productos` (`id_productos`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_cuerpo_recibo_cabecera_recibo1`
    FOREIGN KEY (`cabecera_recibo_id_cabecera_recibo`)
    REFERENCES `cabeceras_recibos` (`id_cabecera_recibo`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `tipos_movimientos` (
  `id_tipo_movimiento` INT NOT NULL AUTO_INCREMENT,
  `nombre_tipo_movimiento` VARCHAR(45) NOT NULL,
  `descripcion_tipo_movimiento` VARCHAR(45) NULL,
  PRIMARY KEY (`id_tipo_movimiento`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `movimientos_caja` (
  `id_movimiento_caja` INT NOT NULL AUTO_INCREMENT,
  `valor_movimiento_caja` FLOAT NOT NULL,
  `descripcion_moviento_caja` VARCHAR(45) NOT NULL,
  `fecha_movimiento_caja` DATE NOT NULL,
  `tipo_movimiento_id_tipo_movimiento` INT NOT NULL,
  `usuario_id_usuario` INT NOT NULL,
  `apertura_cierre_caja_id_apertura_cierre_caja` INT NOT NULL,
  PRIMARY KEY (`id_movimiento_caja`),
  INDEX `fk_movimiento_caja_tipo_movimiento1_idx` (`tipo_movimiento_id_tipo_movimiento`),
  INDEX `fk_movimiento_caja_usuario1_idx` (`usuario_id_usuario`),
  INDEX `fk_movimiento_caja_apertura_cierre_caja1_idx` (`apertura_cierre_caja_id_apertura_cierre_caja`),
  CONSTRAINT `fk_movimiento_caja_tipo_movimiento1`
    FOREIGN KEY (`tipo_movimiento_id_tipo_movimiento`)
    REFERENCES `tipos_movimientos` (`id_tipo_movimiento`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_movimiento_caja_usuario1`
    FOREIGN KEY (`usuario_id_usuario`)
    REFERENCES `usuarios` (`id_usuario`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_movimiento_caja_apertura_cierre_caja1`
    FOREIGN KEY (`apertura_cierre_caja_id_apertura_cierre_caja`)
    REFERENCES `aperturas_cierres_caja` (`id_apertura_cierre_caja`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `permisos` (
  `id_permisos` INT NOT NULL AUTO_INCREMENT,
  `nombre_permiso` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`id_permisos`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `permisos_roles` (
  `permisos_id_permisos` INT NOT NULL,
  `roles_id_roles` INT NOT NULL,
  PRIMARY KEY (`permisos_id_permisos`, `roles_id_roles`),
  INDEX `fk_permisos_has_roles_roles1_idx` (`roles_id_roles`),
  INDEX `fk_permisos_has_roles_permisos1_idx` (`permisos_id_permisos`),
  CONSTRAINT `fk_permisos_has_roles_permisos1`
    FOREIGN KEY (`permisos_id_permisos`)
    REFERENCES `permisos` (`id_permisos`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_permisos_has_roles_roles1`
    FOREIGN KEY (`roles_id_roles`)
    REFERENCES `roles` (`id_roles`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;
