package com.catolica.Artepax.notificacion.service;

import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.notificacion.dto.NotificacionResponseDTO;

import java.util.List;

public interface NotificacionService {

    void notificarAdmins(String tipoNombre, String mensaje, Evento evento);
    void marcarLeida(Long notificacionId);
    List<NotificacionResponseDTO> obtenerMias(Long usuarioId);
}
