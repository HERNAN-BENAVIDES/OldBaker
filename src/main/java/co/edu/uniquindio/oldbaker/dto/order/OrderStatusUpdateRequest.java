package co.edu.uniquindio.oldbaker.dto.order;

import co.edu.uniquindio.oldbaker.model.OrdenCompra;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateRequest {

    private OrdenCompra.DeliveryStatus estado;
    private String comentario;
    private String trackingCode;
    private LocalDateTime fechaEntregaEstimada;
}
