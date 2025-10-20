package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.InsumoProveedorRequest;
import co.edu.uniquindio.oldbaker.dto.InsumoProveedorResponse;
import co.edu.uniquindio.oldbaker.model.InsumoProveedor;
import co.edu.uniquindio.oldbaker.repositories.InsumoProveedorRepository;
import co.edu.uniquindio.oldbaker.repositories.ProveedorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InsumoProveedorService {

    private final InsumoProveedorRepository insumoProveedorRepository;
    private final ProveedorRepository proveedorRepository;

    public InsumoProveedorService(InsumoProveedorRepository insumoProveedorRepository, ProveedorRepository proveedorRepository) {
        this.insumoProveedorRepository = insumoProveedorRepository;
        this.proveedorRepository = proveedorRepository;
    }

    // Paginación de insumos
    public Page<InsumoProveedorResponse> listarInsumosPaginado(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return insumoProveedorRepository.findAll(pageable)
                .map(this::toDTO);
    }

    // Listar todos los insumos
    public List<InsumoProveedorResponse> listarInsumos() {
        return insumoProveedorRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // En InsumoProveedorService.java
    public List<InsumoProveedorResponse> listarInsumosPorIdProveedor(Long idProveedor) {
        return insumoProveedorRepository.findByProveedorIdProveedor(idProveedor)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Obtener insumo por ID
    public InsumoProveedorResponse obtenerInsumoPorId(Long id) {
        InsumoProveedor insumo = insumoProveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado con ID: " + id));
        return toDTO(insumo);
    }

    // Crear un nuevo insumo
    public InsumoProveedorResponse crearInsumo(InsumoProveedorRequest request) {
        InsumoProveedor insumo = toEntity(request);
        return toDTO(insumoProveedorRepository.save(insumo));
    }

    // Actualizar un insumo existente
    public InsumoProveedorResponse actualizarInsumo(Long id, InsumoProveedorRequest request) {
        InsumoProveedor insumo = insumoProveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado con ID: " + id));

        insumo.setNombre(request.getNombre());
        insumo.setDescripcion(request.getDescripcion());
        insumo.setCostoUnitario(request.getCostoUnitario());
        insumo.setFechaVencimiento(request.getFechaVencimiento());
        insumo.setCantidadDisponible(request.getCantidadDisponible());
        insumo.setProveedor(proveedorRepository.findById(request.getIdProveedor())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + request.getIdProveedor())));

        return toDTO(insumoProveedorRepository.save(insumo));
    }

    // Eliminar un insumo
    public void eliminarInsumo(Long id) {
        if (!insumoProveedorRepository.existsById(id)) {
            throw new RuntimeException("Insumo no encontrado con ID: " + id);
        }
        insumoProveedorRepository.deleteById(id);
    }

    // Convertir entidad a DTO
    private InsumoProveedorResponse toDTO(InsumoProveedor insumo) {
        return InsumoProveedorResponse.builder()
                .id(insumo.getIdInsumo())
                .nombre(insumo.getNombre())
                .descripcion(insumo.getDescripcion())
                .costoUnitario(insumo.getCostoUnitario())
                .fechaVencimiento(insumo.getFechaVencimiento())
                .cantidadDisponible(insumo.getCantidadDisponible())
                .idProveedor(insumo.getProveedor().getIdProveedor())
                .build();
    }

    // Convertir DTO a entidad
    private InsumoProveedor toEntity(InsumoProveedorRequest request) {
        return InsumoProveedor.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .costoUnitario(request.getCostoUnitario())
                .fechaVencimiento(request.getFechaVencimiento())
                .cantidadDisponible(request.getCantidadDisponible())
                .proveedor(proveedorRepository.findById(request.getIdProveedor())
                        .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + request.getIdProveedor())))
                .build();
    }
}