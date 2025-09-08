package co.edu.uniquindio.oldbaker.exceptions;

import co.edu.uniquindio.oldbaker.dto.ApiResponse;
import co.edu.uniquindio.oldbaker.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;


/**
 * Manejador global de excepciones para la aplicación.
 * Captura y maneja diversas excepciones lanzadas durante la ejecución de la aplicación,
 * proporcionando respuestas HTTP adecuadas y mensajes de error detallados.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Maneja las excepciones de validación de argumentos de método.
     * Recopila los errores de validación y devuelve una respuesta con detalles de los errores.
     *
     * @param ex      La excepción lanzada.
     * @param request La solicitud HTTP que causó la excepción.
     * @return Una respuesta HTTP con detalles de los errores de validación.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.of(
                "Errores de validación",
                errors.toString(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Maneja las excepciones de argumento ilegal.
     * Devuelve una respuesta HTTP 400 con el mensaje de error.
     *
     * @param ex      La excepción lanzada.
     * @param request La solicitud HTTP que causó la excepción.
     * @return Una respuesta HTTP con el mensaje de error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Argumento inválido en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Maneja las excepciones de credenciales inválidas.
     * Devuelve una respuesta HTTP 401 con un mensaje de error genérico.
     *
     * @param ex      La excepción lanzada.
     * @param request La solicitud HTTP que causó la excepción.
     * @return Una respuesta HTTP con el mensaje de error.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        log.warn("Credenciales inválidas en {}", request.getRequestURI());
        ErrorResponse errorResponse = ErrorResponse.of(
                "Credenciales inválidas",
                "Email o contraseña incorrectos",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Maneja las excepciones de autenticación.
     * Devuelve una respuesta HTTP 401 con un mensaje de error genérico.
     *
     * @param ex      La excepción lanzada.
     * @param request La solicitud HTTP que causó la excepción.
     * @return Una respuesta HTTP con el mensaje de error.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        log.warn("Error de autenticación en {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                "Error de autenticación",
                "No autorizado para acceder a este recurso",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Maneja las excepciones de acceso denegado.
     * Devuelve una respuesta HTTP 403 con un mensaje de error genérico.
     *
     * @param ex      La excepción lanzada.
     * @param request La solicitud HTTP que causó la excepción.
     * @return Una respuesta HTTP con el mensaje de error.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Acceso denegado en {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                "Acceso denegado",
                "No tiene permisos para acceder a este recurso",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Maneja las excepciones en tiempo de ejecución.
     * Devuelve una respuesta HTTP 500 con un mensaje de error genérico.
     *
     * @param ex      La excepción lanzada.
     * @param request La solicitud HTTP que causó la excepción.
     * @return Una respuesta HTTP con el mensaje de error.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        log.error("Error en tiempo de ejecución en {}: ", request.getRequestURI(), ex);
        ErrorResponse errorResponse = ErrorResponse.of(
                "Error interno",
                "Ha ocurrido un error interno en el servidor",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }


    /**
     * Maneja las excepciones genéricas.
     * Devuelve una respuesta HTTP 500 con un mensaje de error genérico.
     *
     * @param ex      La excepción lanzada.
     * @param request La solicitud HTTP que causó la excepción.
     * @return Una respuesta HTTP con el mensaje de error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Error inesperado en {}: ", request.getRequestURI(), ex);
        ErrorResponse errorResponse = ErrorResponse.of(
                "Error inesperado",
                "Ha ocurrido un error inesperado",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}