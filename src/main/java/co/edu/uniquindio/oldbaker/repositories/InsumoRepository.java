package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.Insumo;
import co.edu.uniquindio.oldbaker.model.InsumoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {
    Optional<Insumo> findByInsumoProveedor(InsumoProveedor insumoProveedor);
}
