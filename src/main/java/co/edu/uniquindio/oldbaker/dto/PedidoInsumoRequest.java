package co.edu.uniquindio.oldbaker.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Data
public class PedidoInsumoRequest {

    @NotNull(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "La descripción es obligatoria")
    private String descripcion;

    private LocalDate fechaPedido;

    @NotEmpty(message = "El pedido debe contener al menos un detalle de insumo")
    private List<DetalleProveedorPedidoRequest> detalles;

}
