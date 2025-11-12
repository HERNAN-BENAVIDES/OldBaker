package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.ReporteProveedorRequest;
import co.edu.uniquindio.oldbaker.dto.ReporteProveedorResponse;
import co.edu.uniquindio.oldbaker.model.DetalleProveedorPedido;
import co.edu.uniquindio.oldbaker.model.PedidoInsumo;
import co.edu.uniquindio.oldbaker.model.ReporteProveedor;
import co.edu.uniquindio.oldbaker.repositories.DetalleProveedorPedidoRepository;
import co.edu.uniquindio.oldbaker.repositories.PedidoInsumoRepository;
import co.edu.uniquindio.oldbaker.repositories.ReporteProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ReporteProveedorService {

    private final ReporteProveedorRepository reporteRepository;
    private final DetalleProveedorPedidoRepository detalleRepository;
    private final PedidoInsumoRepository pedidoRepository;

    public ReporteProveedorResponse crearReporte(ReporteProveedorRequest request) {
        ReporteProveedor reporte = new ReporteProveedor();
        reporte.setRazon(request.getRazon());
        reporte.setFechaDevolucion(LocalDate.now());
        reporte.setIdProveedor(request.getIdProveedor());

        // Si viene con detalleId, procesarlo normalmente
        if (request.getDetalleId() != null) {
            DetalleProveedorPedido detalle = detalleRepository.findById(request.getDetalleId())
                    .orElseThrow(() -> new RuntimeException("Detalle no encontrado con ID: " + request.getDetalleId()));

//            if (Boolean.TRUE.equals(detalle.getEsDevuelto())) {
//                throw new RuntimeException("El detalle ya fue marcado como devuelto");
//            }

            // Marcar detalle como devuelto
            detalle.setEsDevuelto(true);

            reporte.setEsDevolucion(true);
            reporte.setDetalle(detalle);

            reporte = reporteRepository.save(reporte);

            // Recalcular costo total del pedido asociado
            PedidoInsumo pedido = detalle.getPedido();
            if (pedido != null) {
                // Restar el costo del detalle devuelto del total actual
                double costoDetalleDevuelto = detalle.getCostoSubtotal();
                double nuevoTotal = pedido.getCostoTotal() - costoDetalleDevuelto;

                // Asegurar que no sea negativo
                if (nuevoTotal < 0) {
                    nuevoTotal = 0;
                }

                pedido.setCostoTotal(nuevoTotal);
                pedidoRepository.save(pedido);
            }
        } else {
            // Si NO viene con detalleId, crear reporte simple
            reporte.setEsDevolucion(false);
            reporte.setDetalle(null);
            reporte = reporteRepository.save(reporte);
        }

        return toDTO(reporte);
    }

    public ReporteProveedorResponse obtenerReportePorId(Long id) {
        ReporteProveedor reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado con ID: " + id));
        return toDTO(reporte);
    }

    public List<ReporteProveedorResponse> listarReportes() {
        return reporteRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public void eliminarReporte(Long id) {
        if (!reporteRepository.existsById(id)) {
            throw new RuntimeException("Reporte no encontrado con ID: " + id);
        }
        reporteRepository.deleteById(id);
    }

    private ReporteProveedorResponse toDTO(ReporteProveedor reporte) {
        ReporteProveedorResponse response = new ReporteProveedorResponse();
        response.setIdDevolucion(reporte.getIdDevolucion());
        response.setRazon(reporte.getRazon());
        response.setEsDevolucion(reporte.getEsDevolucion());
        response.setFechaDevolucion(reporte.getFechaDevolucion());
        response.setIdProveedor(reporte.getIdProveedor());

        DetalleProveedorPedido detalle = reporte.getDetalle();
        if (detalle != null) {
            response.setDetalleId(detalle.getIdDetalle());
            if (detalle.getInsumo() != null) {
                response.setInsumoNombre(detalle.getInsumo().getNombre());
            }
            response.setCantidadDevuelta(detalle.getCantidadInsumo());
        }

        return response;
    }
}

