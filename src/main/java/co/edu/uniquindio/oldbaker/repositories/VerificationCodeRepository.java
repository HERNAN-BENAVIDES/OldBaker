package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.VerificationCode;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


/**
 * Repositorio para la entidad VerificationCode.
 * Proporciona operaciones CRUD y consultas personalizadas para gestionar códigos de verificación en la base de datos.
 */
@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode,Long> {

    // Consulta personalizada para encontrar un código de verificación por el ID de usuario
    Optional<VerificationCode> findByUserId(@NotNull(message = "El ID de usuario es obligatorio") Long idUsuario);

    // Método para eliminar un código de verificación por el ID de usuario
    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.userId = :idUsuario")
    void deleteByIdUser(@Param("idUsuario") Long idUsuario);

}
