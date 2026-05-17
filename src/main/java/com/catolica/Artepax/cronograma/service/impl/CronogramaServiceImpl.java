package com.catolica.Artepax.cronograma.service.impl;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.repository.UsuarioRepository;
import com.catolica.Artepax.cronograma.dto.CronogramaRequestDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaResponseDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaUpdateDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaUsuarioRequestDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaUsuarioResponseDTO;
import com.catolica.Artepax.cronograma.entity.Cronograma;
import com.catolica.Artepax.cronograma.entity.CronogramaEstado;
import com.catolica.Artepax.cronograma.entity.CronogramaEstados;
import com.catolica.Artepax.cronograma.entity.CronogramaPagoTipo;
import com.catolica.Artepax.cronograma.entity.CronogramaUsuario;
import com.catolica.Artepax.cronograma.entity.CronogramaUsuarioId;
import com.catolica.Artepax.cronograma.exception.CronogramaException;
import com.catolica.Artepax.cronograma.exception.CronogramaNotFoundException;
import com.catolica.Artepax.cronograma.mapper.CronogramaMapper;
import com.catolica.Artepax.cronograma.repository.CronogramaRepository;
import com.catolica.Artepax.cronograma.repository.CronogramaUsuarioRepository;
import com.catolica.Artepax.cronograma.service.CronogramaService;
import com.catolica.Artepax.cronograma.service.policy.CronogramaPolicy;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación del servicio de cronograma.
 * Gestiona actividades operativas asociadas a eventos y su asignación de personal.
 */
@Service
public class CronogramaServiceImpl implements CronogramaService {

    private static final Logger log = LoggerFactory.getLogger(CronogramaServiceImpl.class);

    private final CronogramaRepository cronogramaRepository;
    private final CronogramaUsuarioRepository cronogramaUsuarioRepository;
    private final CronogramaMapper cronogramaMapper;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CronogramaPolicy cronogramaPolicy;

    public CronogramaServiceImpl(CronogramaRepository cronogramaRepository,
                                 CronogramaUsuarioRepository cronogramaUsuarioRepository,
                                 CronogramaMapper cronogramaMapper,
                                 EventoRepository eventoRepository,
                                 UsuarioRepository usuarioRepository,
                                 CronogramaPolicy cronogramaPolicy) {
        this.cronogramaRepository = cronogramaRepository;
        this.cronogramaUsuarioRepository = cronogramaUsuarioRepository;
        this.cronogramaMapper = cronogramaMapper;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.cronogramaPolicy = cronogramaPolicy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CronogramaResponseDTO crear(CronogramaRequestDTO dto) {
        Evento evento = eventoRepository.findById(dto.getIdEvento())
                .orElseThrow(() -> new EventoException(
                        "Evento no encontrado con ID: " + dto.getIdEvento()));

        cronogramaPolicy.validarEventoPermiteCronograma(evento);
        cronogramaPolicy.validarFechasActividad(dto.getFechaInicio(), dto.getFechaFin(), evento);

        Cronograma cronograma = cronogramaMapper.toEntity(dto, evento);
        Cronograma saved = cronogramaRepository.save(cronograma);
        log.info("Actividad de cronograma creada: id={}, evento={}, actividad={}",
                saved.getId(), dto.getIdEvento(), dto.getActividad());
        return cronogramaMapper.toDTO(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CronogramaResponseDTO> obtenerPorEvento(Long idEvento) {
        log.debug("Obteniendo cronograma para evento {}", idEvento);
        return cronogramaRepository.findByEventoIdWithEvento(idEvento)
                .stream().map(cronogramaMapper::toDTO).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<CronogramaResponseDTO> obtenerTodos(Pageable pageable) {
        return cronogramaRepository.findAllWithEvento(pageable)
                .map(cronogramaMapper::toDTO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CronogramaResponseDTO obtenerPorId(Long id) {
        Cronograma cronograma = cronogramaRepository.findByIdWithEvento(id)
                .orElseThrow(() -> new CronogramaNotFoundException(id));
        return cronogramaMapper.toDTO(cronograma);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CronogramaResponseDTO actualizar(Long id, CronogramaUpdateDTO dto) {
        Cronograma cronograma = obtenerOFallar(id);
        cronogramaPolicy.validarActividadEditable(cronograma.getCronoEstado());
        cronogramaPolicy.validarEventoPermiteCronograma(cronograma.getEvento());

        if (dto.getActividad() != null) cronograma.setActividad(dto.getActividad());
        if (dto.getFechaInicio() != null) cronograma.setFechaInicio(dto.getFechaInicio());
        if (dto.getFechaFin() != null) cronograma.setFechaFin(dto.getFechaFin());
        if (dto.getCronoObservacion() != null) cronograma.setCronoObservacion(dto.getCronoObservacion());

        LocalDateTime inicio = cronograma.getFechaInicio();
        LocalDateTime fin = cronograma.getFechaFin();
        cronogramaPolicy.validarFechasActividad(inicio, fin, cronograma.getEvento());

        Cronograma saved = cronogramaRepository.save(cronograma);
        log.info("Actividad actualizada: id={}, estado={}", id, saved.getCronoEstado());
        return cronogramaMapper.toDTO(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CronogramaResponseDTO cambiarEstado(Long id, String nuevoEstado) {
        if (!CronogramaEstado.isValid(nuevoEstado)) {
            throw new CronogramaException("Estado inválido: " + nuevoEstado);
        }

        Cronograma cronograma = obtenerOFallar(id);
        cronogramaPolicy.validarTransicion(cronograma.getCronoEstado(), nuevoEstado);
        if (cronograma.getCronoEstado().equals(nuevoEstado)) {
            return cronogramaMapper.toDTO(cronograma);
        }

        if (CronogramaEstados.EN_PROGRESO.equals(nuevoEstado)
                || CronogramaEstados.COMPLETADO.equals(nuevoEstado)) {
            cronogramaPolicy.validarEventoPermiteCronograma(cronograma.getEvento());
        }

        cronograma.setCronoEstado(nuevoEstado);
        Cronograma saved = cronogramaRepository.save(cronograma);
        log.info("Estado de actividad cambiado: id={}, nuevoEstado={}", id, nuevoEstado);
        return cronogramaMapper.toDTO(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void cancelar(Long id) {
        Cronograma cronograma = obtenerOFallar(id);
        if (CronogramaEstados.CANCELADO.equals(cronograma.getCronoEstado())) {
            return;
        }

        cronogramaPolicy.validarTransicion(
                cronograma.getCronoEstado(),
                CronogramaEstados.CANCELADO);

        cronogramaPolicy.validarEventoPermiteCronograma(cronograma.getEvento());

        cronograma.setCronoEstado(CronogramaEstados.CANCELADO);
        cronogramaRepository.save(cronograma);
        log.info("Actividad cancelada: id={}", id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void cancelarPorEvento(Long eventoId) {
        int afectadas = cronogramaRepository.cancelarPorEvento(eventoId);
        log.info("Cronograma cancelado en cascada para evento {}: {} actividades afectadas", eventoId, afectadas);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CronogramaUsuarioResponseDTO asignarUsuario(Long cronogramaId,
                                                       CronogramaUsuarioRequestDTO dto) {
        Cronograma cronograma = obtenerOFallar(cronogramaId);

        if (CronogramaEstado.ESTADOS_FINALES.contains(cronograma.getCronoEstado())) {
            throw new CronogramaException(
                    "No se puede asignar usuarios a una actividad " +
                            cronograma.getCronoEstado());
        }

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new CronogramaException(
                        "Usuario no encontrado: " + dto.getUsuarioId()));

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            throw new CronogramaException("El usuario está inactivo");
        }

        if (!CronogramaPagoTipo.VALORES.contains(dto.getPagoTipo())) {
            throw new CronogramaException("Tipo de pago inválido: " + dto.getPagoTipo());
        }

        if (dto.getPagoAsignado().compareTo(BigDecimal.ZERO) < 0) {
            throw new CronogramaException("El pago asignado no puede ser negativo");
        }

        CronogramaUsuarioId cuId = new CronogramaUsuarioId(cronogramaId, dto.getUsuarioId());

        CronogramaUsuario cu = cronogramaUsuarioRepository.findById(cuId)
                .orElseGet(CronogramaUsuario::new);

        cu.setId(cuId);
        cu.setCronograma(cronograma);
        cu.setUsuario(usuario);
        cu.setPagoAsignado(dto.getPagoAsignado());
        cu.setPagoTipo(dto.getPagoTipo());

        CronogramaUsuario saved = cronogramaUsuarioRepository.save(cu);
        log.info("Usuario asignado a actividad: cronograma={}, usuario={}, pago={}",
                cronogramaId, dto.getUsuarioId(), dto.getPagoTipo());
        return toUsuarioDTO(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CronogramaUsuarioResponseDTO> obtenerUsuarios(Long cronogramaId) {
        obtenerOFallar(cronogramaId);
        return cronogramaUsuarioRepository.findByCronogramaIdWithUsuario(cronogramaId)
                .stream().map(this::toUsuarioDTO).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void removerUsuario(Long cronogramaId, Long usuarioId) {
        CronogramaUsuarioId cuId = new CronogramaUsuarioId(cronogramaId, usuarioId);
        if (!cronogramaUsuarioRepository.existsById(cuId)) {
            throw new CronogramaException(
                    "El usuario no está asignado a esta actividad");
        }
        cronogramaUsuarioRepository.deleteById(cuId);
        log.info("Usuario removido de actividad: cronograma={}, usuario={}",
                cronogramaId, usuarioId);
    }

    /**
     * Obtiene una actividad por su id o lanza {@link CronogramaNotFoundException}.
     *
     * @param id identificador de la actividad
     * @return la actividad encontrada
     * @throws CronogramaNotFoundException si no existe
     */
    private Cronograma obtenerOFallar(Long id) {
        return cronogramaRepository.findById(id)
                .orElseThrow(() -> new CronogramaNotFoundException(id));
    }

    /**
     * Convierte una entidad {@link CronogramaUsuario} a su DTO de respuesta.
     *
     * @param cu entidad de asignación usuario-cronograma
     * @return DTO con datos de la asignación
     */
    private CronogramaUsuarioResponseDTO toUsuarioDTO(CronogramaUsuario cu) {
        CronogramaUsuarioResponseDTO dto = new CronogramaUsuarioResponseDTO();
        dto.setCronogramaId(cu.getId().getIdCronograma());
        dto.setUsuarioId(cu.getId().getIdUsuario());
        dto.setUsuarioNombre(cu.getUsuario().getNombre());
        dto.setPagoAsignado(cu.getPagoAsignado());
        dto.setPagoTipo(cu.getPagoTipo());
        return dto;
    }
}
