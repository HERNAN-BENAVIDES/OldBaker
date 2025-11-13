package co.edu.uniquindio.oldbaker.dto.order;

import co.edu.uniquindio.oldbaker.model.OrdenCompra;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTrackingDTO {

    private OrdenCompra.DeliveryStatus estado;
    private String comentario;
    private LocalDateTime timestamp;
    private String trackingCode;
    private LocalDateTime fechaEntregaEstimada;
}
