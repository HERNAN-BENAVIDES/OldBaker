package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.PedidoInsumoRequest;
import co.edu.uniquindio.oldbaker.dto.PedidoInsumoResponse;
import co.edu.uniquindio.oldbaker.dto.ReporteProveedorRequest;
import co.edu.uniquindio.oldbaker.dto.ReporteProveedorResponse;
import co.edu.uniquindio.oldbaker.services.PedidoInsumoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pedidos-insumos")
@RequiredArgsConstructor
public class PedidoInsumoAdminController {

    private final PedidoInsumoService pedidoInsumoService;

    // 1. Crear un pedido
    @PostMapping
    public ResponseEntity<PedidoInsumoResponse> crearPedido(
            @Valid @RequestBody PedidoInsumoRequest request) {
        PedidoInsumoResponse response = pedidoInsumoService.crearPedido(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/proveedor")
    public ResponseEntity<Long> obtenerProveedorPorPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoInsumoService.obtenerProveedorPorPedido(id));
    }

    // 2. Obtener todos los pedidos
    @GetMapping
    public ResponseEntity<List<PedidoInsumoResponse>> obtenerPedidos() {
        return ResponseEntity.ok(pedidoInsumoService.listarPedidos());
    }

    // 3. Obtener pedido por ID
    @GetMapping("/{id}")
    public ResponseEntity<PedidoInsumoResponse> obtenerPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoInsumoService.buscarPorId(id));
    }

    // 4. Aprobar pedido (Auxiliar)
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<PedidoInsumoResponse> aprobarPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoInsumoService.aprobarPedido(id));
    }

    // 5. Pagar pedido (Admin)
    @PutMapping("/{id}/pagar")
    public ResponseEntity<PedidoInsumoResponse> pagarPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoInsumoService.pagarPedido(id));
    }

    @PostMapping("/{id}/devoluciones")
    public ResponseEntity<ReporteProveedorResponse> devolverInsumo(
            @PathVariable Long id,
            @RequestBody @Valid ReporteProveedorRequest request) {

        ReporteProveedorResponse response = pedidoInsumoService.devolverInsumo(id, request);
        return ResponseEntity.ok(response);
    }

}
