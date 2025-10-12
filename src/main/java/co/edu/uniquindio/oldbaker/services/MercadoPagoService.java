package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.payment.CheckoutItemDTO;
import co.edu.uniquindio.oldbaker.dto.payment.CheckoutRequestDTO;
import co.edu.uniquindio.oldbaker.dto.payment.CheckoutResponseDTO;
import co.edu.uniquindio.oldbaker.model.Producto;
import co.edu.uniquindio.oldbaker.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MercadoPagoService {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoService.class);

    private final RestTemplate restTemplate;
    private final ProductRepository productRepository;

    // Si tienes un OrderService, inyéctalo aquí para actualizar órdenes por external_reference:
    // private final OrderService orderService;

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.notification-url:}")
    private String explicitNotificationUrl;

    @Value("${mercadopago.webhook-secret:}")     // secreto para HMAC; si está vacío, no se valida firma
    private String webhookSecret;

    @Value("${app.base-url:}")
    private String appBaseUrl;

    /**
     * Crea una preferencia con:
     * - items provenientes de la BD (precio confiable)
     * - notification_url para recibir webhooks
     * - external_reference para enlazar con tu orden local
     */
    public CheckoutResponseDTO createPreference(CheckoutRequestDTO request) {
        String url = "https://api.mercadopago.com/checkout/preferences";

        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        for (CheckoutItemDTO it : request.getItems()) {
            Producto prod = productRepository.findById(it.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + it.getProductId()));

            Map<String, Object> item = new HashMap<>();
            item.put("title", prod.getNombre());
            item.put("quantity", it.getQuantity());
            item.put("unit_price", prod.getCostoUnitario().doubleValue()); // precio confiable desde BD
            item.put("currency_id", "COP");
            items.add(item);
        }
        payload.put("items", items);

        // Payer (opcional)
        if (request.getPayerEmail() != null && !request.getPayerEmail().isBlank()) {
            Map<String, Object> payer = new HashMap<>();
            payer.put("email", request.getPayerEmail());
            payload.put("payer", payer);
        }

        // back_urls + auto_return (para redirecciones de la UI)
        if (appBaseUrl != null && !appBaseUrl.isBlank()) {
            Map<String, String> backUrls = new HashMap<>();
            backUrls.put("success", appBaseUrl + "/payment/success");
            backUrls.put("failure", appBaseUrl + "/payment/failure");
            backUrls.put("pending", appBaseUrl + "/payment/pending");
            payload.put("back_urls", backUrls);
            payload.put("auto_return", "approved");
        }

        // notification_url (prioriza mercadopago.notification-url; si no, app.base-url + /webhook)
        String notificationUrl = explicitNotificationUrl;
        if (notificationUrl == null || notificationUrl.isBlank()) {
            if (appBaseUrl != null && !appBaseUrl.isBlank()) {
                notificationUrl = appBaseUrl + "/api/payments/webhook";
            }
        }
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            payload.put("notification_url", notificationUrl);
        }

        // external_reference: ID de la orden local (aquí usamos un UUID como placeholder)
        // En producción: crea la Order en PENDING y usa su id aquí
        String externalReference = UUID.randomUUID().toString();
        payload.put("external_reference", externalReference);

        logger.info("Creando preferencia MP con external_reference={} notification_url={}", externalReference, notificationUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);

        if (resp.getStatusCode() != HttpStatus.CREATED && resp.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("MercadoPago error: " + resp.getStatusCode());
        }

        Map body = resp.getBody();
        String initPoint = body != null && body.get("init_point") != null ? body.get("init_point").toString() : null;
        String preferenceId = body != null && body.get("id") != null ? body.get("id").toString() : null;

        // Aquí podrías persistir externalReference ↔ preferenceId ↔ estado inicial de la orden

        return new CheckoutResponseDTO(initPoint, preferenceId);
    }

    /**
     * Llamada oficial para consultar los detalles del pago por ID.
     */
    public Map getPayment(String paymentId) {
        try {
            String url = "https://api.mercadopago.com/v1/payments/" + paymentId;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                return resp.getBody();
            }
        } catch (HttpClientErrorException e) {
            logger.error("MP error {} al obtener pago {}", e.getStatusCode(), paymentId);
        } catch (Exception e) {
            logger.error("Error al obtener pago {}", paymentId, e);
        }
        return null;
    }

    /**
     * Procesamiento asíncrono del pago:
     * - Consulta el pago por ID
     * - Lee status y external_reference
     * - Actualiza orden local según estado
     * Requiere @EnableAsync en la config de Spring.
     */
    @Async
    public void processPaymentAsync(String paymentId) {
        try {
            Map payment = getPayment(paymentId);
            if (payment == null) {
                logger.warn("No se encontró pago para id={}", paymentId);
                return;
            }

            String status = Objects.toString(payment.get("status"), null); // approved, rejected, pending, in_process...
            String externalRef = Objects.toString(payment.get("external_reference"), null);

            if (externalRef == null) {
                Object order = payment.get("order");
                if (order instanceof Map<?, ?> map) {
                    externalRef = Objects.toString(map.get("external_reference"), null);
                }
            }

            logger.info("Pago {} status={} external_reference={}", paymentId, status, externalRef);

            if (externalRef != null) {
                // Long orderId = Long.valueOf(externalRef); // si usas IDs numéricos
                // switch (status) {
                //     case "approved" -> orderService.markAsPaid(orderId, paymentId);
                //     case "rejected" -> orderService.markAsFailed(orderId, paymentId);
                //     case "pending", "in_process" -> orderService.markAsPending(orderId, paymentId);
                //     default -> logger.info("Estado no manejado: {}", status);
                // }
                // Por ahora solo log:
                logger.info("Actualizar orden local con external_reference={} según status={}", externalRef, status);
            } else {
                logger.warn("external_reference ausente en el pago {}", paymentId);
            }

        } catch (Exception e) {
            logger.error("Error procesando pago {}", paymentId, e);
        }
    }

    /**
     * Verifica la firma del webhook (HMAC-SHA256) si está configurado 'mercadopago.webhook-secret'.
     * Retorna true si:
     *  - no hay secreto configurado (no se aplica validación), o
     *  - la firma es válida.
     *
     * NOTA: La construcción exacta del payload a firmar puede variar según el proveedor;
     * ajusta 'buildSignaturePayload(...)' si es necesario.
     */
    public boolean verifyWebhookSignature(String signatureHeader, String requestId, String rawBody) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            // Sin secreto configurado: no validar firma (útil en desarrollo)
            return true;
        }
        try {
            if (signatureHeader == null || signatureHeader.isBlank()) {
                return false;
            }

            // Ejemplo: muchas pasarelas envían algo como "t=timestamp,v1=hexfirma"
            // Aquí tomamos la parte después de "v1=" si existe; ajusta si tu formato difiere.
            String signature = extractSignatureValue(signatureHeader);

            byte[] key = webhookSecret.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);

            // Construye el payload a firmar (ajusta si tu proveedor requiere concatenar timestamp, etc.)
            String toSign = buildSignaturePayload(requestId, rawBody);
            byte[] hmac = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));
            String expectedHex = bytesToHex(hmac);

            boolean ok = constantTimeEquals(expectedHex, signature);
            if (!ok) {
                logger.warn("Firma no coincide. expectedHex={}, provided={}", expectedHex, signature);
            }
            return ok;
        } catch (Exception e) {
            logger.error("Error verificando firma de webhook", e);
            return false;
        }
    }

    // ===== Helpers firma HMAC =====

    private String extractSignatureValue(String signatureHeader) {
        // Si el header viene como "t=169...,v1=abcdef..." toma v1
        String sig = signatureHeader.trim();
        if (sig.contains("v1=")) {
            int i = sig.indexOf("v1=");
            String sub = sig.substring(i + 3);
            int comma = sub.indexOf(',');
            return (comma > -1) ? sub.substring(0, comma) : sub;
        }
        return sig;
    }

    private String buildSignaturePayload(String requestId, String rawBody) {
        // Estrategia por defecto: firmar solo el body (ajusta si necesitas incluir requestId/timestamp)
        return rawBody != null ? rawBody : "";
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b: bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
