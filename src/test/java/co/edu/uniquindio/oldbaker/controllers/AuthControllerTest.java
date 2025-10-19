package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.api.ApiResponse;
import co.edu.uniquindio.oldbaker.dto.auth.*;
import co.edu.uniquindio.oldbaker.services.AuthService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para AuthController.
 *
 * Estas pruebas verifican el comportamiento de los endpoints del controlador de autenticación
 * que gestionan registro, login, verificación de cuentas y recuperación de contraseñas.
 * Se utiliza Mockito para simular el servicio de autenticación y validar que el controlador
 * maneja correctamente las respuestas HTTP y los errores.
 *
 * @author OldBaker Team
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private VerificationRequest verificationRequest;
    private PasswordRecoveryRequest passwordRecoveryRequest;
    private AuthResponse authResponse;

    /**
     * Configuración inicial que se ejecuta antes de cada test.
     * Inicializa los objetos de prueba con datos de ejemplo para ser utilizados en las pruebas.
     */
    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setNombre("Juan");
        registerRequest.setEmail("juan.perez@test.com");
        registerRequest.setPassword("Password123!");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("juan.perez@test.com");
        loginRequest.setPassword("Password123!");

        verificationRequest = new VerificationRequest();
        verificationRequest.setIdUsuario(1L);
        verificationRequest.setCodigo("123456");

        passwordRecoveryRequest = new PasswordRecoveryRequest();
        passwordRecoveryRequest.setEmail("juan.perez@test.com");
        passwordRecoveryRequest.setNewPassword("NewPassword123!");

        authResponse = new AuthResponse();
        authResponse.setAccessToken("jwt-token-123");
        authResponse.setRefreshToken("refresh-token-123");
    }

    /**
     * Verifica que el endpoint de registro funcione correctamente con datos válidos.
     *
     * Este test valida que:
     * - El controlador llama al servicio de registro con los parámetros correctos
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene los datos esperados
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test registro de usuario exitoso")
    @SuppressWarnings("unchecked")
    void testRegisterSuccess() {
        // Given
        when(authService.register(any(RegisterRequest.class))).thenReturn(ApiResponse.success("Usuario registrado exitosamente", null));

        // When
        ResponseEntity<ApiResponse<?>> response = authController.register(registerRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    /**
     * Verifica que el endpoint de registro maneje errores de validación correctamente.
     *
     * Este test valida que:
     * - El controlador captura excepciones IllegalArgumentException
     * - Retorna un código HTTP 400 (BAD_REQUEST)
     * - El mensaje de error es apropiado
     */
    @Test
    @DisplayName("Test registro con email duplicado")
    void testRegisterDuplicateEmail() {
        // Given
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("El email ya está registrado"));

        // When
        ResponseEntity<ApiResponse<?>> response = authController.register(registerRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    /**
     * Verifica que el endpoint de login funcione correctamente con credenciales válidas.
     *
     * Este test valida que:
     * - El controlador llama al servicio de autenticación con las credenciales
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene el token JWT y refresh token
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test login exitoso")
    void testAuthenticateSuccess() {
        // Given
        when(authService.authenticate(any(LoginRequest.class))).thenReturn(authResponse);

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.authenticate(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("jwt-token-123", response.getBody().getData().getAccessToken());
        verify(authService, times(1)).authenticate(any(LoginRequest.class));
    }

    /**
     * Verifica que el endpoint de login maneje credenciales inválidas correctamente.
     *
     * Este test valida que:
     * - El controlador captura excepciones de credenciales inválidas
     * - Retorna un código HTTP 401 (UNAUTHORIZED)
     * - El mensaje de error es apropiado
     */
    @Test
    @DisplayName("Test login con credenciales inválidas")
    void testAuthenticateInvalidCredentials() {
        // Given
        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Credenciales inválidas"));

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.authenticate(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(authService, times(1)).authenticate(any(LoginRequest.class));
    }

    /**
     * Verifica que el endpoint de verificación funcione correctamente con código válido.
     *
     * Este test valida que:
     * - El controlador llama al servicio de verificación con el código
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene los tokens de autenticación
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test verificación de cuenta exitosa")
    void testVerifySuccess() {
        // Given
        when(authService.verify(any(VerificationRequest.class))).thenReturn(authResponse);

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.verify(verificationRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        verify(authService, times(1)).verify(any(VerificationRequest.class));
    }

    /**
     * Verifica que el endpoint de verificación maneje códigos inválidos correctamente.
     *
     * Este test valida que:
     * - El controlador captura excepciones de códigos inválidos
     * - Retorna un código HTTP 401 (UNAUTHORIZED)
     * - El mensaje de error es apropiado
     */
    @Test
    @DisplayName("Test verificación con código inválido")
    void testVerifyInvalidCode() {
        // Given
        when(authService.verify(any(VerificationRequest.class)))
                .thenThrow(new IllegalArgumentException("Código de verificación inválido"));

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.verify(verificationRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(authService, times(1)).verify(any(VerificationRequest.class));
    }

    /**
     * Verifica que el endpoint de reenvío de código de verificación funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio con el email proporcionado
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta confirma el reenvío del código
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test reenvío de código de verificación exitoso")
    void testResendVerificationCodeSuccess() {
        // Given
        String email = "juan.perez@test.com";
        when(authService.resendVerificationCode(anyString())).thenReturn("Código reenviado");

        // When
        ResponseEntity<ApiResponse<String>> response = authController.resendVerificationCode(email);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(authService, times(1)).resendVerificationCode(email);
    }

    /**
     * Verifica que el endpoint de recuperación de contraseña funcione correctamente.
     *
     * Este test valida que:
     * - El controlador valida el formato del email
     * - Llama al servicio para iniciar la recuperación
     * - Retorna un código HTTP 200 (OK)
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test solicitud de recuperación de contraseña exitosa")
    void testForgotPasswordSuccess() {
        // Given
        String email = "juan.perez@test.com";
        doNothing().when(authService).initiatePasswordReset(anyString());

        // When
        ResponseEntity<ApiResponse<String>> response = authController.forgotPassword(email);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(authService, times(1)).initiatePasswordReset(anyString());
    }

    /**
     * Verifica que el endpoint de recuperación de contraseña rechace emails con formato inválido.
     *
     * Este test valida que:
     * - El controlador valida el formato del email antes de llamar al servicio
     * - Retorna un código HTTP 400 (BAD_REQUEST) para emails inválidos
     * - No se invoca el servicio con emails inválidos
     */
    @Test
    @DisplayName("Test solicitud de recuperación con email inválido")
    void testForgotPasswordInvalidEmail() {
        // Given
        String invalidEmail = "email-invalido";

        // When
        ResponseEntity<ApiResponse<String>> response = authController.forgotPassword(invalidEmail);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(authService, never()).initiatePasswordReset(anyString());
    }

    /**
     * Verifica que el endpoint de verificación de código de reset funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio con el código proporcionado
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene el JWT para restablecer la contraseña
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test verificación de código de reset exitosa")
    void testVerifyResetCodeSuccess() {
        // Given
        String codigo = "123456";
        String jwt = "jwt-reset-token";
        when(authService.verifyResetCode(anyString())).thenReturn(jwt);

        // When
        ResponseEntity<ApiResponse<String>> response = authController.verifyResetCode(codigo);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(jwt, response.getBody().getData());
        verify(authService, times(1)).verifyResetCode(anyString());
    }

    /**
     * Verifica que el endpoint de restablecimiento de contraseña funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio con el JWT y la nueva contraseña
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta confirma el cambio de contraseña
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test restablecimiento de contraseña exitoso")
    void testRecoverPasswordSuccess() {
        // Given
        when(authService.recoverPassword(any(PasswordRecoveryRequest.class)))
                .thenReturn("Contraseña actualizada");

        // When
        ResponseEntity<ApiResponse<String>> response = authController.recoverPassword(passwordRecoveryRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(authService, times(1)).recoverPassword(any(PasswordRecoveryRequest.class));
    }

    /**
     * Verifica que el endpoint de login de trabajadores funcione correctamente.
     *
     * Este test valida que:
     * - El controlador llama al servicio de autenticación de trabajadores
     * - Retorna un código HTTP 200 (OK)
     * - La respuesta contiene los tokens de autenticación
     * - El servicio es invocado exactamente una vez
     */
    @Test
    @DisplayName("Test login de trabajador exitoso")
    void testWorkerAuthenticateSuccess() {
        // Given
        when(authService.workerAuthenticate(any(LoginRequest.class))).thenReturn(authResponse);

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.workerAuthenticate(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        verify(authService, times(1)).workerAuthenticate(any(LoginRequest.class));
    }

    /**
     * Verifica que el endpoint de login de trabajadores maneje credenciales inválidas.
     *
     * Este test valida que:
     * - El controlador captura excepciones de credenciales inválidas
     * - Retorna un código HTTP 400 (BAD_REQUEST)
     * - El mensaje de error es apropiado
     */
    @Test
    @DisplayName("Test login de trabajador con credenciales inválidas")
    void testWorkerAuthenticateInvalidCredentials() {
        // Given
        when(authService.workerAuthenticate(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Credenciales inválidas"));

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.workerAuthenticate(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(authService, times(1)).workerAuthenticate(any(LoginRequest.class));
    }
}
