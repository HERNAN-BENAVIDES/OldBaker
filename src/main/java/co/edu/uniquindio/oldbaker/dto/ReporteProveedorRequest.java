package co.edu.uniquindio.oldbaker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReporteProveedorRequest {

    private Long detalleId;

    @NotBlank(message = "La razón es obligatoria")
    private String razon;

    private Long idProveedor;
}
