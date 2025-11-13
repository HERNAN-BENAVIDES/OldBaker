package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.OrdenCompra;
import co.edu.uniquindio.oldbaker.model.SeguimientoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeguimientoPedidoRepository extends JpaRepository<SeguimientoPedido, Long> {

    List<SeguimientoPedido> findByOrdenOrderByTimestampAsc(OrdenCompra orden);

    List<SeguimientoPedido> findByOrden_IdOrderByTimestampAsc(Long ordenId);
}
