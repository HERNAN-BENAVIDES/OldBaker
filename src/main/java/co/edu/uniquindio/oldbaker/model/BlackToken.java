package co.edu.uniquindio.oldbaker.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


/**
 * Entidad que representa un token inválido (blacklist) para manejar el cierre de sesión y la revocación de tokens JWT.
 */
@Entity
@Table(name = "black_tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlackToken {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "black_token_seq")
    @SequenceGenerator(name = "black_token_seq", sequenceName = "BLACK_TOKEN_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "token", nullable = false, unique = true)
    @NotBlank(message = "El token es obligatorio")
    private String token;

    @Column(name = "expiration", nullable = false)
    @NotNull(message = "La fecha de expiración es obligatoria")
    private LocalDateTime expiration;

}
