package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.PedidoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoInsumoRepository extends JpaRepository<PedidoInsumo, Long> {
}
