package co.edu.uniquindio.oldbaker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    @JsonIgnoreProperties({"ordenes", "password", "direcciones"})
    private Usuario usuario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<ItemOrden> items = new ArrayList<>();

    @Column(name = "payer_email")
    private String payerEmail;

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
        FAILED,       // Pago rechazado
        CANCELLED,    // Orden cancelada
        IN_PROCESS    // Pago en proceso
    }
}
