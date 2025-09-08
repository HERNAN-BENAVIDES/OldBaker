package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;


/**
 * Repositorio para la entidad Usuario.
 * Proporciona operaciones CRUD y consultas personalizadas para gestionar usuarios en la base de datos.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Consulta personalizada para encontrar un usuario por su email
    Optional<Usuario> findByEmail(String email);

    // Método para verificar si un usuario existe por su email
    boolean existsByEmail(String email);

    // Consulta personalizada para encontrar un usuario activo por su email
    @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.activo = true")
    Optional<Usuario> findByEmailAndActivoTrue(@Param("email") String email);

    // Método para actualizar la fecha de la última sesión de un usuario
    @Modifying
    @Query("UPDATE Usuario u SET u.fechaUltimaSesion = :lastSesionDate WHERE u = :usuario")
    int updateUserLastSesionDate(Usuario usuario, LocalDateTime lastSesionDate);

}