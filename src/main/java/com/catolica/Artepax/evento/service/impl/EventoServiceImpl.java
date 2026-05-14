package com.catolica.Artepax.evento.service.impl;

import com.catolica.Artepax.common.audit.EventoAuditSnapshot;
import com.catolica.Artepax.common.audit.ReservaAuditSnapshot;
import com.catolica.Artepax.common.service.AuditoriaService;
import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.cronograma.service.CronogramaService;
import com.catolica.Artepax.evento.dto.CalendarioEventoDTO;
import com.catolica.Artepax.evento.dto.EventoProductoDTO;
import com.catolica.Artepax.evento.dto.EventoExtraItemRequestDTO;
import com.catolica.Artepax.evento.dto.EventoExtraRequestDTO;
import com.catolica.Artepax.evento.dto.EventoRequestDTO;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;
import com.catolica.Artepax.evento.dto.EventoResumenInternoDTO;
import com.catolica.Artepax.evento.dto.EventoUpdateDTO;
import com.catolica.Artepax.evento.dto.RentabilidadEventoDTO;
import com.catolica.Artepax.evento.dto.ResumenGastoEventoDTO;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.exception.EventoNotFoundException;
import com.catolica.Artepax.evento.mapper.EventoMapper;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.evento.model.VistaEventosPeriodo;
import com.catolica.Artepax.evento.model.VistaRentabilidadEvento;
import com.catolica.Artepax.evento.repository.EventoProductoRepository;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.repository.EstadoEventoRepository;
import com.catolica.Artepax.evento.repository.GastoEventoRepository;
import com.catolica.Artepax.evento.repository.TipoEventoRepository;
import com.catolica.Artepax.evento.repository.VistaEventosPeriodoRepository;
import com.catolica.Artepax.evento.repository.VistaRentabilidadEventoRepository;
import com.catolica.Artepax.evento.service.EventoService;
import com.catolica.Artepax.evento.service.handler.EventoNotificacionHandler;
import com.catolica.Artepax.evento.service.handler.EventoPagoValidator;
import com.catolica.Artepax.evento.service.handler.EventoStockHandler;
import com.catolica.Artepax.pago.service.handler.PagoCancelacionHandler;
import com.catolica.Artepax.pago.dto.ResumenPagoEventoDTO;
import com.catolica.Artepax.pago.entity.VistaPagosEvento;
import com.catolica.Artepax.pago.repository.VistaPagosEventoRepository;
import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.clientes.repository.ClienteRepository;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionProductoRepository;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionRepository;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.catolica.Artepax.evento.model.EstadoEventoNames.*;
import static com.catolica.Artepax.common.constant.AuditoriaAcciones.*;
import static com.catolica.Artepax.common.constant.AuditoriaTablas.EVENTO;
import static com.catolica.Artepax.common.constant.AuditoriaTablas.EVENTO_PRODUCTO;
import static com.catolica.Artepax.evento.model.GastoEstadoNames.DESEMBOLSADO;
import static com.catolica.Artepax.evento.model.GastoEstadoNames.PROYECTADO;

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
    private static final int MINIMO_GAP_ENTRE_EVENTOS_MINUTOS = 120;

    private final EventoRepository eventoRepository;
    private final ClienteRepository clienteRepository;
    private final DecoracionRepository decoracionRepository;
    private final EstadoEventoRepository estadoEventoRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final ProductoRepository productoRepository;
    private final GastoEventoRepository gastoEventoRepository;
    private final EventoMapper eventoMapper;
    private final ConfiguracionService configuracionService;
    private final CronogramaService cronogramaService;
    private final VistaRentabilidadEventoRepository vistaRentabilidadEventoRepository;
    private final VistaEventosPeriodoRepository vistaEventosPeriodoRepository;
    private final VistaPagosEventoRepository vistaPagosEventoRepository;
    private final EventoProductoRepository eventoProductoRepository;
    private final EventoStockHandler stockHandler;
    private final EventoNotificacionHandler notificacionHandler;
    private final EventoPagoValidator pagoValidator;
    private final AuditoriaService auditoriaService;
    private final PagoCancelacionHandler pagoCancelacionHandler;
    private final DecoracionProductoRepository decoracionProductoRepository;

    public EventoServiceImpl(EventoRepository eventoRepository,
                             ClienteRepository clienteRepository,
                             DecoracionRepository decoracionRepository,
                             EstadoEventoRepository estadoEventoRepository,
                             TipoEventoRepository tipoEventoRepository,
                             ProductoRepository productoRepository,
                             GastoEventoRepository gastoEventoRepository,
                             EventoMapper eventoMapper,
                             ConfiguracionService configuracionService,
                             @Lazy CronogramaService cronogramaService,
                             VistaRentabilidadEventoRepository vistaRentabilidadEventoRepository,
                             VistaEventosPeriodoRepository vistaEventosPeriodoRepository,
                             VistaPagosEventoRepository vistaPagosEventoRepository,
                             EventoProductoRepository eventoProductoRepository,
                             EventoStockHandler stockHandler,
                              EventoNotificacionHandler notificacionHandler,
                              EventoPagoValidator pagoValidator,
                              AuditoriaService auditoriaService,
                              PagoCancelacionHandler pagoCancelacionHandler,
                              DecoracionProductoRepository decoracionProductoRepository) {
        this.eventoRepository = eventoRepository;
        this.clienteRepository = clienteRepository;
        this.decoracionRepository = decoracionRepository;
        this.estadoEventoRepository = estadoEventoRepository;
        this.tipoEventoRepository = tipoEventoRepository;
        this.productoRepository = productoRepository;
        this.gastoEventoRepository = gastoEventoRepository;
        this.eventoMapper = eventoMapper;
        this.configuracionService = configuracionService;
        this.cronogramaService = cronogramaService;
        this.vistaRentabilidadEventoRepository = vistaRentabilidadEventoRepository;
        this.vistaEventosPeriodoRepository = vistaEventosPeriodoRepository;
        this.vistaPagosEventoRepository = vistaPagosEventoRepository;
        this.eventoProductoRepository = eventoProductoRepository;
        this.stockHandler = stockHandler;
        this.notificacionHandler = notificacionHandler;
        this.pagoValidator = pagoValidator;
        this.auditoriaService = auditoriaService;
        this.pagoCancelacionHandler = pagoCancelacionHandler;
        this.decoracionProductoRepository = decoracionProductoRepository;
    }

    @Override
    @Transactional
    public EventoResponseDTO crearEvento(EventoRequestDTO dto) {
        validarNotaPrecioAdicional(dto.getPrecioAdicional(), dto.getNotaPrecio());

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new EventoException("Cliente no encontrado o inactivo"));
        validarClienteActivo(cliente);

        Decoracion decoracion = decoracionRepository.findById(dto.getDecoracionId())
                .orElseThrow(() -> new EventoException("Decoración no encontrada"));
        validarDecoracionActiva(decoracion);

        TipoEvento tipoEvento = tipoEventoRepository.findById(dto.getTipoEventoId())
                .orElseThrow(() -> new EventoException("Tipo de evento no encontrado o inactivo"));
        validarTipoEventoActivo(tipoEvento);

        validarFechaNoPasada(dto.getFecha());
        validarHorario(dto.getHoraInicio(), dto.getHoraFin());
        validarDisponibilidad(dto.getFecha(), dto.getHoraInicio(), dto.getHoraFin(), dto.getUbicacion(), null);

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

        EventoAuditSnapshot antes = toAuditSnapshot(evento);

        stockHandler.validarYReservarStock(evento);

        EstadoEvento estadoProgramado = estadoEventoRepository.findByNombre(PROGRAMADO)
                .orElseThrow(() -> new EventoException("Estado PROGRAMADO no encontrado en BD"));

        evento.setEstadoEvento(estadoProgramado);
        Evento guardado = eventoRepository.save(evento);
        log.info("evento_programado id={}", id);

        EventoAuditSnapshot despues = toAuditSnapshot(guardado);
        auditoriaService.registrar(PROGRAMAR, EVENTO, id, antes, despues,
                "Evento programado — reservas de stock creadas");

        return eventoMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public EventoResponseDTO confirmarEvento(Long id) {
        Evento evento = obtenerOFallar(id);
        validarTransicion(evento, PROGRAMADO, CONFIRMADO);
        pagoValidator.validarAnticipoMinimo(evento);

        EventoAuditSnapshot antes = toAuditSnapshot(evento);

        EstadoEvento estadoConfirmado = estadoEventoRepository.findByNombre(CONFIRMADO)
                .orElseThrow(() -> new EventoException("Estado CONFIRMADO no encontrado en BD"));

        evento.setEstadoEvento(estadoConfirmado);
        evento.setFechaConfirmacion(LocalDateTime.now());
        Evento guardado = eventoRepository.save(evento);
        log.info("evento_confirmado id={}", id);

        EventoAuditSnapshot despues = toAuditSnapshot(guardado);
        auditoriaService.registrar(CONFIRMAR, EVENTO, id, antes, despues,
                "Evento confirmado manualmente — anticipo mínimo validado");

        notificacionHandler.notificarConfirmacionManual(id, guardado);

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

        EventoAuditSnapshot antes = toAuditSnapshot(evento);

        EstadoEvento estadoConfirmado = estadoEventoRepository.findByNombre(CONFIRMADO)
                .orElseThrow(() -> new EventoException("Estado CONFIRMADO no encontrado en BD"));

        evento.setEstadoEvento(estadoConfirmado);
        evento.setFechaConfirmacion(LocalDateTime.now());
        eventoRepository.save(evento);

        EventoAuditSnapshot despues = toAuditSnapshot(evento);
        auditoriaService.registrar(CONFIRMAR, EVENTO, id, antes, despues,
                "Confirmación automática por anticipo mínimo alcanzado");

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

        boolean cobroCompleto = vistaPagosEventoRepository.findByIdEvento(id)
                .map(VistaPagosEvento::getCobroCompleto)
                .orElse(false);

        String estadoDestino = cobroCompleto ? FINALIZADO : PENDIENTE_COBRO;
        validarTransicion(evento, EN_MONTAJE, estadoDestino);

        stockHandler.consumirStock(evento);

        if (!cobroCompleto) {
            BigDecimal totalPagado = pagoValidator.getTotalPagado(id);
            notificacionHandler.notificarSaldoPendiente(id, evento, totalPagado);
        }

        EstadoEvento estado = estadoEventoRepository.findByNombre(estadoDestino)
                .orElseThrow(() -> new EventoException("Estado " + estadoDestino + " no encontrado en BD"));

        evento.setEstadoEvento(estado);
        Evento guardado = eventoRepository.save(evento);
        log.info("evento_finalizado id={} destino={}", id, estadoDestino);

        return eventoMapper.toResponseDTO(guardado);
    }

    /**
     * Transición PENDIENTE_COBRO → FINALIZADO cuando se completa el cobro.
     * <p>
     * Es seguro llamarlo aunque el evento no esté en PENDIENTE_COBRO o
     * el cobro no esté completo: simplemente no hace nada.
     */
    @Override
    @Transactional
    public void finalizarDesdeCobroCompleto(Long id) {
        Evento evento = obtenerOFallar(id);

        if (!evento.getEstadoEvento().getNombre().equals(PENDIENTE_COBRO)) {
            return;
        }

        boolean cobroCompleto = vistaPagosEventoRepository.findByIdEvento(id)
                .map(VistaPagosEvento::getCobroCompleto)
                .orElse(false);

        if (!cobroCompleto) {
            return;
        }

        EstadoEvento estadoFinalizado = estadoEventoRepository.findByNombre(FINALIZADO)
                .orElseThrow(() -> new EventoException("Estado FINALIZADO no encontrado en BD"));

        evento.setEstadoEvento(estadoFinalizado);
        eventoRepository.save(evento);
        log.info("evento_finalizado_desde_cobro id={}", id);
    }

    @Override
    @Transactional
    public EventoResponseDTO iniciarCancelacion(Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new EventoException("El motivo de cancelación es obligatorio");
        }

        Evento evento = obtenerOFallar(id);

        String estadoActual = evento.getEstadoEvento().getNombre();
        if (estadoActual.equals(COTIZACION)) {
            EstadoEvento estadoCancelado = estadoEventoRepository.findByNombre(CANCELADO)
                    .orElseThrow(() -> new EventoException("Estado CANCELADO no encontrado en BD"));
            evento.setEstadoEvento(estadoCancelado);
            evento.setMotivoCancelacion(motivo);
            evento.setFechaCancelacion(LocalDateTime.now());
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

        BigDecimal totalDevolucion = pagoCancelacionHandler.procesarPagosPorCancelacion(evento, evento.getMotivoCancelacion());

        EstadoEvento estadoCancelado = estadoEventoRepository.findByNombre(CANCELADO)
                .orElseThrow(() -> new EventoException(
                        "Estado CANCELADO no encontrado en BD"));

        stockHandler.liberarReservas(evento, evento.getMotivoCancelacion());
        evento.setEstadoEvento(estadoCancelado);
        evento.setEstadoAnterior(null);
        cronogramaService.cancelarPorEvento(id);
        eventoRepository.save(evento);

        if (totalDevolucion.compareTo(BigDecimal.ZERO) == 0) {
            notificacionHandler.notificarCancelacionConfirmada(id, evento, evento.getMotivoCancelacion());
        }

        log.info("evento_cancelacion_confirmada id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoResponseDTO> obtenerEventos() {
        return eventoRepository.findAllWithRelations().stream()
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
        return vistaEventosPeriodoRepository.findByFechaEventoBetweenOrderByFechaEventoAscHoraInicioAsc(inicio, fin)
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
    public boolean verificarDisponibilidad(LocalDate fecha,
                                           LocalTime horaInicio,
                                           LocalTime horaFin,
                                           String ubicacion,
                                           Long eventoIdExcluir) {
        validarHorario(horaInicio, horaFin);
        validarParametrosDisponibilidad(horaInicio, horaFin, ubicacion);

        return !existeSolapamiento(fecha, horaInicio, horaFin, ubicacion, eventoIdExcluir);
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
    @Transactional(readOnly = true)
    public EventoResumenInternoDTO getResumenInterno(Long id) {
        Evento evento = obtenerOFallar(id);

        EventoResumenInternoDTO dto = new EventoResumenInternoDTO();
        dto.setIdEvento(evento.getId());
        dto.setEstadoEvento(evento.getEstadoEvento() != null ? evento.getEstadoEvento().getNombre() : null);
        dto.setFecha(evento.getFecha());
        dto.setHoraInicio(evento.getHoraInicio());
        dto.setHoraFin(evento.getHoraFin());
        dto.setUbicacion(evento.getUbicacion());
        dto.setClienteNombre(evento.getCliente() != null ? evento.getCliente().getNombre() : null);
        dto.setDecoracionNombre(evento.getDecoracion() != null ? evento.getDecoracion().getNombre() : null);
        dto.setTipoEventoNombre(evento.getTipoEvento() != null ? evento.getTipoEvento().getTipoNombre() : null);
        dto.setNota(evento.getNota());
        dto.setNotaPrecio(evento.getNotaPrecio());
        dto.setPrecioBase(evento.getPrecioBase());
        dto.setPrecioAdicional(evento.getPrecioAdicional());
        dto.setPrecioTotal(evento.getPrecioTotal());
        dto.setFechaConfirmacion(evento.getFechaConfirmacion());
        dto.setResumenPagos(construirResumenPagos(id, evento));
        dto.setResumenGastos(construirResumenGastos(id));
        dto.setRentabilidad(construirRentabilidadOpcional(id));

        return dto;
    }

    @Override
    @Transactional
    public EventoResponseDTO actualizarEvento(Long id, EventoUpdateDTO dto) {
        Evento evento = obtenerOFallar(id);
        ModoEdicion modo = validarEventoEditable(evento);

        if (modo == ModoEdicion.PARCIAL) {
            validarCamposNoEditablesEnParcial(dto);
        }

        Cliente cliente = dto.getClienteId() != null
                ? clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new EventoException("Cliente no encontrado o inactivo"))
                : evento.getCliente();
        validarClienteActivo(cliente);

        Decoracion decoracion = dto.getDecoracionId() != null
                ? decoracionRepository.findById(dto.getDecoracionId())
                .orElseThrow(() -> new EventoException("Decoración no encontrada"))
                : evento.getDecoracion();
        validarDecoracionActiva(decoracion);
        validarTipoEventoActivo(evento.getTipoEvento());

        LocalDate fechaFinal = dto.getFecha() != null ? dto.getFecha() : evento.getFecha();
        LocalTime horaInicioFinal = dto.getHoraInicio() != null ? dto.getHoraInicio() : evento.getHoraInicio();
        LocalTime horaFinFinal = dto.getHoraFin() != null ? dto.getHoraFin() : evento.getHoraFin();
        String ubicacionFinal = dto.getUbicacion() != null ? dto.getUbicacion() : evento.getUbicacion();
        String notaPrecioFinal = dto.getNotaPrecio() != null ? dto.getNotaPrecio() : evento.getNotaPrecio();

        if (modo == ModoEdicion.COMPLETA) {
            validarFechaNoPasada(fechaFinal);
            validarHorario(horaInicioFinal, horaFinFinal);
            validarDisponibilidad(fechaFinal, horaInicioFinal, horaFinFinal, ubicacionFinal, evento.getId());
        } else {
            validarHorarioParcial(dto.getHoraInicio(), dto.getHoraFin());
        }
        validarNotaPrecioAdicional(evento.getPrecioAdicional(), notaPrecioFinal);

        eventoMapper.updateEntityFromDTO(evento, dto, cliente, decoracion);
        evento.setCliente(cliente);
        evento.setDecoracion(decoracion);
        evento.setPrecioBase(decoracion.getPrecio());
        Evento guardado = eventoRepository.save(evento);
        log.info("evento_actualizado id={} modo={}", id, modo);

        return eventoMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public EventoResponseDTO agregarExtras(Long id, EventoExtraRequestDTO dto) {
        Evento evento = obtenerOFallar(id);
        validarEventoPermiteExtras(evento);
        validarNotaPrecioExtra(dto.getNotaPrecio());
        validarMontoAdicionalExtra(dto.getMontoAdicional());

        EventoAuditSnapshot antes = toAuditSnapshot(evento);

        Map<Long, Integer> cantidadesPorProductoId = consolidarCantidadesExtra(dto);
        Map<Producto, Integer> productosExtra = obtenerProductosExtra(cantidadesPorProductoId);

        stockHandler.validarYReservarExtras(evento, productosExtra);

        BigDecimal precioAdicionalActual = evento.getPrecioAdicional() != null
                ? evento.getPrecioAdicional()
                : BigDecimal.ZERO;

        evento.setPrecioAdicional(precioAdicionalActual.add(dto.getMontoAdicional()));
        evento.setNotaPrecio(combinarNotaPrecio(evento.getNotaPrecio(), dto.getNotaPrecio()));

        Evento guardado = eventoRepository.save(evento);
        log.info("evento_extras_agregados id={} productos={} incremento={}",
                id, productosExtra.size(), dto.getMontoAdicional());

        EventoAuditSnapshot despues = toAuditSnapshot(guardado);
        auditoriaService.registrar(AGREGAR_EXTRAS, EVENTO, id, antes, despues,
                "Extras agregados: " + productosExtra.size() + " productos, incremento $" + dto.getMontoAdicional());

        return eventoMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoProductoDTO> obtenerProductosPorEvento(Long id) {
        Evento evento = obtenerOFallar(id);
        String estado = evento.getEstadoEvento().getNombre();

        if (COTIZACION.equals(estado)) {
            return obtenerProductosCotizados(evento);
        }

        return obtenerProductosReservados(evento);
    }

    private List<EventoProductoDTO> obtenerProductosCotizados(Evento evento) {
        List<DecoracionProducto> composicion = decoracionProductoRepository
                .findByDecoracionId(evento.getDecoracion().getId());

        return composicion.stream()
                .map(dp -> new EventoProductoDTO(
                        dp.getProducto().getId(),
                        dp.getProducto().getNombre(),
                        dp.getCantidadRequerida(),
                        dp.getProducto().getPrecioUnitario(),
                        "COTIZADO",
                        "DECORACION"
                ))
                .toList();
    }

    private List<EventoProductoDTO> obtenerProductosReservados(Evento evento) {
        List<Object[]> filas = eventoProductoRepository.findProductosByEventoId(evento.getId());

        return filas.stream()
                .map(fila -> new EventoProductoDTO(
                        ((Number) fila[0]).longValue(),
                        (String) fila[1],
                        (Integer) fila[2],
                        (BigDecimal) fila[3],
                        (String) fila[4],
                        (String) fila[5]
                ))
                .toList();
    }

    private Evento obtenerOFallar(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new EventoNotFoundException("Evento no encontrado con ID: " + id));
    }

    private void validarTransicion(Evento evento, String estadoRequerido, String destino) {
        String actual = evento.getEstadoEvento().getNombre();
        if (!actual.equals(estadoRequerido)) {
            throw new EventoException(
                    "Transición inválida: el evento está en " + actual +
                            ", se requiere " + estadoRequerido + " para pasar a " + destino);
        }
    }

    private void validarNotaPrecioAdicional(BigDecimal precioAdicional, String notaPrecio) {
        if (precioAdicional != null
                && precioAdicional.compareTo(BigDecimal.ZERO) > 0
                && (notaPrecio == null || notaPrecio.isBlank())) {
            throw new EventoException("La nota de precio es obligatoria cuando hay precio adicional");
        }
    }

    private void validarClienteActivo(Cliente cliente) {
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new EventoException("Cliente no encontrado o inactivo");
        }
    }

    private void validarDecoracionActiva(Decoracion decoracion) {
        if (!"ACTIVO".equals(decoracion.getEstado())) {
            throw new EventoException("La decoración seleccionada no está activa");
        }
    }

    private void validarTipoEventoActivo(TipoEvento tipoEvento) {
        if (!Boolean.TRUE.equals(tipoEvento.getTipoActivo())) {
            throw new EventoException("Tipo de evento no encontrado o inactivo");
        }
    }

    private void validarFechaNoPasada(LocalDate fecha) {
        if (fecha.isBefore(LocalDate.now())) {
            throw new EventoException("No se puede crear o editar un evento en el pasado");
        }
    }

    private void validarHorario(LocalTime horaInicio, LocalTime horaFin) {
        if (horaInicio == null && horaFin == null) {
            return;
        }

        if (horaInicio == null || horaFin == null) {
            throw new EventoException("Hora inicio y hora fin son obligatorias juntas");
        }

        if (!horaFin.isAfter(horaInicio)) {
            throw new EventoException("La hora fin debe ser posterior a la hora inicio");
        }
    }

    private void validarDisponibilidad(LocalDate fecha,
                                       LocalTime horaInicio,
                                       LocalTime horaFin,
                                       String ubicacion,
                                       Long eventoIdExcluir) {
        if (horaInicio == null && horaFin == null && (ubicacion == null || ubicacion.isBlank())) {
            return;
        }

        validarParametrosDisponibilidad(horaInicio, horaFin, ubicacion);

        boolean existeSolapamiento = existeSolapamiento(
                fecha, horaInicio, horaFin, ubicacion, eventoIdExcluir);

        if (existeSolapamiento) {
            throw new EventoException("Ya existe un evento solapado en esa fecha y horario");
        }
    }

    private boolean existeSolapamiento(LocalDate fecha,
                                       LocalTime horaInicio,
                                       LocalTime horaFin,
                                       String ubicacion,
                                       Long eventoIdExcluir) {
        int gapMinutos;
        try {
            gapMinutos = configuracionService.getInt("MINUTOS_GAP_ENTRE_EVENTOS");
        } catch (Exception e) {
            gapMinutos = 0;
        }
        gapMinutos = Math.max(gapMinutos, MINIMO_GAP_ENTRE_EVENTOS_MINUTOS);
        LocalTime horaInicioConGap = horaInicio.minusMinutes(gapMinutos);
        LocalTime horaFinConGap = horaFin.plusMinutes(gapMinutos);

        return eventoIdExcluir == null
                ? eventoRepository.existeSolapamiento(fecha, horaInicioConGap, horaFinConGap, ubicacion)
                : eventoRepository.existeSolapamientoExcluyendoEvento(
                        eventoIdExcluir, fecha, horaInicioConGap, horaFinConGap, ubicacion);
    }

    private enum ModoEdicion { COMPLETA, PARCIAL }

    private ModoEdicion validarEventoEditable(Evento evento) {
        String estado = evento.getEstadoEvento().getNombre();
        if (COTIZACION.equals(estado)) {
            return ModoEdicion.COMPLETA;
        }
        if (PROGRAMADO.equals(estado)) {
            return ModoEdicion.PARCIAL;
        }
        throw new EventoException(
                "Solo se pueden editar eventos en estado COTIZACION o PROGRAMADO");
    }

    private void validarCamposNoEditablesEnParcial(EventoUpdateDTO dto) {
        if (dto.getClienteId() != null || dto.getDecoracionId() != null
                || dto.getFecha() != null || dto.getHoraInicio() != null
                || dto.getHoraFin() != null) {
            throw new EventoException(
                    "En estado PROGRAMADO solo se pueden editar ubicación, nota y motivo del precio adicional");
        }
    }

    private void validarEventoPermiteExtras(Evento evento) {
        String estado = evento.getEstadoEvento().getNombre();
        if (!PROGRAMADO.equals(estado) && !CONFIRMADO.equals(estado)) {
            throw new EventoException(
                    "Solo se pueden agregar extras a eventos en estado PROGRAMADO o CONFIRMADO");
        }
    }

    private void validarNotaPrecioExtra(String notaPrecio) {
        if (notaPrecio == null || notaPrecio.isBlank()) {
            throw new EventoException("La nota de precio es obligatoria");
        }
    }

    private void validarMontoAdicionalExtra(BigDecimal montoAdicional) {
        if (montoAdicional == null) {
            throw new EventoException("El monto adicional es obligatorio");
        }

        if (montoAdicional.compareTo(BigDecimal.ZERO) <= 0) {
            throw new EventoException("El monto adicional debe ser mayor a 0");
        }
    }

    private Map<Long, Integer> consolidarCantidadesExtra(EventoExtraRequestDTO dto) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();

        for (EventoExtraItemRequestDTO item : dto.getItems()) {
            cantidades.merge(item.getProductoId(), item.getCantidad(), Integer::sum);
        }

        return cantidades;
    }

    private Map<Producto, Integer> obtenerProductosExtra(Map<Long, Integer> cantidadesPorProductoId) {
        Map<Producto, Integer> productosExtra = new LinkedHashMap<>();

        for (Map.Entry<Long, Integer> entry : cantidadesPorProductoId.entrySet()) {
            Producto producto = productoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new EventoException("Producto no encontrado o inactivo"));

            if (!"ACTIVO".equals(producto.getEstado())) {
                throw new EventoException("Producto no encontrado o inactivo");
            }

            productosExtra.put(producto, entry.getValue());
        }

        return productosExtra;
    }

    private String combinarNotaPrecio(String notaActual, String notaExtra) {
        if (notaActual == null || notaActual.isBlank()) {
            return notaExtra.trim();
        }

        return notaActual + " | " + notaExtra.trim();
    }

    private void validarParametrosDisponibilidad(LocalTime horaInicio,
                                                 LocalTime horaFin,
                                                 String ubicacion) {
        if (horaInicio == null || horaFin == null || ubicacion == null || ubicacion.isBlank()) {
            throw new EventoException(
                    "Para validar disponibilidad debe indicar horaInicio, horaFin y ubicación");
        }
    }

    private void validarHorarioParcial(LocalTime horaInicio, LocalTime horaFin) {
        if ((horaInicio == null) == (horaFin == null)) {
            return;
        }

        throw new EventoException("No se permite actualizar solo una parte del horario");
    }

    private EventoAuditSnapshot toAuditSnapshot(Evento e) {
        return new EventoAuditSnapshot(
                e.getId(),
                e.getEstadoEvento() != null ? e.getEstadoEvento().getNombre() : null,
                e.getPrecioBase(),
                e.getPrecioAdicional(),
                e.getPrecioTotal(),
                e.getFecha(),
                e.getHoraInicio(),
                e.getHoraFin(),
                e.getUbicacion(),
                e.getMotivoCancelacion());
    }

    private ResumenPagoEventoDTO construirResumenPagos(Long eventoId, Evento evento) {
        return vistaPagosEventoRepository.findByIdEvento(eventoId)
                .map(vista -> new ResumenPagoEventoDTO(
                        vista.getIdEvento(),
                        vista.getPrecioTotal(),
                        vista.getTotalCobrado(),
                        vista.getSaldoPendiente(),
                        vista.getCobroCompleto(),
                        vista.getTotalDevolucionPendiente(),
                        vista.getTieneDevolucionPendiente()))
                .orElseGet(() -> new ResumenPagoEventoDTO(
                        eventoId,
                        evento.getPrecioTotal(),
                        BigDecimal.ZERO,
                        evento.getPrecioTotal(),
                        false,
                        BigDecimal.ZERO,
                        false));
    }

    private ResumenGastoEventoDTO construirResumenGastos(Long eventoId) {
        BigDecimal totalProyectado = gastoEventoRepository.sumMontoByEventoIdAndEstado(eventoId, PROYECTADO);
        BigDecimal totalDesembolsado = gastoEventoRepository.sumMontoByEventoIdAndEstado(eventoId, DESEMBOLSADO);
        return new ResumenGastoEventoDTO(totalProyectado, totalDesembolsado, totalProyectado);
    }

    private RentabilidadEventoDTO construirRentabilidadOpcional(Long eventoId) {
        return vistaRentabilidadEventoRepository.findByIdEvento(eventoId)
                .map(vista -> new RentabilidadEventoDTO(
                        vista.getIdEvento(),
                        vista.getFechaEvento(),
                        vista.getEstado(),
                        vista.getPrecioAcordado(),
                        vista.getIngresoCobrado(),
                        vista.getCostoMateriales(),
                        vista.getCostoManoObra(),
                        vista.getGastosDesembolsados(),
                        vista.getGastosProyectados(),
                        vista.getGananciaNeta()))
                .orElse(null);
    }
}
