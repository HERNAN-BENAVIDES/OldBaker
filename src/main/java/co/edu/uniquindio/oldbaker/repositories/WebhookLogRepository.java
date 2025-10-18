package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.model.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {

    Optional<WebhookLog> findByPaymentIdAndProcessedTrue(String paymentId);

    boolean existsByPaymentIdAndProcessedTrue(String paymentId);
}

