package com.catolica.Artepax.evento.service.impl;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.cronograma.service.CronogramaService;
import com.catolica.Artepax.evento.dto.CalendarioEventoDTO;
import com.catolica.Artepax.evento.dto.EventoRequestDTO;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;
import com.catolica.Artepax.evento.dto.EventoUpdateDTO;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.mapper.EventoMapper;
import com.catolica.Artepax.evento.model.*;
import com.catolica.Artepax.evento.repository.*;
import com.catolica.Artepax.evento.service.EventoService;
import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.clientes.repository.ClienteRepository;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionProductoRepository;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionRepository;
import com.catolica.Artepax.inventario.movimiento.entity.MovimientoInventario;
import com.catolica.Artepax.inventario.movimiento.repository.MovimientoInventarioRepository;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.exception.StockInsuficienteException;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import com.catolica.Artepax.inventario.producto.repository.StockProductoRepository;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.stream.Collectors;
import com.catolica.Artepax.inventario.producto.entity.StockProducto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class EventoServiceImpl implements EventoService {


    private final EventoRepository eventoRepository;
    private final ClienteRepository clienteRepository;
    private final DecoracionRepository decoracionRepository;
    private final DecoracionProductoRepository decoracionProductoRepository;
    private final EstadoEventoRepository estadoEventoRepository;
    private final EventoProductoRepository eventoProductoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final EventoMapper eventoMapper;
    private final ConfiguracionService configuracionService;
    private final StockProductoRepository stockProductoRepository;
    private final CronogramaService cronogramaService;
    private final NotificacionService notificacionService;
    private final VistaEventosPeriodoRepository vistaEventosPeriodoRepository;



    public EventoServiceImpl(EventoRepository eventoRepository,
                             ClienteRepository clienteRepository,
                             DecoracionRepository decoracionRepository,
                             DecoracionProductoRepository decoracionProductoRepository,
                             EstadoEventoRepository estadoEventoRepository,
                             EventoProductoRepository eventoProductoRepository,
                             MovimientoInventarioRepository movimientoRepository,
                             ProductoRepository productoRepository,
                             TipoEventoRepository tipoEventoRepository,
                             EventoMapper eventoMapper,
                             ConfiguracionService configuracionService,
                             StockProductoRepository stockProductoRepository,
                             @Lazy CronogramaService cronogramaService,
                             @Lazy NotificacionService notificacionService,
                             VistaEventosPeriodoRepository vistaEventosPeriodoRepository
    ) {
        this.eventoRepository = eventoRepository;
        this.clienteRepository = clienteRepository;
        this.decoracionRepository = decoracionRepository;
        this.decoracionProductoRepository = decoracionProductoRepository;
        this.estadoEventoRepository = estadoEventoRepository;
        this.eventoProductoRepository = eventoProductoRepository;
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.tipoEventoRepository = tipoEventoRepository;
        this.eventoMapper = eventoMapper;
        this.configuracionService = configuracionService;
        this.stockProductoRepository = stockProductoRepository;
        this.cronogramaService = cronogramaService;
        this.notificacionService = notificacionService;
        this.vistaEventosPeriodoRepository = vistaEventosPeriodoRepository;
    }

    // ─── CREAR ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public EventoResponseDTO crearEvento(EventoRequestDTO dto) {

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .filter(c -> c.getActivo())
                .orElseThrow(() -> new EventoException("Cliente no encontrado o inactivo"));

        Decoracion decoracion = decoracionRepository.findById(dto.getDecoracionId())
                .orElseThrow(() -> new EventoException("Decoración no encontrada"));

        TipoEvento tipoEvento = tipoEventoRepository.findById(dto.getTipoEventoId())
                .filter(t -> t.getTipoActivo())
                .orElseThrow(() -> new EventoException("Tipo de evento no encontrado o inactivo"));

        if (dto.getFecha().isBefore(LocalDate.now())) {
            throw new EventoException("No se puede crear un evento en el pasado");
        }

        if (eventoRepository.existsByFechaAndHoraInicioAndUbicacion(
                dto.getFecha(), dto.getHoraInicio(), dto.getUbicacion())) {
            throw new EventoException("Ya existe un evento en esa fecha, hora y ubicación");
        }

        EstadoEvento estadoCotizacion = estadoEventoRepository.findByNombre("COTIZACION")
                .orElseThrow(() -> new EventoException("Estado COTIZACION no encontrado en BD"));

        BigDecimal precioBase = decoracion.getPrecio();
        BigDecimal precioAdicional = dto.getPrecioAdicional() != null
                ? dto.getPrecioAdicional() : BigDecimal.ZERO;

        int diasExpiracion = configuracionService.getInt("DIAS_EXPIRACION_COTIZACION");

        Evento evento = eventoMapper.toEntity(dto, cliente, decoracion);
        evento.setTipoEvento(tipoEvento);
        evento.setEstadoEvento(estadoCotizacion);
        evento.setPrecioBase(precioBase);
        evento.setPrecioAdicional(precioAdicional);
        evento.setFechaExpiracion(LocalDate.now().plusDays(diasExpiracion));
        evento.setClienteNotificado(false);

        // En COTIZACION NO se toca el stock
        return eventoMapper.toResponseDTO(eventoRepository.save(evento));
    }

    // ─── PASAR A PROGRAMADO ───────────────────────────────────────────────────
    @Override
    @Transactional
    public EventoResponseDTO programarEvento(Long id) {
        Evento evento = obtenerOFallar(id);
        validarTransicion(evento, "COTIZACION", "PROGRAMADO");

        List<DecoracionProducto> productosRequeridos =
                decoracionProductoRepository.findByDecoracionId(evento.getDecoracion().getId());

        // Una sola query a la vista — fuente de verdad del stock
        List<Long> productoIds = productosRequeridos.stream()
                .map(dp -> dp.getProducto().getId())
                .toList();

        Map<Long, StockProducto> stockMap = stockProductoRepository
                .findAllByIdProductoIn(productoIds)
                .stream()
                .collect(Collectors.toMap(StockProducto::getIdProducto, s -> s));

        // Validar disponibilidad completa antes de reservar nada
        for (DecoracionProducto dp : productosRequeridos) {
            StockProducto stock = stockMap.get(dp.getProducto().getId());
            int disponible = stock != null ? stock.getCantidadDisponible() : 0;
            if (disponible < dp.getCantidadRequerida()) {
                throw new StockInsuficienteException(
                        dp.getProducto().getNombre(),
                        disponible,
                        dp.getCantidadRequerida());
            }
        }

        EstadoEvento estadoProgramado = estadoEventoRepository.findByNombre("PROGRAMADO")
                .orElseThrow(() -> new EventoException("Estado PROGRAMADO no encontrado en BD"));

        // Crear registros en evento_producto — cantidad_total NO se toca
        for (DecoracionProducto dp : productosRequeridos) {
            EventoProducto ep = new EventoProducto();
            ep.setId(new EventoProductoId(evento.getId(), dp.getProducto().getId()));
            ep.setEvento(evento);
            ep.setProducto(dp.getProducto());
            ep.setCantidadReservada(dp.getCantidadRequerida());
            eventoProductoRepository.save(ep);
        }

        evento.setEstadoEvento(estadoProgramado);
        return eventoMapper.toResponseDTO(eventoRepository.save(evento));
    }

    // ─── CONFIRMAR MANUALMENTE ────────────────────────────────────────────────
    @Override
    @Transactional
    public EventoResponseDTO confirmarEvento(Long id) {
        Evento evento = obtenerOFallar(id);
        validarTransicion(evento, "PROGRAMADO", "CONFIRMADO");

        EstadoEvento estadoConfirmado = estadoEventoRepository.findByNombre("CONFIRMADO")
                .orElseThrow(() -> new EventoException("Estado CONFIRMADO no encontrado en BD"));

        evento.setEstadoEvento(estadoConfirmado);
        evento.setFechaConfirmacion(LocalDateTime.now());
        return eventoMapper.toResponseDTO(eventoRepository.save(evento));
    }

    // ─── CONFIRMAR DESDE ANTICIPO (llamado por PagoService) ──────────────────
    @Override
    @Transactional
    public void confirmarDesdeAnticipo(Long id) {
        Evento evento = obtenerOFallar(id);
        if (!evento.getEstadoEvento().getNombre().equals("PROGRAMADO")) return;

        EstadoEvento estadoConfirmado = estadoEventoRepository.findByNombre("CONFIRMADO")
                .orElseThrow(() -> new EventoException("Estado CONFIRMADO no encontrado en BD"));

        evento.setEstadoEvento(estadoConfirmado);
        evento.setFechaConfirmacion(LocalDateTime.now());
        eventoRepository.save(evento);
        // Notificar confirmación de reserva
        notificacionService.notificarAdmins(
                "CONFIRMACION_RESERVA",
                "El evento #" + id + " - " + evento.getCliente().getNombre() +
                        " alcanzó el anticipo mínimo y fue confirmado automáticamente.",
                evento);
    }

    // ─── PASAR A EN_MONTAJE ───────────────────────────────────────────────────
    @Override
    @Transactional
    public EventoResponseDTO iniciarMontaje(Long id) {
        Evento evento = obtenerOFallar(id);
        validarTransicion(evento, "CONFIRMADO", "EN_MONTAJE");

        EstadoEvento estadoMontaje = estadoEventoRepository.findByNombre("EN_MONTAJE")
                .orElseThrow(() -> new EventoException("Estado EN_MONTAJE no encontrado en BD"));

        evento.setEstadoEvento(estadoMontaje);
        eventoRepository.save(evento);

        // Notificar inicio de montaje
        notificacionService.notificarAdmins(
                "INICIO_MONTAJE",
                "El evento #" + id + " - " + evento.getCliente().getNombre() +
                        " inició la fase de montaje. Fecha: " + evento.getFecha(),
                evento);

        return eventoMapper.toResponseDTO(evento);
    }

    // ─── FINALIZAR ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public EventoResponseDTO finalizarEvento(Long id) {
        Evento evento = obtenerOFallar(id);
        validarTransicion(evento, "EN_MONTAJE", "FINALIZADO");

        List<EventoProducto> reservas = eventoProductoRepository.findByEventoId(id);
        for (EventoProducto reserva : reservas) {
            Producto producto = reserva.getProducto();

            // Aquí SÍ se resta de cantidad_total — el producto se consumió físicamente
            producto.setCantidadTotal(
                    producto.getCantidadTotal() - reserva.getCantidadReservada());
            productoRepository.save(producto);

            // Movimiento SALIDA/EVENTO — obligatorio por constraint de BD
            registrarMovimientoEvento(
                    producto,
                    evento,
                    reserva.getCantidadReservada(),
                    "Salida por evento finalizado ID: " + id);
        }

        EstadoEvento estadoFinalizado = estadoEventoRepository.findByNombre("FINALIZADO")
                .orElseThrow(() -> new EventoException("Estado FINALIZADO no encontrado en BD"));

        evento.setEstadoEvento(estadoFinalizado);
        return eventoMapper.toResponseDTO(eventoRepository.save(evento));
    }

    // ─── INICIAR CANCELACION ──────────────────────────────────────────────────
    @Override
    @Transactional
    public EventoResponseDTO iniciarCancelacion(Long id, String motivo) {
        Evento evento = obtenerOFallar(id);

        String estadoActual = evento.getEstadoEvento().getNombre();
        // Si es COTIZACION → cancelación directa, sin tiempo de gracia
        if (estadoActual.equals("COTIZACION")) {
            EstadoEvento estadoCancelado = estadoEventoRepository.findByNombre("CANCELADO")
                    .orElseThrow(() -> new EventoException("Estado CANCELADO no encontrado en BD"));
            evento.setEstadoEvento(estadoCancelado);
            evento.setMotivoCancelacion(motivo);
            return eventoMapper.toResponseDTO(eventoRepository.save(evento));
        }

        // Para el resto, flujo normal con tiempo de gracia
        if (!List.of("PROGRAMADO", "CONFIRMADO", "EN_MONTAJE").contains(estadoActual)) {
            throw new EventoException(
                    "No se puede cancelar un evento en estado: " + estadoActual);
        }

        EstadoEvento estadoCancelacionPendiente = estadoEventoRepository
                .findByNombre("CANCELACION_PENDIENTE")
                .orElseThrow(() -> new EventoException(
                        "Estado CANCELACION_PENDIENTE no encontrado en BD"));

        evento.setEstadoAnterior(evento.getEstadoEvento());
        evento.setEstadoEvento(estadoCancelacionPendiente);
        evento.setMotivoCancelacion(motivo);
        evento.setFechaCancelacion(LocalDateTime.now());

        // Guardar PRIMERO
        Evento guardado = eventoRepository.save(evento);

        // Notificar cancelación iniciada
        notificacionService.notificarAdmins(
                "CANCELACION_EVENTO",
                "El evento #" + id + " - " + evento.getCliente().getNombre() +
                        " inició proceso de cancelación. Motivo: " + motivo +
                        ". Tiene tiempo de gracia para revertir.",
                evento);

        return eventoMapper.toResponseDTO(guardado);
    }

    // REVERTIR CANCELACION
    @Override
    @Transactional
    public EventoResponseDTO revertirCancelacion(Long id) {
        Evento evento = obtenerOFallar(id);

        if (!evento.getEstadoEvento().getNombre().equals("CANCELACION_PENDIENTE")) {
            throw new EventoException("Solo se puede revertir desde CANCELACION_PENDIENTE");
        }
        if (evento.getEstadoAnterior() == null) {
            throw new EventoException("No hay estado anterior para restaurar");
        }

        evento.setEstadoEvento(evento.getEstadoAnterior());
        evento.setEstadoAnterior(null);
        evento.setMotivoCancelacion(null);
        evento.setFechaCancelacion(null);

        return eventoMapper.toResponseDTO(eventoRepository.save(evento));
    }

    // ─── CONFIRMAR CANCELACION ────────────────────────────────────────────────
    @Override
    @Transactional
    public void confirmarCancelacion(Long id) {
        Evento evento = obtenerOFallar(id);
        validarTransicion(evento, "CANCELACION_PENDIENTE", "CANCELADO");

        // NO se elimina evento_producto — queda como historial
        // La vista_stock_producto ignora eventos CANCELADOS automáticamente
        // NO se toca cantidad_total — los productos nunca salieron físicamente

        EstadoEvento estadoCancelado = estadoEventoRepository.findByNombre("CANCELADO")
                .orElseThrow(() -> new EventoException(
                        "Estado CANCELADO no encontrado en BD"));

        evento.setEstadoEvento(estadoCancelado);
        evento.setEstadoAnterior(null);
        cronogramaService.cancelarPorEvento(id);
        eventoRepository.save(evento);
    }

    // ─── CONSULTAS ────────────────────────────────────────────────────────────
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
    @Transactional
    public EventoResponseDTO actualizarEvento(Long id, EventoUpdateDTO dto) {
        Evento evento = obtenerOFallar(id);

        if (!evento.getEstadoEvento().getNombre().equals("COTIZACION")) {
            throw new EventoException("Solo se pueden editar eventos en estado COTIZACION");
        }

        Cliente cliente = dto.getClienteId() != null
                ? clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new EventoException("Cliente no encontrado"))
                : null;

        Decoracion decoracion = dto.getDecoracionId() != null
                ? decoracionRepository.findById(dto.getDecoracionId())
                .orElseThrow(() -> new EventoException("Decoración no encontrada"))
                : null;

        eventoMapper.updateEntityFromDTO(evento, dto, cliente, decoracion);
        return eventoMapper.toResponseDTO(eventoRepository.save(evento));
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────
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

    // Movimiento genérico (REPOSICION, AJUSTE_MANUAL, DAÑO, PERDIDA)
    private void registrarMovimiento(Producto producto, String tipo,
                                     String origen, int cantidad, String motivo) {
        MovimientoInventario m = new MovimientoInventario();
        m.setProducto(producto);
        m.setTipoMovimiento(tipo);
        m.setTipoOrigen(origen);
        m.setCantidad(cantidad);
        m.setMotivo(motivo);
        movimientoRepository.save(m);
    }

    // Movimiento de evento finalizado — tipo_origen=EVENTO, FK obligatoria por constraint BD
    private void registrarMovimientoEvento(Producto producto, Evento evento,
                                           int cantidad, String motivo) {
        MovimientoInventario m = new MovimientoInventario();
        m.setProducto(producto);
        m.setTipoMovimiento("SALIDA");
        m.setTipoOrigen("EVENTO");
        m.setEvento(evento);
        m.setCantidad(cantidad);
        m.setMotivo(motivo);
        movimientoRepository.save(m);
    }
}
