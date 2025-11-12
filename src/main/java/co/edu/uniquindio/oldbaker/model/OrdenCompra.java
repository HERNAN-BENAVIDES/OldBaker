package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "orden_compra")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenCompra {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orden_compra_seq")
    @SequenceGenerator(name = "orden_compra_seq", sequenceName = "ORDEN_COMPRA_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "external_reference", unique = true, nullable = false)
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden status;

    @Column(name = "payment_id")
    private String paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemOrden> items = new ArrayList<>();

    @Column(name = "payer_email")
    private String payerEmail;

    @Column(name = "tracking_code")
    private String trackingCode;

    @Column(name = "fecha_entrega_estimada")
    private LocalDateTime fechaEntregaEstimada;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (status == null) {
            status = EstadoOrden.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public void addItem(ItemOrden item) {
        items.add(item);
        item.setOrden(this);
    }

    public void removeItem(ItemOrden item) {
        items.remove(item);
        item.setOrden(null);
    }

    public enum EstadoOrden {
        PENDING,      // Orden creada, esperando pago
        PAID,         // Pago confirmado
        FAILED,             // Pago rechazado
        CANCELLED,          // Orden cancelada
        IN_PROCESS,         // Pago en proceso
        PREPARING,          // Pedido en preparación
        READY_FOR_PICKUP,   // Listo para ser recogido
        SHIPPED,            // Enviado al cliente
        DELIVERED           // Entregado al cliente
    }
}
