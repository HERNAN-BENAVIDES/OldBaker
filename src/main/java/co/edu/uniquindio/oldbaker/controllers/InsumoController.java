package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.InsumoRequest;
import co.edu.uniquindio.oldbaker.dto.InsumoResponse;
import co.edu.uniquindio.oldbaker.services.InsumoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/insumos")
@RequiredArgsConstructor
public class InsumoController {
    private final InsumoService insumoService;

    @PostMapping
    public ResponseEntity<InsumoResponse> crearInsumo(@Valid @RequestBody InsumoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(insumoService.crearInsumo(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsumoResponse> obtenerInsumoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(insumoService.obtenerInsumoPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<InsumoResponse>> listarInsumos() {
        return ResponseEntity.ok(insumoService.listarInsumos());
    }

    @PutMapping("/{id}")
    public ResponseEntity<InsumoResponse> actualizarInsumo(@PathVariable Long id,
                                                           @Valid @RequestBody InsumoRequest request) {
        return ResponseEntity.ok(insumoService.actualizarInsumo(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarInsumo(@PathVariable Long id) {
        insumoService.eliminarInsumo(id);
        return ResponseEntity.noContent().build();
    }
}