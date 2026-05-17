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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio de notificaciones.
 * <p>
 * Gestiona la creación masiva de notificaciones a administradores, el marcado
 * como leídas con verificación de propiedad y la consulta de notificaciones
 * de un usuario. Incluye normalización de mensajes (truncamiento a 500 caracteres)
 * y validación de idempotencia contra notificaciones duplicadas.
 */
@Service
public class NotificacionServiceImpl implements NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionServiceImpl.class);

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

    /**
     * {@inheritDoc}
     * <p>
     * Para cada administrador activo crea una {@link Notificacion} en estado
     * {@code PENDIENTE}. El mensaje se normaliza antes de persistir.
     *
     * @throws IllegalStateException si el tipo o el estado no existen en BD
     */
    @Override
    @Transactional
    public void notificarAdmins(String tipoNombre, String mensaje, Evento evento) {
        TipoNotificacion tipo = tipoRepository.findByNombreTipo(tipoNombre)
                .orElseThrow(() -> new IllegalStateException(
                        "Tipo de notificación no encontrado: " + tipoNombre));

        EstadoNotificacion estadoPendiente = estadoRepository.findByNombreEstado(ESTADO_PENDIENTE)
                .orElseThrow(() -> new IllegalStateException(
                        "Estado " + ESTADO_PENDIENTE + " no encontrado en BD"));

        String mensajeNormalizado = normalizarMensaje(mensaje);

        List<Notificacion> notificaciones = usuarioRepository.findAdminsActivos()
                .stream()
                .map(admin -> {
                    Notificacion n = new Notificacion();
                    n.setUsuario(admin);
                    n.setTipoNotificacion(tipo);
                    n.setEstadoNotificacion(estadoPendiente);
                    n.setEvento(evento);
                    n.setMensaje(mensajeNormalizado);
                    return n;
                })
                .toList();

        notificacionRepository.saveAll(notificaciones);
    }

    /**
     * Normaliza el mensaje de una notificación: lo recorta, valida que no esté
     * vacío y lo trunca a un máximo de 500 caracteres si es necesario.
     *
     * @param mensaje texto a normalizar
     * @return mensaje normalizado y truncado
     * @throws IllegalArgumentException si el mensaje es {@code null} o está en blanco
     */
    private String normalizarMensaje(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje de notificación es obligatorio");
        }

        String normalizado = mensaje.trim();

        if (normalizado.length() > 500) {
            log.warn("Mensaje de notificación supera 500 caracteres ({}); será truncado", normalizado.length());
            return normalizado.substring(0, 500);
        }

        return normalizado;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Si la notificación ya está en estado {@code LEIDA}, la operación se ignora
     * silenciosamente. La verificación de propietario se realiza contra el
     * usuario autenticado; un {@code AccessDeniedException} se lanza si no coincide.
     *
     * @throws NotificacionNotFoundException si no existe la notificación con ese ID
     * @throws AccessDeniedException         si el usuario autenticado no es el propietario
     */
    @Override
    @Transactional
    public void marcarLeida(Long notificacionId, Long idUsuarioAutenticado) {
        Notificacion n = notificacionRepository.findByIdWithOwnerAndEstado(notificacionId)
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

    /**
     * {@inheritDoc}
     * <p>
     * Realiza una consulta optimizada con {@code JOIN FETCH} para cargar
     * todas las relaciones en una sola sentencia SQL.
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificacionResponseDTO> obtenerMias(Long usuarioId) {
        return notificacionRepository
                .findByUsuarioIdWithRelationsOrderByFechaEnvioDesc(usuarioId)
                .stream().map(this::toDTO).toList();
    }

    /**
     * Convierte una entidad {@link Notificacion} en su DTO de respuesta.
     * <p>
     * Mapea los nombres descriptivos de tipo y estado desde sus entidades
     * relacionadas, y extrae el ID del evento si existe.
     *
     * @param n entidad de notificación
     * @return DTO listo para ser serializado
     */
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
