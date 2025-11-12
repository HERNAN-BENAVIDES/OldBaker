package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.DetalleProveedorPedidoRequest;
import co.edu.uniquindio.oldbaker.dto.DetalleProveedorPedidoResponse;
import co.edu.uniquindio.oldbaker.services.DetalleProveedorPedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/detalles-proveedor-pedido")
@RequiredArgsConstructor
public class DetalleProveedorPedidoController {

    private final DetalleProveedorPedidoService detalleService;

    // Crear un nuevo detalle para un pedido
    @PostMapping("/pedido/{idPedido}")
    public ResponseEntity<DetalleProveedorPedidoResponse> crearDetalle(
            @PathVariable Long idPedido,
            @Valid @RequestBody DetalleProveedorPedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(detalleService.crearDetalle(request, idPedido));
    }

    // Obtener un detalle por ID
    @GetMapping("/{id}")
    public ResponseEntity<DetalleProveedorPedidoResponse> obtenerDetallePorId(@PathVariable Long id) {
        return ResponseEntity.ok(detalleService.obtenerDetallePorId(id));
    }

    // Listar todos los detalles
    @GetMapping
    public ResponseEntity<List<DetalleProveedorPedidoResponse>> listarDetalles() {
        return ResponseEntity.ok(detalleService.listarDetalles());
    }

    // Listar detalles por pedido
    @GetMapping("/pedido/{idPedido}")
    public ResponseEntity<List<DetalleProveedorPedidoResponse>> listarDetallesPorPedido(@PathVariable Long idPedido) {
        return ResponseEntity.ok(detalleService.listarDetallesPorPedido(idPedido));
    }

    // Listar detalles por insumo
    @GetMapping("/insumo/{idInsumo}")
    public ResponseEntity<List<DetalleProveedorPedidoResponse>> listarDetallesPorInsumo(@PathVariable Long idInsumo) {
        return ResponseEntity.ok(detalleService.listarDetallesPorInsumo(idInsumo));
    }

    // Listar detalles devueltos
    @GetMapping("/devueltos")
    public ResponseEntity<List<DetalleProveedorPedidoResponse>> listarDetallesDevueltos() {
        return ResponseEntity.ok(detalleService.listarDetallesDevueltos());
    }

    // Actualizar un detalle
    @PutMapping("/{id}")
    public ResponseEntity<DetalleProveedorPedidoResponse> actualizarDetalle(
            @PathVariable Long id,
            @Valid @RequestBody DetalleProveedorPedidoRequest request) {
        return ResponseEntity.ok(detalleService.actualizarDetalle(id, request));
    }

    // Eliminar un detalle
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDetalle(@PathVariable Long id) {
        detalleService.eliminarDetalle(id);
        return ResponseEntity.noContent().build();
    }

    // Marcar un detalle como devuelto
    @PatchMapping("/{id}/devolver")
    public ResponseEntity<DetalleProveedorPedidoResponse> marcarComoDevuelto(@PathVariable Long id) {
        return ResponseEntity.ok(detalleService.marcarComoDevuelto(id));
    }
}

