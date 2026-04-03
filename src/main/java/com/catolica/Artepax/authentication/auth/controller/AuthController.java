package com.catolica.Artepax.authentication.auth.controller;

import com.catolica.Artepax.authentication.auth.dto.GoogleAuthRequest;
import com.catolica.Artepax.authentication.auth.dto.AuthResponse;
import com.catolica.Artepax.authentication.auth.service.GoogleAuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleAuthService googleAuthService;

    // 🔐 LOGIN CON GOOGLE
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@RequestBody GoogleAuthRequest request) {

        AuthResponse response = googleAuthService.authenticateWithGoogle(request);

        return ResponseEntity.ok(response);
    }
}