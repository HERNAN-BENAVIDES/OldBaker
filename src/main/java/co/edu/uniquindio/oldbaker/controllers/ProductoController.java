package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.ProductoRequest;
import co.edu.uniquindio.oldbaker.dto.ProductoResponse;
import co.edu.uniquindio.oldbaker.services.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    // Crear producto con receta (Admin)
    @PostMapping
    public ResponseEntity<ProductoResponse> crearProductoConReceta(
            @RequestBody @Valid ProductoRequest request) {

        ProductoResponse response = productoService.crearProductoConReceta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Listar productos
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listarProductos() {
        List<ProductoResponse> productos = productoService.listarProductos();
        return ResponseEntity.ok(productos);
    }

    // Obtener producto por ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtenerProducto(@PathVariable Long id) {
        ProductoResponse response = productoService.obtenerProductoPorId(id);
        return ResponseEntity.ok(response);
    }

    // Eliminar producto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }
}
