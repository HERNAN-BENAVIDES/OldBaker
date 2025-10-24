package co.edu.uniquindio.oldbaker.config;

import co.edu.uniquindio.oldbaker.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Manejador de éxito de autenticación OAuth2 que redirige al cliente con los datos de autenticación.
 */
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);
    private final AuthService authService;
    private final String urlRedirect;

    // Constructor que inyecta el servicio de autenticación y la URL de redirección.
    public OAuth2SuccessHandler(AuthService authService, String urlRedirect) {
        this.authService = authService;
        // Fallback seguro para entornos de desarrollo en caso de no tener la variable configurada.
        String fallback = "http://localhost:4200/oauth-callback?data=";
        this.urlRedirect = (urlRedirect == null || urlRedirect.isBlank()) ? fallback : urlRedirect;
        log.info("OAuth2SuccessHandler initialized. FRONTEND_REDIRECT_URL resolved to: {}", this.urlRedirect);
    }

    /**
     * Maneja el éxito de la autenticación OAuth2.
     * Extrae el usuario OAuth2, procesa la información y redirige al cliente con los datos.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Verificar si el usuario OAuth2 es nulo
        if (oAuth2User == null) {
            log.error("OAuth2User is null");
            response.sendRedirect(this.urlRedirect + URLEncoder.encode("{\"error\":\"Authentication failed\"}", StandardCharsets.UTF_8));
            return;
        }

        try {
            // Imprimir los atributos del usuario para depuración
            log.info("OAuth2User attributes: {}", oAuth2User.getAttributes());

            var authResponse = authService.processOAuth2User(oAuth2User);
            var data = Objects.requireNonNull(authResponse.getData(), "Auth response data is null");

            // Serializar únicamente el payload necesario para el frontend (tokens + datos del usuario)
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("accessToken", data.getAccessToken());
            payload.put("refreshToken", data.getRefreshToken());
            var user = new java.util.LinkedHashMap<String, Object>();
            user.put("id", data.getUsuario().getId());
            user.put("nombre", data.getUsuario().getNombre());
            user.put("email", data.getUsuario().getEmail());
            user.put("rol", data.getUsuario().getRol());
            payload.put("usuario", user);

            String serialized = new ObjectMapper().writeValueAsString(payload);

            // FRONTEND_REDIRECT_URL debe terminar típicamente en .../oauth-callback?data=
            String target = this.urlRedirect + URLEncoder.encode(serialized, StandardCharsets.UTF_8);

            log.info("Redirecting to: {}", target);
            response.sendRedirect(target);

        } catch (Exception e) {
            log.error("Error processing OAuth2 user", e);
            String err = URLEncoder.encode("{\"error\":\"" + e.getMessage() + "\"}", StandardCharsets.UTF_8);
            response.sendRedirect(this.urlRedirect + err);
        }
    }
}
