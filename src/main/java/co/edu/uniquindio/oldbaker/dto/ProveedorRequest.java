package co.edu.uniquindio.oldbaker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProveedorRequest {

    private Long idProveedor;

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 7, max = 15, message = "El teléfono debe tener entre 7 y 15 dígitos")
    private String telefono;

    @Email(message = "Debe ser un correo válido")
    @NotBlank(message = "El correo no puede estar vacío")
    private String email;

    @NotBlank(message = "El número de cuenta es obligatorio")
    private String numeroCuenta;
}
