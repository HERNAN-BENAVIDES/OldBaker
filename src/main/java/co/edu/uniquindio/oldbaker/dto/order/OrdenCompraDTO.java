package co.edu.uniquindio.oldbaker.dto.order;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@RequiredArgsConstructor
public class OrdenCompraDTO {
    private Long id;
    private String externalReference;
    private String status; // compat: usamos paymentStatus
    private String paymentStatus;
    private String deliveryStatus;
    private String paymentId;
    private BigDecimal total;
    private LocalDateTime fechaCreacion;
    private String payerEmail;
    private List<ItemOrdenDTO> items;


    public OrdenCompraDTO(Long id, String externalReference, String status, String paymentId, BigDecimal total, LocalDateTime fechaCreacion, String payerEmail, List<ItemOrdenDTO> items) {
        this.id = id;
        this.externalReference = externalReference;
        this.status = status;
        this.paymentId = paymentId;
        this.total = total;
        this.fechaCreacion = fechaCreacion;
        this.payerEmail = payerEmail;
        this.items = items;
    }

}
