package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.*;
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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"https://localhost:3000", "https://localhost:4200"})
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(
                    ApiResponse.success("Usuario registrado exitosamente", response)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error durante el registro: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno del servidor"));
        }
    }

    @PostMapping("/login")
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

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody LogoutRequest request) {

        try {
            String response = authService.logout(request);
            return ResponseEntity.ok(ApiResponse.success("Logout exitoso", response));
        }catch (IllegalArgumentException e) {
            return  ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     *
     * Google
     *
     */

    @GetMapping("/google/success")
    public void googleAuthSuccess(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            HttpServletResponse response
    ) throws IOException {

    }

    @GetMapping("/google/failure")
    public void googleAuthFailure(HttpServletResponse response) throws IOException {
        System.out.println("Entró a /google/failure");
        response.sendRedirect("https://localhost:3000/auth/error");
    }

//    @PostMapping("/refresh-token")
//    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
//            @Valid @RequestBody TokenRefreshRequest request
//    ) {
//        try {
//            AuthResponse response = authService.refreshToken(request);
//            return ResponseEntity.ok(
//                    ApiResponse.success("Token renovado exitosamente", response)
//            );
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error(e.getMessage()));
//        } catch (Exception e) {
//            log.error("Error durante la renovación del token: ", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.error("Error interno del servidor"));
//        }
//    }
}