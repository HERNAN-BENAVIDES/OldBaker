package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.services.MercadoPagoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/mercadopago")
@RequiredArgsConstructor
public class MercadoPagoController {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoController.class);

    private final MercadoPagoService mercadoPagoService;

    /**
     * Webhook oficial para notificaciones de Mercado Pago.
     * Retorna 200 siempre para evitar reintentos excesivos del proveedor.
     */
        @PostMapping(value = "/webhook", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<?> handleWebhook(HttpServletRequest request, @RequestBody(required = false) String rawBody) {
        // Buscar encabezados comunes de firma (case-insensitive por HttpServletRequest)
        String signatureHeader = request.getHeader("X-Hub-Signature");
        if (signatureHeader == null) signatureHeader = request.getHeader("X-Hub-Signature-256");
        if (signatureHeader == null) signatureHeader = request.getHeader("X-Mercadopago-Signature");
        if (signatureHeader == null) signatureHeader = request.getHeader("X-Meli-Signature");
        if (signatureHeader == null) signatureHeader = request.getHeader("signature");

        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null) requestId = request.getHeader("x-request-id");

        boolean ok = mercadoPagoService.handleWebhook(signatureHeader, requestId, rawBody);
        if (!ok) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok().build();
    }

    // Alias para la URL usada en createPreference: /api/payments/webhook
    @PostMapping(value = "/payments/webhook", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<?> handlePaymentsWebhook(HttpServletRequest request, @RequestBody(required = false) String rawBody) {
        return handleWebhook(request, rawBody);
    }

}
