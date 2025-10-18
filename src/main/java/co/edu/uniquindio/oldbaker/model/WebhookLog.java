package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro de webhooks recibidos de MercadoPago.
 * Permite detectar duplicados y tener trazabilidad completa.
 */
@Entity
@Table(name = "webhook_log", indexes = {
    @Index(name = "idx_payment_id", columnList = "payment_id"),
    @Index(name = "idx_external_ref", columnList = "external_reference"),
    @Index(name = "idx_request_id", columnList = "request_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhook_log_seq")
    @SequenceGenerator(name = "webhook_log_seq", sequenceName = "WEBHOOK_LOG_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "topic")
    private String topic;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "processed", nullable = false)
    private Boolean processed;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "raw_body", columnDefinition = "TEXT")
    private String rawBody;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime fechaRecepcion;

    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;

    @PrePersist
    protected void onCreate() {
        fechaRecepcion = LocalDateTime.now();
        if (processed == null) {
            processed = false;
        }
    }
}

