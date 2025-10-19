package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.services.MercadoPagoService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para MercadoPagoController.
 *
 * Estas pruebas verifican el comportamiento de los endpoints del controlador de MercadoPago
 * que gestionan los webhooks de notificaciones de pago. Se utiliza Mockito para simular
 * el servicio de MercadoPago y validar que el controlador maneja correctamente las respuestas HTTP.
 *
 * @author OldBaker Team
 */
@ExtendWith(MockitoExtension.class)
class MercadoPagoControllerTest {

    @Mock
    private MercadoPagoService mercadoPagoService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private MercadoPagoController mercadoPagoController;

    private String rawBody;
    private String signatureHeader;
    private String requestId;

    /**
     * Configuración inicial que se ejecuta antes de cada test.
     * Inicializa los datos de prueba para simular webhooks de MercadoPago.
     */
    @BeforeEach
    void setUp() {
        rawBody = "{\"action\":\"payment.created\",\"data\":{\"id\":\"123456789\"}}";
        signatureHeader = "sha256=abc123def456";
        requestId = "request-id-12345";
    }

    /**
     * Verifica que el endpoint de webhook maneje correctamente una notificación válida.
     *
     * Este test valida que:
     * - El controlador extrae los headers de firma y request ID correctamente
     * - Llama al servicio con los parámetros correctos
     * - Retorna un código HTTP 200 (OK) cuando el webhook es válido
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test webhook de MercadoPago con firma válida")
    void testHandleWebhookValid() {
        // Given
        when(httpServletRequest.getHeader("X-Hub-Signature")).thenReturn(signatureHeader);
        when(httpServletRequest.getHeader("X-Request-Id")).thenReturn(requestId);
        when(mercadoPagoService.handleWebhook(signatureHeader, requestId, rawBody)).thenReturn(true);

        // When
        ResponseEntity<?> response = mercadoPagoController.handleWebhook(httpServletRequest, rawBody);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mercadoPagoService, times(1)).handleWebhook(signatureHeader, requestId, rawBody);
    }

    /**
     * Verifica que el endpoint de webhook rechace notificaciones con firma inválida.
     *
     * Este test valida que:
     * - El controlador detecta cuando la firma es inválida
     * - Retorna un código HTTP 401 (UNAUTHORIZED)
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test webhook de MercadoPago con firma inválida")
    void testHandleWebhookInvalidSignature() {
        // Given
        when(httpServletRequest.getHeader("X-Hub-Signature")).thenReturn(signatureHeader);
        when(httpServletRequest.getHeader("X-Request-Id")).thenReturn(requestId);
        when(mercadoPagoService.handleWebhook(signatureHeader, requestId, rawBody)).thenReturn(false);

        // When
        ResponseEntity<?> response = mercadoPagoController.handleWebhook(httpServletRequest, rawBody);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(mercadoPagoService, times(1)).handleWebhook(signatureHeader, requestId, rawBody);
    }

    /**
     * Verifica que el controlador busque la firma en headers alternativos.
     *
     * Este test valida que:
     * - El controlador intenta encontrar la firma en diferentes headers
     * - Procesa correctamente cuando encuentra X-Mercadopago-Signature
     * - Retorna HTTP 200 cuando el webhook es válido
     */
    @Test
    @DisplayName("Test webhook con header alternativo X-Mercadopago-Signature")
    void testHandleWebhookAlternativeHeader() {
        // Given
        when(httpServletRequest.getHeader("X-Hub-Signature")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Hub-Signature-256")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Mercadopago-Signature")).thenReturn(signatureHeader);
        when(httpServletRequest.getHeader("X-Request-Id")).thenReturn(requestId);
        when(mercadoPagoService.handleWebhook(signatureHeader, requestId, rawBody)).thenReturn(true);

        // When
        ResponseEntity<?> response = mercadoPagoController.handleWebhook(httpServletRequest, rawBody);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mercadoPagoService, times(1)).handleWebhook(signatureHeader, requestId, rawBody);
    }

    /**
     * Verifica que el controlador maneje correctamente webhooks sin firma.
     *
     * Este test valida que:
     * - El controlador maneja el caso cuando no hay header de firma
     * - Llama al servicio con firma null
     * - Retorna HTTP 401 cuando el servicio rechaza el webhook sin firma
     */
    @Test
    @DisplayName("Test webhook sin header de firma")
    void testHandleWebhookNoSignature() {
        // Given
        when(httpServletRequest.getHeader(anyString())).thenReturn(null);
        when(mercadoPagoService.handleWebhook(nullable(String.class), nullable(String.class), anyString()))
                .thenReturn(false);

        // When
        ResponseEntity<?> response = mercadoPagoController.handleWebhook(httpServletRequest, rawBody);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(mercadoPagoService, times(1)).handleWebhook(nullable(String.class), nullable(String.class), anyString());
    }

    /**
     * Verifica que el endpoint alternativo /payments/webhook funcione correctamente.
     *
     * Este test valida que:
     * - El método handlePaymentsWebhook delega correctamente al método handleWebhook
     * - Retorna HTTP 200 cuando el webhook es válido
     * - El servicio es invocado correctamente
     */
    @Test
    @DisplayName("Test webhook alternativo /payments/webhook")
    void testHandlePaymentsWebhook() {
        // Given
        when(httpServletRequest.getHeader("X-Hub-Signature")).thenReturn(signatureHeader);
        when(httpServletRequest.getHeader("X-Request-Id")).thenReturn(requestId);
        when(mercadoPagoService.handleWebhook(signatureHeader, requestId, rawBody)).thenReturn(true);

        // When
        ResponseEntity<?> response = mercadoPagoController.handlePaymentsWebhook(httpServletRequest, rawBody);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mercadoPagoService, times(1)).handleWebhook(signatureHeader, requestId, rawBody);
    }

    /**
     * Verifica que el controlador maneje correctamente webhooks con cuerpo vacío.
     *
     * Este test valida que:
     * - El controlador acepta webhooks con rawBody null o vacío
     * - Retorna HTTP 200 cuando el servicio acepta el webhook
     * - El servicio es invocado con los parámetros correctos incluyendo body null
     */
    @Test
    @DisplayName("Test webhook con body vacío")
    void testHandleWebhookEmptyBody() {
        // Given
        when(httpServletRequest.getHeader("X-Hub-Signature")).thenReturn(signatureHeader);
        when(httpServletRequest.getHeader("X-Request-Id")).thenReturn(requestId);
        when(mercadoPagoService.handleWebhook(signatureHeader, requestId, null)).thenReturn(true);

        // When
        ResponseEntity<?> response = mercadoPagoController.handleWebhook(httpServletRequest, null);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mercadoPagoService, times(1)).handleWebhook(signatureHeader, requestId, null);
    }
}

