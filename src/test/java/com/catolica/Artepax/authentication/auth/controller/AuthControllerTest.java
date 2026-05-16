package com.catolica.Artepax.authentication.auth.controller;

import com.catolica.Artepax.authentication.auth.dto.AuthResponse;
import com.catolica.Artepax.authentication.auth.dto.LoginRequest;
import com.catolica.Artepax.authentication.auth.service.GoogleAuthService;
import com.catolica.Artepax.authentication.auth.service.LocalAuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthController} — verifies delegation to auth services.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController unit tests")
class AuthControllerTest {

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private LocalAuthService localAuthService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(googleAuthService, localAuthService);
    }

    @Test
    @DisplayName("POST /auth/login delegates to LocalAuthService and returns 200 with token")
    void loginLocal_delegatesToService() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@artepax.com");
        request.setPassword("secret123");

        AuthResponse mockResponse = AuthResponse.builder().token("jwt.test.token").build();
        when(localAuthService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        ResponseEntity<AuthResponse> response = authController.loginLocal(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt.test.token", response.getBody().getToken());
    }
}
