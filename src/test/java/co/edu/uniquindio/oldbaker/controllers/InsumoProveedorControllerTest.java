package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.InsumoProveedorResponse;
import co.edu.uniquindio.oldbaker.services.InsumoProveedorService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para InsumoProveedorController.
 *
 * Estas pruebas verifican el comportamiento de los endpoints del controlador de insumos de proveedores
 * que gestionan la consulta de insumos disponibles y sus detalles. Se utiliza Mockito para simular
 * el servicio de insumos de proveedores y validar que el controlador maneja correctamente las respuestas HTTP.
 *
 * @author OldBaker Team
 */
@ExtendWith(MockitoExtension.class)
class InsumoProveedorControllerTest {

    @Mock
    private InsumoProveedorService insumoProveedorService;

    @InjectMocks
    private InsumoProveedorController insumoProveedorController;

    private InsumoProveedorResponse insumoProveedorResponse1;
    private InsumoProveedorResponse insumoProveedorResponse2;

    /**
     * Configuración inicial que se ejecuta antes de cada test.
     * Inicializa los objetos de prueba con datos de ejemplo para ser utilizados en las pruebas.
     */
    @BeforeEach
    void setUp() {
        insumoProveedorResponse1 = new InsumoProveedorResponse();
        insumoProveedorResponse1.setId(1L);
        insumoProveedorResponse1.setNombre("Harina de Trigo");
        insumoProveedorResponse1.setDescripcion("Harina integral de alta calidad");
        insumoProveedorResponse1.setProveedorNombre("Distribuidora La Esperanza");
        insumoProveedorResponse1.setCostoUnitario(2500.0);
        insumoProveedorResponse1.setCantidadDisponible(1000);

        insumoProveedorResponse2 = new InsumoProveedorResponse();
        insumoProveedorResponse2.setId(2L);
        insumoProveedorResponse2.setNombre("Azúcar");
        insumoProveedorResponse2.setDescripcion("Azúcar refinada");
        insumoProveedorResponse2.setProveedorNombre("Distribuidora La Esperanza");
        insumoProveedorResponse2.setCostoUnitario(3000.0);
        insumoProveedorResponse2.setCantidadDisponible(500);
    }

    /**
     * Verifica que el endpoint de listado de insumos funcione correctamente.
     *
     * Este test valida que:
     * - El controlador obtiene la lista completa de insumos del servicio
     * - Retorna un código HTTP 200 (OK)
     * - La lista contiene el número correcto de insumos
     * - Los datos de los insumos son correctos
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test listar insumos de proveedores")
    void testListarInsumos() {
        // Given
        List<InsumoProveedorResponse> insumos = Arrays.asList(insumoProveedorResponse1, insumoProveedorResponse2);
        when(insumoProveedorService.listarInsumos()).thenReturn(insumos);

        // When
        ResponseEntity<List<InsumoProveedorResponse>> response = insumoProveedorController.listarInsumos();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Harina de Trigo", response.getBody().get(0).getNombre());
        assertEquals("Azúcar", response.getBody().get(1).getNombre());
        verify(insumoProveedorService, times(1)).listarInsumos();
    }

    /**
     * Verifica que el endpoint de listado retorne una lista vacía cuando no hay insumos.
     *
     * Este test valida que:
     * - El controlador maneja correctamente el caso de lista vacía
     * - Retorna un código HTTP 200 (OK)
     * - La lista está vacía
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test listar insumos cuando no hay registros")
    void testListarInsumosVacio() {
        // Given
        when(insumoProveedorService.listarInsumos()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<InsumoProveedorResponse>> response = insumoProveedorController.listarInsumos();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(insumoProveedorService, times(1)).listarInsumos();
    }

    /**
     * Verifica que el endpoint de consulta de insumo por ID funcione correctamente.
     *
     * Este test valida que:
     * - El controlador consulta el insumo usando el ID proporcionado
     * - Retorna un código HTTP 200 (OK)
     * - El insumo retornado contiene los datos esperados
     * - El servicio es llamado con el ID correcto exactamente una vez
     */
    @Test
    @DisplayName("Test obtener insumo por ID")
    void testObtenerInsumoPorId() {
        // Given
        Long insumoId = 1L;
        when(insumoProveedorService.obtenerInsumoPorId(insumoId)).thenReturn(insumoProveedorResponse1);

        // When
        ResponseEntity<InsumoProveedorResponse> response = insumoProveedorController.obtenerInsumoPorId(insumoId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Harina de Trigo", response.getBody().getNombre());
        assertEquals("Distribuidora La Esperanza", response.getBody().getProveedorNombre());
        assertEquals(2500.0, response.getBody().getCostoUnitario());
        verify(insumoProveedorService, times(1)).obtenerInsumoPorId(insumoId);
    }

    /**
     * Verifica que el endpoint maneje correctamente cuando se busca un insumo que no existe.
     *
     * Este test valida que:
     * - El controlador propaga la excepción del servicio cuando el insumo no existe
     * - El servicio es invocado exactamente una vez con el ID correcto
     */
    @Test
    @DisplayName("Test obtener insumo por ID cuando no existe")
    void testObtenerInsumoPorIdNoExiste() {
        // Given
        Long insumoId = 999L;
        when(insumoProveedorService.obtenerInsumoPorId(insumoId))
                .thenThrow(new IllegalArgumentException("Insumo de proveedor no encontrado"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            insumoProveedorController.obtenerInsumoPorId(insumoId);
        });
        verify(insumoProveedorService, times(1)).obtenerInsumoPorId(insumoId);
    }
}
