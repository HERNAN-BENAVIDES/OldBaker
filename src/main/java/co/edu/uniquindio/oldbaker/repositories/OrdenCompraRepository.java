package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {
}
