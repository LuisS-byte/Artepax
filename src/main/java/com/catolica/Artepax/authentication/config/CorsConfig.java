package com.catolica.Artepax.authentication.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for the application.
 * <p>
 * Reads allowed origins from the {@code app.cors.allowed-origins} property
 * (comma-separated, defaults to {@code http://localhost:5500,http://127.0.0.1:5500})
 * and configures a {@link CorsConfigurationSource} that permits common HTTP methods,
 * all headers, and credentials.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5500,http://127.0.0.1:5500}")
    private String allowedOriginsRaw;

    /**
     * Creates the CORS configuration source used by Spring Security's filter chain.
     * <p>
     * Parses the comma-separated origins property, configures GET/POST/PATCH/PUT/DELETE/OPTIONS
     * as allowed methods, allows all headers, and enables credentials.
     *
     * @return a {@link CorsConfigurationSource} applied to all paths ({@code /**})
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
