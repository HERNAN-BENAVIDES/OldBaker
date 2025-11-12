package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.DetalleProveedorPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleProveedorPedidoRepository extends JpaRepository<DetalleProveedorPedido, Long> {

    List<DetalleProveedorPedido> findByPedidoIdPedido(Long idPedido);

    List<DetalleProveedorPedido> findByInsumoIdInsumo(Long idInsumo);

    List<DetalleProveedorPedido> findByEsDevuelto(Boolean esDevuelto);
}
