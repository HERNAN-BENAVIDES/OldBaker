package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.Receta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecetaRepository extends JpaRepository<Receta, Long> {
    // Busca recetas por el id del producto asociado
    List<Receta> findByProducto_IdProducto(Long idProducto);
}

