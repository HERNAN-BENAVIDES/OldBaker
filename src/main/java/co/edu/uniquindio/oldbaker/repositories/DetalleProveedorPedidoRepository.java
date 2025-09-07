package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.DetalleProveedorPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleProveedorPedidoRepository extends JpaRepository<DetalleProveedorPedido, Long> {
}
