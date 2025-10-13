package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.ProveedorRequest;
import co.edu.uniquindio.oldbaker.services.ProveedorService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @PostMapping
    public ResponseEntity<ProveedorRequest> crearProveedor(@Validated @RequestBody ProveedorRequest dto) {
        return ResponseEntity.ok(proveedorService.crearProveedor(dto));
    }

    @GetMapping
    public ResponseEntity<List<ProveedorRequest>> listarProveedores() {
        return ResponseEntity.ok(proveedorService.listarProveedores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorRequest> obtenerProveedor(@PathVariable Long id) {
        return proveedorService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorRequest> actualizarProveedor(@PathVariable Long id,
                                                                @Validated @RequestBody ProveedorRequest dto) {
        return ResponseEntity.ok(proveedorService.actualizarProveedor(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProveedor(@PathVariable Long id) {
        proveedorService.eliminarProveedor(id);
        return ResponseEntity.noContent().build();
    }
}
