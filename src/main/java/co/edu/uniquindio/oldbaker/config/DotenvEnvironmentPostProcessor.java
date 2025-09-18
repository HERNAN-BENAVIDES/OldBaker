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
        String profile = env.getProperty("spring.profiles.active", "production");
        String filename = "production".equalsIgnoreCase(profile) ? ".env.production" : ".env.development";

        Dotenv dotenv = Dotenv.configure()
                .filename(filename)
                .ignoreIfMissing()
                .load();

        Map<String, Object> map = new HashMap<>();
        dotenv.entries().forEach(e -> map.put(e.getKey(), e.getValue()));

        env.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", map));
    }
}
