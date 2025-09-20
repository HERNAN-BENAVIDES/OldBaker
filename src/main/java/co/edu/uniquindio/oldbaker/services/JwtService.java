package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


/**
 * Servicio para la gestión de JSON Web Tokens (JWT).
 * Proporciona métodos para generar, validar y extraer información de tokens JWT.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * Extrae el nombre de usuario (subject) del token JWT.
     *
     * @param token El token JWT del cual se extraerá el nombre de usuario.
     * @return El nombre de usuario extraído del token.
     * @throws SignatureException Si el token no es válido o ha sido manipulado.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae una reclamación específica del token JWT utilizando una función de resolución.
     *
     * @param token          El token JWT del cual se extraerá la reclamación.
     * @param claimsResolver Una función que define cómo extraer la reclamación de los Claims.
     * @param <T>            El tipo de la reclamación a extraer.
     * @return La reclamación extraída del token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Genera un token JWT para el usuario proporcionado.
     *
     * @param userDetails Los detalles del usuario para el cual se generará el token.
     * @return El token JWT generado.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Genera un token JWT con reclamaciones adicionales para el usuario proporcionado.
     *
     * @param extraClaims Reclamaciones adicionales a incluir en el token.
     * @param userDetails Los detalles del usuario para el cual se generará el token.
     * @return El token JWT generado.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Map<String, Object> claims = enrichClaimsWithUserId(extraClaims, userDetails);
        return buildToken(claims, userDetails, jwtExpiration);
    }

    /**
     * Genera un token de actualización (refresh token) para el usuario proporcionado.
     * El token de actualización tiene una duración más larga que el token de acceso estándar.
     *
     * @param userDetails Los detalles del usuario para el cual se generará el token de actualización.
     * @return El token de actualización JWT generado.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = enrichClaimsWithUserId(new HashMap<>(), userDetails);
        return buildToken(claims, userDetails, jwtExpiration * 7); // 7 días
    }

    /**
     * Construye el token JWT con las reclamaciones, el sujeto y la expiración especificados.
     *
     * @param extraClaims Reclamaciones adicionales a incluir en el token.
     * @param userDetails Los detalles del usuario para el cual se generará el token.
     * @param expiration  La duración en milisegundos antes de que el token expire.
     * @return El token JWT generado.
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Enriquecer las reclamaciones con información adicional del usuario.
     *
     * @param extraClaims Reclamaciones adicionales a incluir en el token.
     * @param userDetails Los detalles del usuario para el cual se generará el token.
     * @return Un mapa con las reclamaciones actualizadas.
     */
    private Map<String, Object> enrichClaimsWithUserId(Map<String, Object> extraClaims, UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        if (userDetails instanceof Usuario usuario && usuario.getId() != null) {
            claims.put("userId", usuario.getId());
        }
        return claims;
    }

    /**
     * Valida si el token JWT es válido para el usuario proporcionado.
     *
     * @param token       El token JWT a validar.
     * @param userDetails Los detalles del usuario contra los cuales se validará el token.
     * @return true si el token es válido y no ha expirado; false en caso contrario.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        if (!username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
            return false;
        }

        if (userDetails instanceof Usuario usuario && usuario.getId() != null) {
            Long tokenUserId = extractUserId(token);
            return tokenUserId != null && tokenUserId.equals(usuario.getId());
        }

        return true;
    }

    /**
     * Extrae el identificador del usuario desde el token JWT.
     *
     * @param token El token JWT del cual se extraerá el identificador.
     * @return El identificador del usuario si está presente; de lo contrario, null.
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object userId = claims.get("userId");
            if (userId instanceof Number number) {
                return number.longValue();
            }
            if (userId instanceof String string) {
                try {
                    return Long.parseLong(string);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        });
    }

    /**
     * Verifica si el token JWT ha expirado.
     *
     * @param token El token JWT a verificar.
     * @return true si el token ha expirado; false en caso contrario.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrae la fecha de expiración del token JWT.
     *
     * @param token El token JWT del cual se extraerá la fecha de expiración.
     * @return La fecha de expiración del token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae todas las reclamaciones del token JWT.
     *
     * @param token El token JWT del cual se extraerán las reclamaciones.
     * @return Las reclamaciones extraídas del token.
     * @throws SignatureException Si el token no es válido o ha sido manipulado.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Obtiene la clave secreta utilizada para firmar y verificar los tokens JWT.
     *
     * @return La clave secreta como un objeto SecretKey.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}