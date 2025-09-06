package co.edu.uniquindio.oldbaker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordRecoveryRequest {
    @NotBlank(message = "El email es obligatorio")
    private String email;
    @NotBlank(message = "La nueva contraseña es obligatoria")
    private String newPassword;

}
