package co.edu.uniquindio.oldbaker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * DTO para la respuesta de autenticación
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private UserResponse usuario;


    /**
     * DTO anidado para la información del usuario
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserResponse {
        private Long id;
        private String email;
        private String nombre;
        private String rol;
    }
}
