package com.catolica.Artepax.evento.service.impl;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.cronograma.service.CronogramaService;
import com.catolica.Artepax.evento.dto.CalendarioEventoDTO;
import com.catolica.Artepax.evento.dto.EventoRequestDTO;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;
import com.catolica.Artepax.evento.dto.EventoUpdateDTO;
import com.catolica.Artepax.evento.dto.RentabilidadEventoDTO;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.mapper.EventoMapper;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.evento.model.VistaEventosPeriodo;
import com.catolica.Artepax.evento.model.VistaRentabilidadEvento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.repository.EstadoEventoRepository;
import com.catolica.Artepax.evento.repository.TipoEventoRepository;
import com.catolica.Artepax.evento.repository.VistaEventosPeriodoRepository;
import com.catolica.Artepax.evento.repository.VistaRentabilidadEventoRepository;
import com.catolica.Artepax.evento.service.EventoService;
import com.catolica.Artepax.evento.service.handler.EventoNotificacionHandler;
import com.catolica.Artepax.evento.service.handler.EventoPagoValidator;
import com.catolica.Artepax.evento.service.handler.EventoStockHandler;
import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.clientes.repository.ClienteRepository;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.catolica.Artepax.evento.model.EstadoEventoNames.*;

/**
 * Orquestador del ciclo de vida de eventos.
 * <p>
 * Delega la gestión de stock a {@link EventoStockHandler}, las notificaciones
 * a {@link EventoNotificacionHandler} y las validaciones de pago a
 * {@link EventoPagoValidator}.
 */
@Service
public class EventoServiceImpl implements EventoService {

    private static final Logger log = LoggerFactory.getLogger(EventoServiceImpl.class);

    private final EventoRepository eventoRepository;
    private final ClienteRepository clienteRepository;
    private final DecoracionRepository decoracionRepository;
    private final EstadoEventoRepository estadoEventoRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final EventoMapper eventoMapper;
    private final ConfiguracionService configuracionService;
    private final CronogramaService cronogramaService;
    private final VistaRentabilidadEventoRepository vistaRentabilidadEventoRepository;
    private final VistaEventosPeriodoRepository vistaEventosPeriodoRepository;
    private final EventoStockHandler stockHandler;
    private final EventoNotificacionHandler notificacionHandler;
    private final EventoPagoValidator pagoValidator;

    public EventoServiceImpl(EventoRepository eventoRepository,
                             ClienteRepository clienteRepository,
                             DecoracionRepository decoracionRepository,
                             EstadoEventoRepository estadoEventoRepository,
                             TipoEventoRepository tipoEventoRepository,
                             EventoMapper eventoMapper,
                             ConfiguracionService configuracionService,
                             @Lazy CronogramaService cronogramaService,
                             VistaRentabilidadEventoRepository vistaRentabilidadEventoRepository,
                             VistaEventosPeriodoRepository vistaEventosPeriodoRepository,
                             EventoStockHandler stockHandler,
                             EventoNotificacionHandler notificacionHandler,
                             EventoPagoValidator pagoValidator) {
        this.eventoRepository = eventoRepository;
        this.clienteRepository = clienteRepository;
        this.decoracionRepository = decoracionRepository;
        this.estadoEventoRepository = estadoEventoRepository;
        this.tipoEventoRepository = tipoEventoRepository;
        this.eventoMapper = eventoMapper;
        this.configuracionService = configuracionService;
        this.cronogramaService = cronogramaService;
        this.vistaRentabilidadEventoRepository = vistaRentabilidadEventoRepository;
        this.vistaEventosPeriodoRepository = vistaEventosPeriodoRepository;
        this.stockHandler = stockHandler;
        this.notificacionHandler = notificacionHandler;
        this.pagoValidator = pagoValidator;
    }

    @Override
    @Transactional
    public EventoResponseDTO crearEvento(EventoRequestDTO dto) {

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .filter(Cliente::getActivo)
                .orElseThrow(() -> new EventoException("Cliente no encontrado o inactivo"));

        Decoracion decoracion = decoracionRepository.findById(dto.getDecoracionId())
                .orElseThrow(() -> new EventoException("Decoración no encontrada"));

        if (!"ACTIVO".equals(decoracion.getEstado())) {
            throw new EventoException("La decoración seleccionada no está activa");
        }

        TipoEvento tipoEvento = tipoEventoRepository.findById(dto.getTipoEventoId())
                .filter(TipoEvento::getTipoActivo)
                .orElseThrow(() -> new EventoException("Tipo de evento no encontrado o inactivo"));

        if (dto.getFecha().isBefore(LocalDate.now())) {
            throw new EventoException("No se puede crear un evento en el pasado");
        }

        if (eventoRepository.existsByFechaAndHoraInicioAndUbicacion(
                dto.getFecha(), dto.getHoraInicio(), dto.getUbicacion())) {
            throw new EventoException("Ya existe un evento en esa fecha, hora y ubicación");
        }

        EstadoEvento estadoCotizacion = estadoEventoRepository.findByNombre(COTIZACION)
                .orElseThrow(() -> new EventoException("Estado COTIZACION no encontrado en BD"));

        Evento evento = eventoMapper.toEntity(dto, cliente, decoracion);
        evento.setTipoEvento(tipoEvento);
        evento.setEstadoEvento(estadoCotizacion);
        evento.setPrecioBase(decoracion.getPrecio());
        evento.setPrecioAdicional(dto.getPrecioAdicional() != null
                ? dto.getPrecioAdicional() : BigDecimal.ZERO);
        evento.setFechaExpiracion(LocalDate.now().plusDays(
                configuracionService.getInt("DIAS_EXPIRACION_COTIZACION")));
        evento.setClienteNotificado(false);

        Evento guardado = eventoRepository.save(evento);
        log.info("evento_creado id={} cliente={} decoracion={} estado={}",
                guardado.getId(), cliente.getNombre(), decoracion.getNombre(), COTIZACION);

        return eventoMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public EventoResponseDTO programarEvento(Long id) {
        Evento evento = obtenerOFallar(id);
        validarTransicion(evento, COTIZACION, PROGRAMADO);

        stockHandler.validarYReservarStock(evento);

        EstadoEvento estadoProgramado = estadoEventoRepository.findByNombre(PROGRAMADO)
                .orElseThrow(() -> new EventoException("Estado PROGRAMADO no encontrado en BD"));

        evento.setEstadoEvento(estadoProgramado);
        Evento guardado = eventoRepository.save(evento);
        log.info("evento_programado id={}", id);

        return eventoMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public EventoResponseDTO confirmarEvento(Long id) {
        Evento evento = obtenerOFallar(id);
        validarTransicion(evento, PROGRAMADO, CONFIRMADO);
        pagoValidator.validarAnticipoMinimo(evento);

        EstadoEvento estadoConfirmado = estadoEventoRepository.findByNombre(CONFIRMADO)
                .orElseThrow(() -> new EventoException("Estado CONFIRMADO no encontrado en BD"));

        evento.setEstadoEvento(estadoConfirmado);
        evento.setFechaConfirmacion(LocalDateTime.now());
        Evento guardado = eventoRepository.save(evento);
        log.info("evento_confirmado id={}", id);

        return eventoMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public void confirmarDesdeAnticipo(Long id) {
        Evento evento = obtenerOFallar(id);
        if (!evento.getEstadoEvento().getNombre().equals(PROGRAMADO)) {
            return;
        }

        pagoValidator.validarAnticipoMinimo(evento);

        EstadoEvento estadoConfirmado = estadoEventoRepository.findByNombre(CONFIRMADO)
                .orElseThrow(() -> new EventoException("Estado CONFIRMADO no encontrado en BD"));

        evento.setEstadoEvento(estadoConfirmado);
        evento.setFechaConfirmacion(LocalDateTime.now());
        eventoRepository.save(evento);

        notificacionHandler.notificarConfirmacionAutomatica(id, evento);
    }

    @Override
    @Transactional
    public EventoResponseDTO iniciarMontaje(Long id) {
        Evento evento = obtenerOFallar(id);
        validarTransicion(evento, CONFIRMADO, EN_MONTAJE);

        EstadoEvento estadoMontaje = estadoEventoRepository.findByNombre(EN_MONTAJE)
                .orElseThrow(() -> new EventoException("Estado EN_MONTAJE no encontrado en BD"));

        evento.setEstadoEvento(estadoMontaje);
        eventoRepository.save(evento);

        notificacionHandler.notificarInicioMontaje(id, evento);

        return eventoMapper.toResponseDTO(evento);
    }

    @Override
    @Transactional
    public EventoResponseDTO finalizarEvento(Long id) {
        Evento evento = obtenerOFallar(id);
        validarTransicion(evento, EN_MONTAJE, FINALIZADO);

        BigDecimal totalPagado = pagoValidator.getTotalPagado(id);
        if (totalPagado.compareTo(evento.getPrecioTotal()) < 0) {
            notificacionHandler.notificarSaldoPendiente(id, evento, totalPagado);
        }

        stockHandler.consumirStock(evento);

        EstadoEvento estadoFinalizado = estadoEventoRepository.findByNombre(FINALIZADO)
                .orElseThrow(() -> new EventoException("Estado FINALIZADO no encontrado en BD"));

        evento.setEstadoEvento(estadoFinalizado);
        Evento guardado = eventoRepository.save(evento);
        log.info("evento_finalizado id={}", id);

        return eventoMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public EventoResponseDTO iniciarCancelacion(Long id, String motivo) {
        Evento evento = obtenerOFallar(id);

        String estadoActual = evento.getEstadoEvento().getNombre();
        if (estadoActual.equals(COTIZACION)) {
            EstadoEvento estadoCancelado = estadoEventoRepository.findByNombre(CANCELADO)
                    .orElseThrow(() -> new EventoException("Estado CANCELADO no encontrado en BD"));
            evento.setEstadoEvento(estadoCancelado);
            evento.setMotivoCancelacion(motivo);
            Evento guardado = eventoRepository.save(evento);
            log.info("evento_cancelado_directo id={} estado_previo={}", id, COTIZACION);
            return eventoMapper.toResponseDTO(guardado);
        }

        if (!ESTADOS_PERMITIDOS_CANCELACION.contains(estadoActual)) {
            throw new EventoException(
                    "No se puede cancelar un evento en estado: " + estadoActual);
        }

        EstadoEvento estadoCancelacionPendiente = estadoEventoRepository
                .findByNombre(CANCELACION_PENDIENTE)
                .orElseThrow(() -> new EventoException(
                        "Estado CANCELACION_PENDIENTE no encontrado en BD"));

        evento.setEstadoAnterior(evento.getEstadoEvento());
        evento.setEstadoEvento(estadoCancelacionPendiente);
        evento.setMotivoCancelacion(motivo);
        evento.setFechaCancelacion(LocalDateTime.now());

        Evento guardado = eventoRepository.save(evento);

        notificacionHandler.notificarCancelacionIniciada(id, evento, motivo);

        return eventoMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public EventoResponseDTO revertirCancelacion(Long id) {
        Evento evento = obtenerOFallar(id);

        if (!evento.getEstadoEvento().getNombre().equals(CANCELACION_PENDIENTE)) {
            throw new EventoException("Solo se puede revertir desde CANCELACION_PENDIENTE");
        }
        if (evento.getEstadoAnterior() == null) {
            throw new EventoException("No hay estado anterior para restaurar");
        }

        evento.setEstadoEvento(evento.getEstadoAnterior());
        evento.setEstadoAnterior(null);
        evento.setMotivoCancelacion(null);
        evento.setFechaCancelacion(null);

        Evento guardado = eventoRepository.save(evento);
        log.info("evento_cancelacion_revertida id={}", id);

        return eventoMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public void confirmarCancelacion(Long id) {
        Evento evento = obtenerOFallar(id);
        validarTransicion(evento, CANCELACION_PENDIENTE, CANCELADO);

        EstadoEvento estadoCancelado = estadoEventoRepository.findByNombre(CANCELADO)
                .orElseThrow(() -> new EventoException(
                        "Estado CANCELADO no encontrado en BD"));

        evento.setEstadoEvento(estadoCancelado);
        evento.setEstadoAnterior(null);
        cronogramaService.cancelarPorEvento(id);
        eventoRepository.save(evento);
        log.info("evento_cancelacion_confirmada id={}", id);
    }

    @Override
    public List<EventoResponseDTO> obtenerEventos() {
        return eventoRepository.findAll().stream()
                .map(eventoMapper::toResponseDTO).toList();
    }

    @Override
    public EventoResponseDTO obtenerEventoPorId(Long id) {
        return eventoMapper.toResponseDTO(obtenerOFallar(id));
    }

    @Override
    public List<CalendarioEventoDTO> obtenerCalendario(LocalDate inicio, LocalDate fin) {
        if (inicio.isAfter(fin)) {
            throw new EventoException("La fecha inicio no puede ser mayor a la fecha fin");
        }
        return vistaEventosPeriodoRepository.findByFechaEventoBetween(inicio, fin)
                .stream()
                .map(v -> {
                    CalendarioEventoDTO dto = new CalendarioEventoDTO();
                    dto.setIdEvento(v.getIdEvento());
                    dto.setFechaEvento(v.getFechaEvento());
                    dto.setHoraInicio(v.getHoraInicio());
                    dto.setHoraFin(v.getHoraFin());
                    dto.setUbicacion(v.getUbicacion());
                    dto.setEstado(v.getEstado());
                    dto.setTipoEvento(v.getTipoEvento());
                    dto.setClienteNombre(v.getClienteNombre());
                    dto.setClienteTelefono(v.getClienteTelefono());
                    dto.setClienteCorreo(v.getClienteCorreo());
                    dto.setDecoracion(v.getDecoracion());
                    dto.setDecoracionImagen(v.getDecoracionImagen());
                    dto.setPrecioTotal(v.getPrecioTotal());
                    dto.setClienteNotificado(v.getClienteNotificado());
                    dto.setFechaConfirmacion(v.getFechaConfirmacion());
                    dto.setFechaExpiracion(v.getFechaExpiracion());
                    dto.setFechaCreacion(v.getFechaCreacion());
                    dto.setTotalCobrado(v.getTotalCobrado());
                    dto.setSaldoPendiente(v.getSaldoPendiente());
                    dto.setCobroCompleto(v.getCobroCompleto());
                    dto.setTieneCronograma(v.getTieneCronograma());
                    return dto;
                })
                .toList();
    }

    @Override
    public boolean verificarDisponibilidad(LocalDate fecha, LocalTime horaInicio,
                                           String ubicacion) {
        return !eventoRepository.existsByFechaAndHoraInicioAndUbicacion(
                fecha, horaInicio, ubicacion);
    }

    @Override
    public RentabilidadEventoDTO getRentabilidad(Long id) {
        VistaRentabilidadEvento vista = vistaRentabilidadEventoRepository.findByIdEvento(id)
                .orElseThrow(() -> new EventoException(
                        "No se encontró rentabilidad para el evento " + id));
        return new RentabilidadEventoDTO(
                vista.getIdEvento(),
                vista.getFechaEvento(),
                vista.getEstado(),
                vista.getPrecioAcordado(),
                vista.getIngresoCobrado(),
                vista.getCostoMateriales(),
                vista.getCostoManoObra(),
                vista.getGastosDesembolsados(),
                vista.getGastosProyectados(),
                vista.getGananciaNeta()
        );
    }

    @Override
    @Transactional
    public EventoResponseDTO actualizarEvento(Long id, EventoUpdateDTO dto) {
        Evento evento = obtenerOFallar(id);

        if (!evento.getEstadoEvento().getNombre().equals(COTIZACION)) {
            throw new EventoException("Solo se pueden editar eventos en estado COTIZACION");
        }

        Cliente cliente = dto.getClienteId() != null
                ? clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new EventoException("Cliente no encontrado"))
                : null;

        Decoracion decoracion = null;
        if (dto.getDecoracionId() != null) {
            decoracion = decoracionRepository.findById(dto.getDecoracionId())
                    .orElseThrow(() -> new EventoException("Decoración no encontrada"));
            if (!"ACTIVO".equals(decoracion.getEstado())) {
                throw new EventoException("La decoración seleccionada no está activa");
            }
        }

        eventoMapper.updateEntityFromDTO(evento, dto, cliente, decoracion);
        Evento guardado = eventoRepository.save(evento);
        log.info("evento_actualizado id={}", id);

        return eventoMapper.toResponseDTO(guardado);
    }

    private Evento obtenerOFallar(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new EventoException(
                        "Evento no encontrado con ID: " + id));
    }

    private void validarTransicion(Evento evento, String estadoRequerido, String destino) {
        String actual = evento.getEstadoEvento().getNombre();
        if (!actual.equals(estadoRequerido)) {
            throw new EventoException(
                    "Transición inválida: el evento está en " + actual +
                            ", se requiere " + estadoRequerido + " para pasar a " + destino);
        }
    }
}
