package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.*;
import co.edu.uniquindio.oldbaker.model.BlackToken;
import co.edu.uniquindio.oldbaker.model.Usuario;
import co.edu.uniquindio.oldbaker.model.VerificationCode;
import co.edu.uniquindio.oldbaker.repositories.BlackTokenRepository;
import co.edu.uniquindio.oldbaker.repositories.UsuarioRepository;
import co.edu.uniquindio.oldbaker.repositories.VerificationCodeRepository;
import jakarta.mail.MessagingException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    private final MailService mailService;
    private final VerificationCodeRepository verificationCodeRepository;

    @Transactional
    public ApiResponse<?> register(RegisterRequest request) {
        log.info("Registrando nuevo usuario con email: {}", request.getEmail());

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        String codigo;

        try {
            codigo = generarCodigoToken();
            enviarCodigo(request.getEmail(), codigo);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
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

        var verificationCode = VerificationCode.builder()
                .userId(usuario.getId())
                .code(codigo)
                .expirationDate(LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(10))
                .build();

        verificationCodeRepository.save(verificationCode);

        log.info("Usuario registrado exitosamente: {}", usuario.getEmail());


        return ApiResponse.success("Usuario registrado exitosamente", mapToUserResponse(usuario));
    }

    private void enviarCodigo(
            @Email(message = "El formato del email no es válido")
            @NotBlank(message = "El email es obligatorio")
            String email,
            String codigo) throws MessagingException {

        String subject = "Verificación de correo electrónico";

        Map<String, Object> variables = new HashMap<>();
        variables.put("codigo", codigo);
        //String body = "Tu código de verificación es: " + codigo;

        try{
            mailService.sendEmail(email, subject, variables);
        }catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        log.info("Enviando codigo a {}: {}", email, codigo);
    }


    private String generarCodigoToken() {
        int codigo = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(codigo);
    }

    public AuthResponse authenticate(LoginRequest request) {
        log.info("Autenticando usuario: {}", request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            log.warn("Intento de autenticación fallido para el usuario: {}", request.getEmail());
            throw new IllegalArgumentException("Credenciales inválidas");
        }


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
    public ApiResponse<AuthResponse> processOAuth2User(OAuth2User oAuth2User) {
        // Toda la lógica de autenticación OAuth2 está en OAuth2SuccessHandler.
        String email = oAuth2User.getAttribute("email");
        String nombre = oAuth2User.getAttribute("name");

        log.info("Procesando usuario OAuth2: {}", email);

        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(email);

        Usuario usuario;
        if (usuarioExistente.isPresent()) {
            usuario = usuarioExistente.get();
            log.info("Usuario OAuth2 procesado exitosamente: {}", usuario.getEmail());

            var jwtToken = jwtService.generateToken(usuario);
            var refreshToken = jwtService.generateRefreshToken(usuario);

            return ApiResponse.success("Inicio de sesión exitoso", AuthResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .usuario(mapToUserResponse(usuario))
                    .build());
        } else {
            // Crear nuevo usuario
            usuario = Usuario.builder()
                    .email(email)
                    .nombre(nombre)
                    .verificado(false)
                    .rol(Usuario.Rol.CLIENTE)
                    .verificado(true)
                    .tipoAutenticacion(Usuario.TipoAutenticacion.GOOGLE)
                    .activo(true)
                    .build();
            usuarioRepository.save(usuario);

            var jwtToken = jwtService.generateToken(usuario);
            var refreshToken = jwtService.generateRefreshToken(usuario);

            return ApiResponse.success("Registro exitoso", AuthResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .usuario(mapToUserResponse(usuario))
                    .build());
        }

    }

    private AuthResponse.UserResponse mapToUserResponse(Usuario usuario) {
        return AuthResponse.UserResponse.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .rol(usuario.getRol().name())
                .build();
    }

    @Transactional
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

    @Transactional
    public AuthResponse verify(VerificationRequest request) {
        var usuario = usuarioRepository.findAllById(Collections.singleton(request.getIdUsuario()))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        var codigo = verificationCodeRepository.findByUserId(request.getIdUsuario())
                .orElseThrow(() -> new IllegalArgumentException("Código de verificación inválido"));

        if (!codigo.getCode().equals(request.getCodigo())) {
            throw new IllegalArgumentException("Código de verificación inválido");
        }

        usuario.setVerificado(true);
        usuarioRepository.save(usuario);
        verificationCodeRepository.deleteByIdUser(request.getIdUsuario());

        var jwtToken = jwtService.generateToken(usuario);
        var refreshToken = jwtService.generateRefreshToken(usuario);

        log.info("Usuario verificado exitosamente: {}", usuario.getEmail());

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .usuario(mapToUserResponse(usuario))
                .build();
    }

    @Transactional
    public String resendVerificationCode(String email) {
        var usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (usuario.getVerificado()) {
            throw new IllegalArgumentException("El usuario ya está verificado");
        }

        String codigo;

        try {
            codigo = generarCodigoToken();
            enviarCodigo(email, codigo);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        var existingCode = verificationCodeRepository.findByUserId(usuario.getId());
        if (existingCode.isPresent()) {
            var verificationCode = existingCode.get();
            verificationCode.setCode(codigo);
            verificationCode.setExpirationDate(LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(10));
            verificationCodeRepository.save(verificationCode);
        } else {
            var verificationCode = VerificationCode.builder()
                    .userId(usuario.getId())
                    .code(codigo)
                    .expirationDate(LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(10))
                    .build();
            verificationCodeRepository.save(verificationCode);
        }

        log.info("Código de verificación reenviado exitosamente a: {}", email);

        return "Código de verificación reenviado exitosamente";
    }
}