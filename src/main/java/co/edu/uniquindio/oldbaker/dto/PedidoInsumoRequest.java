package co.edu.uniquindio.oldbaker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PedidoInsumoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @PastOrPresent(message = "La fecha del pedido no puede estar en el futuro")
    private LocalDate fechaPedido;

    @NotEmpty(message = "El pedido debe contener al menos un detalle de insumo")
    private List<DetalleProveedorPedidoRequest> detalles;

}
