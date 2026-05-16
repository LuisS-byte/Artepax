package com.catolica.Artepax.authentication.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Standalone configuration for the {@link PasswordEncoder} bean.
 * <p>
 * Extracted from {@link SecurityConfig} to break a circular dependency:
 * {@code JwtAuthenticationFilter → UsuarioService → SecurityConfig → JwtAuthenticationFilter}.
 * {@code PasswordConfig} has no dependencies, so there is no cycle.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
