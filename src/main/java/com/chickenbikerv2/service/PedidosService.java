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
        resultado.put("idEstadoAnulado", catalogoEstadosService.idEstadoPedidoAnuladoOSinDefinir());
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

        // 2. Productos y cantidades
        if (productosIds == null || cantidades == null || productosIds.isEmpty()
                || productosIds.size() != cantidades.size()) {
            throw new IllegalArgumentException("Debes agregar al menos un producto válido al pedido.");
        }

        // Consolidar cantidades si un mismo producto viene repetido
        Map<Integer, Integer> itemsConsolidados = new LinkedHashMap<>();
        for (int i = 0; i < productosIds.size(); i++) {
            Integer idProd = productosIds.get(i);
            Integer cant = cantidades.get(i);
            if (idProd != null && cant != null && cant > 0) {
                itemsConsolidados.merge(idProd, cant, Integer::sum);
            }
        }
        if (itemsConsolidados.isEmpty()) {
            throw new IllegalArgumentException("Las cantidades de los productos deben ser mayores a cero.");
        }

        BigDecimal totalPedido = BigDecimal.ZERO;
        List<DetallesPedidosVenta> lineasPedido = new ArrayList<>();
        List<CuerposRecibos> lineasRecibo = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : itemsConsolidados.entrySet()) {
            int idProducto = entry.getKey();
            int cantidad = entry.getValue();

            Productos producto = productosRepository.findById(idProducto)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado (ID: " + idProducto + ")."));
            if (producto.getActivoProducto() == null || producto.getActivoProducto() != 1) {
                throw new IllegalStateException("El producto '" + producto.getNombrePlato() + "' no está disponible.");
            }

            BigDecimal precioUnitario = producto.getPrecioVenta();
            BigDecimal subTotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
            totalPedido = totalPedido.add(subTotal);

            DetallesPedidosVenta detalle = new DetallesPedidosVenta();
            detalle.setProductosIdProductos(producto.getIdProductos());
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(subTotal);
            lineasPedido.add(detalle);

            CuerposRecibos cuerpo = new CuerposRecibos();
            cuerpo.setProductosIdProductos(producto.getIdProductos());
            cuerpo.setCantidad(cantidad);
            cuerpo.setPrecioUnitario(precioUnitario);
            cuerpo.setSubTotal(subTotal);
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

    private static String mensajeMesaNoDisponible(Mesas mesa) {
        return switch (mesa.getEstadoMesaIdEstadoMesa()) {
            case ESTADO_MESA_OCUPADA -> "La mesa " + mesa.getNumeroMesa() + " está ocupada.";
            case ESTADO_MESA_RESERVADA -> "La mesa " + mesa.getNumeroMesa() + " está reservada.";
            case ESTADO_MESA_INACTIVA -> "La mesa " + mesa.getNumeroMesa() + " está inactiva.";
            default -> "La mesa " + mesa.getNumeroMesa() + " no está disponible.";
        };
    }
}
