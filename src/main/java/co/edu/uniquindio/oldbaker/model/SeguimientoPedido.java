package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "seguimiento_pedido")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeguimientoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seguimiento_pedido_seq")
    @SequenceGenerator(name = "seguimiento_pedido_seq", sequenceName = "SEGUIMIENTO_PEDIDO_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenCompra orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrdenCompra.DeliveryStatus estado;

    @Column(length = 500)
    private String comentario;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
