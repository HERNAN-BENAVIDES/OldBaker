package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.ProveedorRequest;
import co.edu.uniquindio.oldbaker.dto.ProveedorResponse;
import co.edu.uniquindio.oldbaker.model.Proveedor;
import co.edu.uniquindio.oldbaker.repositories.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    // Convertir DTO a entidad
    private Proveedor toEntity(ProveedorRequest dto) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(dto.getNombre());
        proveedor.setTelefono(dto.getTelefono());
        proveedor.setEmail(dto.getEmail());
        proveedor.setNumeroCuenta(dto.getNumeroCuenta());
        return proveedor;
    }

    // Convertir entidad a DTO
    private ProveedorResponse toDTO(Proveedor proveedor) {
        ProveedorResponse dto = new ProveedorResponse();
        dto.setIdProveedor(proveedor.getIdProveedor());
        dto.setNombre(proveedor.getNombre());
        dto.setTelefono(proveedor.getTelefono());
        dto.setEmail(proveedor.getEmail());
        dto.setNumeroCuenta(proveedor.getNumeroCuenta());
        return dto;
    }

    // Crear un nuevo proveedor
    public ProveedorResponse crearProveedor(ProveedorRequest dto) {
        Proveedor proveedor = toEntity(dto);
        return toDTO(proveedorRepository.save(proveedor));
    }

    // Listar todos los proveedores
    public List<ProveedorResponse> listarProveedores() {
        return proveedorRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Buscar proveedor por ID
    public Optional<ProveedorResponse> buscarPorId(Long id) {
        return proveedorRepository.findById(id).map(this::toDTO);
    }

    // Actualizar proveedor
    public ProveedorResponse actualizarProveedor(Long id, ProveedorRequest dto) {
        return proveedorRepository.findById(id).map(proveedor -> {
            proveedor.setNombre(dto.getNombre());
            proveedor.setTelefono(dto.getTelefono());
            proveedor.setEmail(dto.getEmail());
            proveedor.setNumeroCuenta(dto.getNumeroCuenta());
            return toDTO(proveedorRepository.save(proveedor));
        }).orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
    }

    // Eliminar proveedor
    public void eliminarProveedor(Long id) {
        if (!proveedorRepository.existsById(id)) {
            throw new RuntimeException("Proveedor no encontrado");
        }
        proveedorRepository.deleteById(id);
    }
}
