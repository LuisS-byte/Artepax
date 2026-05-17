package com.catolica.Artepax.evento.service.impl;

import com.catolica.Artepax.common.service.AuditoriaService;
import com.catolica.Artepax.evento.dto.EventoGastoControlDTO;
import com.catolica.Artepax.evento.dto.GastoEventoRequestDTO;
import com.catolica.Artepax.evento.dto.GastoEventoResponseDTO;
import com.catolica.Artepax.evento.dto.ResumenGastoEventoDTO;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.exception.EventoNotFoundException;
import com.catolica.Artepax.evento.exception.GastoEventoException;
import com.catolica.Artepax.evento.exception.GastoEventoNotFoundException;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.GastoEvento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.repository.GastoEventoRepository;
import com.catolica.Artepax.evento.service.GastoEventoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.catolica.Artepax.common.constant.AuditoriaAcciones.CREAR;
import static com.catolica.Artepax.common.constant.AuditoriaAcciones.DESEMBOLSAR;
import static com.catolica.Artepax.common.constant.AuditoriaAcciones.ELIMINAR;
import static com.catolica.Artepax.common.constant.AuditoriaTablas.GASTO_EVENTO;
import static com.catolica.Artepax.evento.model.EstadoEventoNames.ESTADOS_PERMITIDOS_GASTOS;
import static com.catolica.Artepax.evento.model.GastoEstadoNames.DESEMBOLSADO;
import static com.catolica.Artepax.evento.model.GastoEstadoNames.PROYECTADO;

@Service
public class GastoEventoServiceImpl implements GastoEventoService {

    private static final Logger log = LoggerFactory.getLogger(GastoEventoServiceImpl.class);

    private final GastoEventoRepository gastoRepository;
    private final EventoRepository eventoRepository;
    private final AuditoriaService auditoriaService;

    public GastoEventoServiceImpl(GastoEventoRepository gastoRepository,
                                  EventoRepository eventoRepository,
                                  AuditoriaService auditoriaService) {
        this.gastoRepository = gastoRepository;
        this.eventoRepository = eventoRepository;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public GastoEventoResponseDTO crear(GastoEventoRequestDTO dto) {
        Evento evento = eventoRepository.findById(dto.getEventoId())
                .orElseThrow(() -> new EventoNotFoundException(dto.getEventoId()));

        String estado = evento.getEstadoEvento().getNombre();
        if (!ESTADOS_PERMITIDOS_GASTOS.contains(estado)) {
            throw new GastoEventoException(
                    "Solo se pueden registrar gastos en eventos PROGRAMADO, " +
                            "CONFIRMADO o EN_MONTAJE. Estado actual: " + estado);
        }

        // Normalizar strings: trim en concepto y nota.
        // Solo se permite crear como PROYECTADO; el desembolso debe usar PATCH /desembolsar.
        String concepto = dto.getGastoConcepto().trim();
        String nota = dto.getGastoNota() != null ? dto.getGastoNota().trim() : null;

        GastoEvento gasto = new GastoEvento();
        gasto.setEvento(evento);
        gasto.setGastoConcepto(concepto);
        gasto.setGastoMonto(dto.getGastoMonto());
        gasto.setGastoEstado(PROYECTADO);
        gasto.setGastoNota(nota);

        GastoEvento guardado = gastoRepository.save(gasto);
        log.info("gasto_creado id={} evento={} monto={}",
                guardado.getId(), dto.getEventoId(), dto.getGastoMonto());

        Map<String, Object> valorNuevo = new LinkedHashMap<>();
        valorNuevo.put("id", guardado.getId());
        valorNuevo.put("eventoId", guardado.getEvento().getId());
        valorNuevo.put("concepto", guardado.getGastoConcepto());
        valorNuevo.put("monto", guardado.getGastoMonto());
        valorNuevo.put("estado", guardado.getGastoEstado());

        auditoriaService.registrar(CREAR, GASTO_EVENTO, guardado.getId(),
                null,
                valorNuevo,
                "Gasto creado para evento " + dto.getEventoId());

        return toDTO(guardado);
    }

    @Override
    public List<GastoEventoResponseDTO> obtenerPorEvento(Long eventoId) {
        return gastoRepository.findByEventoIdWithEvento(eventoId)
                .stream().map(this::toDTO).toList();
    }

    @Override
    public Page<GastoEventoResponseDTO> obtenerTodos(Pageable pageable) {
        return gastoRepository.findAllWithEvento(pageable)
                .map(this::toDTO);
    }

    @Override
    public GastoEventoResponseDTO obtenerPorId(Long id) {
        return toDTO(obtenerConEventoOFallar(id));
    }

    @Override
    @Transactional
    public GastoEventoResponseDTO desembolsar(Long id) {
        GastoEvento gasto = obtenerConEventoOFallar(id);
        String estadoAnterior = gasto.getGastoEstado();

        if (gasto.getGastoEstado().equals(DESEMBOLSADO)) {
            throw new GastoEventoException("El gasto ya fue desembolsado");
        }

        gasto.setGastoEstado(DESEMBOLSADO);
        GastoEvento guardado = gastoRepository.save(gasto);
        log.info("gasto_desembolsado id={}", id);

        auditoriaService.registrar(DESEMBOLSAR, GASTO_EVENTO, id,
                Map.of("estado", estadoAnterior),
                Map.of("estado", guardado.getGastoEstado()),
                "Gasto desembolsado");

        return toDTO(guardado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        GastoEvento gasto = obtenerOFallar(id);

        if (gasto.getGastoEstado().equals(DESEMBOLSADO)) {
            throw new GastoEventoException(
                    "No se puede eliminar un gasto ya desembolsado");
        }

        String concepto = gasto.getGastoConcepto();
        String estado = gasto.getGastoEstado();
        gastoRepository.deleteById(id);
        log.info("gasto_eliminado id={}", id);

        auditoriaService.registrar(ELIMINAR, GASTO_EVENTO, id,
                Map.of("concepto", concepto, "estado", estado),
                null,
                "Gasto eliminado");
    }

    private GastoEvento obtenerOFallar(Long id) {
        return gastoRepository.findById(id)
                .orElseThrow(() -> new GastoEventoNotFoundException(id));
    }

    /**
     * Obtiene un gasto con sus relaciones eager (evento, cliente, estadoEvento)
     * para evitar LazyInitializationException al mapear a DTO.
     */
    private GastoEvento obtenerConEventoOFallar(Long id) {
        return gastoRepository.findByIdWithEvento(id)
                .orElseThrow(() -> new GastoEventoNotFoundException(id));
    }

    private GastoEventoResponseDTO toDTO(GastoEvento g) {
        GastoEventoResponseDTO dto = new GastoEventoResponseDTO();
        dto.setId(g.getId());
        dto.setEventoId(g.getEvento().getId());
        dto.setGastoConcepto(g.getGastoConcepto());
        dto.setGastoMonto(g.getGastoMonto());
        dto.setGastoEstado(g.getGastoEstado());
        dto.setGastoFecha(g.getGastoFecha());
        dto.setGastoNota(g.getGastoNota());

        if (g.getEvento() != null) {
            dto.setClienteNombre(g.getEvento().getCliente().getNombre());
            dto.setEstadoEventoNombre(g.getEvento().getEstadoEvento().getNombre());
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenGastoEventoDTO getResumen(Long eventoId) {
        if (!eventoRepository.existsById(eventoId)) {
            throw new EventoNotFoundException(eventoId);
        }

        BigDecimal totalProyectado = gastoRepository.sumMontoByEventoIdAndEstado(eventoId, PROYECTADO);
        BigDecimal totalDesembolsado = gastoRepository.sumMontoByEventoIdAndEstado(eventoId, DESEMBOLSADO);
        BigDecimal totalPendiente = totalProyectado;

        return new ResumenGastoEventoDTO(totalProyectado, totalDesembolsado, totalPendiente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoGastoControlDTO> obtenerEventosControl() {
        List<Object[]> rows = gastoRepository.findEventosControlGastosNative();
        return rows.stream().map(this::mapearEventoControl).toList();
    }

    private EventoGastoControlDTO mapearEventoControl(Object[] row) {
        int i = 0;
        Long eventoId = ((Number) row[i++]).longValue();
        String clienteNombre = (String) row[i++];
        String estadoEventoNombre = (String) row[i++];
        java.sql.Date fechaSql = (java.sql.Date) row[i++];
        LocalDate fecha = fechaSql != null ? fechaSql.toLocalDate() : null;
        java.sql.Time horaInicioSql = (java.sql.Time) row[i++];
        LocalTime horaInicio = horaInicioSql != null ? horaInicioSql.toLocalTime() : null;
        java.sql.Time horaFinSql = (java.sql.Time) row[i++];
        LocalTime horaFin = horaFinSql != null ? horaFinSql.toLocalTime() : null;
        String ubicacion = (String) row[i++];
        BigDecimal totalProyectado = (BigDecimal) row[i++];
        BigDecimal totalDesembolsado = (BigDecimal) row[i++];
        Long cantidadGastos = ((Number) row[i++]).longValue();
        Long cantidadProyectados = ((Number) row[i++]).longValue();
        Long cantidadDesembolsados = ((Number) row[i++]).longValue();

        BigDecimal totalPendiente = totalProyectado;

        boolean permiteRegistrarGasto = ESTADOS_PERMITIDOS_GASTOS.contains(estadoEventoNombre);

        return new EventoGastoControlDTO(
            eventoId, clienteNombre, estadoEventoNombre, fecha, horaInicio, horaFin, ubicacion,
            totalProyectado, totalDesembolsado, totalPendiente,
            cantidadGastos, cantidadProyectados, cantidadDesembolsados,
            permiteRegistrarGasto
        );
    }
}
