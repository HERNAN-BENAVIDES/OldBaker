package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.activo = true")
    Optional<Usuario> findByEmailAndActivoTrue(@Param("email") String email);

    @Modifying
    @Query("UPDATE Usuario u SET u.fechaUltimaSesion = :lastSesionDate WHERE u = :usuario")
    int updateUserLastSesionDate(Usuario usuario, LocalDateTime lastSesionDate);

}