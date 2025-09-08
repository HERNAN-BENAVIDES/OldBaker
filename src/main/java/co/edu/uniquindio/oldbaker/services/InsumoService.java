package co.edu.uniquindio.oldbaker.services;


import co.edu.uniquindio.oldbaker.model.Insumo;
import co.edu.uniquindio.oldbaker.repositories.InsumoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InsumoService {

    private final InsumoRepository insumoRepository;

    public Insumo obtenerInsumoPorId(Long id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("El insumo con ID " + id + " no fue encontrado."));
    }

    public List<Insumo> listarTodosLosInsumos() {
        return insumoRepository.findAll();
    }
}

