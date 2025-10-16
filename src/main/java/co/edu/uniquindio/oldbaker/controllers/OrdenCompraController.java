package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.payment.CheckoutRequestDTO;
import co.edu.uniquindio.oldbaker.dto.payment.CheckoutResponseDTO;
import co.edu.uniquindio.oldbaker.services.MercadoPagoService;
import co.edu.uniquindio.oldbaker.services.StockValidationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://old-baker-front.vercel.app", "https://localhost:4200", "http://localhost:4200", "https://www.oldbaker.shop"})
public class OrdenCompraController {
    private static final Logger logger = LoggerFactory.getLogger(OrdenCompraController.class);

    private final MercadoPagoService mercadoPagoService;
    private final StockValidationService stockValidationService;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequestDTO request) {
        var check = stockValidationService.checkAvailability(request);
        if (!check.isValid()) {
            logger.warn("Stock insuficiente: {}", check.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", check.getMessage()));
        }

        CheckoutResponseDTO resp = mercadoPagoService.createPreference(request);
        return ResponseEntity.ok(resp);
    }

    /**
     * Webhook de Mercado Pago.
     * - Responde 200 lo más rápido posible
     * - Valida firma (HMAC) si hay secreto configurado
     * - Normaliza topic/type e id (query o body)
     * - Lanza proceso asíncrono para consultar pago y actualizar orden
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            HttpServletRequest httpRequest,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "id", required = false) String id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        try {
            // Leer raw body (necesario para verificar HMAC correctamente)
            String rawBody = "";
            if (httpRequest.getInputStream() != null) {
                rawBody = StreamUtils.copyToString(httpRequest.getInputStream(), StandardCharsets.UTF_8);
            }

            // Normalizar topic y id: preferir query params; si no, tomar del body
            String normalizedTopic = (topic != null ? topic : type);
            String normalizedId = id;

            if ((normalizedTopic == null || normalizedId == null) && body != null) {
                if (normalizedTopic == null) {
                    Object t = body.get("topic");
                    if (t == null) t = body.get("type");
                    if (t != null) normalizedTopic = String.valueOf(t);
                }
                if (normalizedId == null) {
                    Object data = body.get("data");
                    if (data instanceof Map<?, ?> map) {
                        Object dataId = map.get("id");
                        if (dataId != null) normalizedId = String.valueOf(dataId);
                    }
                    if (normalizedId == null && body.get("id") != null) {
                        normalizedId = String.valueOf(body.get("id"));
                    }
                }
            }

            logger.info("MP webhook headers: x-request-id={} x-signature={}", requestId, signature);
            logger.info("MP webhook received: topic={} id={}", normalizedTopic, normalizedId);

            // Verificar firma (si está activada). Si no pasa, se ignora.
            boolean signatureOk = mercadoPagoService.verifyWebhookSignature(signature, requestId, rawBody);
            if (!signatureOk) {
                logger.warn("Firma de webhook inválida; ignorando evento.");
                // Respondemos 200 para evitar reintentos agresivos, pero no procesamos
                return ResponseEntity.ok().build();
            }

            // Responder rápido y procesar asíncrono
            if ("payment".equalsIgnoreCase(normalizedTopic) && normalizedId != null) {
                mercadoPagoService.processPaymentAsync(normalizedId);
            } else {
                logger.warn("Webhook no manejado o id ausente: topic={} id={}", normalizedTopic, normalizedId);
            }

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("Error procesando webhook", ex);
            // Devolver 200 igualmente para evitar bucles de reintento excesivo
            return ResponseEntity.ok().build();
        }
    }
}
