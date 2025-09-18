package co.edu.uniquindio.oldbaker.config;

import co.edu.uniquindio.oldbaker.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Manejador de éxito de autenticación OAuth2 que redirige al cliente con los datos de autenticación.
 */
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);
    private final AuthService authService;

    @Value("${FRONTEND_REDIRECT_URL}")
    private String urlRedirect;

    // Constructor que inyecta el servicio de autenticación.
    public OAuth2SuccessHandler(AuthService authService) {
        this.authService = authService;
        System.out.println("OAuth2SuccessHandler initialized");
    }

    /**
     * Maneja el éxito de la autenticación OAuth2.
     * Extrae el usuario OAuth2, procesa la información y redirige al cliente con los datos.
     *
     * @param request        La solicitud HTTP.
     * @param response       La respuesta HTTP.
     * @param authentication La autenticación exitosa.
     * @throws IOException      Si ocurre un error de E/S.
     * @throws ServletException Si ocurre un error del servlet.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Verificar si el usuario OAuth2 es nulo
        if (oAuth2User == null) {
            System.out.println("OAuth2User is null");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
            return;
        }

        // Imprimir los atributos del usuario para depuración
        log.info("OAuth2User attributes: {}", oAuth2User.getAttributes());
        var authResponse = authService.processOAuth2User(oAuth2User);

        String serializedData = new ObjectMapper().writeValueAsString(authResponse);
        String redirectUrl = urlRedirect
                + URLEncoder.encode(serializedData, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);

    }
}
