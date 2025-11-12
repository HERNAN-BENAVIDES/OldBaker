package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.DetalleProveedorPedidoRequest;
import co.edu.uniquindio.oldbaker.dto.DetalleProveedorPedidoResponse;
import co.edu.uniquindio.oldbaker.dto.InsumoProveedorResponse;
import co.edu.uniquindio.oldbaker.model.DetalleProveedorPedido;
import co.edu.uniquindio.oldbaker.model.InsumoProveedor;
import co.edu.uniquindio.oldbaker.model.PedidoInsumo;
import co.edu.uniquindio.oldbaker.repositories.DetalleProveedorPedidoRepository;
import co.edu.uniquindio.oldbaker.repositories.InsumoProveedorRepository;
import co.edu.uniquindio.oldbaker.repositories.PedidoInsumoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DetalleProveedorPedidoService {

    private final DetalleProveedorPedidoRepository detalleRepository;
    private final InsumoProveedorRepository insumoProveedorRepository;
    private final PedidoInsumoRepository pedidoInsumoRepository;

    // Crear un nuevo detalle
    public DetalleProveedorPedidoResponse crearDetalle(DetalleProveedorPedidoRequest request, Long idPedido) {
        DetalleProveedorPedido detalle = toEntity(request, idPedido);
        return toDTO(detalleRepository.save(detalle));
    }

    // Obtener detalle por ID
    public DetalleProveedorPedidoResponse obtenerDetallePorId(Long id) {
        DetalleProveedorPedido detalle = detalleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado con ID: " + id));
        return toDTO(detalle);
    }

    // Listar todos los detalles
    public List<DetalleProveedorPedidoResponse> listarDetalles() {
        return detalleRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Listar detalles por pedido
    public List<DetalleProveedorPedidoResponse> listarDetallesPorPedido(Long idPedido) {
        return detalleRepository.findByPedidoIdPedido(idPedido)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Listar detalles por insumo
    public List<DetalleProveedorPedidoResponse> listarDetallesPorInsumo(Long idInsumo) {
        return detalleRepository.findByInsumoIdInsumo(idInsumo)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Listar detalles devueltos
    public List<DetalleProveedorPedidoResponse> listarDetallesDevueltos() {
        return detalleRepository.findByEsDevuelto(true)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Actualizar un detalle existente
    public DetalleProveedorPedidoResponse actualizarDetalle(Long id, DetalleProveedorPedidoRequest request) {
        DetalleProveedorPedido detalle = detalleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado con ID: " + id));

        InsumoProveedor insumo = insumoProveedorRepository.findById(request.getInsumoProveedorId())
                .orElseThrow(() -> new RuntimeException("Insumo proveedor no encontrado con ID: " + request.getInsumoProveedorId()));

        detalle.setInsumo(insumo);
        detalle.setCantidadInsumo(request.getCantidadInsumo());
        detalle.setCostoSubtotal(request.getCantidadInsumo() * request.getPrecioUnitario());

        return toDTO(detalleRepository.save(detalle));
    }

    // Eliminar un detalle
    public void eliminarDetalle(Long id) {
        if (!detalleRepository.existsById(id)) {
            throw new RuntimeException("Detalle no encontrado con ID: " + id);
        }
        detalleRepository.deleteById(id);
    }

    // Marcar como devuelto
    public DetalleProveedorPedidoResponse marcarComoDevuelto(Long id) {
        DetalleProveedorPedido detalle = detalleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado con ID: " + id));

        detalle.setEsDevuelto(true);
        return toDTO(detalleRepository.save(detalle));
    }

    // Convertir entidad a DTO
    private DetalleProveedorPedidoResponse toDTO(DetalleProveedorPedido detalle) {
        DetalleProveedorPedidoResponse response = new DetalleProveedorPedidoResponse();
        response.setId(detalle.getIdDetalle());
        response.setCantidadInsumo(detalle.getCantidadInsumo());
        response.setCostoSubtotal(detalle.getCostoSubtotal());
        response.setEsDevuelto(detalle.getEsDevuelto());

        // Mapear el InsumoProveedor a InsumoProveedorResponse
        if (detalle.getInsumo() != null) {
            InsumoProveedorResponse insumoResponse = new InsumoProveedorResponse();
            insumoResponse.setId(detalle.getInsumo().getIdInsumo());
            insumoResponse.setNombre(detalle.getInsumo().getNombre());
            insumoResponse.setDescripcion(detalle.getInsumo().getDescripcion());
            insumoResponse.setCostoUnitario(detalle.getInsumo().getCostoUnitario());
            insumoResponse.setFechaVencimiento(detalle.getInsumo().getFechaVencimiento());
            insumoResponse.setCantidadDisponible(detalle.getInsumo().getCantidadDisponible());
            if (detalle.getInsumo().getProveedor() != null) {
                insumoResponse.setIdProveedor(detalle.getInsumo().getProveedor().getIdProveedor());
            }
            response.setInsumo(insumoResponse);
        }

        return response;
    }

    // Convertir DTO a entidad
    private DetalleProveedorPedido toEntity(DetalleProveedorPedidoRequest request, Long idPedido) {
        DetalleProveedorPedido detalle = new DetalleProveedorPedido();

        // Buscar el InsumoProveedor
        InsumoProveedor insumo = insumoProveedorRepository.findById(request.getInsumoProveedorId())
                .orElseThrow(() -> new RuntimeException("Insumo proveedor no encontrado con ID: " + request.getInsumoProveedorId()));

        // Buscar el PedidoInsumo
        PedidoInsumo pedido = pedidoInsumoRepository.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con ID: " + idPedido));

        detalle.setInsumo(insumo);
        detalle.setPedido(pedido);
        detalle.setCantidadInsumo(request.getCantidadInsumo());
        detalle.setCostoSubtotal(request.getCantidadInsumo() * request.getPrecioUnitario());
        detalle.setEsDevuelto(false);

        return detalle;
    }
}

