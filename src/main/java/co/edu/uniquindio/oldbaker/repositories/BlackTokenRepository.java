package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.BlackToken;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio para la entidad BlackToken.
 * Proporciona operaciones CRUD y consultas personalizadas para gestionar tokens en la lista negra.
 */
public interface BlackTokenRepository extends JpaRepository<BlackToken,Long> {

}
