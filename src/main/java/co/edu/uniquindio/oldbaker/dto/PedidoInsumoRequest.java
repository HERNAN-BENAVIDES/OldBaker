package co.edu.uniquindio.oldbaker.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;

@Data
public class PedidoInsumoRequest {

    @NotNull(message = "El insumo es obligatorio")
    private Long insumoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    @NotNull(message = "La fecha del pedido es obligatoria")
    @FutureOrPresent(message = "La fecha del pedido no puede ser pasada")
    private LocalDate fechaPedido;

}
