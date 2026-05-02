package com.catolica.Artepax.authentication.auth.service;

import com.catolica.Artepax.authentication.config.GoogleConfig;
import com.catolica.Artepax.authentication.security.JwtService;
import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.service.UsuarioService;

import com.catolica.Artepax.authentication.auth.dto.GoogleAuthRequest;
import com.catolica.Artepax.authentication.auth.dto.AuthResponse;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleConfig googleConfig;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    // 🔥 ESTE ES EL QUE USA EL CONTROLLER
    public AuthResponse authenticateWithGoogle(GoogleAuthRequest request) {

        String token = authenticate(request.getIdToken());

        return AuthResponse.builder()
                .token(token)
                .build();
    }

    // 🔐 LÓGICA REAL
    public String authenticate(String idToken) {

        GoogleIdToken.Payload payload = googleConfig.verifyToken(idToken);

        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }

        String email = payload.getEmail();

        Usuario usuario = usuarioService.findByCorreo(email);

        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no autorizado");
        }

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario inactivo");
        }

        if (usuario.getRol() == null ||
            !usuario.getRol().getNombre().equals("ADMIN")) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos");
        }

        return jwtService.generateToken(usuario);
    }
}