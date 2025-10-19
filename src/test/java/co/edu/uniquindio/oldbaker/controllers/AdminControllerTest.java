package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.ProductoRequest;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para AdminController.
 *
 * Estas pruebas verifican el comportamiento de los endpoints del controlador de administración
 * que gestionan operaciones CRUD sobre productos. Se utiliza Mockito para simular el servicio
 * de productos y validar que el controlador maneja correctamente las respuestas HTTP.
 *
 * @author OldBaker Team
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private AdminController adminController;

    private ProductoRequest productoRequest;
    private ProductoResponse productoResponse;

    /**
     * Configuración inicial que se ejecuta antes de cada test.
     * Inicializa los objetos de prueba (ProductoRequest y ProductoResponse)
     * con datos de ejemplo para ser utilizados en las pruebas.
     */
    @BeforeEach
    void setUp() {
        productoRequest = new ProductoRequest();
        productoRequest.setNombre("Pan Integral");
        productoRequest.setDescripcion("Pan integral con semillas");
        productoRequest.setCostoUnitario(3500.0);
        productoRequest.setDiasVidaUtil(5);
        productoRequest.setCategoriaId(1L);
        productoRequest.setPedidoMinimo(10);
        productoRequest.setInsumoId(1L);
        productoRequest.setCantidadInsumo(100);
        productoRequest.setCantidadProductos(20);

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
     * Verifica que el endpoint de creación de producto con receta funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio con los parámetros correctos
     * - Retorna un código HTTP 201 (CREATED)
     * - El cuerpo de la respuesta contiene los datos del producto creado
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test crear producto con receta")
    void testCrearProductoConReceta() {
        // Given
        when(productoService.crearProductoConReceta(any(ProductoRequest.class)))
                .thenReturn(productoResponse);

        // When
        ResponseEntity<ProductoResponse> response = adminController.crearProductoConReceta(productoRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Pan Integral", response.getBody().getNombre());
        assertEquals(3500.0, response.getBody().getCostoUnitario());
        verify(productoService, times(1)).crearProductoConReceta(any(ProductoRequest.class));
    }

    /**
     * Verifica que el endpoint de listado de productos funcione correctamente.
     *
     * Este test valida que:
     * - El controlador obtiene la lista completa de productos del servicio
     * - Retorna un código HTTP 200 (OK)
     * - La lista contiene el número correcto de productos
     * - Los datos de los productos son correctos
     */
    @Test
    @DisplayName("Test listar productos")
    void testListarProductos() {
        // Given
        ProductoResponse producto2 = new ProductoResponse();
        producto2.setIdProducto(2L);
        producto2.setNombre("Croissant");

        List<ProductoResponse> productos = Arrays.asList(productoResponse, producto2);
        when(productoService.listarProductos()).thenReturn(productos);

        // When
        ResponseEntity<List<ProductoResponse>> response = adminController.listarProductos();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Pan Integral", response.getBody().get(0).getNombre());
        verify(productoService, times(1)).listarProductos();
    }

    /**
     * Verifica que el endpoint de consulta de producto por ID funcione correctamente.
     *
     * Este test valida que:
     * - El controlador consulta el producto usando el ID proporcionado
     * - Retorna un código HTTP 200 (OK)
     * - El producto retornado contiene los datos esperados
     * - El servicio es llamado con el ID correcto
     */
    @Test
    @DisplayName("Test obtener producto por ID")
    void testObtenerProductoPorId() {
        // Given
        Long productoId = 1L;
        when(productoService.obtenerProductoPorId(productoId)).thenReturn(productoResponse);

        // When
        ResponseEntity<ProductoResponse> response = adminController.obtenerProducto(productoId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getIdProducto());
        assertEquals("Pan Integral", response.getBody().getNombre());
        verify(productoService, times(1)).obtenerProductoPorId(productoId);
    }

    /**
     * Verifica que el endpoint de eliminación de producto funcione correctamente.
     *
     * Este test valida que:
     * - El controlador invoca el método de eliminación del servicio
     * - Retorna un código HTTP 204 (NO_CONTENT)
     * - No retorna ningún cuerpo en la respuesta
     * - El servicio es invocado exactamente una vez con el ID correcto
     */
    @Test
    @DisplayName("Test eliminar producto")
    void testEliminarProducto() {
        // Given
        Long productoId = 1L;
        doNothing().when(productoService).eliminarProducto(productoId);

        // When
        ResponseEntity<Void> response = adminController.eliminarProducto(productoId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(productoService, times(1)).eliminarProducto(productoId);
    }
}
