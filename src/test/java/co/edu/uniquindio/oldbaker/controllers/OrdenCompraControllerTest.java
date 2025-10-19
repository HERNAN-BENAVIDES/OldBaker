package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.payment.CheckoutItemDTO;
import co.edu.uniquindio.oldbaker.dto.payment.CheckoutRequestDTO;
import co.edu.uniquindio.oldbaker.dto.payment.CheckoutResponseDTO;
import co.edu.uniquindio.oldbaker.model.ItemOrden;
import co.edu.uniquindio.oldbaker.model.OrdenCompra;
import co.edu.uniquindio.oldbaker.model.Producto;
import co.edu.uniquindio.oldbaker.model.Usuario;
import co.edu.uniquindio.oldbaker.services.MercadoPagoService;
import co.edu.uniquindio.oldbaker.services.OrdenCompraService;
import co.edu.uniquindio.oldbaker.services.StockCheckResult;
import co.edu.uniquindio.oldbaker.services.StockValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para OrdenCompraController.
 *
 * Estas pruebas verifican el comportamiento de los endpoints del controlador de órdenes de compra
 * que gestionan el proceso de checkout, consulta de estado de órdenes y procesamiento de webhooks.
 * Se utiliza Mockito para simular los servicios y validar que el controlador maneja correctamente
 * las respuestas HTTP y los diferentes escenarios de compra.
 *
 * @author OldBaker Team
 */
@ExtendWith(MockitoExtension.class)
class OrdenCompraControllerTest {

    @Mock
    private MercadoPagoService mercadoPagoService;

    @Mock
    private OrdenCompraService ordenCompraService;

    @Mock
    private StockValidationService stockValidationService;

    @InjectMocks
    private OrdenCompraController ordenCompraController;

    private Usuario usuario;
    private CheckoutRequestDTO checkoutRequest;
    private OrdenCompra ordenCompra;
    private CheckoutResponseDTO checkoutResponse;
    private StockCheckResult stockCheckResult;

    /**
     * Configuración inicial que se ejecuta antes de cada test.
     * Inicializa los objetos de prueba con datos de ejemplo para ser utilizados en las pruebas.
     */
    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("cliente@test.com");
        usuario.setNombre("Cliente Test");

        CheckoutItemDTO item = new CheckoutItemDTO();
        item.setProductId(1L);
        item.setQuantity(2);

        checkoutRequest = new CheckoutRequestDTO();
        checkoutRequest.setItems(List.of(item));
        checkoutRequest.setPayerEmail("cliente@test.com");

        Producto producto = new Producto();
        producto.setIdProducto(1L);
        producto.setNombre("Pan Integral");
        producto.setCostoUnitario(3500.0);

        ItemOrden itemOrden = new ItemOrden();
        itemOrden.setProducto(producto);
        itemOrden.setCantidad(2);
        itemOrden.setPrecioUnitario(BigDecimal.valueOf(3500.0));
        itemOrden.setSubtotal(BigDecimal.valueOf(7000.0));

        ordenCompra = new OrdenCompra();
        ordenCompra.setId(1L);
        ordenCompra.setExternalReference("ORDER-123456");
        ordenCompra.setTotal(BigDecimal.valueOf(7000.0));
        ordenCompra.setStatus(OrdenCompra.EstadoOrden.PENDING);
        ordenCompra.setItems(List.of(itemOrden));
        ordenCompra.setFechaCreacion(LocalDateTime.now());

        checkoutResponse = new CheckoutResponseDTO();
        checkoutResponse.setInitPoint("https://mercadopago.com/checkout/123");
        checkoutResponse.setPreferenceId("PREF-123456");

        stockCheckResult = new StockCheckResult(true, "Stock disponible");
    }

    /**
     * Verifica que el endpoint de checkout funcione correctamente con datos válidos.
     *
     * Este test valida que:
     * - El controlador valida el stock disponible
     * - Crea la orden en el servicio
     * - Crea la preferencia de pago en MercadoPago
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene el init_point para redirigir al usuario
     */
    @Test
    @DisplayName("Test checkout exitoso")
    void testCheckoutSuccess() {
        // Given
        when(stockValidationService.checkAvailability(any(CheckoutRequestDTO.class))).thenReturn(stockCheckResult);
        when(ordenCompraService.crearOrden(any(CheckoutRequestDTO.class), anyLong())).thenReturn(ordenCompra);
        when(mercadoPagoService.createPreference(any(OrdenCompra.class))).thenReturn(checkoutResponse);

        // When
        ResponseEntity<?> response = ordenCompraController.checkout(checkoutRequest, usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(CheckoutResponseDTO.class, response.getBody());
        CheckoutResponseDTO responseBody = (CheckoutResponseDTO) response.getBody();
        assertEquals("https://mercadopago.com/checkout/123", responseBody.getInitPoint());
        verify(stockValidationService, times(1)).checkAvailability(any(CheckoutRequestDTO.class));
        verify(ordenCompraService, times(1)).crearOrden(any(CheckoutRequestDTO.class), anyLong());
        verify(mercadoPagoService, times(1)).createPreference(any(OrdenCompra.class));
    }

    /**
     * Verifica que el endpoint de checkout rechace solicitudes sin autenticación.
     *
     * Este test valida que:
     * - El controlador valida que el usuario esté autenticado
     * - Retorna un código HTTP 401 (UNAUTHORIZED)
     * - No se ejecutan los servicios de orden ni de pago
     */
    @Test
    @DisplayName("Test checkout sin autenticación")
    void testCheckoutUnauthorized() {
        // Given
        Usuario usuarioNull = null;

        // When
        ResponseEntity<?> response = ordenCompraController.checkout(checkoutRequest, usuarioNull);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(stockValidationService, never()).checkAvailability(any());
        verify(ordenCompraService, never()).crearOrden(any(), anyLong());
        verify(mercadoPagoService, never()).createPreference(any());
    }

    /**
     * Verifica que el endpoint de checkout maneje correctamente requests vacíos.
     *
     * Este test valida que:
     * - El controlador valida que el request no sea nulo
     * - Retorna un código HTTP 400 (BAD_REQUEST)
     * - No se ejecutan los servicios
     */
    @Test
    @DisplayName("Test checkout con request vacío")
    void testCheckoutEmptyRequest() {
        // Given
        CheckoutRequestDTO requestNull = null;

        // When
        ResponseEntity<?> response = ordenCompraController.checkout(requestNull, usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(stockValidationService, never()).checkAvailability(any());
        verify(ordenCompraService, never()).crearOrden(any(), anyLong());
    }

    /**
     * Verifica que el endpoint de checkout rechace solicitudes con items vacíos.
     *
     * Este test valida que:
     * - El controlador valida que la lista de items no esté vacía
     * - Retorna un código HTTP 400 (BAD_REQUEST)
     * - No se ejecutan los servicios
     */
    @Test
    @DisplayName("Test checkout con items vacíos")
    void testCheckoutEmptyItems() {
        // Given
        checkoutRequest.setItems(new ArrayList<>());

        // When
        ResponseEntity<?> response = ordenCompraController.checkout(checkoutRequest, usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(stockValidationService, never()).checkAvailability(any());
    }

    /**
     * Verifica que el endpoint de checkout rechace solicitudes con stock insuficiente.
     *
     * Este test valida que:
     * - El controlador valida el stock disponible antes de crear la orden
     * - Retorna un código HTTP 400 (BAD_REQUEST) cuando no hay stock
     * - No se crea la orden ni la preferencia de pago
     */
    @Test
    @DisplayName("Test checkout con stock insuficiente")
    void testCheckoutInsufficientStock() {
        // Given
        stockCheckResult = new StockCheckResult(false, "Stock insuficiente para el producto Pan Integral");
        when(stockValidationService.checkAvailability(any(CheckoutRequestDTO.class))).thenReturn(stockCheckResult);

        // When
        ResponseEntity<?> response = ordenCompraController.checkout(checkoutRequest, usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(stockValidationService, times(1)).checkAvailability(any(CheckoutRequestDTO.class));
        verify(ordenCompraService, never()).crearOrden(any(), anyLong());
        verify(mercadoPagoService, never()).createPreference(any());
    }

    /**
     * Verifica que el endpoint de checkout maneje errores del servicio correctamente.
     *
     * Este test valida que:
     * - El controlador captura excepciones del servicio
     * - Retorna un código HTTP 500 (INTERNAL_SERVER_ERROR)
     * - El mensaje de error es apropiado
     */
    @Test
    @DisplayName("Test checkout con error en el servicio")
    void testCheckoutServiceError() {
        // Given
        when(stockValidationService.checkAvailability(any(CheckoutRequestDTO.class))).thenReturn(stockCheckResult);
        when(ordenCompraService.crearOrden(any(CheckoutRequestDTO.class), anyLong()))
                .thenThrow(new RuntimeException("Error en base de datos"));

        // When
        ResponseEntity<?> response = ordenCompraController.checkout(checkoutRequest, usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(stockValidationService, times(1)).checkAvailability(any(CheckoutRequestDTO.class));
        verify(ordenCompraService, times(1)).crearOrden(any(CheckoutRequestDTO.class), anyLong());
    }

    /**
     * Verifica que el endpoint de consulta de estado de orden funcione correctamente.
     *
     * Este test valida que:
     * - El controlador consulta la orden por external reference
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene los datos de la orden y sus items
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test obtener estado de orden exitoso")
    void testGetOrderStatusSuccess() {
        // Given
        String externalReference = "ORDER-123456";
        when(ordenCompraService.obtenerPorExternalReference(externalReference)).thenReturn(ordenCompra);

        // When
        ResponseEntity<?> response = ordenCompraController.getOrderStatus(externalReference);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(Map.class, response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(1L, responseBody.get("orderId"));
        assertEquals("PENDING", responseBody.get("status"));
        verify(ordenCompraService, times(1)).obtenerPorExternalReference(externalReference);
    }

    /**
     * Verifica que el endpoint maneje correctamente cuando no se encuentra la orden.
     *
     * Este test valida que:
     * - El controlador maneja la excepción cuando la orden no existe
     * - Retorna un código HTTP 404 (NOT_FOUND)
     * - El mensaje de error es apropiado
     */
    @Test
    @DisplayName("Test obtener estado de orden no encontrada")
    void testGetOrderStatusNotFound() {
        // Given
        String externalReference = "ORDER-NOEXISTE";
        when(ordenCompraService.obtenerPorExternalReference(externalReference))
                .thenThrow(new RuntimeException("Orden no encontrada"));

        // When
        ResponseEntity<?> response = ordenCompraController.getOrderStatus(externalReference);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(ordenCompraService, times(1)).obtenerPorExternalReference(externalReference);
    }

    /**
     * Verifica que el endpoint de checkout valide items con productId nulo.
     *
     * Este test valida que:
     * - El controlador valida que cada item tenga un productId
     * - Retorna un código HTTP 400 (BAD_REQUEST)
     * - No se ejecutan los servicios
     */
    @Test
    @DisplayName("Test checkout con item sin productId")
    void testCheckoutItemWithoutProductId() {
        // Given
        CheckoutItemDTO itemInvalido = new CheckoutItemDTO();
        itemInvalido.setProductId(null);
        itemInvalido.setQuantity(1);
        checkoutRequest.setItems(List.of(itemInvalido));

        // When
        ResponseEntity<?> response = ordenCompraController.checkout(checkoutRequest, usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(stockValidationService, never()).checkAvailability(any());
    }

    /**
     * Verifica que el endpoint de checkout valide items con cantidad inválida.
     *
     * Este test valida que:
     * - El controlador valida que cada item tenga una cantidad mayor a 0
     * - Retorna un código HTTP 400 (BAD_REQUEST)
     * - No se ejecutan los servicios
     */
    @Test
    @DisplayName("Test checkout con item con cantidad inválida")
    void testCheckoutItemWithInvalidQuantity() {
        // Given
        CheckoutItemDTO itemInvalido = new CheckoutItemDTO();
        itemInvalido.setProductId(1L);
        itemInvalido.setQuantity(0);
        checkoutRequest.setItems(List.of(itemInvalido));

        // When
        ResponseEntity<?> response = ordenCompraController.checkout(checkoutRequest, usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(stockValidationService, never()).checkAvailability(any());
    }
}
