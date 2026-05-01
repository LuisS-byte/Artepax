package com.catolica.Artepax.notificacion.controller;
import com.catolica.Artepax.notificacion.dto.NotificacionResponseDTO;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import com.catolica.Artepax.authentication.user.entity.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("hasRole('ADMIN')")
public class NotificacionController {
    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    // Notificaciones del usuario autenticado
    @GetMapping("/mias")
    public ResponseEntity<List<NotificacionResponseDTO>> obtenerMias(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(
                notificacionService.obtenerMias(usuario.getIdUsuario()));
    }

    // Marcar como leída
    @PatchMapping("/{id}/leer")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id) {
        notificacionService.marcarLeida(id);
        return ResponseEntity.noContent().build();
    }
}