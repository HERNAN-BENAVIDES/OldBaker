package co.edu.uniquindio.oldbaker.dto.order;

import co.edu.uniquindio.oldbaker.model.OrdenCompra;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderStatusUpdateRequest {

    private OrdenCompra.EstadoOrden estado;
    private String comentario;
    private String trackingCode;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaEntregaEstimada;
}
