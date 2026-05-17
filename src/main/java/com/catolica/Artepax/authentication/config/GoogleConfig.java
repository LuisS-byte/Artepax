package com.catolica.Artepax.authentication.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * Configuration and utility bean for Google OAuth ID token verification.
 * <p>
 * Reads the {@code google.client.id} property and builds a
 * {@link GoogleIdTokenVerifier} to validate incoming Google ID tokens.
 * Startup validation ensures the required property is present.
 */
@Configuration
public class GoogleConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleConfig.class);

    @Value("${google.client.id}")
    private String googleClientId;

    /**
     * Validates that {@code google.client.id} is configured at application startup.
     *
     * @throws IllegalStateException if the property is missing or blank
     */
    @PostConstruct
    public void validateConfig() {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException(
                    "La propiedad 'google.client.id' es obligatoria y no puede estar vacía");
        }
    }

    /**
     * Verifies a Google ID token and returns its payload.
     * <p>
     * Uses the configured {@code google.client.id} as the expected audience.
     * If verification fails or an exception occurs, {@code null} is returned.
     *
     * @param idTokenString the raw ID token string from Google's OAuth response
     * @return the token payload if verification succeeds, {@code null} otherwise
     */
    public GoogleIdToken.Payload verifyToken(String idTokenString) {

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken != null) {
                return idToken.getPayload();
            }

        } catch (Exception e) {
            log.warn("Error verificando token de Google", e);
        }

        return null;
    }
}
