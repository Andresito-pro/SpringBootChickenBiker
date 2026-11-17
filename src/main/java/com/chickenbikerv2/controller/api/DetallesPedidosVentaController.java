package com.chickenbikerv2.controller.api;

import com.chickenbikerv2.entity.DetallesPedidosVenta;
import com.chickenbikerv2.service.DetallesPedidosVentaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/detalles-pedidos-venta")
public class DetallesPedidosVentaController {

    private final DetallesPedidosVentaService detallesPedidosVentaService;

    public DetallesPedidosVentaController(DetallesPedidosVentaService detallesPedidosVentaService) {
        this.detallesPedidosVentaService = detallesPedidosVentaService;
    }

    @GetMapping({"/listar", "/listarRoles"})
    public List<DetallesPedidosVenta> listar() {
        return detallesPedidosVentaService.listar();
    }

    @PostMapping("/guardar")
    public DetallesPedidosVenta guardar(@RequestBody DetallesPedidosVenta detallesPedidosVenta) {
        return detallesPedidosVentaService.guardar(detallesPedidosVenta);
    }
}
