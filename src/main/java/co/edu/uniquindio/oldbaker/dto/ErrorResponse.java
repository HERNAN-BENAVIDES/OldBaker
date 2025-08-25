package co.edu.uniquindio.oldbaker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    private String mensaje;
    private String detalle;
    private long timestamp;
    private String path;

    public static ErrorResponse of(String mensaje, String detalle, String path) {
        return ErrorResponse.builder()
                .mensaje(mensaje)
                .detalle(detalle)
                .timestamp(System.currentTimeMillis())
                .path(path)
                .build();
    }
}
