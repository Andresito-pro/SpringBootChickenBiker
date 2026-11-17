package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.CabecerasRecibos;
import com.chickenbikerv2.entity.EstadosRecibos;
import com.chickenbikerv2.entity.MetodosPago;
import com.chickenbikerv2.entity.Productos;
import com.chickenbikerv2.repository.CabecerasRecibosRepository;
import com.chickenbikerv2.repository.CuerposRecibosRepository;
import com.chickenbikerv2.repository.EstadosRecibosRepository;
import com.chickenbikerv2.repository.MetodosPagoRepository;
import com.chickenbikerv2.repository.PedidosVentasRepository;
import com.chickenbikerv2.repository.ProductosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Recibos internos de control y trazabilidad (no son facturas electrónicas DIAN).
 */
@Service
public class CabecerasRecibosService {

    private final CabecerasRecibosRepository cabecerasRecibosRepository;
    private final CuerposRecibosRepository cuerposRecibosRepository;
    private final PedidosVentasRepository pedidosVentasRepository;
    private final ProductosRepository productosRepository;
    private final MetodosPagoRepository metodosPagoRepository;
    private final EstadosRecibosRepository estadosRecibosRepository;

    public CabecerasRecibosService(CabecerasRecibosRepository cabecerasRecibosRepository,
                                   CuerposRecibosRepository cuerposRecibosRepository,
                                   PedidosVentasRepository pedidosVentasRepository,
                                   ProductosRepository productosRepository,
                                   MetodosPagoRepository metodosPagoRepository,
                                   EstadosRecibosRepository estadosRecibosRepository) {
        this.cabecerasRecibosRepository = cabecerasRecibosRepository;
        this.cuerposRecibosRepository = cuerposRecibosRepository;
        this.pedidosVentasRepository = pedidosVentasRepository;
        this.productosRepository = productosRepository;
        this.metodosPagoRepository = metodosPagoRepository;
        this.estadosRecibosRepository = estadosRecibosRepository;
    }

    public List<CabecerasRecibos> listar() {
        return cabecerasRecibosRepository.findAll();
    }

    public List<CabecerasRecibos> listarRecientes() {
        return cabecerasRecibosRepository.findAllByOrderByFechaEmisionDesc();
    }

    public Optional<CabecerasRecibos> buscarPorId(Integer id) {
        return cabecerasRecibosRepository.findById(id);
    }

    public Optional<CabecerasRecibos> buscarPorNumeroRecibo(String numeroRecibo) {
        return cabecerasRecibosRepository.findByNumeroRecibo(numeroRecibo);
    }

    public CabecerasRecibos guardar(CabecerasRecibos cabecerasRecibos) {
        return cabecerasRecibosRepository.save(cabecerasRecibos);
    }

    public void eliminar(Integer id) {
        cabecerasRecibosRepository.deleteById(id);
    }

    public Map<Integer, String> mapaMetodosPago() {
        return metodosPagoRepository.findAll().stream()
                .collect(Collectors.toMap(MetodosPago::getIdMetodoPago, MetodosPago::getNombreMetodo, (a, b) -> a));
    }

    public Map<Integer, String> mapaEstadosRecibo() {
        return estadosRecibosRepository.findAll().stream()
                .collect(Collectors.toMap(EstadosRecibos::getIdEstadoRecibo, EstadosRecibos::getNombreEstadoRecibo,
                        (a, b) -> a));
    }

    /** Datos completos de un recibo para su vista/impresión. */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetalleRecibo(int idCabeceraRecibo) {
        CabecerasRecibos recibo = cabecerasRecibosRepository.findById(idCabeceraRecibo)
                .orElseThrow(() -> new IllegalArgumentException("El recibo #" + idCabeceraRecibo + " no existe."));

        Map<Integer, String> nombresProductos = productosRepository.findAll().stream()
                .collect(Collectors.toMap(Productos::getIdProductos, Productos::getNombrePlato, (a, b) -> a));

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("recibo", recibo);
        resultado.put("pedido", pedidosVentasRepository.findById(recibo.getPedidoVentaIdPedidoVenta()).orElse(null));
        resultado.put("detalles", cuerposRecibosRepository.findByCabeceraReciboIdCabeceraRecibo(idCabeceraRecibo));
        resultado.put("nombresProductos", nombresProductos);
        resultado.put("nombreMetodoPago", mapaMetodosPago().get(recibo.getMetodoPagoIdMetodoPago()));
        resultado.put("nombreEstadoRecibo", mapaEstadosRecibo().get(recibo.getEstadoReciboIdEstadoRecibo()));
        return resultado;
    }
}
