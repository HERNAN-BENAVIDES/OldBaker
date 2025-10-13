package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.InsumoProveedorResponse;
import co.edu.uniquindio.oldbaker.model.InsumoProveedor;
import co.edu.uniquindio.oldbaker.repositories.InsumoProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InsumoProveedorService {

    private final InsumoProveedorRepository insumoProveedorRepository;

    public InsumoProveedorService(InsumoProveedorRepository insumoProveedorRepository) {
        this.insumoProveedorRepository = insumoProveedorRepository;
    }

    public List<InsumoProveedorResponse> listarInsumos() {
        return insumoProveedorRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public InsumoProveedorResponse obtenerInsumoPorId(Long id) {
        InsumoProveedor insumo = insumoProveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado con ID: " + id));
        return mapToResponse(insumo);
    }

    private InsumoProveedorResponse mapToResponse(InsumoProveedor insumo) {
        InsumoProveedorResponse response = new InsumoProveedorResponse();
        response.setId(insumo.getIdInsumo());
        response.setNombre(insumo.getNombre());
        response.setDescripcion(insumo.getDescripcion());
        response.setCostoUnitario(insumo.getCostoUnitario());
        response.setFechaVencimiento(insumo.getFechaVencimiento());
        response.setCantidadDisponible(insumo.getCantidadDisponible());
        response.setProveedorNombre(insumo.getProveedor() != null ? insumo.getProveedor().getNombre() : null);
        return response;
    }
}
