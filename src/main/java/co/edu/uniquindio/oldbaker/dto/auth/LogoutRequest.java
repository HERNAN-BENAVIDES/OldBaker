package co.edu.uniquindio.oldbaker.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * DTO para la solicitud de cierre de sesión
 */
@Data
public class LogoutRequest {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;
    @NotBlank(message = "El token es obligatorio")
    private String token;
}