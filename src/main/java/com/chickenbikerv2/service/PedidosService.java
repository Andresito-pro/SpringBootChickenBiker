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

@Service
@RequiredArgsConstructor
public class PedidosService {

    public static final String TIPO_MESA = "Mesa";
    public static final String TIPO_PARA_LLEVAR = "Para llevar";

    public static final int ESTADO_PEDIDO_PENDIENTE = 1;
    public static final int ESTADO_PEDIDO_ENTREGADO = 4;

    public static final int ESTADO_MESA_DISPONIBLE = 1;
    public static final int ESTADO_MESA_OCUPADA = 2;

    public static final int METODO_PAGO_PENDIENTE_ID = 5;
    public static final int TIPO_MOVIMIENTO_INGRESO_ID = 1;

    private final PedidosVentasRepository pedidosVentasRepository;
    private final ProductosRepository productosRepository;
    private final MesasRepository mesasRepository;
    private final CabecerasRecibosRepository cabecerasRecibosRepository;
    private final CuerposRecibosRepository cuerposRecibosRepository;
    private final AperturasCierresCajaRepository aperturasCierresCajaRepository;

    @Transactional(readOnly = true)
    public boolean hayCajaAbierta() {
        return obtenerCajaAbierta().isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<AperturasCierresCaja> obtenerCajaAbierta() {
        return aperturasCierresCajaRepository
                .findFirstByEstadoCierreCajaIgnoreCaseOrderByFechaAperturaDesc("Abierta");
    }

    @Transactional(readOnly = true)
    public List<Mesas> listarMesas() {
        return mesasRepository.findAllByOrderByNumeroMesaAsc();
    }

    @Transactional(readOnly = true)
    public List<Productos> listarProductosActivos() {
        return productosRepository.findByActivoProductoOrderByNombrePlatoAsc((byte) 1);
    }

    @Transactional(readOnly = true)
    public List<PedidosVentas> listarPedidos() {
        return pedidosVentasRepository.findAllByOrderByFechaPedidoDesc();
    }

    @Transactional(readOnly = true)
    public Map<Integer, CabecerasRecibos> obtenerMapaRecibosPorPedido() {
        return cabecerasRecibosRepository.findAll().stream()
                .collect(Collectors.toMap(
                        CabecerasRecibos::getPedidoVentaIdPedidoVenta,
                        Function.identity(),
                        (existente, reemplazo) -> reemplazo
                ));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetallePedido(int idPedido) {
        PedidosVentas pedido = pedidosVentasRepository.findById(idPedido)
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
        return resultado;
    }

    @Transactional
    public PedidosVentas registrarPedido(String tipoPedido,
                                         Integer mesaId,
                                         List<Integer> productosIds,
                                         List<Integer> cantidades,
                                         int idUsuario) {
        if (!hayCajaAbierta()) {
            throw new IllegalStateException("No hay una caja abierta. Abre la caja antes de registrar un pedido.");
        }

        String tipoNormalizado = TIPO_PARA_LLEVAR.equalsIgnoreCase(tipoPedido != null ? tipoPedido.trim() : "")
                ? TIPO_PARA_LLEVAR
                : TIPO_MESA;

        Integer mesaAsignada = null;
        if (TIPO_MESA.equals(tipoNormalizado)) {
            if (mesaId == null || mesaId <= 0) {
                throw new IllegalArgumentException("Debes seleccionar una mesa disponible.");
            }
            Mesas mesa = mesasRepository.findById(mesaId)
                    .orElseThrow(() -> new IllegalArgumentException("La mesa seleccionada no existe."));

            if (mesa.getEstadoMesaIdEstadoMesa() != ESTADO_MESA_DISPONIBLE) {
                throw new IllegalStateException("La Mesa " + mesa.getNumeroMesa() + " no está disponible.");
            }

            mesa.setEstadoMesaIdEstadoMesa(ESTADO_MESA_OCUPADA);
            mesasRepository.save(mesa);
            mesaAsignada = mesa.getIdMesa();
        }

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

            CuerposRecibos cuerpo = new CuerposRecibos();
            cuerpo.setProductosIdProductos(producto.getIdProductos());
            cuerpo.setCantidad(cantidad);
            cuerpo.setPrecioUnitario(precioUnitario);
            cuerpo.setSubTotal(subTotal);
            lineasRecibo.add(cuerpo);
        }

        LocalDateTime ahora = LocalDateTime.now();

        PedidosVentas pedido = new PedidosVentas();
        pedido.setFechaPedido(ahora);
        pedido.setTipoPedido(tipoNormalizado);
        pedido.setTotalPedido(totalPedido);
        pedido.setUsuarioIdUsuario(idUsuario);
        pedido.setMesaIdMesa(mesaAsignada);
        pedido.setEstadoPedidoIdEstado(ESTADO_PEDIDO_PENDIENTE);
        pedido = pedidosVentasRepository.save(pedido);

        CabecerasRecibos recibo = new CabecerasRecibos();
        recibo.setNumeroRecibo("TEMP");
        recibo.setFechaEmision(ahora);
        recibo.setTotalGeneral(totalPedido);
        recibo.setPedidoVentaIdPedidoVenta(pedido.getIdPedidoVenta());
        recibo.setMetodoPagoIdMetodoPago(METODO_PAGO_PENDIENTE_ID);
        recibo = cabecerasRecibosRepository.save(recibo);

        recibo.setNumeroRecibo("REC-" + recibo.getIdCabeceraRecibo());
        recibo = cabecerasRecibosRepository.save(recibo);

        for (CuerposRecibos cuerpo : lineasRecibo) {
            cuerpo.setCabeceraReciboIdCabeceraRecibo(recibo.getIdCabeceraRecibo());
        }
        cuerposRecibosRepository.saveAll(lineasRecibo);

        return pedido;
    }
}