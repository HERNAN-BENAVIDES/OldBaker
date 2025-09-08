package co.edu.uniquindio.oldbaker.config;

import co.edu.uniquindio.oldbaker.services.JwtService;
import co.edu.uniquindio.oldbaker.services.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


/**
 * Filtro de autenticación JWT que intercepta las solicitudes HTTP para validar el token JWT.
 * Si el token es válido, establece la autenticación en el contexto de seguridad de Spring.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioService usuarioService;


    /**
     * Filtra las solicitudes HTTP para validar el token JWT en el encabezado de autorización.
     * Si el token es válido, establece la autenticación en el contexto de seguridad.
     *
     * @param request     La solicitud HTTP entrante.
     * @param response    La respuesta HTTP.
     * @param filterChain La cadena de filtros para continuar con la solicitud.
     * @throws ServletException Si ocurre un error durante el filtrado.
     * @throws IOException      Si ocurre un error de E/S durante el filtrado.
     */
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Obtener el encabezado de autorización
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String userEmail = jwtService.extractUsername(jwt);

        // Validar el token y establecer la autenticación si es válido
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var user = usuarioService.findByEmailAndActivoTrue(userEmail);

            // Verificar si el token es válido
            if (user != null && jwtService.isTokenValid(jwt, user)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
