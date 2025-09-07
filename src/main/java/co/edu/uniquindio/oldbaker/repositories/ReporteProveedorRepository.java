package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.ReporteProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReporteProveedorRepository extends JpaRepository<ReporteProveedor, Long> {
}
