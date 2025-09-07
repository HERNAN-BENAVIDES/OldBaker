package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.*;
import co.edu.uniquindio.oldbaker.model.Usuario;
import co.edu.uniquindio.oldbaker.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"https://old-baker-front.vercel.app", "https://localhost:4200", "http://localhost:4200"})
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<?>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        try {
            var response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error durante el registro: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno del servidor"));
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(
            @Valid @RequestBody LoginRequest request
    ) {
        try {
            AuthResponse response = authService.authenticate(request);
            return ResponseEntity.ok(
                    ApiResponse.success("Inicio de sesión exitoso", response)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Credenciales inválidas"));
        } catch (Exception e) {
            log.error("Error durante la autenticación: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno del servidor"));
        }
    }

    @PostMapping("/user/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody LogoutRequest request) {

        try {
            String response = authService.logout(request);
            return ResponseEntity.ok(ApiResponse.success("Logout exitoso", response));
        }catch (IllegalArgumentException e) {
            return  ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/auth/verify")
    public  ResponseEntity<ApiResponse<AuthResponse>> verify(@Valid @RequestBody VerificationRequest request){
        try {
            AuthResponse response = authService.verify(request);
            return ResponseEntity.ok(
                    ApiResponse.success("Verificación exitosa", response)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error durante la verificación: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno del servidor"));
        }

    }

    @GetMapping("/verify/resend")
    public ResponseEntity<ApiResponse<String>> resendVerificationCode(@RequestParam String email) {
        try {
            String response = authService.resendVerificationCode(email);
            return ResponseEntity.ok(ApiResponse.success("Código reenviado exitosamente", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error durante el reenvío del código de verificación: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno del servidor"));
        }
    }

    @PostMapping("/auth/forgot")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody String email) {
        try {
            String emailTrim = email == null ? "" : email.trim();
            if (emailTrim.isEmpty() || !emailTrim.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Formato de email inválido"));
            }

            // Delegar la generación de token, hashing, guardado en BD y envío de email al servicio.
            // El servicio debe implementar límites y auditoría.
            authService.initiatePasswordReset(emailTrim);

            // Respuesta genérica para no revelar existencia del email
            return ResponseEntity.ok(ApiResponse.success("Si el email existe, hemos enviado instrucciones.", ""));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error durante solicitud de recuperación de contraseña: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno del servidor"));
        }
    }

    @PostMapping("/auth/reset/verify")
    public ResponseEntity<ApiResponse<String>> verifyResetCode(@Valid @RequestBody String codigo) {
        try {
            String codeTrim = codigo == null ? "" : codigo.trim();
            if (codeTrim.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Código inválido"));
            }

            // Delegar al servicio: valida el código y retorna un JWT asociado al usuario
            String jwt = authService.verifyResetCode(codeTrim);
            return ResponseEntity.ok(ApiResponse.success("Código verificado. JWT generado", jwt));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error al verificar código de recuperación: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno del servidor"));
        }
    }

    @PostMapping("/auth/reset")
    public ResponseEntity<ApiResponse<String>> recoverPassword(@Valid @RequestBody PasswordRecoveryRequest request) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Solicitud inválida"));
            }

            String response = authService.recoverPassword(request);
            return ResponseEntity.ok(ApiResponse.success("Contraseña actualizada exitosamente", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error al recuperar contraseña: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno del servidor"));
        }
    }


    /**
     *
     * Google
     *
     */


//    @GetMapping("/google/success")
//    public void googleAuthSuccess(
//            @AuthenticationPrincipal OAuth2User oAuth2User,
//            HttpServletResponse response
//    ) throws IOException {
//        System.out.println("Entró a /google/success");
//        // Procesa el usuario OAuth2 y genera tokens
//        AuthResponse authResponse = authService.processOAuth2User(oAuth2User);
//
//        // Redirige al frontend con los tokens como parámetros
//        String redirectUrl = String.format(
//                "https://localhost:3000/auth/google/callback?accessToken=%s&refreshToken=%s",
//                authResponse.getAccessToken(),
//                authResponse.getRefreshToken()
//        );
//        response.sendRedirect(redirectUrl);
//    }

    @GetMapping("/google/failure")
    public void googleAuthFailure(HttpServletResponse response) throws IOException {
        System.out.println("Entró a /google/failure");
        response.sendRedirect("https://localhost:3000/auth/error");
    }

}