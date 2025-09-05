package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.VerificationCode;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode,Long> {

    @Query("SELECT v.code FROM VerificationCode v WHERE v.userId = :idUsuario")
    Optional<Object> findByIdUser(@NotNull(message = "El ID de usuario es obligatorio") Long idUsuario);

    @Query("DELETE FROM VerificationCode v WHERE v.userId = :idUsuario")
    void deleteByIdUser(@NotNull(message = "El ID de usuario es obligatorio") Long idUsuario);

}
