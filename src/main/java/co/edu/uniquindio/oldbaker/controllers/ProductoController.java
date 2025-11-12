package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.ProductoHomeResponse;
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
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://old-baker-front.vercel.app", "https://localhost:4200", "http://localhost:4200", "https://www.oldbaker.shop"})
public class ProductoController {

    private final ProductoService productoService;

    // Listar productos
    @GetMapping
    public ResponseEntity<List<ProductoHomeResponse>> listarProductos() {
        List<ProductoHomeResponse> productos = productoService.listarProductosHome();
        return ResponseEntity.ok(productos);
    }

    // Obtener producto por ID
    //@RequestMapping("/api/productos")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtenerProducto(@PathVariable Long id) {
        ProductoResponse response = productoService.obtenerProductoPorId(id);
        return ResponseEntity.ok(response);
    }
}
