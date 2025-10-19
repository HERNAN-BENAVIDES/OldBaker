package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.DetalleProveedorPedidoRequest;
import co.edu.uniquindio.oldbaker.dto.PedidoInsumoRequest;
import co.edu.uniquindio.oldbaker.dto.PedidoInsumoResponse;
import co.edu.uniquindio.oldbaker.dto.ReporteProveedorRequest;
import co.edu.uniquindio.oldbaker.dto.ReporteProveedorResponse;
import co.edu.uniquindio.oldbaker.model.PedidoInsumo;
import co.edu.uniquindio.oldbaker.services.PedidoInsumoService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para PedidoInsumoAdminController.
 *
 * Estas pruebas verifican el comportamiento de los endpoints del controlador de pedidos de insumos
 * para administradores. Se validan operaciones CRUD, aprobación, pago y devoluciones de insumos.
 * Se utiliza Mockito para simular el servicio y validar que el controlador maneja correctamente
 * las respuestas HTTP.
 *
 * @author OldBaker Team
 */
@ExtendWith(MockitoExtension.class)
class PedidoInsumoAdminControllerTest {

    @Mock
    private PedidoInsumoService pedidoInsumoService;

    @InjectMocks
    private PedidoInsumoAdminController pedidoInsumoAdminController;

    private PedidoInsumoRequest pedidoInsumoRequest;
    private PedidoInsumoResponse pedidoInsumoResponse;
    private ReporteProveedorRequest reporteProveedorRequest;
    private ReporteProveedorResponse reporteProveedorResponse;

    /**
     * Configuración inicial que se ejecuta antes de cada test.
     * Inicializa los objetos de prueba con datos de ejemplo para ser utilizados en las pruebas.
     */
    @BeforeEach
    void setUp() {
        DetalleProveedorPedidoRequest detalle = new DetalleProveedorPedidoRequest();
        detalle.setInsumoProveedorId(1L);
        detalle.setCantidadInsumo(100);
        detalle.setPrecioUnitario(2500.0);

        pedidoInsumoRequest = new PedidoInsumoRequest();
        pedidoInsumoRequest.setNombre("Pedido de Harina");
        pedidoInsumoRequest.setDescripcion("Pedido urgente de harina");
        pedidoInsumoRequest.setFechaPedido(LocalDate.now());
        pedidoInsumoRequest.setDetalles(List.of(detalle));

        pedidoInsumoResponse = new PedidoInsumoResponse();
        pedidoInsumoResponse.setId(1L);
        pedidoInsumoResponse.setNombre("Pedido de Harina");
        pedidoInsumoResponse.setDescripcion("Pedido urgente de harina");
        pedidoInsumoResponse.setCostoTotal(250000.0);
        pedidoInsumoResponse.setEstado(PedidoInsumo.EstadoPedido.PENDIENTE);
        pedidoInsumoResponse.setFechaPedido(LocalDate.now());

        reporteProveedorRequest = new ReporteProveedorRequest();
        reporteProveedorRequest.setDetalleId(1L);
        reporteProveedorRequest.setRazon("Producto en mal estado");

        reporteProveedorResponse = new ReporteProveedorResponse();
        reporteProveedorResponse.setIdDevolucion(1L);
        reporteProveedorResponse.setDetalleId(1L);
        reporteProveedorResponse.setCantidadDevuelta(10);
        reporteProveedorResponse.setRazon("Producto en mal estado");
        reporteProveedorResponse.setFechaDevolucion(LocalDate.now());
    }

    /**
     * Verifica que el endpoint de creación de pedido funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio con los datos del pedido
     * - Retorna un código HTTP 201 (CREATED)
     * - La respuesta contiene los datos del pedido creado
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test crear pedido de insumo exitoso")
    void testCrearPedidoSuccess() {
        // Given
        when(pedidoInsumoService.crearPedido(any(PedidoInsumoRequest.class))).thenReturn(pedidoInsumoResponse);

        // When
        ResponseEntity<PedidoInsumoResponse> response = pedidoInsumoAdminController.crearPedido(pedidoInsumoRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Pedido de Harina", response.getBody().getNombre());
        assertEquals(PedidoInsumo.EstadoPedido.PENDIENTE, response.getBody().getEstado());
        verify(pedidoInsumoService, times(1)).crearPedido(any(PedidoInsumoRequest.class));
    }

    /**
     * Verifica que el endpoint de listado de pedidos funcione correctamente.
     *
     * Este test valida que:
     * - El controlador obtiene la lista completa de pedidos del servicio
     * - Retorna un código HTTP 200 (OK)
     * - La lista contiene el número correcto de pedidos
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test listar pedidos de insumos")
    void testObtenerPedidos() {
        // Given
        PedidoInsumoResponse pedido2 = new PedidoInsumoResponse();
        pedido2.setId(2L);
        pedido2.setNombre("Pedido de Azúcar");
        pedido2.setCostoTotal(150000.0);

        List<PedidoInsumoResponse> pedidos = Arrays.asList(pedidoInsumoResponse, pedido2);
        when(pedidoInsumoService.listarPedidos()).thenReturn(pedidos);

        // When
        ResponseEntity<List<PedidoInsumoResponse>> response = pedidoInsumoAdminController.obtenerPedidos();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Pedido de Harina", response.getBody().get(0).getNombre());
        assertEquals("Pedido de Azúcar", response.getBody().get(1).getNombre());
        verify(pedidoInsumoService, times(1)).listarPedidos();
    }

    /**
     * Verifica que el endpoint retorne una lista vacía cuando no hay pedidos.
     *
     * Este test valida que:
     * - El controlador maneja correctamente el caso de lista vacía
     * - Retorna un código HTTP 200 (OK)
     * - La lista está vacía
     */
    @Test
    @DisplayName("Test listar pedidos cuando no hay registros")
    void testObtenerPedidosVacio() {
        // Given
        when(pedidoInsumoService.listarPedidos()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<PedidoInsumoResponse>> response = pedidoInsumoAdminController.obtenerPedidos();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(pedidoInsumoService, times(1)).listarPedidos();
    }

    /**
     * Verifica que el endpoint de consulta de pedido por ID funcione correctamente.
     *
     * Este test valida que:
     * - El controlador consulta el pedido usando el ID proporcionado
     * - Retorna un código HTTP 200 (OK)
     * - El pedido retornado contiene los datos esperados
     * - El servicio es llamado con el ID correcto
     */
    @Test
    @DisplayName("Test obtener pedido por ID")
    void testObtenerPedidoPorId() {
        // Given
        Long pedidoId = 1L;
        when(pedidoInsumoService.buscarPorId(pedidoId)).thenReturn(pedidoInsumoResponse);

        // When
        ResponseEntity<PedidoInsumoResponse> response = pedidoInsumoAdminController.obtenerPedido(pedidoId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Pedido de Harina", response.getBody().getNombre());
        assertEquals(250000.0, response.getBody().getCostoTotal());
        verify(pedidoInsumoService, times(1)).buscarPorId(pedidoId);
    }

    /**
     * Verifica que el endpoint maneje correctamente cuando no se encuentra el pedido.
     *
     * Este test valida que:
     * - El controlador propaga la excepción cuando el pedido no existe
     * - El servicio es invocado con el ID correcto
     */
    @Test
    @DisplayName("Test obtener pedido por ID cuando no existe")
    void testObtenerPedidoPorIdNoExiste() {
        // Given
        Long pedidoId = 999L;
        when(pedidoInsumoService.buscarPorId(pedidoId))
                .thenThrow(new IllegalArgumentException("Pedido no encontrado"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            pedidoInsumoAdminController.obtenerPedido(pedidoId)
        );
        verify(pedidoInsumoService, times(1)).buscarPorId(pedidoId);
    }

    /**
     * Verifica que el endpoint de aprobación de pedido funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio para aprobar el pedido
     * - Retorna un código HTTP 200 (OK)
     * - El pedido retornado tiene el estado actualizado
     * - El servicio es invocado exactamente una vez con el ID correcto
     */
    @Test
    @DisplayName("Test aprobar pedido exitoso")
    void testAprobarPedido() {
        // Given
        Long pedidoId = 1L;
        pedidoInsumoResponse.setEstado(PedidoInsumo.EstadoPedido.APROBADO);
        when(pedidoInsumoService.aprobarPedido(pedidoId)).thenReturn(pedidoInsumoResponse);

        // When
        ResponseEntity<PedidoInsumoResponse> response = pedidoInsumoAdminController.aprobarPedido(pedidoId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PedidoInsumo.EstadoPedido.APROBADO, response.getBody().getEstado());
        verify(pedidoInsumoService, times(1)).aprobarPedido(pedidoId);
    }

    /**
     * Verifica que el endpoint de pago de pedido funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio para marcar el pedido como pagado
     * - Retorna un código HTTP 200 (OK)
     * - El pedido retornado tiene el estado actualizado a PAGADO
     * - El servicio es invocado exactamente una vez con el ID correcto
     */
    @Test
    @DisplayName("Test pagar pedido exitoso")
    void testPagarPedido() {
        // Given
        Long pedidoId = 1L;
        pedidoInsumoResponse.setEstado(PedidoInsumo.EstadoPedido.PAGADO);
        when(pedidoInsumoService.pagarPedido(pedidoId)).thenReturn(pedidoInsumoResponse);

        // When
        ResponseEntity<PedidoInsumoResponse> response = pedidoInsumoAdminController.pagarPedido(pedidoId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PedidoInsumo.EstadoPedido.PAGADO, response.getBody().getEstado());
        verify(pedidoInsumoService, times(1)).pagarPedido(pedidoId);
    }

    /**
     * Verifica que el endpoint de devolución de insumo funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio con el ID del pedido y los datos de la devolución
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene los datos del reporte de devolución
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test devolver insumo exitoso")
    void testDevolverInsumo() {
        // Given
        Long pedidoId = 1L;
        when(pedidoInsumoService.devolverInsumo(pedidoId, reporteProveedorRequest))
                .thenReturn(reporteProveedorResponse);

        // When
        ResponseEntity<ReporteProveedorResponse> response =
                pedidoInsumoAdminController.devolverInsumo(pedidoId, reporteProveedorRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getIdDevolucion());
        assertEquals(10, response.getBody().getCantidadDevuelta());
        assertEquals("Producto en mal estado", response.getBody().getRazon());
        verify(pedidoInsumoService, times(1)).devolverInsumo(pedidoId, reporteProveedorRequest);
    }

    /**
     * Verifica que el endpoint de aprobación maneje errores cuando el pedido no puede ser aprobado.
     *
     * Este test valida que:
     * - El controlador propaga excepciones del servicio
     * - El servicio es invocado con el ID correcto
     */
    @Test
    @DisplayName("Test aprobar pedido con error")
    void testAprobarPedidoError() {
        // Given
        Long pedidoId = 1L;
        when(pedidoInsumoService.aprobarPedido(pedidoId))
                .thenThrow(new IllegalStateException("El pedido ya está aprobado"));

        // When & Then
        assertThrows(IllegalStateException.class, () ->
            pedidoInsumoAdminController.aprobarPedido(pedidoId)
        );
        verify(pedidoInsumoService, times(1)).aprobarPedido(pedidoId);
    }

    /**
     * Verifica que el endpoint de pago maneje errores cuando el pedido no puede ser pagado.
     *
     * Este test valida que:
     * - El controlador propaga excepciones del servicio
     * - El servicio es invocado con el ID correcto
     */
    @Test
    @DisplayName("Test pagar pedido con error")
    void testPagarPedidoError() {
        // Given
        Long pedidoId = 1L;
        when(pedidoInsumoService.pagarPedido(pedidoId))
                .thenThrow(new IllegalStateException("El pedido debe estar aprobado primero"));

        // When & Then
        assertThrows(IllegalStateException.class, () ->
            pedidoInsumoAdminController.pagarPedido(pedidoId)
        );
        verify(pedidoInsumoService, times(1)).pagarPedido(pedidoId);
    }
}
