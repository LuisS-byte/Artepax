package com.catolica.Artepax.notificacion.service.impl;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.repository.UsuarioRepository;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.notificacion.dto.NotificacionResponseDTO;
import com.catolica.Artepax.notificacion.entity.EstadoNotificacion;
import com.catolica.Artepax.notificacion.entity.Notificacion;
import com.catolica.Artepax.notificacion.entity.TipoNotificacion;
import com.catolica.Artepax.notificacion.exception.NotificacionNotFoundException;
import com.catolica.Artepax.notificacion.repository.EstadoNotificacionRepository;
import com.catolica.Artepax.notificacion.repository.NotificacionRepository;
import com.catolica.Artepax.notificacion.repository.TipoNotificacionRepository;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_LEIDA = "LEIDA";

    private final NotificacionRepository notificacionRepository;
    private final TipoNotificacionRepository tipoRepository;
    private final EstadoNotificacionRepository estadoRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionServiceImpl(NotificacionRepository notificacionRepository,
                                   TipoNotificacionRepository tipoRepository,
                                   EstadoNotificacionRepository estadoRepository,
                                   UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.tipoRepository = tipoRepository;
        this.estadoRepository = estadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public void notificarAdmins(String tipoNombre, String mensaje, Evento evento) {
        TipoNotificacion tipo = tipoRepository.findByNombreTipo(tipoNombre)
                .orElseThrow(() -> new IllegalStateException(
                        "Tipo de notificación no encontrado: " + tipoNombre));

        EstadoNotificacion estadoPendiente = estadoRepository.findByNombreEstado(ESTADO_PENDIENTE)
                .orElseThrow(() -> new IllegalStateException(
                        "Estado " + ESTADO_PENDIENTE + " no encontrado en BD"));

        List<Usuario> admins = usuarioRepository.findAdminsActivos();

        for (Usuario admin : admins) {
            Notificacion n = new Notificacion();
            n.setUsuario(admin);
            n.setTipoNotificacion(tipo);
            n.setEstadoNotificacion(estadoPendiente);
            n.setEvento(evento);
            n.setMensaje(mensaje);
            notificacionRepository.save(n);
        }
    }

    @Override
    @Transactional
    public void marcarLeida(Long notificacionId, Long idUsuarioAutenticado) {
        Notificacion n = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new NotificacionNotFoundException(notificacionId));

        Long ownerId = n.getUsuario().getIdUsuario();
        if (!ownerId.equals(idUsuarioAutenticado)) {
            throw new AccessDeniedException("No puede modificar esta notificación");
        }

        if (ESTADO_LEIDA.equals(n.getEstadoNotificacion().getNombreEstado())) {
            return;
        }

        EstadoNotificacion estadoLeida = estadoRepository.findByNombreEstado(ESTADO_LEIDA)
                .orElseThrow(() -> new IllegalStateException(
                        "Estado " + ESTADO_LEIDA + " no encontrado en BD"));

        n.setEstadoNotificacion(estadoLeida);
        notificacionRepository.save(n);
    }

    @Override
    public List<NotificacionResponseDTO> obtenerMias(Long usuarioId) {
        return notificacionRepository
                .findByUsuarioIdUsuarioOrderByFechaEnvioDesc(usuarioId)
                .stream().map(this::toDTO).toList();
    }

    private NotificacionResponseDTO toDTO(Notificacion n) {
        NotificacionResponseDTO dto = new NotificacionResponseDTO();
        dto.setId(n.getId());
        dto.setMensaje(n.getMensaje());
        dto.setTipoNotificacion(n.getTipoNotificacion().getNombreTipo());
        dto.setEstadoNotificacion(n.getEstadoNotificacion().getNombreEstado());
        dto.setEventoId(n.getEvento() != null ? n.getEvento().getId() : null);
        dto.setFechaEnvio(n.getFechaEnvio());
        return dto;
    }
}
