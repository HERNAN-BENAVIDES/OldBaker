package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.order.OrderStatusUpdateRequest;
import co.edu.uniquindio.oldbaker.dto.order.OrderTrackingDTO;
import co.edu.uniquindio.oldbaker.dto.payment.CheckoutRequestDTO;
import co.edu.uniquindio.oldbaker.dto.payment.CheckoutResponseDTO;
import co.edu.uniquindio.oldbaker.model.OrdenCompra;
import co.edu.uniquindio.oldbaker.model.Usuario;
import co.edu.uniquindio.oldbaker.services.MercadoPagoService;
import co.edu.uniquindio.oldbaker.services.OrdenCompraService;
import co.edu.uniquindio.oldbaker.services.StockValidationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://old-baker-front.vercel.app", "https://localhost:4200", "http://localhost:4200", "https://www.oldbaker.shop"})
public class OrdenCompraController {
    private static final Logger logger = LoggerFactory.getLogger(OrdenCompraController.class);

    private final MercadoPagoService mercadoPagoService;
    private final OrdenCompraService ordenCompraService;
    private final StockValidationService stockValidationService;

    /**
     * Paso 3: Crear orden ANTES de redirigir a MercadoPago
     * 1. Valida stock disponible
     * 2. Crea orden en BD con estado PENDING
     * 3. Crea preferencia en MercadoPago usando external_reference de la orden
     * 4. Retorna init_point para redirigir al usuario
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @RequestBody CheckoutRequestDTO request,
            @AuthenticationPrincipal Usuario usuario) {

        // Validar que el usuario esté autenticado
        if (usuario == null) {
            logger.warn("Intento de checkout sin autenticación");
            return ResponseEntity.status(401).body(Map.of("error", "Usuario no autenticado"));
        }

        // Validaciones preventivas del request para evitar fallos en el servicio
        if (request == null) {
            logger.warn("Checkout request nulo para usuario {}", usuario.getId());
            return ResponseEntity.badRequest().body(Map.of("error", "Request vacío"));
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            logger.warn("Checkout con items vacíos para usuario {}", usuario.getId());
            return ResponseEntity.badRequest().body(Map.of("error", "Lista de items vacía"));
        }

        for (var it : request.getItems()) {
            if (it == null || it.getProductId() == null) {
                logger.warn("Item sin productId en checkout por usuario {}: item={}", usuario.getId(), it);
                return ResponseEntity.badRequest().body(Map.of("error", "Cada item debe incluir productId"));
            }
            if (it.getQuantity() == null || it.getQuantity() <= 0) {
                logger.warn("Item con quantity inválida en checkout por usuario {}: item={}", usuario.getId(), it);
                return ResponseEntity.badRequest().body(Map.of("error", "Cada item debe incluir quantity mayor a 0"));
            }
        }

        // payerEmail es opcional en algunos flows, pero si viene vacío lo aceptamos; si es requerido cambia la validación

        // Validar disponibilidad de stock
        var check = stockValidationService.checkAvailability(request);
        if (!check.isValid()) {
            logger.warn("Stock insuficiente para usuario {}: {}", usuario.getId(), check.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", check.getMessage()));
        }

        try {
            // Paso 3: Crear orden en estado PENDING
            OrdenCompra orden = ordenCompraService.crearOrden(request, usuario.getId());

            // Crear preferencia en MercadoPago usando la orden creada
            CheckoutResponseDTO resp = mercadoPagoService.createPreference(orden);

            logger.info("Checkout exitoso: ordenId={} usuario={} initPoint={}",
                    orden.getId(), usuario.getId(), resp.getInitPoint());

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            logger.error("Error en checkout para usuario {}", usuario.getId(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Error procesando checkout: " + e.getMessage()));
        }
    }

    /**
     * Consultar el estado de una orden por external_reference.
     * El frontend llama a este endpoint después de la redirección de MercadoPago.
     */
    @GetMapping("/status")
    public ResponseEntity<?> getOrderStatus(@RequestParam String externalReference) {
        try {
            OrdenCompra orden = ordenCompraService.obtenerPorExternalReference(externalReference);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orden.getId());
            response.put("paymentStatus", orden.getPaymentStatus() != null ? orden.getPaymentStatus().name() : null);
            response.put("deliveryStatus", orden.getDeliveryStatus() != null ? orden.getDeliveryStatus().name() : null);
            response.put("total", orden.getTotal());
            response.put("paymentId", orden.getPaymentId());
            response.put("fechaCreacion", orden.getFechaCreacion());
            response.put("trackingCode", orden.getTrackingCode());
            response.put("fechaEntregaEstimada", orden.getFechaEntregaEstimada());

            // Lista simplificada de items
            ArrayList<Map<String, Object>> items = new ArrayList<>();
            for (var item : orden.getItems()) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("producto", item.getProducto().getNombre());
                itemData.put("cantidad", item.getCantidad());
                itemData.put("precioUnitario", item.getPrecioUnitario());
                itemData.put("subtotal", item.getSubtotal());
                items.add(itemData);
            }
            response.put("items", items);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("Orden no encontrada: {}", externalReference);
            return ResponseEntity.status(404).body(Map.of("error", "Orden no encontrada"));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> actualizarEstadoOrden(@PathVariable Long id,
                                                   @RequestBody OrderStatusUpdateRequest request,
                                                   @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null || usuario.getRol() == Usuario.Rol.CLIENTE) {
            logger.warn("Usuario {} no autorizado para actualizar estado de la orden {}", usuario != null ? usuario.getId() : null, id);
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        if (request == null || request.getEstado() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El estado es obligatorio"));
        }

        try {
            List<OrderTrackingDTO> timeline = ordenCompraService.registrarCambioEstadoEntrega(
                    id,
                    request.getEstado(),
                    request.getComentario(),
                    request.getTrackingCode(),
                    request.getFechaEntregaEstimada()
            );

            OrdenCompra ordenActualizada = ordenCompraService.obtenerPorId(id);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", ordenActualizada.getId());
            response.put("paymentStatus", ordenActualizada.getPaymentStatus() != null ? ordenActualizada.getPaymentStatus().name() : null);
            response.put("deliveryStatus", ordenActualizada.getDeliveryStatus() != null ? ordenActualizada.getDeliveryStatus().name() : null);
            response.put("trackingCode", ordenActualizada.getTrackingCode());
            response.put("fechaEntregaEstimada", ordenActualizada.getFechaEntregaEstimada());
            response.put("timeline", timeline);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Datos inválidos al actualizar estado de la orden {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Transición no permitida para la orden {}: {}", id, e.getMessage());
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            logger.warn("Orden {} no encontrada al intentar actualizar estado", id);
            return ResponseEntity.status(404).body(Map.of("error", "Orden no encontrada"));
        }
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<?> obtenerSeguimientoOrden(@PathVariable Long id,
                                                     @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            OrdenCompra orden = ordenCompraService.obtenerPorId(id);
            boolean esStaff = usuario.getRol() == Usuario.Rol.ADMINISTRADOR || usuario.getRol() == Usuario.Rol.AUXILIAR;
            boolean esPropietario = orden.getUsuario() != null && Objects.equals(orden.getUsuario().getId(), usuario.getId());

            if (!esStaff && !esPropietario) {
                return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
            }

            List<OrderTrackingDTO> timeline = ordenCompraService.obtenerSeguimientoOrden(id);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orden.getId());
            response.put("paymentStatus", orden.getPaymentStatus() != null ? orden.getPaymentStatus().name() : null);
            response.put("deliveryStatus", orden.getDeliveryStatus() != null ? orden.getDeliveryStatus().name() : null);
            response.put("trackingCode", orden.getTrackingCode());
            response.put("fechaEntregaEstimada", orden.getFechaEntregaEstimada());
            response.put("timeline", timeline);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.warn("Orden {} no encontrada al consultar seguimiento", id);
            return ResponseEntity.status(404).body(Map.of("error", "Orden no encontrada"));
        }
    }

    /**
     * Webhook de Mercado Pago.
     * - Responde 200 lo más rápido posible
     * - Valida firma (HMAC) si hay secreto configurado
     * - Normaliza topic/type e id (query o body)
     * - Lanza proceso asíncrono para consultar pago y actualizar orden
     * - Paso 5: Registra webhook en BD para idempotencia mejorada
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
            // Leer raw body (necesario para verificar HMAC correctamente y registrar webhook)
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
                // Paso 5: Llamar a la versión mejorada con registro de webhook
                mercadoPagoService.processPaymentAsync(normalizedId, requestId, rawBody);
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
