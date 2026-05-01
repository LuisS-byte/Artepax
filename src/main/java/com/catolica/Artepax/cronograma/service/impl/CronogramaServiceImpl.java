package com.catolica.Artepax.cronograma.service.impl;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.repository.UsuarioRepository;
import com.catolica.Artepax.cronograma.dto.*;
import com.catolica.Artepax.cronograma.entity.*;
import com.catolica.Artepax.cronograma.exception.CronogramaNotFoundException;
import com.catolica.Artepax.cronograma.mapper.CronogramaMapper;
import com.catolica.Artepax.cronograma.repository.*;
import com.catolica.Artepax.cronograma.service.CronogramaService;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CronogramaServiceImpl implements CronogramaService {

    private final CronogramaRepository cronogramaRepository;
    private final CronogramaUsuarioRepository cronogramaUsuarioRepository;
    private final CronogramaMapper cronogramaMapper;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    public CronogramaServiceImpl(CronogramaRepository cronogramaRepository,
            CronogramaUsuarioRepository cronogramaUsuarioRepository,
            CronogramaMapper cronogramaMapper,
            EventoRepository eventoRepository,
            UsuarioRepository usuarioRepository) {
        this.cronogramaRepository = cronogramaRepository;
        this.cronogramaUsuarioRepository = cronogramaUsuarioRepository;
        this.cronogramaMapper = cronogramaMapper;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public CronogramaResponseDTO crear(CronogramaRequestDTO dto) {
        Evento evento = eventoRepository.findById(dto.getIdEvento())
                .orElseThrow(() -> new EventoException(
                        "Evento no encontrado con ID: " + dto.getIdEvento()));

        // Solo desde CONFIRMADO en adelante
        String estadoEvento = evento.getEstadoEvento().getNombre();
        if (!List.of("CONFIRMADO", "EN_MONTAJE").contains(estadoEvento)) {
            throw new IllegalArgumentException(
                    "Solo se pueden crear actividades en eventos CONFIRMADO o EN_MONTAJE. " +
                            "Estado actual: " + estadoEvento);
        }

        if (dto.getFechaInicio().isAfter(dto.getFechaFin())) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        // B8 FIX — validar que las fechas estén dentro del rango del evento
        if (evento.getFecha() != null) {
            LocalDateTime fechaEvento = evento.getFecha().atStartOfDay();
            LocalDateTime fechaFinEvento = evento.getFecha().atTime(23, 59, 59);

            if (dto.getFechaInicio().isBefore(fechaEvento) ||
                    dto.getFechaFin().isAfter(fechaFinEvento)) {
                throw new IllegalArgumentException(
                        "Las fechas de la actividad deben estar dentro de la fecha del evento: "
                                + evento.getFecha());
            }
        }

        Cronograma cronograma = cronogramaMapper.toEntity(dto, evento);
        return cronogramaMapper.toDTO(cronogramaRepository.save(cronograma));
    }

    @Override
    public List<CronogramaResponseDTO> obtenerPorEvento(Long idEvento) {
        return cronogramaRepository.findByEventoId(idEvento)
                .stream().map(cronogramaMapper::toDTO).toList();
    }

    @Override
    public CronogramaResponseDTO obtenerPorId(Long id) {
        return cronogramaMapper.toDTO(obtenerOFallar(id));
    }

    @Override
    @Transactional
    public CronogramaResponseDTO actualizar(Long id, CronogramaUpdateDTO dto) {
        Cronograma cronograma = obtenerOFallar(id);

        if (cronograma.getCronoEstado().equals("CANCELADO") ||
                cronograma.getCronoEstado().equals("COMPLETADO")) {
            throw new IllegalArgumentException(
                    "No se puede editar una actividad en estado: " + cronograma.getCronoEstado());
        }

        if (dto.getActividad() != null) cronograma.setActividad(dto.getActividad());
        if (dto.getFechaInicio() != null) cronograma.setFechaInicio(dto.getFechaInicio());
        if (dto.getFechaFin() != null) cronograma.setFechaFin(dto.getFechaFin());
        if (dto.getCronoObservacion() != null) cronograma.setCronoObservacion(dto.getCronoObservacion());

        LocalDateTime inicio = cronograma.getFechaInicio();
        LocalDateTime fin = cronograma.getFechaFin();
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        return cronogramaMapper.toDTO(cronogramaRepository.save(cronograma));
    }

    @Override
    @Transactional
    public CronogramaResponseDTO cambiarEstado(Long id, String nuevoEstado) {
        Cronograma cronograma = obtenerOFallar(id);

        List<String> estadosValidos = List.of(
                "PENDIENTE", "EN_PROGRESO", "COMPLETADO", "CANCELADO");
        if (!estadosValidos.contains(nuevoEstado)) {
            throw new IllegalArgumentException("Estado inválido: " + nuevoEstado);
        }

        // No se puede reactivar una actividad cancelada o completada
        if (cronograma.getCronoEstado().equals("CANCELADO") ||
                cronograma.getCronoEstado().equals("COMPLETADO")) {
            throw new IllegalArgumentException(
                    "No se puede cambiar el estado de una actividad " +
                            cronograma.getCronoEstado());
        }

        cronograma.setCronoEstado(nuevoEstado);
        return cronogramaMapper.toDTO(cronogramaRepository.save(cronograma));
    }

    @Override
    @Transactional
    public void cancelar(Long id) {
        Cronograma cronograma = obtenerOFallar(id);

        if (cronograma.getCronoEstado().equals("CANCELADO")) return;
        if (cronograma.getCronoEstado().equals("COMPLETADO")) {
            throw new IllegalArgumentException(
                    "No se puede cancelar una actividad ya completada");
        }

        cronograma.setCronoEstado("CANCELADO");
        cronogramaRepository.save(cronograma);
    }

    @Override
    @Transactional
    public void cancelarPorEvento(Long eventoId) {
        // Una sola query — cancela todas las actividades activas del evento
        cronogramaRepository.cancelarPorEvento(eventoId);
    }

    // ─── CRONOGRAMA USUARIO ───────────────────────────────────────────────────

    @Override
    @Transactional
    public CronogramaUsuarioResponseDTO asignarUsuario(Long cronogramaId,
            CronogramaUsuarioRequestDTO dto) {
        Cronograma cronograma = obtenerOFallar(cronogramaId);

        if (cronograma.getCronoEstado().equals("CANCELADO") ||
                cronograma.getCronoEstado().equals("COMPLETADO")) {
            throw new IllegalArgumentException(
                    "No se puede asignar usuarios a una actividad " +
                            cronograma.getCronoEstado());
        }

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + dto.getUsuarioId()));

        if (usuario.getEstado() == null || !usuario.getEstado()){
            throw new IllegalArgumentException("El usuario está inactivo");
        }

        // Validar tipo de pago
        if (!List.of("FIJO", "POR_HORA").contains(dto.getPagoTipo())) {
            throw new IllegalArgumentException("Tipo de pago inválido: " + dto.getPagoTipo());
        }

        CronogramaUsuarioId cuId = new CronogramaUsuarioId(cronogramaId, dto.getUsuarioId());

        // Si ya está asignado, actualizar pago
        CronogramaUsuario cu = cronogramaUsuarioRepository.findById(cuId)
                .orElse(new CronogramaUsuario());

        cu.setId(cuId);
        cu.setCronograma(cronograma);
        cu.setUsuario(usuario);
        cu.setPagoAsignado(dto.getPagoAsignado());
        cu.setPagoTipo(dto.getPagoTipo());

        return toUsuarioDTO(cronogramaUsuarioRepository.save(cu));
    }

    @Override
    public List<CronogramaUsuarioResponseDTO> obtenerUsuarios(Long cronogramaId) {
        obtenerOFallar(cronogramaId); // validar que existe
        return cronogramaUsuarioRepository.findByIdIdCronograma(cronogramaId)
                .stream().map(this::toUsuarioDTO).toList();
    }

    @Override
    @Transactional
    public void removerUsuario(Long cronogramaId, Long usuarioId) {
        CronogramaUsuarioId cuId = new CronogramaUsuarioId(cronogramaId, usuarioId);
        if (!cronogramaUsuarioRepository.existsById(cuId)) {
            throw new IllegalArgumentException(
                    "El usuario no está asignado a esta actividad");
        }
        cronogramaUsuarioRepository.deleteById(cuId);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────
    private Cronograma obtenerOFallar(Long id) {
        return cronogramaRepository.findById(id)
                .orElseThrow(() -> new CronogramaNotFoundException(id));
    }

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