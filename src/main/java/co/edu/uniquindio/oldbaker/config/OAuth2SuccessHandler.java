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


public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);
    private final AuthService authService;

    public OAuth2SuccessHandler(AuthService authService) {
        this.authService = authService;
        System.out.println("OAuth2SuccessHandler initialized");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        if (oAuth2User == null) {
            System.out.println("OAuth2User is null");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
            return;
        }

        log.info("OAuth2User attributes: {}", oAuth2User.getAttributes());
        var authResponse = authService.processOAuth2User(oAuth2User);

        String serializedData = new ObjectMapper().writeValueAsString(authResponse);
        String redirectUrl = "http://localhost:4200/oauth-callback?data=" + URLEncoder.encode(serializedData, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);

    }
}
