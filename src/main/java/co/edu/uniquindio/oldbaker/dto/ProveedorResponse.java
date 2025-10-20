package co.edu.uniquindio.oldbaker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProveedorResponse {
    private Long idProveedor;
    private String nombre;
    private String telefono;
    private String email;
    private String numeroCuenta;
}
