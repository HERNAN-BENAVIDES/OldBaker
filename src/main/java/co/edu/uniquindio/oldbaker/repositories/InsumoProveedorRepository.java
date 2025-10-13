package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.InsumoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsumoProveedorRepository extends JpaRepository<InsumoProveedor, Long> {
}
