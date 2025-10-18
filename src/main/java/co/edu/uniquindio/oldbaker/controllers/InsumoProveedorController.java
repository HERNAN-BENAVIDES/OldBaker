package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.InsumoProveedorResponse;
import co.edu.uniquindio.oldbaker.services.InsumoProveedorService;
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

    @GetMapping
    public ResponseEntity<List<InsumoProveedorResponse>> listarInsumos() {
        return ResponseEntity.ok(insumoProveedorService.listarInsumos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsumoProveedorResponse> obtenerInsumoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(insumoProveedorService.obtenerInsumoPorId(id));
    }
}
