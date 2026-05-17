package com.catolica.Artepax.notificacion.service;

import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.notificacion.dto.NotificacionResponseDTO;

import java.util.List;

/**
 * Servicio de notificaciones del sistema.
 * <p>
 * Define las operaciones para crear notificaciones masivas dirigidas a
 * administradores, consultar las notificaciones de un usuario y marcarlas
 * como leídas.
 */
public interface NotificacionService {

    /**
     * Crea una notificación para todos los administradores activos.
     *
     * @param tipoNombre nombre del tipo de notificación (ej. {@code RECORDATORIO_EVENTO})
     * @param mensaje    contenido textual de la notificación
     * @param evento     evento asociado (puede ser {@code null})
     */
    void notificarAdmins(String tipoNombre, String mensaje, Evento evento);

    /**
     * Marca una notificación como leída, verificando que el usuario autenticado
     * sea el propietario de la misma.
     *
     * @param notificacionId          identificador de la notificación
     * @param idUsuarioAutenticado    identificador del usuario que solicita la operación
     * @throws com.catolica.Artepax.notificacion.exception.NotificacionNotFoundException si la notificación no existe
     * @throws org.springframework.security.access.AccessDeniedException si el usuario no es el propietario
     */
    void marcarLeida(Long notificacionId, Long idUsuarioAutenticado);

    /**
     * Obtiene todas las notificaciones de un usuario, ordenadas por fecha de envío
     * descendente.
     *
     * @param usuarioId identificador del usuario
     * @return lista de DTO con los datos de cada notificación
     */
    List<NotificacionResponseDTO> obtenerMias(Long usuarioId);
}
