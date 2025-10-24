package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.ProveedorRequest;
import co.edu.uniquindio.oldbaker.dto.ProveedorResponse;
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
 */
@ExtendWith(MockitoExtension.class)
class ProveedorControllerTest {

    @Mock
    private ProveedorService proveedorService;

    @InjectMocks
    private ProveedorController proveedorController;

    private ProveedorRequest proveedorRequest;
    private ProveedorResponse proveedorResponse;

    @BeforeEach
    void setUp() {
        proveedorRequest = new ProveedorRequest();
        proveedorRequest.setNombre("Distribuidora La Esperanza");
        proveedorRequest.setTelefono("3001234567");
        proveedorRequest.setEmail("contacto@laesperanza.com");
        proveedorRequest.setNumeroCuenta("1234567890");

        proveedorResponse = new ProveedorResponse();
        proveedorResponse.setIdProveedor(1L);
        proveedorResponse.setNombre("Distribuidora La Esperanza");
        proveedorResponse.setTelefono("3001234567");
        proveedorResponse.setEmail("contacto@laesperanza.com");
        proveedorResponse.setNumeroCuenta("1234567890");
    }

    @Test
    @DisplayName("Test crear proveedor exitoso")
    void testCrearProveedorSuccess() {
        // Given
        when(proveedorService.crearProveedor(any(ProveedorRequest.class))).thenReturn(proveedorResponse);

        // When
        ResponseEntity<ProveedorResponse> response = proveedorController.crearProveedor(proveedorRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Distribuidora La Esperanza", response.getBody().getNombre());
        assertEquals("contacto@laesperanza.com", response.getBody().getEmail());
        verify(proveedorService, times(1)).crearProveedor(any(ProveedorRequest.class));
    }

    @Test
    @DisplayName("Test listar proveedores")
    void testListarProveedores() {
        // Given
        ProveedorResponse proveedor2 = new ProveedorResponse();
        proveedor2.setIdProveedor(2L);
        proveedor2.setNombre("Distribuidora El Sol");
        proveedor2.setEmail("contacto@elsol.com");
        proveedor2.setTelefono("3009876543");
        proveedor2.setNumeroCuenta("0987654321");

        List<ProveedorResponse> proveedores = Arrays.asList(proveedorResponse, proveedor2);
        when(proveedorService.listarProveedores()).thenReturn(proveedores);

        // When
        ResponseEntity<List<ProveedorResponse>> response = proveedorController.listarProveedores();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Distribuidora La Esperanza", response.getBody().get(0).getNombre());
        assertEquals("Distribuidora El Sol", response.getBody().get(1).getNombre());
        verify(proveedorService, times(1)).listarProveedores();
    }

    @Test
    @DisplayName("Test listar proveedores cuando no hay registros")
    void testListarProveedoresVacio() {
        // Given
        when(proveedorService.listarProveedores()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<ProveedorResponse>> response = proveedorController.listarProveedores();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(proveedorService, times(1)).listarProveedores();
    }

    @Test
    @DisplayName("Test obtener proveedor por ID")
    void testObtenerProveedorPorId() {
        // Given
        Long proveedorId = 1L;
        when(proveedorService.buscarPorId(proveedorId)).thenReturn(Optional.of(proveedorResponse));

        // When
        ResponseEntity<ProveedorResponse> response = proveedorController.obtenerProveedor(proveedorId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Distribuidora La Esperanza", response.getBody().getNombre());
        assertEquals("3001234567", response.getBody().getTelefono());
        assertEquals("1234567890", response.getBody().getNumeroCuenta());
        verify(proveedorService, times(1)).buscarPorId(proveedorId);
    }

    @Test
    @DisplayName("Test obtener proveedor por ID cuando no existe")
    void testObtenerProveedorPorIdNoExiste() {
        // Given
        Long proveedorId = 999L;
        when(proveedorService.buscarPorId(proveedorId)).thenReturn(Optional.empty());

        // When
        ResponseEntity<ProveedorResponse> response = proveedorController.obtenerProveedor(proveedorId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(proveedorService, times(1)).buscarPorId(proveedorId);
    }

    @Test
    @DisplayName("Test actualizar proveedor exitoso")
    void testActualizarProveedorSuccess() {
        // Given
        Long proveedorId = 1L;
        ProveedorResponse actualizado = new ProveedorResponse();
        actualizado.setIdProveedor(1L);
        actualizado.setNombre("Distribuidora La Esperanza S.A.");
        actualizado.setTelefono("3001234567");
        actualizado.setEmail("contacto@laesperanza.com");
        actualizado.setNumeroCuenta("1234567890");

        when(proveedorService.actualizarProveedor(anyLong(), any(ProveedorRequest.class)))
                .thenReturn(actualizado);

        // When
        ResponseEntity<ProveedorResponse> response = proveedorController.actualizarProveedor(proveedorId, proveedorRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Distribuidora La Esperanza S.A.", response.getBody().getNombre());
        verify(proveedorService, times(1)).actualizarProveedor(proveedorId, proveedorRequest);
    }

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
