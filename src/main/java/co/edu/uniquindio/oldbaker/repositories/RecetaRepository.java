package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.Producto;
import co.edu.uniquindio.oldbaker.model.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {
    Receta findByProducto(Producto producto);
}
