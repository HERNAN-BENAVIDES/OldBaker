package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM Usuario u WHERE u.activo = true")
    List<Usuario> findAllActivos();

    @Query("SELECT u FROM Usuario u WHERE u.rol = :rol AND u.activo = true")
    List<Usuario> findByRolAndActivoTrue(@Param("rol") Usuario.Rol rol);

    @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.activo = true")
    Optional<Usuario> findByEmailAndActivoTrue(@Param("email") String email);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol = :rol AND u.activo = true")
    long countByRolAndActivoTrue(@Param("rol") Usuario.Rol rol);


    @Modifying
    @Query("UPDATE Usuario u SET u.fechaUltimaSesion = :lastSesionDate WHERE u = :usuario")
    int updateUserLastSesionDate(Usuario usuario, LocalDateTime lastSesionDate);
}