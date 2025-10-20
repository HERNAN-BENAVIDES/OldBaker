package co.edu.uniquindio.oldbaker.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsumoProveedorRequest {

    @NotBlank(message = "El nombre no puede estar vacío")
    private String nombre;

    @NotBlank(message = "La descripción no puede estar vacía")
    private String descripcion;

    @NotNull(message = "El costo unitario no puede ser nulo")
    @Positive(message = "El costo unitario debe ser mayor a cero")
    private Double costoUnitario;

    @NotNull(message = "La fecha de vencimiento no puede ser nula")
    @Future(message = "La fecha de vencimiento debe ser en el futuro")
    private LocalDate fechaVencimiento;

    @NotNull(message = "La cantidad disponible no puede ser nula")
    @PositiveOrZero(message = "La cantidad disponible debe ser cero o mayor")
    private Integer cantidadDisponible;

    @NotNull(message = "El ID del proveedor no puede ser nulo")
    private Long idProveedor;
}
