package co.edu.uniquindio.oldbaker.config;

import java.util.HashMap;
import java.util.Map;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        String active = env.getProperty("spring.profiles.active");
        String effectiveProfile = (active == null || active.isBlank()) ? "development" : active;
        String filename = "production".equalsIgnoreCase(effectiveProfile) ? ".env.production" : ".env.development";

        Dotenv dotenv = Dotenv.configure()
                .filename(filename)
                .ignoreIfMissing()
                .load();

        Map<String, Object> map = new HashMap<>();
        dotenv.entries().forEach(e -> map.put(e.getKey(), e.getValue()));

        // Si no hay perfil activo explícito, fuerza 'development' para que Spring cargue application-development.properties
        if (active == null || active.isBlank()) {
            map.put("spring.profiles.active", "development");
        }

        env.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", map));
    }
}
