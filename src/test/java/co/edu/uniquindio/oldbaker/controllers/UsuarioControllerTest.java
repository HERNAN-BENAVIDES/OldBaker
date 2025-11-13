package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.DireccionResponseDTO;
import co.edu.uniquindio.oldbaker.dto.api.ApiResponse;
import co.edu.uniquindio.oldbaker.dto.auth.LogoutRequest;
import co.edu.uniquindio.oldbaker.dto.order.OrdenCompraDTO;
import co.edu.uniquindio.oldbaker.model.ItemOrden;
import co.edu.uniquindio.oldbaker.model.OrdenCompra;
import co.edu.uniquindio.oldbaker.model.Producto;
import co.edu.uniquindio.oldbaker.services.AuthService;
import co.edu.uniquindio.oldbaker.services.OrdenCompraService;
import co.edu.uniquindio.oldbaker.services.UsuarioService;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para UsuarioController.
 *
 * Estas pruebas verifican el comportamiento de los endpoints del controlador de usuarios
 * que gestionan desactivación de usuarios, consulta de órdenes, logout y direcciones.
 * Se utiliza Mockito para simular los servicios y validar que el controlador maneja
 * correctamente las respuestas HTTP.
 *
 * @author OldBaker Team
 */
@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private AuthService authService;

    @Mock
    private OrdenCompraService ordenCompraService;

    @InjectMocks
    private UsuarioController usuarioController;

    private OrdenCompra ordenCompra;
    private LogoutRequest logoutRequest;
    private DireccionResponseDTO direccionResponseDTO;

    /**
     * Configuración inicial que se ejecuta antes de cada test.
     * Inicializa los objetos de prueba con datos de ejemplo para ser utilizados en las pruebas.
     */
    @BeforeEach
    void setUp() {
        Producto producto = new Producto();
        producto.setIdProducto(1L);
        producto.setNombre("Pan Integral");

        ItemOrden itemOrden = new ItemOrden();
        itemOrden.setProducto(producto);
        itemOrden.setCantidad(2);
        itemOrden.setPrecioUnitario(BigDecimal.valueOf(3500.0));
        itemOrden.setSubtotal(BigDecimal.valueOf(7000.0));

        ordenCompra = new OrdenCompra();
        ordenCompra.setId(1L);
        ordenCompra.setExternalReference("ORDER-123456");
        ordenCompra.setPaymentId("PAY-123");
        ordenCompra.setTotal(BigDecimal.valueOf(7000.0));
        ordenCompra.setFechaCreacion(LocalDateTime.now());
        ordenCompra.setPayerEmail("cliente@test.com");
        ordenCompra.setItems(List.of(itemOrden));

        logoutRequest = new LogoutRequest();
        logoutRequest.setToken("jwt-token-123");

        direccionResponseDTO = new DireccionResponseDTO();
        direccionResponseDTO.setId(1L);
        direccionResponseDTO.setCalle("Calle 123");
        direccionResponseDTO.setCiudad("Armenia");
        direccionResponseDTO.setBarrio("Centro");
        direccionResponseDTO.setCarrera("Carrera 14");
        direccionResponseDTO.setNumero("15-30");
        direccionResponseDTO.setNumeroTelefono("3001234567");
    }

    /**
     * Verifica que el endpoint de desactivación de usuario funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio para desactivar el usuario
     * - Retorna un código HTTP 200 (OK) cuando la desactivación es exitosa
     * - El servicio es invocado exactamente una vez con el ID correcto
     */
    @Test
    @DisplayName("Test desactivar usuario exitoso")
    void testDeactivateUserSuccess() {
        // Given
        Long userId = 1L;
        when(usuarioService.deactivateUser(userId)).thenReturn(true);

        // When
        ResponseEntity<?> response = usuarioController.deactivateUser(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(usuarioService, times(1)).deactivateUser(userId);
    }

    /**
     * Verifica que el endpoint retorne 404 cuando el usuario no existe.
     *
     * Este test valida que:
     * - El controlador maneja correctamente cuando el usuario no existe
     * - Retorna un código HTTP 404 (NOT_FOUND)
     * - El servicio es invocado con el ID correcto
     */
    @Test
    @DisplayName("Test desactivar usuario cuando no existe")
    void testDeactivateUserNotFound() {
        // Given
        Long userId = 999L;
        when(usuarioService.deactivateUser(userId)).thenReturn(false);

        // When
        ResponseEntity<?> response = usuarioController.deactivateUser(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(usuarioService, times(1)).deactivateUser(userId);
    }

    /**
     * Verifica que el endpoint de obtención de órdenes funcione correctamente.
     *
     * Este test valida que:
     * - El controlador obtiene las órdenes del usuario del servicio
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene las órdenes correctamente mapeadas a DTO
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test obtener órdenes por usuario exitoso")
    void testObtenerOrdenesPorUsuarioSuccess() {
        // Given
        Long userId = 1L;
        ordenCompra.setPaymentStatus(OrdenCompra.PaymentStatus.PAID);
        when(ordenCompraService.listarOrdenesPorUsuario(userId)).thenReturn(List.of(ordenCompra));

        // When
        ResponseEntity<?> response = usuarioController.obtenerOrdenesPorUsuario(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(List.class, response.getBody());
        @SuppressWarnings("unchecked")
        List<OrdenCompraDTO> ordenesDTO = (List<OrdenCompraDTO>) response.getBody();
        assertEquals(1, ordenesDTO.size());
        assertEquals("ORDER-123456", ordenesDTO.get(0).getExternalReference());
        assertEquals("PAID", ordenesDTO.get(0).getPaymentStatus());
        verify(ordenCompraService, times(1)).listarOrdenesPorUsuario(userId);
    }

    /**
     * Verifica que el endpoint retorne lista vacía cuando el usuario no tiene órdenes.
     *
     * Este test valida que:
     * - El controlador maneja correctamente cuando no hay órdenes
     * - Retorna un código HTTP 200 (OK)
     * - La lista está vacía
     */
    @Test
    @DisplayName("Test obtener órdenes cuando el usuario no tiene órdenes")
    void testObtenerOrdenesPorUsuarioVacio() {
        // Given
        Long userId = 1L;
        when(ordenCompraService.listarOrdenesPorUsuario(userId)).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<?> response = usuarioController.obtenerOrdenesPorUsuario(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(List.class, response.getBody());
        @SuppressWarnings("unchecked")
        List<OrdenCompraDTO> ordenesDTO = (List<OrdenCompraDTO>) response.getBody();
        assertEquals(0, ordenesDTO.size());
        verify(ordenCompraService, times(1)).listarOrdenesPorUsuario(userId);
    }

    /**
     * Verifica que el endpoint maneje errores del servicio correctamente.
     *
     * Este test valida que:
     * - El controlador captura excepciones del servicio
     * - Retorna un código HTTP 500 (INTERNAL_SERVER_ERROR)
     * - El servicio es invocado correctamente
     */
    @Test
    @DisplayName("Test obtener órdenes con error en el servicio")
    void testObtenerOrdenesPorUsuarioError() {
        // Given
        Long userId = 1L;
        when(ordenCompraService.listarOrdenesPorUsuario(userId))
                .thenThrow(new RuntimeException("Error en base de datos"));

        // When
        ResponseEntity<?> response = usuarioController.obtenerOrdenesPorUsuario(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(ordenCompraService, times(1)).listarOrdenesPorUsuario(userId);
    }

    /**
     * Verifica que el endpoint de logout funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio de autenticación para cerrar sesión
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene el mensaje de éxito
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test logout exitoso")
    void testLogoutSuccess() {
        // Given
        when(authService.logout(any(LogoutRequest.class))).thenReturn("Sesión cerrada exitosamente");

        // When
        ResponseEntity<ApiResponse<String>> response = usuarioController.logout(logoutRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        verify(authService, times(1)).logout(any(LogoutRequest.class));
    }

    /**
     * Verifica que el endpoint de logout maneje errores correctamente.
     *
     * Este test valida que:
     * - El controlador captura excepciones de argumentos inválidos
     * - Retorna un código HTTP 400 (BAD_REQUEST)
     * - El servicio es invocado correctamente
     */
    @Test
    @DisplayName("Test logout con token inválido")
    void testLogoutInvalidToken() {
        // Given
        when(authService.logout(any(LogoutRequest.class)))
                .thenThrow(new IllegalArgumentException("Token inválido"));

        // When
        ResponseEntity<ApiResponse<String>> response = usuarioController.logout(logoutRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        verify(authService, times(1)).logout(any(LogoutRequest.class));
    }

    /**
     * Verifica que el endpoint de obtención de direcciones funcione correctamente.
     *
     * Este test valida que:
     * - El controlador obtiene la dirección del usuario del servicio
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene los datos de la dirección
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test obtener direcciones de usuario exitoso")
    void testObtenerDireccionUsuarioSuccess() {
        // Given
        Long userId = 1L;
        when(usuarioService.obtenerDireccionUsuario(userId)).thenReturn(List.of(direccionResponseDTO));

        // When
        ResponseEntity<List<DireccionResponseDTO>> response = usuarioController.obtenerDireccionUsuario(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Calle 123", response.getBody().get(0).getCalle());
        assertEquals("Armenia", response.getBody().get(0).getCiudad());
        verify(usuarioService, times(1)).obtenerDireccionUsuario(userId);
    }

    /**
     * Verifica que el endpoint de direcciones maneje errores cuando el usuario no existe.
     *
     * Este test valida que:
     * - El controlador propaga excepciones del servicio
     * - El servicio es invocado con el ID correcto
     */
    @Test
    @DisplayName("Test obtener direcciones cuando el usuario no existe")
    void testObtenerDireccionUsuarioNoExiste() {
        // Given
        Long userId = 999L;
        when(usuarioService.obtenerDireccionUsuario(userId)).thenThrow(new IllegalArgumentException("Usuario no encontrado"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            usuarioController.obtenerDireccionUsuario(userId)
        );
        verify(usuarioService, times(1)).obtenerDireccionUsuario(userId);
    }
}
