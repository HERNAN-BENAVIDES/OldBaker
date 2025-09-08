package co.edu.uniquindio.oldbaker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReporteProveedorRequest {

    @NotNull(message = "El detalle es obligatorio")
    private Long detalleId;

    @NotBlank(message = "La razón es obligatoria")
    private String razon;
}
