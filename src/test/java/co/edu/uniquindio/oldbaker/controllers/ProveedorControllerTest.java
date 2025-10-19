package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.ProveedorRequest;
import co.edu.uniquindio.oldbaker.services.ProveedorService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para ProveedorController.
 *
 * Estas pruebas verifican el comportamiento de los endpoints del controlador de proveedores
 * que gestionan operaciones CRUD sobre proveedores. Se utiliza Mockito para simular el servicio
 * de proveedores y validar que el controlador maneja correctamente las respuestas HTTP.
 *
 * @author OldBaker Team
 */
@ExtendWith(MockitoExtension.class)
class ProveedorControllerTest {

    @Mock
    private ProveedorService proveedorService;

    @InjectMocks
    private ProveedorController proveedorController;

    private ProveedorRequest proveedorRequest;

    /**
     * Configuración inicial que se ejecuta antes de cada test.
     * Inicializa los objetos de prueba con datos de ejemplo para ser utilizados en las pruebas.
     */
    @BeforeEach
    void setUp() {
        proveedorRequest = new ProveedorRequest();
        proveedorRequest.setNombre("Distribuidora La Esperanza");
        proveedorRequest.setTelefono("3001234567");
        proveedorRequest.setEmail("contacto@laesperanza.com");
        proveedorRequest.setNumeroCuenta("1234567890");
    }

    /**
     * Verifica que el endpoint de creación de proveedor funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio con los datos del proveedor
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene los datos del proveedor creado
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test crear proveedor exitoso")
    void testCrearProveedorSuccess() {
        // Given
        when(proveedorService.crearProveedor(any(ProveedorRequest.class))).thenReturn(proveedorRequest);

        // When
        ResponseEntity<ProveedorRequest> response = proveedorController.crearProveedor(proveedorRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Distribuidora La Esperanza", response.getBody().getNombre());
        assertEquals("contacto@laesperanza.com", response.getBody().getEmail());
        verify(proveedorService, times(1)).crearProveedor(any(ProveedorRequest.class));
    }

    /**
     * Verifica que el endpoint de listado de proveedores funcione correctamente.
     *
     * Este test valida que:
     * - El controlador obtiene la lista completa de proveedores del servicio
     * - Retorna un código HTTP 200 (OK)
     * - La lista contiene el número correcto de proveedores
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test listar proveedores")
    void testListarProveedores() {
        // Given
        ProveedorRequest proveedor2 = new ProveedorRequest();
        proveedor2.setNombre("Distribuidora El Sol");
        proveedor2.setEmail("contacto@elsol.com");
        proveedor2.setTelefono("3009876543");
        proveedor2.setNumeroCuenta("0987654321");

        List<ProveedorRequest> proveedores = Arrays.asList(proveedorRequest, proveedor2);
        when(proveedorService.listarProveedores()).thenReturn(proveedores);

        // When
        ResponseEntity<List<ProveedorRequest>> response = proveedorController.listarProveedores();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Distribuidora La Esperanza", response.getBody().get(0).getNombre());
        assertEquals("Distribuidora El Sol", response.getBody().get(1).getNombre());
        verify(proveedorService, times(1)).listarProveedores();
    }

    /**
     * Verifica que el endpoint retorne una lista vacía cuando no hay proveedores.
     *
     * Este test valida que:
     * - El controlador maneja correctamente el caso de lista vacía
     * - Retorna un código HTTP 200 (OK)
     * - La lista está vacía
     */
    @Test
    @DisplayName("Test listar proveedores cuando no hay registros")
    void testListarProveedoresVacio() {
        // Given
        when(proveedorService.listarProveedores()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<ProveedorRequest>> response = proveedorController.listarProveedores();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(proveedorService, times(1)).listarProveedores();
    }

    /**
     * Verifica que el endpoint de consulta de proveedor por ID funcione correctamente.
     *
     * Este test valida que:
     * - El controlador consulta el proveedor usando el ID proporcionado
     * - Retorna un código HTTP 200 (OK)
     * - El proveedor retornado contiene los datos esperados
     * - El servicio es llamado con el ID correcto
     */
    @Test
    @DisplayName("Test obtener proveedor por ID")
    void testObtenerProveedorPorId() {
        // Given
        Long proveedorId = 1L;
        when(proveedorService.buscarPorId(proveedorId)).thenReturn(Optional.of(proveedorRequest));

        // When
        ResponseEntity<ProveedorRequest> response = proveedorController.obtenerProveedor(proveedorId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Distribuidora La Esperanza", response.getBody().getNombre());
        assertEquals("3001234567", response.getBody().getTelefono());
        assertEquals("1234567890", response.getBody().getNumeroCuenta());
        verify(proveedorService, times(1)).buscarPorId(proveedorId);
    }

    /**
     * Verifica que el endpoint retorne 404 cuando el proveedor no existe.
     *
     * Este test valida que:
     * - El controlador maneja correctamente cuando el servicio retorna Optional.empty()
     * - Retorna un código HTTP 404 (NOT_FOUND)
     * - El servicio es invocado con el ID correcto
     */
    @Test
    @DisplayName("Test obtener proveedor por ID cuando no existe")
    void testObtenerProveedorPorIdNoExiste() {
        // Given
        Long proveedorId = 999L;
        when(proveedorService.buscarPorId(proveedorId)).thenReturn(Optional.empty());

        // When
        ResponseEntity<ProveedorRequest> response = proveedorController.obtenerProveedor(proveedorId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(proveedorService, times(1)).buscarPorId(proveedorId);
    }

    /**
     * Verifica que el endpoint de actualización de proveedor funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio con el ID y los nuevos datos
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene los datos actualizados del proveedor
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test actualizar proveedor exitoso")
    void testActualizarProveedorSuccess() {
        // Given
        Long proveedorId = 1L;
        proveedorRequest.setNombre("Distribuidora La Esperanza S.A.");
        when(proveedorService.actualizarProveedor(anyLong(), any(ProveedorRequest.class)))
                .thenReturn(proveedorRequest);

        // When
        ResponseEntity<ProveedorRequest> response = proveedorController.actualizarProveedor(proveedorId, proveedorRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Distribuidora La Esperanza S.A.", response.getBody().getNombre());
        verify(proveedorService, times(1)).actualizarProveedor(proveedorId, proveedorRequest);
    }

    /**
     * Verifica que el endpoint de actualización maneje errores cuando el proveedor no existe.
     *
     * Este test valida que:
     * - El controlador propaga excepciones del servicio
     * - El servicio es invocado con los parámetros correctos
     */
    @Test
    @DisplayName("Test actualizar proveedor cuando no existe")
    void testActualizarProveedorNoExiste() {
        // Given
        Long proveedorId = 999L;
        when(proveedorService.actualizarProveedor(anyLong(), any(ProveedorRequest.class)))
                .thenThrow(new IllegalArgumentException("Proveedor no encontrado"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            proveedorController.actualizarProveedor(proveedorId, proveedorRequest)
        );
        verify(proveedorService, times(1)).actualizarProveedor(proveedorId, proveedorRequest);
    }

    /**
     * Verifica que el endpoint de eliminación de proveedor funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio para eliminar el proveedor
     * - Retorna un código HTTP 204 (NO_CONTENT)
     * - No retorna ningún cuerpo en la respuesta
     * - El servicio es invocado exactamente una vez con el ID correcto
     */
    @Test
    @DisplayName("Test eliminar proveedor exitoso")
    void testEliminarProveedorSuccess() {
        // Given
        Long proveedorId = 1L;
        doNothing().when(proveedorService).eliminarProveedor(proveedorId);

        // When
        ResponseEntity<Void> response = proveedorController.eliminarProveedor(proveedorId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(proveedorService, times(1)).eliminarProveedor(proveedorId);
    }

    /**
     * Verifica que el endpoint de eliminación maneje errores cuando el proveedor no existe.
     *
     * Este test valida que:
     * - El controlador propaga excepciones del servicio
     * - El servicio es invocado con el ID correcto
     */
    @Test
    @DisplayName("Test eliminar proveedor cuando no existe")
    void testEliminarProveedorNoExiste() {
        // Given
        Long proveedorId = 999L;
        doThrow(new IllegalArgumentException("Proveedor no encontrado"))
                .when(proveedorService).eliminarProveedor(proveedorId);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            proveedorController.eliminarProveedor(proveedorId)
        );
        verify(proveedorService, times(1)).eliminarProveedor(proveedorId);
    }
}
