package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.ReporteProveedorRequest;
import co.edu.uniquindio.oldbaker.dto.ReporteProveedorResponse;
import co.edu.uniquindio.oldbaker.services.ReporteProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes-proveedor")
@RequiredArgsConstructor
@Validated
public class ReporteProveedorController {

    private final ReporteProveedorService reporteService;

    @PostMapping
    public ResponseEntity<ReporteProveedorResponse> crearReporte(@RequestBody ReporteProveedorRequest request) {
        ReporteProveedorResponse response = reporteService.crearReporte(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ReporteProveedorResponse>> listarReportes() {
        return ResponseEntity.ok(reporteService.listarReportes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReporteProveedorResponse> obtenerReporte(@PathVariable Long id) {
        return ResponseEntity.ok(reporteService.obtenerReportePorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarReporte(@PathVariable Long id) {
        reporteService.eliminarReporte(id);
        return ResponseEntity.noContent().build();
    }
}

