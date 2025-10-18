package co.edu.uniquindio.oldbaker.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrdenCompraDTO {
    private Long id;
    private String externalReference;
    private String status;
    private String paymentId;
    private BigDecimal total;
    private LocalDateTime fechaCreacion;
    private String payerEmail;
    private List<ItemOrdenDTO> items;

    public OrdenCompraDTO() {}

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public List<ItemOrdenDTO> getItems() {
        return items;
    }

    public void setItems(List<ItemOrdenDTO> items) {
        this.items = items;
    }
}

