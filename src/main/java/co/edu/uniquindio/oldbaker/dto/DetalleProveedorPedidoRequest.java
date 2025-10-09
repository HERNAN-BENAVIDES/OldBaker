package co.edu.uniquindio.oldbaker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DetalleProveedorPedidoRequest {

    @NotNull(message = "El insumo del proveedor es obligatorio")
    private Long insumoProveedorId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer cantidadInsumo;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio unitario debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal precioUnitario;
}

