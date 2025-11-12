package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.DetalleProveedorPedidoRequest;
import co.edu.uniquindio.oldbaker.dto.DetalleProveedorPedidoResponse;
import co.edu.uniquindio.oldbaker.dto.InsumoProveedorResponse;
import co.edu.uniquindio.oldbaker.services.DetalleProveedorPedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para DetalleProveedorPedidoController.
 *
 * Estas pruebas verifican el comportamiento de los endpoints del controlador de detalles de pedidos
 * que gestionan la creación, consulta, actualización y eliminación de detalles de pedidos a proveedores.
 * Se utiliza Mockito para simular el servicio y validar que el controlador maneja correctamente
 * las respuestas HTTP y los diferentes escenarios de gestión de detalles.
 *
 * @author OldBaker Team
 */
@ExtendWith(MockitoExtension.class)
class DetalleProveedorPedidoControllerTest {

    @Mock
    private DetalleProveedorPedidoService detalleService;

    @InjectMocks
    private DetalleProveedorPedidoController detalleController;

    private DetalleProveedorPedidoRequest detalleRequest;
    private DetalleProveedorPedidoResponse detalleResponse;
    private InsumoProveedorResponse insumoResponse;

    /**
     * Configuración inicial que se ejecuta antes de cada test.
     * Inicializa los objetos de prueba con datos de ejemplo para ser utilizados en las pruebas.
     */
    @BeforeEach
    void setUp() {
        // Configurar InsumoProveedorResponse
        insumoResponse = new InsumoProveedorResponse();
        insumoResponse.setId(1L);
        insumoResponse.setNombre("Harina de Trigo");
        insumoResponse.setDescripcion("Harina de trigo premium");
        insumoResponse.setCostoUnitario(2500.0);
        insumoResponse.setFechaVencimiento(LocalDate.now().plusMonths(6));
        insumoResponse.setCantidadDisponible(100);
        insumoResponse.setIdProveedor(1L);

        // Configurar Request
        detalleRequest = new DetalleProveedorPedidoRequest();
        detalleRequest.setInsumoProveedorId(1L);
        detalleRequest.setCantidadInsumo(10);
        detalleRequest.setPrecioUnitario(2500.0);

        // Configurar Response
        detalleResponse = new DetalleProveedorPedidoResponse();
        detalleResponse.setId(1L);
        detalleResponse.setCantidadInsumo(10);
        detalleResponse.setCostoSubtotal(25000.0);
        detalleResponse.setEsDevuelto(false);
        detalleResponse.setInsumo(insumoResponse);
    }

    /**
     * Verifica que el endpoint de creación de detalle funcione correctamente.
     *
     * Este test valida que:
     * - El controlador crea el detalle correctamente
     * - Retorna un código HTTP 201 (CREATED)
     * - La respuesta contiene los datos del detalle creado
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test crear detalle exitoso")
    void testCrearDetalleSuccess() {
        // Given
        Long idPedido = 1L;
        when(detalleService.crearDetalle(any(DetalleProveedorPedidoRequest.class), eq(idPedido)))
                .thenReturn(detalleResponse);

        // When
        ResponseEntity<DetalleProveedorPedidoResponse> response =
                detalleController.crearDetalle(idPedido, detalleRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(10, response.getBody().getCantidadInsumo());
        assertEquals(25000.0, response.getBody().getCostoSubtotal());
        assertFalse(response.getBody().getEsDevuelto());
        verify(detalleService, times(1)).crearDetalle(any(DetalleProveedorPedidoRequest.class), eq(idPedido));
    }

    /**
     * Verifica que el endpoint de obtener detalle por ID funcione correctamente.
     *
     * Este test valida que:
     * - El controlador obtiene el detalle correctamente
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene los datos del detalle
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test obtener detalle por ID exitoso")
    void testObtenerDetallePorIdSuccess() {
        // Given
        Long idDetalle = 1L;
        when(detalleService.obtenerDetallePorId(idDetalle)).thenReturn(detalleResponse);

        // When
        ResponseEntity<DetalleProveedorPedidoResponse> response =
                detalleController.obtenerDetallePorId(idDetalle);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Harina de Trigo", response.getBody().getInsumo().getNombre());
        verify(detalleService, times(1)).obtenerDetallePorId(idDetalle);
    }

    /**
     * Verifica que el endpoint maneje correctamente cuando no se encuentra el detalle.
     *
     * Este test valida que:
     * - El controlador maneja la excepción cuando el detalle no existe
     * - Retorna la excepción lanzada por el servicio
     */
    @Test
    @DisplayName("Test obtener detalle no encontrado")
    void testObtenerDetalleNotFound() {
        // Given
        Long idDetalle = 999L;
        when(detalleService.obtenerDetallePorId(idDetalle))
                .thenThrow(new RuntimeException("Detalle no encontrado con ID: " + idDetalle));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            detalleController.obtenerDetallePorId(idDetalle);
        });
        verify(detalleService, times(1)).obtenerDetallePorId(idDetalle);
    }

    /**
     * Verifica que el endpoint de listar todos los detalles funcione correctamente.
     *
     * Este test valida que:
     * - El controlador lista todos los detalles
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene una lista de detalles
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test listar todos los detalles exitoso")
    void testListarDetallesSuccess() {
        // Given
        List<DetalleProveedorPedidoResponse> detalles = List.of(detalleResponse);
        when(detalleService.listarDetalles()).thenReturn(detalles);

        // When
        ResponseEntity<List<DetalleProveedorPedidoResponse>> response =
                detalleController.listarDetalles();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(detalleService, times(1)).listarDetalles();
    }

    /**
     * Verifica que el endpoint de listar detalles por pedido funcione correctamente.
     *
     * Este test valida que:
     * - El controlador lista los detalles de un pedido específico
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene los detalles del pedido
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test listar detalles por pedido exitoso")
    void testListarDetallesPorPedidoSuccess() {
        // Given
        Long idPedido = 1L;
        List<DetalleProveedorPedidoResponse> detalles = List.of(detalleResponse);
        when(detalleService.listarDetallesPorPedido(idPedido)).thenReturn(detalles);

        // When
        ResponseEntity<List<DetalleProveedorPedidoResponse>> response =
                detalleController.listarDetallesPorPedido(idPedido);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(detalleService, times(1)).listarDetallesPorPedido(idPedido);
    }

    /**
     * Verifica que el endpoint de listar detalles por pedido retorne lista vacía cuando no hay detalles.
     *
     * Este test valida que:
     * - El controlador retorna una lista vacía cuando el pedido no tiene detalles
     * - Retorna un código HTTP 200 (OK)
     */
    @Test
    @DisplayName("Test listar detalles por pedido sin resultados")
    void testListarDetallesPorPedidoEmpty() {
        // Given
        Long idPedido = 999L;
        when(detalleService.listarDetallesPorPedido(idPedido)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<DetalleProveedorPedidoResponse>> response =
                detalleController.listarDetallesPorPedido(idPedido);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(detalleService, times(1)).listarDetallesPorPedido(idPedido);
    }

    /**
     * Verifica que el endpoint de listar detalles por insumo funcione correctamente.
     *
     * Este test valida que:
     * - El controlador lista los detalles de un insumo específico
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene los detalles del insumo
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test listar detalles por insumo exitoso")
    void testListarDetallesPorInsumoSuccess() {
        // Given
        Long idInsumo = 1L;
        List<DetalleProveedorPedidoResponse> detalles = List.of(detalleResponse);
        when(detalleService.listarDetallesPorInsumo(idInsumo)).thenReturn(detalles);

        // When
        ResponseEntity<List<DetalleProveedorPedidoResponse>> response =
                detalleController.listarDetallesPorInsumo(idInsumo);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(detalleService, times(1)).listarDetallesPorInsumo(idInsumo);
    }

    /**
     * Verifica que el endpoint de actualizar detalle funcione correctamente.
     *
     * Este test valida que:
     * - El controlador actualiza el detalle correctamente
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene los datos actualizados
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test actualizar detalle exitoso")
    void testActualizarDetalleSuccess() {
        // Given
        Long idDetalle = 1L;
        detalleRequest.setCantidadInsumo(20);
        detalleResponse.setCantidadInsumo(20);
        detalleResponse.setCostoSubtotal(50000.0);

        when(detalleService.actualizarDetalle(eq(idDetalle), any(DetalleProveedorPedidoRequest.class)))
                .thenReturn(detalleResponse);

        // When
        ResponseEntity<DetalleProveedorPedidoResponse> response =
                detalleController.actualizarDetalle(idDetalle, detalleRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(20, response.getBody().getCantidadInsumo());
        assertEquals(50000.0, response.getBody().getCostoSubtotal());
        verify(detalleService, times(1)).actualizarDetalle(eq(idDetalle), any(DetalleProveedorPedidoRequest.class));
    }

    /**
     * Verifica que el endpoint de actualizar detalle maneje errores correctamente.
     *
     * Este test valida que:
     * - El controlador maneja excepciones al actualizar
     * - Propaga la excepción del servicio
     */
    @Test
    @DisplayName("Test actualizar detalle con error")
    void testActualizarDetalleError() {
        // Given
        Long idDetalle = 999L;
        when(detalleService.actualizarDetalle(eq(idDetalle), any(DetalleProveedorPedidoRequest.class)))
                .thenThrow(new RuntimeException("Detalle no encontrado con ID: " + idDetalle));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            detalleController.actualizarDetalle(idDetalle, detalleRequest);
        });
        verify(detalleService, times(1)).actualizarDetalle(eq(idDetalle), any(DetalleProveedorPedidoRequest.class));
    }

    /**
     * Verifica que el endpoint de eliminar detalle funcione correctamente.
     *
     * Este test valida que:
     * - El controlador elimina el detalle correctamente
     * - Retorna un código HTTP 204 (NO_CONTENT)
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test eliminar detalle exitoso")
    void testEliminarDetalleSuccess() {
        // Given
        Long idDetalle = 1L;
        doNothing().when(detalleService).eliminarDetalle(idDetalle);

        // When
        ResponseEntity<Void> response = detalleController.eliminarDetalle(idDetalle);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(detalleService, times(1)).eliminarDetalle(idDetalle);
    }

    /**
     * Verifica que el endpoint de eliminar detalle maneje errores correctamente.
     *
     * Este test valida que:
     * - El controlador maneja excepciones al eliminar
     * - Propaga la excepción del servicio
     */
    @Test
    @DisplayName("Test eliminar detalle no encontrado")
    void testEliminarDetalleNotFound() {
        // Given
        Long idDetalle = 999L;
        doThrow(new RuntimeException("Detalle no encontrado con ID: " + idDetalle))
                .when(detalleService).eliminarDetalle(idDetalle);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            detalleController.eliminarDetalle(idDetalle);
        });
        verify(detalleService, times(1)).eliminarDetalle(idDetalle);
    }

    /**
     * Verifica que el endpoint de marcar como devuelto funcione correctamente.
     *
     * Este test valida que:
     * - El controlador marca el detalle como devuelto correctamente
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta indica que el detalle está devuelto
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test marcar como devuelto exitoso")
    void testMarcarComoDevueltoSuccess() {
        // Given
        Long idDetalle = 1L;
        detalleResponse.setEsDevuelto(true);
        when(detalleService.marcarComoDevuelto(idDetalle)).thenReturn(detalleResponse);

        // When
        ResponseEntity<DetalleProveedorPedidoResponse> response =
                detalleController.marcarComoDevuelto(idDetalle);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getEsDevuelto());
        verify(detalleService, times(1)).marcarComoDevuelto(idDetalle);
    }

    /**
     * Verifica que el endpoint de marcar como devuelto maneje errores correctamente.
     *
     * Este test valida que:
     * - El controlador maneja excepciones al marcar como devuelto
     * - Propaga la excepción del servicio
     */
    @Test
    @DisplayName("Test marcar como devuelto con error")
    void testMarcarComoDevueltoError() {
        // Given
        Long idDetalle = 999L;
        when(detalleService.marcarComoDevuelto(idDetalle))
                .thenThrow(new RuntimeException("Detalle no encontrado con ID: " + idDetalle));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            detalleController.marcarComoDevuelto(idDetalle);
        });
        verify(detalleService, times(1)).marcarComoDevuelto(idDetalle);
    }

    /**
     * Verifica que el endpoint de crear detalle valide el request correctamente.
     *
     * Este test valida que:
     * - El controlador acepta requests válidos
     * - Los datos son procesados correctamente
     */
    @Test
    @DisplayName("Test crear detalle con request válido")
    void testCrearDetalleValidRequest() {
        // Given
        Long idPedido = 1L;
        DetalleProveedorPedidoRequest validRequest = new DetalleProveedorPedidoRequest();
        validRequest.setInsumoProveedorId(1L);
        validRequest.setCantidadInsumo(5);
        validRequest.setPrecioUnitario(3000.0);

        DetalleProveedorPedidoResponse expectedResponse = new DetalleProveedorPedidoResponse();
        expectedResponse.setId(2L);
        expectedResponse.setCantidadInsumo(5);
        expectedResponse.setCostoSubtotal(15000.0);
        expectedResponse.setEsDevuelto(false);

        when(detalleService.crearDetalle(any(DetalleProveedorPedidoRequest.class), eq(idPedido)))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<DetalleProveedorPedidoResponse> response =
                detalleController.crearDetalle(idPedido, validRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(15000.0, response.getBody().getCostoSubtotal());
        verify(detalleService, times(1)).crearDetalle(any(DetalleProveedorPedidoRequest.class), eq(idPedido));
    }

    /**
     * Verifica que el endpoint de crear detalle maneje errores del servicio.
     *
     * Este test valida que:
     * - El controlador maneja excepciones del servicio correctamente
     * - Propaga la excepción cuando hay problemas en la creación
     */
    @Test
    @DisplayName("Test crear detalle con error del servicio")
    void testCrearDetalleServiceError() {
        // Given
        Long idPedido = 1L;
        when(detalleService.crearDetalle(any(DetalleProveedorPedidoRequest.class), eq(idPedido)))
                .thenThrow(new RuntimeException("Error al crear detalle: Insumo proveedor no encontrado"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            detalleController.crearDetalle(idPedido, detalleRequest);
        });
        verify(detalleService, times(1)).crearDetalle(any(DetalleProveedorPedidoRequest.class), eq(idPedido));
    }
}

