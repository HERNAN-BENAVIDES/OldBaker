package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.InsumoProveedorRequest;
import co.edu.uniquindio.oldbaker.dto.InsumoProveedorResponse;
import co.edu.uniquindio.oldbaker.services.InsumoProveedorService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/insumos-proveedores")
@CrossOrigin(origins = {"https://old-baker-front.vercel.app", "https://localhost:4200", "http://localhost:4200", "https://www.oldbaker.shop"})
public class InsumoProveedorController {

    private final InsumoProveedorService insumoProveedorService;

    public InsumoProveedorController(InsumoProveedorService insumoProveedorService) {
        this.insumoProveedorService = insumoProveedorService;
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<InsumoProveedorResponse>> listarInsumosPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(insumoProveedorService.listarInsumosPaginado(page, size));
    }

    // Listar todos los insumos
    @GetMapping
    public ResponseEntity<List<InsumoProveedorResponse>> listarInsumos() {
        return ResponseEntity.ok(insumoProveedorService.listarInsumos());
    }

    // Obtener un insumo por ID
    @GetMapping("/{id}")
    public ResponseEntity<InsumoProveedorResponse> obtenerInsumoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(insumoProveedorService.obtenerInsumoPorId(id));
    }

    // Listar insumos por ID de proveedor
    @GetMapping("/proveedor/{idProveedor}")
    public ResponseEntity<List<InsumoProveedorResponse>> listarInsumosPorIdProveedor(@PathVariable Long idProveedor) {
        return ResponseEntity.ok(insumoProveedorService.listarInsumosPorIdProveedor(idProveedor));
    }

    // Crear un nuevo insumo
    @PostMapping
    public ResponseEntity<InsumoProveedorResponse> crearInsumo(@RequestBody InsumoProveedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insumoProveedorService.crearInsumo(request));
    }

    // Actualizar un insumo existente
    @PutMapping("/{id}")
    public ResponseEntity<InsumoProveedorResponse> actualizarInsumo(@PathVariable Long id, @RequestBody InsumoProveedorRequest request) {
        return ResponseEntity.ok(insumoProveedorService.actualizarInsumo(id, request));
    }

    // Eliminar un insumo
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarInsumo(@PathVariable Long id) {
        insumoProveedorService.eliminarInsumo(id);
        return ResponseEntity.noContent().build();
    }
}