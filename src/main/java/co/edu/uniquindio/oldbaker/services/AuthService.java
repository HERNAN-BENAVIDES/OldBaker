package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.LogoutRequest;
import co.edu.uniquindio.oldbaker.model.BlackToken;
import co.edu.uniquindio.oldbaker.model.Usuario;
import co.edu.uniquindio.oldbaker.repositories.BlackTokenRepository;
import co.edu.uniquindio.oldbaker.repositories.UsuarioRepository;
import co.edu.uniquindio.oldbaker.dto.AuthResponse;
import co.edu.uniquindio.oldbaker.dto.LoginRequest;
import co.edu.uniquindio.oldbaker.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final BlackTokenRepository blackTokenRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registrando nuevo usuario con email: {}", request.getEmail());

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        var usuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(Usuario.Rol.CLIENTE)
                .tipoAutenticacion(Usuario.TipoAutenticacion.EMAIL)
                .activo(true)
                .verificado(false)
                .build();

        usuarioRepository.save(usuario);

        var jwtToken = jwtService.generateToken(usuario);
        var refreshToken = jwtService.generateRefreshToken(usuario);

        log.info("Usuario registrado exitosamente: {}", usuario.getEmail());

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .usuario(mapToUserResponse(usuario))
                .build();
    }

    public AuthResponse authenticate(LoginRequest request) {
        log.info("Autenticando usuario: {}", request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var usuario = usuarioRepository.findByEmailAndActivoTrue(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        var jwtToken = jwtService.generateToken(usuario);
        var refreshToken = jwtService.generateRefreshToken(usuario);

        log.info("Usuario autenticado exitosamente: {}", usuario.getEmail());

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .usuario(mapToUserResponse(usuario))
                .build();
    }

    @Transactional
    public AuthResponse processOAuth2User(OAuth2User oAuth2User) {
        // Toda la lógica de autenticación OAuth2 está en OAuth2SuccessHandler.
        String email = oAuth2User.getAttribute("email");
        String nombre = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");

        log.info("Procesando usuario OAuth2: {}", email);

        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(email);

        Usuario usuario;
        if (usuarioExistente.isPresent()) {
            usuario = usuarioExistente.get();
        } else {
            // Crear nuevo usuario
            usuario = Usuario.builder()
                    .email(email)
                    .nombre(nombre)
                    .verificado(false)
                    .rol(Usuario.Rol.CLIENTE)
                    .tipoAutenticacion(Usuario.TipoAutenticacion.GOOGLE)
                    .activo(true)
                    .build();
            usuarioRepository.save(usuario);
        }

        var jwtToken = jwtService.generateToken(usuario);
        var refreshToken = jwtService.generateRefreshToken(usuario);

        log.info("Usuario OAuth2 procesado exitosamente: {}", usuario.getEmail());

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .usuario(mapToUserResponse(usuario))
                .build();
    }

    private AuthResponse.UserResponse mapToUserResponse(Usuario usuario) {
        return AuthResponse.UserResponse.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .rol(usuario.getRol().name())
                .build();
    }

    public String logout(LogoutRequest request) {
        Optional<Usuario> user = usuarioRepository.findByEmail(request.getEmail());
        if (user.isEmpty()) {
            log.warn("Intento de cierre de sesión para usuario no encontrado: {}", request.getEmail());
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        log.info("Cierre de sesión para el usuario: {}", request.getEmail());

        var token = request.getToken();
        var tokenExpirationDate = jwtService.extractExpiration(token);

        var blackToken = BlackToken.builder()
                .token(token)
                .expiration(LocalDateTime.ofInstant(tokenExpirationDate.toInstant(), ZoneId.systemDefault()))
                .build();

        blackTokenRepository.save(blackToken);

        usuarioRepository.updateUserLastSesionDate(user.get(), LocalDateTime.now(ZoneId.systemDefault()));

        return "Cierre de sesión exitoso";
    }
}