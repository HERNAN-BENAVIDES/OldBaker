package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.payment.CheckoutResponseDTO;
import co.edu.uniquindio.oldbaker.model.OrdenCompra;
import co.edu.uniquindio.oldbaker.model.WebhookLog;
import co.edu.uniquindio.oldbaker.repositories.ProductRepository;
import co.edu.uniquindio.oldbaker.repositories.WebhookLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MercadoPagoService {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoService.class);

    private final RestTemplate restTemplate;
    private final ProductRepository productRepository;
    private final OrdenCompraService ordenCompraService;
    private final WebhookLogRepository webhookLogRepository;

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.notification-url:}")
    private String explicitNotificationUrl;

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    @Value("${app.base-url:}")
    private String appBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Crea una preferencia usando el external_reference de una orden previamente creada.
     * Configura back_urls para redirigir al usuario después del pago.
     */
    public CheckoutResponseDTO createPreference(OrdenCompra orden) {
        String url = "https://api.mercadopago.com/checkout/preferences";

        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        for (var item : orden.getItems()) {
            Map<String, Object> mpItem = new HashMap<>();
            mpItem.put("title", item.getProducto().getNombre());
            mpItem.put("quantity", item.getCantidad());
            mpItem.put("unit_price", item.getPrecioUnitario().doubleValue());
            mpItem.put("currency_id", "COP");
            items.add(mpItem);
        }
        payload.put("items", items);

        // Payer
        if (orden.getPayerEmail() != null && !orden.getPayerEmail().isBlank()) {
            Map<String, Object> payer = new HashMap<>();
            payer.put("email", orden.getPayerEmail());
            payload.put("payer", payer);
        }

        // back_urls: URLs del frontend para redirigir después del pago
        // MercadoPago redirige automáticamente a estas URLs con parámetros en query string

        if (appBaseUrl != null && !appBaseUrl.isBlank()) {
            Map<String, String> backUrls = new HashMap<>();
            // Incluir external_reference en la URL para que el frontend identifique la orden
            backUrls.put("success", appBaseUrl + "/payment/success?external_reference=" + orden.getExternalReference());
            backUrls.put("failure", appBaseUrl + "/payment/failure?external_reference=" + orden.getExternalReference());
            backUrls.put("pending", appBaseUrl + "/payment/pending?external_reference=" + orden.getExternalReference());
            payload.put("back_urls", backUrls);
            // auto_return: sólo agregar si la URL success es HTTPS (MercadoPago exige https para auto_return)
            String successUrl = backUrls.get("success");
            if (successUrl != null && successUrl.toLowerCase().startsWith("https://")) {
                payload.put("auto_return", "approved");
            } else {
                logger.debug("No se añadió auto_return porque successUrl no es HTTPS: {}", successUrl);
            }
        }

        // notification_url: webhook del backend para recibir notificaciones
        String notificationUrl = explicitNotificationUrl;
        if (notificationUrl == null || notificationUrl.isBlank()) {
            if (appBaseUrl != null && !appBaseUrl.isBlank()) {
                notificationUrl = appBaseUrl + "/api/payments/webhook";
            }
        }
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            payload.put("notification_url", notificationUrl);
        }

        // external_reference: usar el de la orden ya creada
        payload.put("external_reference", orden.getExternalReference());

        logger.info("Creando preferencia MP: externalRef={} backUrls={} notificationUrl={}",
                orden.getExternalReference(), appBaseUrl, notificationUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> resp = null;
        try {
            resp = restTemplate.postForEntity(url, entity, Map.class);
        } catch (HttpClientErrorException ex) {
            // Loguear detalle de la respuesta de error para diagnóstico
            String respBody = ex.getResponseBodyAsString();
            logger.error("Error creando preferencia MP: status={} body={}", ex.getStatusCode(), respBody);

            // Si la causa es auto_return invalid, intentar crear preferencia sin auto_return como fallback
            if (payload.containsKey("auto_return") && respBody != null && respBody.contains("auto_return invalid")) {
                logger.warn("MercadoPago rechazó auto_return; reintentando crear preferencia sin auto_return");
                payload.remove("auto_return");
                entity = new HttpEntity<>(payload, headers);
                try {
                    resp = restTemplate.postForEntity(url, entity, Map.class);
                } catch (HttpClientErrorException ex2) {
                    logger.error("Segundo intento crear preferencia MP falló: status={} body={}", ex2.getStatusCode(), ex2.getResponseBodyAsString());
                    throw new RuntimeException("Error MercadoPago: " + ex2.getResponseBodyAsString(), ex2);
                }
            } else {
                throw new RuntimeException("Error MercadoPago: " + respBody, ex);
            }
        }

        if (resp == null || (resp.getStatusCode() != HttpStatus.CREATED && resp.getStatusCode() != HttpStatus.OK)) {
            throw new RuntimeException("MercadoPago error: " + (resp != null ? resp.getStatusCode() : "no response"));
        }

        Map body = resp.getBody();
        String initPoint = body != null && body.get("init_point") != null ? body.get("init_point").toString() : null;
        String preferenceId = body != null && body.get("id") != null ? body.get("id").toString() : null;

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
     * - Actualiza orden local según estado usando OrdenCompraService
     */
    @Async
    public void processPaymentAsync(String paymentId) {
        try {
            Map payment = getPayment(paymentId);
            if (payment == null) {
                logger.warn("No se encontró pago para id={}", paymentId);
                return;
            }

            String status = Objects.toString(payment.get("status"), null);
            String externalRef = Objects.toString(payment.get("external_reference"), null);

            if (externalRef == null) {
                Object order = payment.get("order");
                if (order instanceof Map<?, ?> map) {
                    externalRef = Objects.toString(map.get("external_reference"), null);
                }
            }

            logger.info("Pago {} status={} external_reference={}", paymentId, status, externalRef);

            if (externalRef != null) {
                try {
                    switch (status) {
                        case "approved" -> ordenCompraService.marcarComoPagada(externalRef, paymentId);
                        case "rejected", "cancelled" -> ordenCompraService.marcarComoFallida(externalRef, paymentId);
                        case "pending", "in_process", "in_mediation" -> ordenCompraService.marcarComoEnProceso(externalRef, paymentId);
                        default -> logger.info("Estado no manejado: {}", status);
                    }
                } catch (Exception e) {
                    logger.error("Error actualizando orden con external_reference={}", externalRef, e);
                }
            } else {
                logger.warn("external_reference ausente en el pago {}", paymentId);
            }

        } catch (Exception e) {
            logger.error("Error procesando pago {}", paymentId, e);
        }
    }

    /**
     * Paso 5: Procesamiento asíncrono del pago con idempotencia mejorada.
     * - Registra el webhook en la BD para detectar duplicados
     * - Consulta el pago por ID
     * - Lee status y external_reference
     * - Actualiza orden local según estado usando OrdenCompraService
     */
    @Async
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void processPaymentAsync(String paymentId, String requestId, String rawBody) {
        WebhookLog webhookLog = null;
        try {
            // Paso 5: Verificar idempotencia - ¿ya procesamos este webhook?
            if (webhookLogRepository.existsByPaymentIdAndProcessedTrue(paymentId)) {
                logger.info("Webhook ya procesado previamente para paymentId={}, ignorando duplicado", paymentId);
                return;
            }

            // Crear registro de webhook
            webhookLog = WebhookLog.builder()
                    .paymentId(paymentId)
                    .requestId(requestId)
                    .rawBody(rawBody != null && rawBody.length() > 5000 ? rawBody.substring(0, 5000) : rawBody)
                    .processed(false)
                    .build();
            webhookLog = webhookLogRepository.save(webhookLog);

            Map payment = getPayment(paymentId);
            if (payment == null) {
                webhookLog.setErrorMessage("No se encontró pago en MercadoPago API");
                webhookLog.setFechaProcesamiento(LocalDateTime.now());
                webhookLogRepository.save(webhookLog);
                logger.warn("No se encontró pago para id={}", paymentId);
                return;
            }

            String status = Objects.toString(payment.get("status"), null);
            String externalRef = Objects.toString(payment.get("external_reference"), null);

            if (externalRef == null) {
                Object order = payment.get("order");
                if (order instanceof Map<?, ?> map) {
                    externalRef = Objects.toString(map.get("external_reference"), null);
                }
            }

            // Actualizar log con datos del pago
            webhookLog.setPaymentStatus(status);
            webhookLog.setExternalReference(externalRef);
            webhookLog.setTopic("payment");

            logger.info("Pago {} status={} external_reference={}", paymentId, status, externalRef);

            if (externalRef != null) {
                try {
                    // Paso 5: Actualizar orden (con transaccionalidad y validaciones)
                    switch (status) {
                        case "approved" -> ordenCompraService.marcarComoPagada(externalRef, paymentId);
                        case "rejected", "cancelled" -> ordenCompraService.marcarComoFallida(externalRef, paymentId);
                        case "pending", "in_process", "in_mediation" -> ordenCompraService.marcarComoEnProceso(externalRef, paymentId);
                        default -> logger.info("Estado no manejado: {}", status);
                    }

                    // Marcar webhook como procesado exitosamente
                    webhookLog.setProcessed(true);
                    webhookLog.setFechaProcesamiento(LocalDateTime.now());
                    webhookLogRepository.save(webhookLog);

                } catch (IllegalStateException e) {
                    // Transición de estado inválida (ej: intentar marcar como PAID una orden cancelada)
                    webhookLog.setErrorMessage("Transición de estado inválida: " + e.getMessage());
                    webhookLog.setFechaProcesamiento(LocalDateTime.now());
                    webhookLogRepository.save(webhookLog);
                    logger.error("Error de transición de estado para external_reference={}", externalRef, e);
                } catch (Exception e) {
                    webhookLog.setErrorMessage("Error procesando: " + e.getMessage());
                    webhookLog.setFechaProcesamiento(LocalDateTime.now());
                    webhookLogRepository.save(webhookLog);
                    logger.error("Error actualizando orden con external_reference={}", externalRef, e);
                }
            } else {
                webhookLog.setErrorMessage("external_reference ausente en el pago");
                webhookLog.setFechaProcesamiento(LocalDateTime.now());
                webhookLogRepository.save(webhookLog);
                logger.warn("external_reference ausente en el pago {}", paymentId);
            }

        } catch (Exception e) {
            if (webhookLog != null) {
                webhookLog.setErrorMessage("Error general: " + e.getMessage());
                webhookLog.setFechaProcesamiento(LocalDateTime.now());
                webhookLogRepository.save(webhookLog);
            }
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

    /**
     * Método que valida la firma (si aplica), parsea el body del webhook buscando ids de pago
     * y dispara el procesamiento asíncrono para cada paymentId encontrado.
     *
     * Retorna true si el webhook fue aceptado/procesado (o si no se pudo validar pero se decidió no fallar),
     * false si la firma inválida o payload irreconocible.
     */
    public boolean handleWebhook(String signatureHeader, String requestId, String rawBody) {
        try {
            logger.info("Recibido webhook MP requestId={} signaturePresent={} bodyLen={}",
                    requestId, signatureHeader != null && !signatureHeader.isBlank(),
                    rawBody != null ? rawBody.length() : 0);

            // validar firma (si está configurado)
            boolean valid = verifyWebhookSignature(signatureHeader, requestId, rawBody);
            if (!valid) {
                logger.warn("Webhook rechazado por firma inválida requestId={}", requestId);
                return false;
            }

            if (rawBody == null || rawBody.isBlank()) {
                logger.warn("Webhook con body vacío requestId={}", requestId);
                return false;
            }

            // Intentar parsear como Map
            List<String> paymentIds = new ArrayList<>();

            try {
                Object parsed = objectMapper.readValue(rawBody, Object.class);

                if (parsed instanceof Map<?, ?> map) {
                    // casos comunes: {"type":"payment","data":{"id":"12345"}} o {"data":{"id":"12345"}}
                    Object data = map.get("data");
                    if (data instanceof Map<?, ?> dataMap) {
                        Object idVal = dataMap.get("id");
                        if (idVal != null) paymentIds.add(String.valueOf(idVal));
                    }
                    // también verificar si hay id en top-level
                    if (map.get("id") != null) paymentIds.add(String.valueOf(map.get("id")));

                    // algunos webhooks usan "topic" o "type" y un campo "resource" o similar
                    if (paymentIds.isEmpty()) {
                        // buscar cualquier key que parezca id de pago
                        for (String key : List.of("payment_id", "paymentId", "payment", "id")) {
                            Object v = map.get(key);
                            if (v != null) paymentIds.add(String.valueOf(v));
                        }
                    }
                } else if (parsed instanceof List<?> list) {
                    // si viene un array de eventos
                    for (Object el : list) {
                        if (el instanceof Map<?, ?> m) {
                            Object data = m.get("data");
                            if (data instanceof Map<?, ?> dm && dm.get("id") != null) {
                                paymentIds.add(String.valueOf(dm.get("id")));
                            } else if (m.get("id") != null) {
                                paymentIds.add(String.valueOf(m.get("id")));
                            }
                        }
                    }
                } else {
                    logger.debug("Webhook payload tipo inesperado: {}", parsed.getClass());
                }
            } catch (Exception e) {
                logger.warn("No se pudo parsear JSON del webhook, rawBody={}", rawBody, e);
                return false;
            }

            if (paymentIds.isEmpty()) {
                logger.warn("No se encontraron paymentId en webhook requestId={}", requestId);
                return false;
            }

            // Disparar procesamiento asíncrono para cada paymentId encontrado
            for (String pid : paymentIds) {
                logger.info("Desencadenando procesamiento asíncrono para paymentId={}", pid);
                processPaymentAsync(pid);
            }

            return true;
        } catch (Exception e) {
            logger.error("Error manejando webhook MP", e);
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
