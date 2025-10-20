package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.PedidoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoInsumoRepository extends JpaRepository<PedidoInsumo, Long> {
    @Query("SELECT DISTINCT d.insumo.proveedor.idProveedor FROM PedidoInsumo p " +
            "JOIN p.detalles d " +
            "WHERE p.idPedido = :idPedido")
    Long findProveedorByPedidoId(@Param("idPedido") Long idPedido);
}
