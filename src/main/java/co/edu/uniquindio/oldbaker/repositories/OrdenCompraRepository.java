package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.OrdenCompra;
import co.edu.uniquindio.oldbaker.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    Optional<OrdenCompra> findByExternalReference(String externalReference);

    Optional<OrdenCompra> findByPaymentId(String paymentId);

    List<OrdenCompra> findByUsuarioOrderByFechaCreacionDesc(Usuario usuario);

    List<OrdenCompra> findByUsuario_IdOrderByFechaCreacionDesc(Long usuarioId);

    List<OrdenCompra> findByStatusOrderByFechaCreacionDesc(OrdenCompra.EstadoOrden status);
}

