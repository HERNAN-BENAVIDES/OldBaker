package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_seq")
    @SequenceGenerator(name = "usuario_seq", sequenceName = "USUARIO_SEQ", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false)
    @Email(message = "El formato del email no es válido")
    @NotBlank(message = "El email es obligatorio")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Column
    private String password; // Puede ser null para usuarios de OAuth2

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol = Rol.CLIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_autenticacion", nullable = false)
    private TipoAutenticacion tipoAutenticacion = TipoAutenticacion.EMAIL;

    @Column(name = "verificado", nullable = false)
    private Boolean verificado = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_ultima_sesion")
    private LocalDateTime fechaUltimaSesion;

    @Column(nullable = false)
    private Boolean activo = true;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    // Implementación de UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return activo;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    public enum Rol {
        CLIENTE, ADMINISTRADOR, AUXILIAR
    }

    public enum TipoAutenticacion {
        EMAIL, GOOGLE
    }
}