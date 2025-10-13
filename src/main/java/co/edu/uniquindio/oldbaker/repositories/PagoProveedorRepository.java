package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.PagoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagoProveedorRepository extends JpaRepository<PagoProveedor, Long> {
}
