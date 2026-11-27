package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.*;
import com.chickenbikerv2.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registro y consulta de pedidos.
 *
 * Al registrar un pedido (en una sola transacción, como PedidoServlet):
 *  1. Se exige una caja abierta.
 *  2. Pedido en mesa: la mesa debe estar disponible y queda ocupada.
 *     Pedido para llevar: no se consulta ni se modifica ninguna mesa (mesa NULL).
 *  3. Se guardan el pedido, su detalle (detalles_pedidos_venta), la cabecera del
 *     recibo interno en estado "Generado" asociada al turno de caja, y su cuerpo.
 *
 * Mientras el recibo siga "Generado" el pedido se puede editar
 * ({@link #actualizarPedido}): p. ej. cuando llega otra persona a una mesa ya
 * ocupada se agregan productos al mismo pedido en lugar de ocupar otra mesa.
 */
@Service
@RequiredArgsConstructor
public class PedidosService {

    public static final String TIPO_MESA = "Mesa";
    public static final String TIPO_PARA_LLEVAR = "Para llevar";

    public static final int ESTADO_PEDIDO_PENDIENTE = 1;
    public static final int ESTADO_PEDIDO_ENTREGADO = 4;

    public static final int ESTADO_MESA_DISPONIBLE = 1;
    public static final int ESTADO_MESA_OCUPADA = 2;
    public static final int ESTADO_MESA_RESERVADA = 3;
    public static final int ESTADO_MESA_INACTIVA = 4;

    /** estados_recibos: 1 = Generado (pendiente de cobro), 2 = Pagado. */
    public static final int ESTADO_RECIBO_GENERADO = 1;
    public static final int ESTADO_RECIBO_PAGADO = 2;

    /**
     * Método de pago placeholder (inactivo) creado por el script de la fase 3:
     * la columna metodo_pago_id_metodo_pago es NOT NULL y el método real se
     * conoce recién al cobrar.
     */
    public static final int METODO_PAGO_PENDIENTE_ID = 5;
    public static final int TIPO_MOVIMIENTO_INGRESO_ID = MovimientosCajaService.TIPO_MOV_INGRESO;

    private final PedidosVentasRepository pedidosVentasRepository;
    private final DetallesPedidosVentaRepository detallesPedidosVentaRepository;
    private final ProductosRepository productosRepository;
    private final MesasRepository mesasRepository;
    private final CabecerasRecibosRepository cabecerasRecibosRepository;
    private final CuerposRecibosRepository cuerposRecibosRepository;
    private final AperturasCierresCajaService aperturasCierresCajaService;
    private final CatalogoEstadosService catalogoEstadosService;

    public boolean hayCajaAbierta() {
        return aperturasCierresCajaService.hayCajaAbierta();
    }

    public Optional<AperturasCierresCaja> obtenerCajaAbierta() {
        return aperturasCierresCajaService.consultarCajaAbierta();
    }

    @Transactional(readOnly = true)
    public List<Mesas> listarMesas() {
        return mesasRepository.findAllByOrderByNumeroMesaAsc();
    }

    @Transactional(readOnly = true)
    public List<Productos> listarProductosActivos() {
        return productosRepository.findByActivoProductoOrderByNombrePlatoAsc((byte) 1);
    }

    /**
     * Pedidos visibles para el usuario autenticado. La restricción se aplica en
     * la CONSULTA: sin el permiso "Ver todos los pedidos" (meseros) solo se
     * obtienen los pedidos que registró el propio usuario. {@code idUsuario}
     * sale siempre de la sesión del servidor, nunca de un parámetro del cliente.
     */
    @Transactional(readOnly = true)
    public List<PedidosVentas> listarPedidos(int idUsuario, boolean verTodos) {
        return verTodos
                ? pedidosVentasRepository.findAllByOrderByFechaPedidoDesc()
                : pedidosVentasRepository.findByUsuarioIdUsuarioOrderByFechaPedidoDesc(idUsuario);
    }

    /** Recibo de cada pedido del listado (solo de los pedidos recibidos, no de todos). */
    @Transactional(readOnly = true)
    public Map<Integer, CabecerasRecibos> obtenerMapaRecibosPorPedido(Collection<PedidosVentas> pedidos) {
        Set<Integer> ids = pedidos.stream().map(PedidosVentas::getIdPedidoVenta).collect(Collectors.toSet());
        return cabecerasRecibosRepository.findAll().stream()
                .filter(r -> ids.contains(r.getPedidoVentaIdPedidoVenta()))
                .collect(Collectors.toMap(
                        CabecerasRecibos::getPedidoVentaIdPedidoVenta,
                        Function.identity(),
                        (existente, reemplazo) -> reemplazo
                ));
    }

    /** Ids de los pedidos en estado Anulado (los recibos no cambian de estado al anular). */
    @Transactional(readOnly = true)
    public Set<Integer> idsPedidosAnulados() {
        return new HashSet<>(pedidosVentasRepository.idsPorEstado(catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir()));
    }

    /**
     * Pedido individual con la misma regla de visibilidad que el listado. Si el
     * usuario no puede verlo, se responde igual que si no existiera (no se
     * revela la existencia de pedidos de otros meseros).
     */
    @Transactional(readOnly = true)
    public Optional<PedidosVentas> buscarPedidoVisible(int idPedido, int idUsuario, boolean verTodos) {
        return verTodos
                ? pedidosVentasRepository.findById(idPedido)
                : pedidosVentasRepository.findByIdPedidoVentaAndUsuarioIdUsuario(idPedido, idUsuario);
    }

    /** Número visible de cada mesa (las vistas muestran el número, no el id). */
    @Transactional(readOnly = true)
    public Map<Integer, Integer> obtenerMapaNumerosMesa() {
        return mesasRepository.findAll().stream()
                .collect(Collectors.toMap(Mesas::getIdMesa, Mesas::getNumeroMesa, (a, b) -> a));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetallePedido(int idPedido, int idUsuario, boolean verTodos) {
        PedidosVentas pedido = buscarPedidoVisible(idPedido, idUsuario, verTodos)
                .orElseThrow(() -> new IllegalArgumentException("El pedido #" + idPedido + " no existe."));

        CabecerasRecibos recibo = cabecerasRecibosRepository
                .findByPedidoVentaIdPedidoVenta(idPedido)
                .orElse(null);

        List<CuerposRecibos> detalles = (recibo != null)
                ? cuerposRecibosRepository.findByCabeceraReciboIdCabeceraRecibo(recibo.getIdCabeceraRecibo())
                : Collections.emptyList();

        Map<Integer, String> nombresProductos = productosRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Productos::getIdProductos,
                        Productos::getNombrePlato,
                        (a, b) -> a
                ));

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("pedido", pedido);
        resultado.put("recibo", recibo);
        resultado.put("detalles", detalles);
        resultado.put("nombresProductos", nombresProductos);
        resultado.put("numeroMesa", pedido.getMesaIdMesa() == null ? null
                : mesasRepository.findById(pedido.getMesaIdMesa()).map(Mesas::getNumeroMesa).orElse(null));
        int idEstadoAnulado = catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir();
        resultado.put("idEstadoAnulado", idEstadoAnulado);
        resultado.put("editable", esEditable(pedido, recibo, idEstadoAnulado));
        resultado.put("nombresEstadosPedido", catalogoEstadosService.nombresEstadosPedido());
        return resultado;
    }

    @Transactional
    public PedidosVentas registrarPedido(String tipoPedido,
                                         Integer mesaId,
                                         List<Integer> productosIds,
                                         List<Integer> cantidades,
                                         int idUsuario) {

        // 1. Caja abierta (con bloqueo de la caja 1 para no competir con un cierre simultáneo)
        AperturasCierresCaja cajaAbierta = aperturasCierresCajaService.bloquearYConsultarCajaAbierta()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay una caja abierta. Abre la caja antes de registrar un pedido."));

        String tipoNormalizado = TIPO_PARA_LLEVAR.equalsIgnoreCase(tipoPedido != null ? tipoPedido.trim() : "")
                ? TIPO_PARA_LLEVAR
                : TIPO_MESA;

        // 2. Productos y cantidades (todos al precio de venta actual)
        List<LineaPedido> lineas = calcularLineas(consolidarItems(productosIds, cantidades), Map.of());
        BigDecimal totalPedido = sumarTotal(lineas);

        List<DetallesPedidosVenta> lineasPedido = new ArrayList<>();
        List<CuerposRecibos> lineasRecibo = new ArrayList<>();
        for (LineaPedido linea : lineas) {
            DetallesPedidosVenta detalle = new DetallesPedidosVenta();
            linea.aplicarA(detalle);
            lineasPedido.add(detalle);

            CuerposRecibos cuerpo = new CuerposRecibos();
            linea.aplicarA(cuerpo);
            lineasRecibo.add(cuerpo);
        }

        // 3. Mesa: solo para pedidos en mesa (bloqueada para que dos meseros no la tomen a la vez)
        Integer mesaAsignada = null;
        if (TIPO_MESA.equals(tipoNormalizado)) {
            if (mesaId == null || mesaId <= 0) {
                throw new IllegalArgumentException("Debes seleccionar una mesa disponible o elegir \"Para llevar\".");
            }
            Mesas mesa = mesasRepository.bloquearPorId(mesaId)
                    .orElseThrow(() -> new IllegalArgumentException("La mesa seleccionada no existe."));
            if (mesa.getEstadoMesaIdEstadoMesa() != ESTADO_MESA_DISPONIBLE) {
                throw new IllegalStateException(mensajeMesaNoDisponible(mesa));
            }
            mesa.setEstadoMesaIdEstadoMesa(ESTADO_MESA_OCUPADA);
            mesasRepository.save(mesa);
            mesaAsignada = mesa.getIdMesa();
        }

        LocalDateTime ahora = LocalDateTime.now();

        // 4. Pedido
        PedidosVentas pedido = new PedidosVentas();
        pedido.setFechaPedido(ahora);
        pedido.setTipoPedido(tipoNormalizado);
        pedido.setTotalPedido(totalPedido);
        pedido.setUsuarioIdUsuario(idUsuario);
        pedido.setMesaIdMesa(mesaAsignada);
        pedido.setEstadoPedidoIdEstado(ESTADO_PEDIDO_PENDIENTE);
        pedido = pedidosVentasRepository.save(pedido);

        for (DetallesPedidosVenta detalle : lineasPedido) {
            detalle.setPedidoVentaIdPedidoVenta(pedido.getIdPedidoVenta());
        }
        detallesPedidosVentaRepository.saveAll(lineasPedido);

        // 5. Recibo interno (no es factura electrónica): queda "Generado" hasta el cobro
        CabecerasRecibos recibo = new CabecerasRecibos();
        recibo.setNumeroRecibo("REC-" + pedido.getIdPedidoVenta());
        recibo.setFechaEmision(ahora);
        recibo.setTotalGeneral(totalPedido);
        recibo.setPedidoVentaIdPedidoVenta(pedido.getIdPedidoVenta());
        recibo.setEstadoReciboIdEstadoRecibo(ESTADO_RECIBO_GENERADO);
        recibo.setMetodoPagoIdMetodoPago(METODO_PAGO_PENDIENTE_ID);
        recibo.setAperturaCierreCajaIdAperturaCierreCaja(cajaAbierta.getIdAperturaCierreCaja());
        recibo = cabecerasRecibosRepository.save(recibo);

        for (CuerposRecibos cuerpo : lineasRecibo) {
            cuerpo.setCabeceraReciboIdCabeceraRecibo(recibo.getIdCabeceraRecibo());
        }
        cuerposRecibosRepository.saveAll(lineasRecibo);

        // La campana de Administrador / Cajero deriva sus avisos de pedidos_ventas
        // (NotificacionesService): no hay que registrar nada más aquí.
        return pedido;
    }

    // =====================================================================
    // EDICIÓN DE PEDIDOS
    // =====================================================================

    /**
     * Un pedido se puede editar mientras siga pendiente de cobro: tiene recibo
     * en estado "Generado", no fue entregado/cobrado y no está anulado.
     */
    public static boolean esEditable(PedidosVentas pedido, CabecerasRecibos recibo, int idEstadoAnulado) {
        return pedido != null
                && recibo != null
                && recibo.getEstadoReciboIdEstadoRecibo() == ESTADO_RECIBO_GENERADO
                && pedido.getEstadoPedidoIdEstado() != ESTADO_PEDIDO_ENTREGADO
                && pedido.getEstadoPedidoIdEstado() != idEstadoAnulado;
    }

    /** Ids de los pedidos del listado que todavía se pueden editar. */
    @Transactional(readOnly = true)
    public Set<Integer> idsPedidosEditables(Collection<PedidosVentas> pedidos,
                                            Map<Integer, CabecerasRecibos> recibosPorPedido) {
        int idAnulado = catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir();
        return pedidos.stream()
                .filter(p -> esEditable(p, recibosPorPedido.get(p.getIdPedidoVenta()), idAnulado))
                .map(PedidosVentas::getIdPedidoVenta)
                .collect(Collectors.toSet());
    }

    /**
     * Mesa ocupada -> pedido editable que la ocupa, solo con los pedidos que el
     * usuario puede ver. Permite que "Tomar pedido" ofrezca agregar productos al
     * pedido activo de la mesa en vez de ocupar otra.
     */
    @Transactional(readOnly = true)
    public Map<Integer, Integer> obtenerPedidosEditablesPorMesa(int idUsuario, boolean verTodos) {
        int idAnulado = catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir();
        Map<Integer, Integer> resultado = new HashMap<>();
        // Orden ascendente por fecha: si una mesa tuviera varios, gana el más reciente.
        for (PedidosVentas pedido : pedidosVentasRepository
                .listarPedidosEnMesaExcluyendoEstados(List.of(ESTADO_PEDIDO_ENTREGADO, idAnulado))) {
            if (!verTodos && pedido.getUsuarioIdUsuario() != idUsuario) {
                continue;
            }
            CabecerasRecibos recibo = cabecerasRecibosRepository
                    .findByPedidoVentaIdPedidoVenta(pedido.getIdPedidoVenta()).orElse(null);
            if (esEditable(pedido, recibo, idAnulado)) {
                resultado.put(pedido.getMesaIdMesa(), pedido.getIdPedidoVenta());
            }
        }
        return resultado;
    }

    /**
     * Datos para el formulario de edición: el pedido, su mesa y sus productos
     * actuales (con el precio al que se pidieron). Falla si no es visible para
     * el usuario o si ya no se puede editar.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerPedidoParaEditar(int idPedido, int idUsuario, boolean verTodos) {
        PedidosVentas pedido = buscarPedidoVisible(idPedido, idUsuario, verTodos)
                .orElseThrow(() -> new IllegalArgumentException("El pedido #" + idPedido + " no existe."));
        CabecerasRecibos recibo = cabecerasRecibosRepository.findByPedidoVentaIdPedidoVenta(idPedido).orElse(null);
        verificarEditable(pedido, recibo);

        Map<Integer, Productos> productos = productosRepository.findAll().stream()
                .collect(Collectors.toMap(Productos::getIdProductos, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> items = new ArrayList<>();
        for (DetallesPedidosVenta detalle : detallesPedidosVentaRepository.findByPedidoVentaIdPedidoVenta(idPedido)) {
            Productos producto = productos.get(detalle.getProductosIdProductos());
            Map<String, Object> item = new LinkedHashMap<>();
            // El id va como texto: el carrito (tomarPedido.js) compara con data-id.
            item.put("id", String.valueOf(detalle.getProductosIdProductos()));
            item.put("nombre", producto != null ? producto.getNombrePlato()
                    : "Producto #" + detalle.getProductosIdProductos());
            item.put("precio", detalle.getPrecioUnitario().toPlainString());
            item.put("cantidad", detalle.getCantidad());
            items.add(item);
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("pedido", pedido);
        resultado.put("items", items);
        resultado.put("numeroMesa", pedido.getMesaIdMesa() == null ? null
                : mesasRepository.findById(pedido.getMesaIdMesa()).map(Mesas::getNumeroMesa).orElse(null));
        return resultado;
    }

    /**
     * Reemplaza los productos de un pedido pendiente (agregar, cambiar
     * cantidades o quitar líneas) y deja sincronizados el detalle del pedido,
     * el cuerpo del recibo y ambos totales.
     *
     * - La mesa y su estado no cambian: el pedido sigue ocupándola.
     * - Los productos que ya estaban conservan el precio al que se pidieron
     *   (aunque luego se hayan desactivado); los nuevos toman el precio de
     *   venta actual y deben estar activos.
     * - Bloqueos en el mismo orden que el cobro y la anulación
     *   (caja -> recibo -> pedido) para no cruzarse con ellos.
     */
    @Transactional
    public PedidosVentas actualizarPedido(int idPedido,
                                          List<Integer> productosIds,
                                          List<Integer> cantidades,
                                          int idUsuario,
                                          boolean verTodos) {

        aperturasCierresCajaService.bloquearYConsultarCajaAbierta()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay una caja abierta. Abre la caja antes de modificar un pedido."));

        // Visibilidad primero: si el usuario no puede verlo, se responde como si no existiera.
        buscarPedidoVisible(idPedido, idUsuario, verTodos)
                .orElseThrow(() -> new IllegalArgumentException("El pedido #" + idPedido + " no existe."));

        CabecerasRecibos recibo = cabecerasRecibosRepository.findByPedidoVentaIdPedidoVenta(idPedido)
                .flatMap(r -> cabecerasRecibosRepository.bloquearPorId(r.getIdCabeceraRecibo()))
                .orElse(null);
        PedidosVentas pedido = pedidosVentasRepository.bloquearPorId(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("El pedido #" + idPedido + " no existe."));
        verificarEditable(pedido, recibo);

        List<DetallesPedidosVenta> detallesActuales =
                detallesPedidosVentaRepository.findByPedidoVentaIdPedidoVenta(idPedido);
        List<CuerposRecibos> cuerposActuales =
                cuerposRecibosRepository.findByCabeceraReciboIdCabeceraRecibo(recibo.getIdCabeceraRecibo());

        Map<Integer, BigDecimal> preciosOriginales = new HashMap<>();
        for (DetallesPedidosVenta detalle : detallesActuales) {
            preciosOriginales.putIfAbsent(detalle.getProductosIdProductos(), detalle.getPrecioUnitario());
        }

        List<LineaPedido> lineas = calcularLineas(consolidarItems(productosIds, cantidades), preciosOriginales);
        BigDecimal total = sumarTotal(lineas);

        sincronizarDetalles(idPedido, detallesActuales, lineas);
        sincronizarCuerposRecibo(recibo.getIdCabeceraRecibo(), cuerposActuales, lineas);

        pedido.setTotalPedido(total);
        recibo.setTotalGeneral(total);
        cabecerasRecibosRepository.save(recibo);
        return pedidosVentasRepository.save(pedido);
    }

    private void verificarEditable(PedidosVentas pedido, CabecerasRecibos recibo) {
        int idAnulado = catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir();
        if (esEditable(pedido, recibo, idAnulado)) {
            return;
        }
        int id = pedido.getIdPedidoVenta();
        if (pedido.getEstadoPedidoIdEstado() == idAnulado) {
            throw new IllegalStateException("El pedido #" + id + " está anulado y no se puede modificar.");
        }
        if (recibo == null) {
            throw new IllegalStateException("El pedido #" + id + " no tiene recibo asociado y no se puede modificar.");
        }
        throw new IllegalStateException("El pedido #" + id + " ya fue cobrado y no se puede modificar.");
    }

    /** Reutiliza las filas de los productos que siguen, crea las nuevas y borra las quitadas. */
    private void sincronizarDetalles(int idPedido, List<DetallesPedidosVenta> actuales, List<LineaPedido> lineas) {
        Map<Integer, DetallesPedidosVenta> porProducto = new HashMap<>();
        List<DetallesPedidosVenta> sobrantes = new ArrayList<>();
        for (DetallesPedidosVenta detalle : actuales) {
            if (porProducto.putIfAbsent(detalle.getProductosIdProductos(), detalle) != null) {
                sobrantes.add(detalle); // filas repetidas del mismo producto
            }
        }
        List<DetallesPedidosVenta> aGuardar = new ArrayList<>();
        for (LineaPedido linea : lineas) {
            DetallesPedidosVenta detalle = porProducto.remove(linea.idProducto());
            if (detalle == null) {
                detalle = new DetallesPedidosVenta();
                detalle.setPedidoVentaIdPedidoVenta(idPedido);
            }
            linea.aplicarA(detalle);
            aGuardar.add(detalle);
        }
        sobrantes.addAll(porProducto.values());
        detallesPedidosVentaRepository.deleteAll(sobrantes);
        detallesPedidosVentaRepository.saveAll(aGuardar);
    }

    private void sincronizarCuerposRecibo(int idRecibo, List<CuerposRecibos> actuales, List<LineaPedido> lineas) {
        Map<Integer, CuerposRecibos> porProducto = new HashMap<>();
        List<CuerposRecibos> sobrantes = new ArrayList<>();
        for (CuerposRecibos cuerpo : actuales) {
            if (porProducto.putIfAbsent(cuerpo.getProductosIdProductos(), cuerpo) != null) {
                sobrantes.add(cuerpo);
            }
        }
        List<CuerposRecibos> aGuardar = new ArrayList<>();
        for (LineaPedido linea : lineas) {
            CuerposRecibos cuerpo = porProducto.remove(linea.idProducto());
            if (cuerpo == null) {
                cuerpo = new CuerposRecibos();
                cuerpo.setCabeceraReciboIdCabeceraRecibo(idRecibo);
            }
            linea.aplicarA(cuerpo);
            aGuardar.add(cuerpo);
        }
        sobrantes.addAll(porProducto.values());
        cuerposRecibosRepository.deleteAll(sobrantes);
        cuerposRecibosRepository.saveAll(aGuardar);
    }

    // =====================================================================
    // CÁLCULO DE LÍNEAS (compartido por registrar y actualizar)
    // =====================================================================

    /** Línea ya validada y calculada de un pedido. */
    private record LineaPedido(int idProducto, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {

        void aplicarA(DetallesPedidosVenta detalle) {
            detalle.setProductosIdProductos(idProducto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(subtotal);
        }

        void aplicarA(CuerposRecibos cuerpo) {
            cuerpo.setProductosIdProductos(idProducto);
            cuerpo.setCantidad(cantidad);
            cuerpo.setPrecioUnitario(precioUnitario);
            cuerpo.setSubTotal(subtotal);
        }
    }

    /** Valida las listas recibidas y suma las cantidades de un mismo producto repetido. */
    private static Map<Integer, Integer> consolidarItems(List<Integer> productosIds, List<Integer> cantidades) {
        if (productosIds == null || cantidades == null || productosIds.isEmpty()
                || productosIds.size() != cantidades.size()) {
            throw new IllegalArgumentException("Debes agregar al menos un producto válido al pedido.");
        }
        Map<Integer, Integer> items = new LinkedHashMap<>();
        for (int i = 0; i < productosIds.size(); i++) {
            Integer idProd = productosIds.get(i);
            Integer cant = cantidades.get(i);
            if (idProd != null && cant != null && cant > 0) {
                items.merge(idProd, cant, Integer::sum);
            }
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Las cantidades de los productos deben ser mayores a cero.");
        }
        return items;
    }

    /**
     * Calcula precio y subtotal de cada producto. Si el producto está en
     * {@code preciosConservados} (ya estaba en el pedido) se respeta ese precio;
     * si no, debe existir, estar activo y se usa su precio de venta actual.
     */
    private List<LineaPedido> calcularLineas(Map<Integer, Integer> items, Map<Integer, BigDecimal> preciosConservados) {
        List<LineaPedido> lineas = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            int idProducto = entry.getKey();
            int cantidad = entry.getValue();

            BigDecimal precioUnitario = preciosConservados.get(idProducto);
            if (precioUnitario == null) {
                Productos producto = productosRepository.findById(idProducto)
                        .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado (ID: " + idProducto + ")."));
                if (producto.getActivoProducto() == null || producto.getActivoProducto() != 1) {
                    throw new IllegalStateException("El producto '" + producto.getNombrePlato() + "' no está disponible.");
                }
                precioUnitario = producto.getPrecioVenta();
            }
            lineas.add(new LineaPedido(idProducto, cantidad, precioUnitario,
                    precioUnitario.multiply(BigDecimal.valueOf(cantidad))));
        }
        return lineas;
    }

    private static BigDecimal sumarTotal(List<LineaPedido> lineas) {
        return lineas.stream().map(LineaPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String mensajeMesaNoDisponible(Mesas mesa) {
        return switch (mesa.getEstadoMesaIdEstadoMesa()) {
            case ESTADO_MESA_OCUPADA -> "La mesa " + mesa.getNumeroMesa() + " está ocupada.";
            case ESTADO_MESA_RESERVADA -> "La mesa " + mesa.getNumeroMesa() + " está reservada.";
            case ESTADO_MESA_INACTIVA -> "La mesa " + mesa.getNumeroMesa() + " está inactiva.";
            default -> "La mesa " + mesa.getNumeroMesa() + " no está disponible.";
        };
    }
}
