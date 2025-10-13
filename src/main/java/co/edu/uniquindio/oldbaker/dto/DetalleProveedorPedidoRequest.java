package co.edu.uniquindio.oldbaker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DetalleProveedorPedidoRequest {

    @NotNull(message = "El insumo del proveedor es obligatorio")
    private Long insumoProveedorId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer cantidadInsumo;

    @NotNull(message = "El precio unitario es obligatorio")
    @Positive(message = "El precio unitario debe ser mayor a 0")
    private Double precioUnitario;
}

