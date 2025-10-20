package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.InsumoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsumoProveedorRepository extends JpaRepository<InsumoProveedor, Long> {
    List<InsumoProveedor> findByProveedorIdProveedor(Long idProveedor);}
