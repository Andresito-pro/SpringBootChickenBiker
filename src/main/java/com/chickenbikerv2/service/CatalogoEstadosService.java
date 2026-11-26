package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.EstadosPedidos;
import com.chickenbikerv2.repository.EstadosPedidosRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Estado de pedido "Anulado": se localiza por NOMBRE porque su id depende de la
 * base de datos (el script sql/04_...sql lo agrega con el siguiente id libre del
 * catálogo estados_pedidos). El id encontrado se guarda en memoria.
 */
@Service
public class CatalogoEstadosService {

    public static final String ANULADO = "Anulado";

    private static final String FALTA_MIGRACION =
            "El estado \"Anulado\" no existe en la base de datos. Ejecute el script "
                    + "sql/04_mejoras_permisos_anulacion_notificaciones_caja.sql.";

    private final EstadosPedidosRepository estadosPedidosRepository;

    private volatile Integer idPedidoAnulado;

    public CatalogoEstadosService(EstadosPedidosRepository estadosPedidosRepository) {
        this.estadosPedidosRepository = estadosPedidosRepository;
    }

    public int idEstadoPedidoAnulado() {
        Integer id = idPedidoAnulado;
        if (id == null) {
            id = estadosPedidosRepository.findFirstByNombreEstadoPedidoIgnoreCase(ANULADO)
                    .map(EstadosPedidos::getIdEstado)
                    .orElseThrow(() -> new IllegalStateException(FALTA_MIGRACION));
            idPedidoAnulado = id;
        }
        return id;
    }

    /** Igual que {@link #idEstadoPedidoAnulado()} pero devuelve -1 si falta (para consultas de solo lectura). */
    public int idEstadoPedidoAnuladoOSinDefinir() {
        try {
            return idEstadoPedidoAnulado();
        } catch (IllegalStateException e) {
            return -1;
        }
    }

    /** id -> nombre de todos los estados de pedido (para las etiquetas de los listados). */
    public Map<Integer, String> nombresEstadosPedido() {
        return estadosPedidosRepository.findAll().stream()
                .collect(Collectors.toMap(EstadosPedidos::getIdEstado, EstadosPedidos::getNombreEstadoPedido, (a, b) -> a));
    }
}
