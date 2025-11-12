package co.edu.uniquindio.oldbaker.config;

import co.edu.uniquindio.oldbaker.services.AuthService;
import co.edu.uniquindio.oldbaker.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static co.edu.uniquindio.oldbaker.config.OAuth2SuccessHandler.log;


/**
 * Configuración de seguridad para la aplicación utilizando Spring Security.
 * Define las reglas de seguridad, los filtros de autenticación y los proveedores de autenticación.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UsuarioService usuarioService;
    private final Environment env;


    /**
     * Configura la cadena de filtros de seguridad.
     *
     * @param http        El objeto HttpSecurity para configurar la seguridad HTTP.
     * @param authService El servicio de autenticación para manejar el inicio de sesión OAuth2.
     * @return La cadena de filtros de seguridad configurada.
     * @throws Exception Si ocurre un error durante la configuración.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthService authService) throws Exception {
        String frontendRedirect = env.getProperty("FRONTEND_REDIRECT_URL");
        http
                // Deshabilitar CSRF para APIs REST
                .csrf(AbstractHttpConfigurer::disable)
                // Configurar CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Configurar las reglas de autorización
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Endpoints públicos
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/verify",
                                "/api/auth/google/**",
                                "/api/auth/forgot/**",
                                "/api/auth/reset/**",
                                "/api/auth/worker/login",
                                "/login/oauth2/**",
                                "/api/mercadopago/**",
                                "/api/payments/**",
                                "/oauth2/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/productos/**"
                        ).permitAll()
                        // Endpoints para administradores
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                        // Endpoints para usuarios autenticados
                        .requestMatchers("/api/user/**").hasRole("CLIENTE")
                        .requestMatchers("/api/orders/**").hasRole("CLIENTE")
                        .requestMatchers("/api/cart/**").hasRole("CLIENTE")
                        // Endpoints para usuarios con rol AUXILIAR
                        .requestMatchers("/api/aux/**").hasRole("AUXILIAR")
                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )
                // Configurar la gestión de sesiones como sin estado
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Configurar el inicio de sesión OAuth2
                .oauth2Login(oauth2 -> oauth2
                        // Configurar el manejador de éxito y fracaso en la autenticación OAuth2
                        .successHandler(new OAuth2SuccessHandler(authService, frontendRedirect))
                        .failureHandler((request, response, exception) -> {
                            System.out.println("OAuth2 login FAILURE: " + exception.getMessage());
                            response.sendRedirect("/api/auth/google/failure");
                        })
                )
                // Configurar el proveedor de autenticación
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    /**
     * Configura la fuente de configuración CORS.
     *
     * @return La fuente de configuración CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:4200",
                        "https://www.oldbaker.shop"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configura el servicio de detalles del usuario.
     *
     * @return El servicio de detalles del usuario.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return usuarioService::findByEmailAndActivo;
    }

    /**
     * Configura el proveedor de autenticación.
     *
     * @return El proveedor de autenticación.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Configura el gestor de autenticación.
     *
     * @param config La configuración de autenticación.
     * @return El gestor de autenticación.
     * @throws Exception Si ocurre un error durante la configuración.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configura el codificador de contraseñas.
     *
     * @return El codificador de contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    CommandLineRunner logProps(Environment env) {
        return args -> {
            log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
            log.info("spring.datasource.url = {}", env.getProperty("spring.datasource.url"));
            log.info("DB_URL (raw)          = {}", env.getProperty("DB_URL"));
        };
    }


}