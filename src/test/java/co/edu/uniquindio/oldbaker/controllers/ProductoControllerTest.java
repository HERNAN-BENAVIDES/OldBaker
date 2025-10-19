package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.ProductoHomeResponse;
import co.edu.uniquindio.oldbaker.dto.ProductoResponse;
import co.edu.uniquindio.oldbaker.services.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para ProductoController.
 *
 * Estas pruebas verifican el comportamiento de los endpoints del controlador de productos públicos
 * que gestionan la consulta de productos disponibles para clientes. Se utiliza Mockito para simular
 * el servicio de productos y validar que el controlador maneja correctamente las respuestas HTTP.
 *
 * @author OldBaker Team
 */
@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private ProductoController productoController;

    private ProductoHomeResponse productoHomeResponse1;
    private ProductoHomeResponse productoHomeResponse2;
    private ProductoResponse productoResponse;

    /**
     * Configuración inicial que se ejecuta antes de cada test.
     * Inicializa los objetos de prueba con datos de ejemplo para ser utilizados en las pruebas.
     */
    @BeforeEach
    void setUp() {
        productoHomeResponse1 = new ProductoHomeResponse(
                1L,
                "Pan Integral",
                "Pan integral con semillas",
                3500.0,
                5,
                "Panadería",
                "https://example.com/pan.jpg",
                10
        );

        productoHomeResponse2 = new ProductoHomeResponse(
                2L,
                "Croissant",
                "Croissant francés",
                4000.0,
                3,
                "Panadería",
                "https://example.com/croissant.jpg",
                5
        );

        productoResponse = new ProductoResponse();
        productoResponse.setIdProducto(1L);
        productoResponse.setNombre("Pan Integral");
        productoResponse.setDescripcion("Pan integral con semillas");
        productoResponse.setCostoUnitario(3500.0);
        productoResponse.setCategoriaNombre("Panadería");
        productoResponse.setVidaUtilDias(5);
        productoResponse.setPedidoMinimo(10);
    }

    /**
     * Verifica que el endpoint de listado de productos funcione correctamente.
     *
     * Este test valida que:
     * - El controlador obtiene la lista completa de productos del servicio
     * - Retorna un código HTTP 200 (OK)
     * - La lista contiene el número correcto de productos
     * - Los datos de los productos son correctos
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test listar productos para home")
    void testListarProductos() {
        // Given
        List<ProductoHomeResponse> productos = Arrays.asList(productoHomeResponse1, productoHomeResponse2);
        when(productoService.listarProductosHome()).thenReturn(productos);

        // When
        ResponseEntity<List<ProductoHomeResponse>> response = productoController.listarProductos();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Pan Integral", response.getBody().get(0).getNombre());
        assertEquals(3500.0, response.getBody().get(0).getCostoUnitario());
        assertEquals("Croissant", response.getBody().get(1).getNombre());
        assertEquals(4000.0, response.getBody().get(1).getCostoUnitario());
        verify(productoService, times(1)).listarProductosHome();
    }

    /**
     * Verifica que el endpoint retorne una lista vacía cuando no hay productos.
     *
     * Este test valida que:
     * - El controlador maneja correctamente el caso de lista vacía
     * - Retorna un código HTTP 200 (OK)
     * - La lista está vacía
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test listar productos cuando no hay registros")
    void testListarProductosVacio() {
        // Given
        when(productoService.listarProductosHome()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<ProductoHomeResponse>> response = productoController.listarProductos();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(productoService, times(1)).listarProductosHome();
    }

    /**
     * Verifica que el endpoint de consulta de producto por ID funcione correctamente.
     *
     * Este test valida que:
     * - El controlador consulta el producto usando el ID proporcionado
     * - Retorna un código HTTP 200 (OK)
     * - El producto retornado contiene los datos esperados
     * - El servicio es llamado con el ID correcto exactamente una vez
     */
    @Test
    @DisplayName("Test obtener producto por ID")
    void testObtenerProductoPorId() {
        // Given
        Long productoId = 1L;
        when(productoService.obtenerProductoPorId(productoId)).thenReturn(productoResponse);

        // When
        ResponseEntity<ProductoResponse> response = productoController.obtenerProducto(productoId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getIdProducto());
        assertEquals("Pan Integral", response.getBody().getNombre());
        assertEquals("Pan integral con semillas", response.getBody().getDescripcion());
        assertEquals(3500.0, response.getBody().getCostoUnitario());
        assertEquals("Panadería", response.getBody().getCategoriaNombre());
        assertEquals(5, response.getBody().getVidaUtilDias());
        assertEquals(10, response.getBody().getPedidoMinimo());
        verify(productoService, times(1)).obtenerProductoPorId(productoId);
    }

    /**
     * Verifica que el endpoint maneje correctamente cuando no se encuentra el producto.
     *
     * Este test valida que:
     * - El controlador propaga la excepción cuando el producto no existe
     * - El servicio es invocado exactamente una vez con el ID correcto
     */
    @Test
    @DisplayName("Test obtener producto por ID cuando no existe")
    void testObtenerProductoPorIdNoExiste() {
        // Given
        Long productoId = 999L;
        when(productoService.obtenerProductoPorId(productoId))
                .thenThrow(new IllegalArgumentException("Producto no encontrado"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            productoController.obtenerProducto(productoId)
        );
        verify(productoService, times(1)).obtenerProductoPorId(productoId);
    }

    /**
     * Verifica que el endpoint maneje errores del servicio correctamente.
     *
     * Este test valida que:
     * - El controlador propaga excepciones del servicio
     * - El servicio es invocado correctamente
     */
    @Test
    @DisplayName("Test listar productos con error en el servicio")
    void testListarProductosConError() {
        // Given
        when(productoService.listarProductosHome())
                .thenThrow(new RuntimeException("Error en base de datos"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
            productoController.listarProductos()
        );
        verify(productoService, times(1)).listarProductosHome();
    }
}
