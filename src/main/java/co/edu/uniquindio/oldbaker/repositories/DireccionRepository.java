package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.Direccion;
import co.edu.uniquindio.oldbaker.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Long> {
    @Query("""
        SELECT d FROM Direccion d
        WHERE d.idCliente = :idCliente
    """)
    List<Direccion> obtenerDireccionUsuario(Usuario idCliente);
}
