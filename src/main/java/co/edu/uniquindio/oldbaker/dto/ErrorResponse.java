package co.edu.uniquindio.oldbaker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta de error
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    private String mensaje;
    private String detalle;
    private long timestamp;
    private String path;


    /**
     * Crea una instancia de ErrorResponse con los detalles proporcionados.
     *
     * @param mensaje El mensaje de error.
     * @param detalle Detalles adicionales sobre el error.
     * @param path    La ruta del endpoint donde ocurrió el error.
     * @return Una instancia de ErrorResponse.
     */
    public static ErrorResponse of(String mensaje, String detalle, String path) {
        return ErrorResponse.builder()
                .mensaje(mensaje)
                .detalle(detalle)
                .timestamp(System.currentTimeMillis())
                .path(path)
                .build();
    }
}
