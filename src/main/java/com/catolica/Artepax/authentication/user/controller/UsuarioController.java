package com.catolica.Artepax.authentication.user.controller;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> obtenerUsuarioActual(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Usuario usuario = (Usuario) auth.getPrincipal();

        Map<String, Object> body = Map.of(
                "id", usuario.getIdUsuario(),
                "nombre", usuario.getNombre(),
                "correo", usuario.getCorreo(),
                "telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "",
                "rol", usuario.getRol() != null ? usuario.getRol().getNombre() : ""
        );

        return ResponseEntity.ok(body);
    }
}
