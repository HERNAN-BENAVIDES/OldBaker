package co.edu.uniquindio.oldbaker.services;


import co.edu.uniquindio.oldbaker.dto.InsumoRequest;
import co.edu.uniquindio.oldbaker.dto.InsumoResponse;
import co.edu.uniquindio.oldbaker.model.Insumo;
import co.edu.uniquindio.oldbaker.repositories.InsumoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class InsumoService {
    private final InsumoRepository insumoRepository;

    public InsumoResponse crearInsumo(InsumoRequest request) {
        Insumo insumo = toEntity(request);
        return toDTO(insumoRepository.save(insumo));
    }

    public InsumoResponse obtenerInsumoPorId(Long id) {
        return toDTO(insumoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado con ID: " + id)));
    }

    public List<InsumoResponse> listarInsumos() {
        return insumoRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public InsumoResponse actualizarInsumo(Long id, InsumoRequest request) {
        Insumo insumo = insumoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado con ID: " + id));

        insumo.setNombre(request.getNombre());
        insumo.setDescripcion(request.getDescripcion());
        insumo.setCostoUnitario(request.getCostoUnitario());
        insumo.setCantidadActual(request.getCantidadActual());

        return toDTO(insumoRepository.save(insumo));
    }

    public void eliminarInsumo(Long id) {
        if (!insumoRepository.existsById(id)) {
            throw new RuntimeException("Insumo no encontrado con ID: " + id);
        }
        insumoRepository.deleteById(id);
    }

    private InsumoResponse toDTO(Insumo insumo) {
        return InsumoResponse.builder()
                .idInsumo(insumo.getIdInsumo())
                .nombre(insumo.getNombre())
                .descripcion(insumo.getDescripcion())
                .costoUnitario(insumo.getCostoUnitario())
                .cantidadActual(insumo.getCantidadActual())
                .build();
    }

    private Insumo toEntity(InsumoRequest request) {
        return Insumo.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .costoUnitario(request.getCostoUnitario())
                .cantidadActual(request.getCantidadActual())
                .insumoProveedor(null)
                .build();
    }
}