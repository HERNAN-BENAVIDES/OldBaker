package co.edu.uniquindio.oldbaker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Clase genérica para estructurar las respuestas de la API.
 *
 * @param <T> El tipo de datos que se incluirá en la respuesta.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String mensaje;
    private T data;
    private long timestamp;

    /**
     * Crea una respuesta exitosa con un mensaje y datos.
     *
     * @param mensaje El mensaje de la respuesta.
     * @param data    Los datos a incluir en la respuesta.
     * @param <T>     El tipo de datos.
     * @return Una instancia de ApiResponse con éxito.
     */
    public static <T> ApiResponse<T> success(String mensaje, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .mensaje(mensaje)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Crea una respuesta exitosa con datos y un mensaje por defecto.
     *
     * @param data Los datos a incluir en la respuesta.
     * @param <T>  El tipo de datos.
     * @return Una instancia de ApiResponse con éxito.
     */
    public static <T> ApiResponse<T> success(T data) {
        return success("Operación exitosa", data);
    }

    /**
     * Crea una respuesta de error con un mensaje.
     *
     * @param mensaje El mensaje de error.
     * @param <T>     El tipo de datos.
     * @return Una instancia de ApiResponse con error.
     */
    public static <T> ApiResponse<T> error(String mensaje) {
        return ApiResponse.<T>builder()
                .success(false)
                .mensaje(mensaje)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
