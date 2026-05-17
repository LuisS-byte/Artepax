package com.catolica.Artepax.notificacion.controller;
import com.catolica.Artepax.notificacion.dto.NotificacionResponseDTO;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import com.catolica.Artepax.authentication.user.entity.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de notificaciones.
 * <p>
 * Expone endpoints para que los usuarios administradores consulten sus
 * notificaciones y las marquen como leídas. Todos los endpoints requieren
 * el rol {@code ADMIN}.
 */
@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("hasRole('ADMIN')")
public class NotificacionController {
    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /**
     * Obtiene las notificaciones del usuario autenticado.
     *
     * @param usuario usuario autenticado inyectado por Spring Security
     * @return lista de notificaciones del usuario, ordenadas por fecha descendente
     */
    @GetMapping("/mias")
    public ResponseEntity<List<NotificacionResponseDTO>> obtenerMias(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(
                notificacionService.obtenerMias(usuario.getIdUsuario()));
    }

    /**
     * Marca una notificación como leída.
     * <p>
     * Solo el propietario de la notificación puede marcarla como leída.
     * Si ya está en estado {@code LEIDA}, la operación se ignora.
     *
     * @param id      identificador de la notificación
     * @param usuario usuario autenticado inyectado por Spring Security
     * @return {@code 204 No Content} si la operación se completa exitosamente
     */
    @PatchMapping("/{id}/leer")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id,
                                            @AuthenticationPrincipal Usuario usuario) {
        notificacionService.marcarLeida(id, usuario.getIdUsuario());
        return ResponseEntity.noContent().build();
    }
}
