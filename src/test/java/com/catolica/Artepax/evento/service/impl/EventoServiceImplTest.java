package com.catolica.Artepax.evento.service.impl;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.clientes.repository.ClienteRepository;
import com.catolica.Artepax.common.service.AuditoriaService;
import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.cronograma.service.CronogramaService;
import com.catolica.Artepax.evento.dto.EventoRequestDTO;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;
import com.catolica.Artepax.evento.dto.EventoExtraItemRequestDTO;
import com.catolica.Artepax.evento.dto.EventoExtraRequestDTO;
import com.catolica.Artepax.evento.dto.EventoUpdateDTO;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.mapper.EventoMapper;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.evento.repository.EstadoEventoRepository;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.repository.TipoEventoRepository;
import com.catolica.Artepax.evento.repository.VistaEventosPeriodoRepository;
import com.catolica.Artepax.evento.repository.VistaRentabilidadEventoRepository;
import com.catolica.Artepax.evento.service.handler.EventoNotificacionHandler;
import com.catolica.Artepax.evento.service.handler.EventoPagoValidator;
import com.catolica.Artepax.evento.service.handler.EventoStockHandler;
import com.catolica.Artepax.pago.service.handler.PagoCancelacionHandler;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionRepository;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.catolica.Artepax.evento.model.EstadoEventoNames.CANCELADO;
import static com.catolica.Artepax.evento.model.EstadoEventoNames.CANCELACION_PENDIENTE;
import static com.catolica.Artepax.evento.model.EstadoEventoNames.COTIZACION;
import static com.catolica.Artepax.evento.model.EstadoEventoNames.FINALIZADO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoServiceImplTest {

    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private DecoracionRepository decoracionRepository;
    @Mock
    private EstadoEventoRepository estadoEventoRepository;
    @Mock
    private TipoEventoRepository tipoEventoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ConfiguracionService configuracionService;
    @Mock
    private CronogramaService cronogramaService;
    @Mock
    private VistaRentabilidadEventoRepository vistaRentabilidadEventoRepository;
    @Mock
    private VistaEventosPeriodoRepository vistaEventosPeriodoRepository;
    @Mock
    private EventoStockHandler stockHandler;
    @Mock
    private EventoNotificacionHandler notificacionHandler;
    @Mock
    private EventoPagoValidator pagoValidator;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private PagoCancelacionHandler pagoCancelacionHandler;

    private EventoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventoServiceImpl(
                eventoRepository,
                clienteRepository,
                decoracionRepository,
                estadoEventoRepository,
                tipoEventoRepository,
                productoRepository,
                new EventoMapper(),
                configuracionService,
                cronogramaService,
                vistaRentabilidadEventoRepository,
                vistaEventosPeriodoRepository,
                stockHandler,
                notificacionHandler,
                pagoValidator,
                auditoriaService,
                pagoCancelacionHandler);

        lenient().when(configuracionService.getInt("MINUTOS_GAP_ENTRE_EVENTOS")).thenReturn(0);
    }

    @Test
    void crearEventoFallaSiHayPrecioAdicionalSinNotaPrecio() {
        EventoRequestDTO dto = crearRequestBase();
        dto.setPrecioAdicional(new BigDecimal("100.00"));
        dto.setNotaPrecio("   ");

        assertThrows(EventoException.class, () -> service.crearEvento(dto));

        verify(clienteRepository, never()).findById(1L);
    }

    @Test
    void crearEventoSolapadoMismaUbicacionLanzaError() {
        EventoRequestDTO dto = crearRequestBase();
        stubCatalogosActivosCreacion();
        when(eventoRepository.existeSolapamiento(eq(dto.getFecha()), any(LocalTime.class), any(LocalTime.class), eq(dto.getUbicacion())))
                .thenReturn(true);

        assertThrows(EventoException.class, () -> service.crearEvento(dto));

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void crearEventoMismoDiaSinSolapamientoPermitido() {
        EventoRequestDTO dto = crearRequestBase();
        stubCatalogosActivosCreacion();
        when(eventoRepository.existeSolapamiento(eq(dto.getFecha()), any(LocalTime.class), any(LocalTime.class), eq(dto.getUbicacion())))
                .thenReturn(false);
        when(configuracionService.getInt("DIAS_EXPIRACION_COTIZACION")).thenReturn(7);
        when(estadoEventoRepository.findByNombre(COTIZACION)).thenReturn(Optional.of(crearEstadoEvento(COTIZACION)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoResponseDTO response = service.crearEvento(dto);

        assertEquals(dto.getFecha(), response.getFecha());
        assertEquals(dto.getHoraInicio(), response.getHoraInicio());
        assertEquals(new BigDecimal("2500.00"), response.getPrecioBase());
        verify(eventoRepository).save(any(Evento.class));
    }

    @Test
    void crearEventoMismaHoraOtraUbicacionPermitido() {
        EventoRequestDTO dto = crearRequestBase();
        dto.setUbicacion("Otro salon");
        stubCatalogosActivosCreacion();
        when(eventoRepository.existeSolapamiento(eq(dto.getFecha()), any(LocalTime.class), any(LocalTime.class), eq(dto.getUbicacion())))
                .thenReturn(false);
        when(configuracionService.getInt("DIAS_EXPIRACION_COTIZACION")).thenReturn(7);
        when(estadoEventoRepository.findByNombre(COTIZACION)).thenReturn(Optional.of(crearEstadoEvento(COTIZACION)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoResponseDTO response = service.crearEvento(dto);

        assertEquals("Otro salon", response.getUbicacion());
        verify(eventoRepository).existeSolapamiento(eq(dto.getFecha()), any(LocalTime.class), any(LocalTime.class), eq("Otro salon"));
    }

    @Test
    void actualizarEventoEnProgramadoSoloPermiteUbicacionYNota() {
        Evento evento = crearEventoBase("PROGRAMADO");

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

        EventoUpdateDTO dto = new EventoUpdateDTO();
        dto.setFecha(LocalDate.now().plusDays(5));

        assertThrows(EventoException.class, () -> service.actualizarEvento(10L, dto));

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void actualizarEventoEnProgramadoAceptaUbicacion() {
        Evento evento = crearEventoBase("PROGRAMADO");
        evento.setId(10L);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoUpdateDTO dto = new EventoUpdateDTO();
        dto.setUbicacion("Nuevo salon");

        EventoResponseDTO response = service.actualizarEvento(10L, dto);

        assertEquals("Nuevo salon", response.getUbicacion());
    }

    @Test
    void actualizarEventoEnCotizacionFechaPasadaLanzaError() {
        Evento evento = crearEventoBase(COTIZACION);
        EventoUpdateDTO dto = new EventoUpdateDTO();
        dto.setFecha(LocalDate.now().minusDays(1));

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

        assertThrows(EventoException.class, () -> service.actualizarEvento(10L, dto));

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void actualizarEventoHorarioInvalidoLanzaError() {
        Evento evento = crearEventoBase(COTIZACION);
        EventoUpdateDTO dto = new EventoUpdateDTO();
        dto.setHoraInicio(LocalTime.of(15, 0));
        dto.setHoraFin(LocalTime.of(14, 0));

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

        assertThrows(EventoException.class, () -> service.actualizarEvento(10L, dto));

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void actualizarEventoCambiaDecoracionRecalculaPrecioBase() {
        Evento evento = crearEventoBase(COTIZACION);
        EventoUpdateDTO dto = new EventoUpdateDTO();
        dto.setDecoracionId(99L);
        dto.setFecha(evento.getFecha());
        dto.setHoraInicio(evento.getHoraInicio());
        dto.setHoraFin(evento.getHoraFin());
        dto.setUbicacion(evento.getUbicacion());

        Decoracion nuevaDecoracion = crearDecoracion("Premium", new BigDecimal("3200.00"), "ACTIVO");

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(decoracionRepository.findById(99L)).thenReturn(Optional.of(nuevaDecoracion));
        when(eventoRepository.existeSolapamientoExcluyendoEvento(eq(10L), eq(dto.getFecha()), any(LocalTime.class), any(LocalTime.class), eq(dto.getUbicacion())))
                .thenReturn(false);
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoResponseDTO response = service.actualizarEvento(10L, dto);

        assertEquals(new BigDecimal("3200.00"), evento.getPrecioBase());
        assertEquals(new BigDecimal("3200.00"), response.getPrecioBase());
        assertEquals("Premium", response.getDecoracionNombre());
    }

    @Test
    void actualizarEventoSolapadoMismaUbicacionLanzaError() {
        Evento evento = crearEventoBase(COTIZACION);
        EventoUpdateDTO dto = crearUpdateAgenda(evento.getFecha(), LocalTime.of(12, 0), LocalTime.of(15, 0), evento.getUbicacion());

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(eventoRepository.existeSolapamientoExcluyendoEvento(eq(10L), eq(dto.getFecha()), any(LocalTime.class), any(LocalTime.class), eq(dto.getUbicacion())))
                .thenReturn(true);

        assertThrows(EventoException.class, () -> service.actualizarEvento(10L, dto));

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void actualizarEventoMismoDiaSinSolapamientoPermitido() {
        Evento evento = crearEventoBase(COTIZACION);
        EventoUpdateDTO dto = crearUpdateAgenda(evento.getFecha(), LocalTime.of(16, 0), LocalTime.of(18, 0), evento.getUbicacion());

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(eventoRepository.existeSolapamientoExcluyendoEvento(eq(10L), eq(dto.getFecha()), any(LocalTime.class), any(LocalTime.class), eq(dto.getUbicacion())))
                .thenReturn(false);
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoResponseDTO response = service.actualizarEvento(10L, dto);

        assertEquals(LocalTime.of(16, 0), response.getHoraInicio());
        assertEquals(LocalTime.of(18, 0), response.getHoraFin());
        verify(eventoRepository).existeSolapamientoExcluyendoEvento(eq(10L), eq(dto.getFecha()), any(LocalTime.class), any(LocalTime.class), eq(dto.getUbicacion()));
    }

    @Test
    void actualizarEventoMismaHoraOtraUbicacionPermitido() {
        Evento evento = crearEventoBase(COTIZACION);
        EventoUpdateDTO dto = crearUpdateAgenda(evento.getFecha(), evento.getHoraInicio(), evento.getHoraFin(), "Jardin norte");

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(eventoRepository.existeSolapamientoExcluyendoEvento(eq(10L), eq(dto.getFecha()), any(LocalTime.class), any(LocalTime.class), eq(dto.getUbicacion())))
                .thenReturn(false);
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoResponseDTO response = service.actualizarEvento(10L, dto);

        assertEquals("Jardin norte", response.getUbicacion());
    }

    @Test
    void iniciarCancelacionDirectaEnCotizacionGuardaFechaCancelacion() {
        Evento evento = crearEventoBase(COTIZACION);
        EstadoEvento estadoCancelado = crearEstadoEvento(CANCELADO);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(estadoEventoRepository.findByNombre(CANCELADO)).thenReturn(Optional.of(estadoCancelado));
        when(eventoRepository.save(evento)).thenReturn(evento);

        EventoResponseDTO response = service.iniciarCancelacion(10L, "Cliente desistió");

        assertNotNull(evento.getFechaCancelacion());
        assertNotNull(response.getFechaCancelacion());
    }

    @Test
    void confirmarCancelacionLiberaReservasActivasSinBorrarlas() {
        Evento evento = crearEventoBase(CANCELACION_PENDIENTE);
        evento.setMotivoCancelacion("Cliente canceló");
        evento.setEstadoAnterior(crearEstadoEvento("CONFIRMADO"));
        EstadoEvento estadoCancelado = crearEstadoEvento(CANCELADO);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(estadoEventoRepository.findByNombre(CANCELADO)).thenReturn(Optional.of(estadoCancelado));

        service.confirmarCancelacion(10L);

        verify(stockHandler).liberarReservas(evento, "Cliente canceló");
        verify(cronogramaService).cancelarPorEvento(10L);
        verify(eventoRepository).save(evento);
    }

    @Test
    void programarEvento_errorAlReservar_noCambiaAProgramado() {
        Evento evento = crearEventoBase(COTIZACION);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        doThrow(new EventoException("Sin stock")).when(stockHandler).validarYReservarStock(evento);

        assertThrows(EventoException.class, () -> service.programarEvento(10L));

        assertEquals(COTIZACION, evento.getEstadoEvento().getNombre());
        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void iniciarCancelacionFallaSiMotivoEsVacio() {
        assertThrows(EventoException.class, () -> service.iniciarCancelacion(10L, "   "));

        verify(eventoRepository, never()).findById(10L);
    }

    @Test
    void obtenerEventosUsaQueryConRelaciones() {
        when(eventoRepository.findAllWithRelations()).thenReturn(List.of());

        service.obtenerEventos();

        verify(eventoRepository).findAllWithRelations();
    }

    @Test
    void agregarExtrasEnCotizacionLanzaError() {
        Evento evento = crearEventoBase(COTIZACION);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

        assertThrows(EventoException.class, () -> service.agregarExtras(10L, crearExtrasRequest(
                "Extra luces", new BigDecimal("180.00"), 7L, 2)));

        verify(eventoRepository, never()).save(any(Evento.class));
        verify(stockHandler, never()).validarYReservarExtras(any(Evento.class), any());
    }

    @Test
    void agregarExtrasEnProgramadoActualizaPrecioAdicionalYNota() {
        Evento evento = crearEventoBase("PROGRAMADO");
        Producto producto = crearProducto(7L, "Luces LED", "ACTIVO", new BigDecimal("75.00"));
        EventoExtraRequestDTO dto = crearExtrasRequest("Extra luces", new BigDecimal("180.00"), 7L, 2);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(productoRepository.findById(7L)).thenReturn(Optional.of(producto));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoResponseDTO response = service.agregarExtras(10L, dto);

        assertEquals(new BigDecimal("180.00"), response.getPrecioAdicional());
        assertEquals("Extra luces", response.getNotaPrecio());
        verify(stockHandler).validarYReservarExtras(eq(evento), any());
    }

    @Test
    void agregarExtrasEnConfirmadoAcumulaNotaYPrecioAdicional() {
        Evento evento = crearEventoBase("CONFIRMADO");
        evento.setPrecioAdicional(new BigDecimal("100.00"));
        evento.setNotaPrecio("Cargo previo");

        Producto producto = crearProducto(8L, "Flores", "ACTIVO", new BigDecimal("50.00"));
        EventoExtraRequestDTO dto = crearExtrasRequest("Centro floral", new BigDecimal("90.00"), 8L, 3);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(productoRepository.findById(8L)).thenReturn(Optional.of(producto));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoResponseDTO response = service.agregarExtras(10L, dto);

        assertEquals(new BigDecimal("190.00"), response.getPrecioAdicional());
        assertEquals("Cargo previo | Centro floral", response.getNotaPrecio());
    }

    @Test
    void agregarExtrasConProductoInactivoLanzaError() {
        Evento evento = crearEventoBase("PROGRAMADO");
        Producto producto = crearProducto(7L, "Luces LED", "INACTIVO", new BigDecimal("75.00"));

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(productoRepository.findById(7L)).thenReturn(Optional.of(producto));

        assertThrows(EventoException.class, () -> service.agregarExtras(10L, crearExtrasRequest("Extra luces", new BigDecimal("180.00"), 7L, 2)));

        verify(stockHandler, never()).validarYReservarExtras(any(Evento.class), any());
        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void agregarExtrasEnFinalizadoFalla() {
        Evento evento = crearEventoBase(FINALIZADO);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

        assertThrows(EventoException.class, () -> service.agregarExtras(10L, crearExtrasRequest("Extra luces", new BigDecimal("180.00"), 7L, 2)));
    }

    @Test
    void agregarExtrasConMontoNullFalla() {
        Evento evento = crearEventoBase("PROGRAMADO");
        EventoExtraRequestDTO dto = crearExtrasRequest("Extra luces", null, 7L, 2);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

        assertThrows(EventoException.class, () -> service.agregarExtras(10L, dto));
        verify(stockHandler, never()).validarYReservarExtras(any(Evento.class), any());
    }

    @Test
    void agregarExtrasConMontoCeroFalla() {
        Evento evento = crearEventoBase("PROGRAMADO");
        EventoExtraRequestDTO dto = crearExtrasRequest("Extra luces", BigDecimal.ZERO, 7L, 2);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

        assertThrows(EventoException.class, () -> service.agregarExtras(10L, dto));
        verify(stockHandler, never()).validarYReservarExtras(any(Evento.class), any());
    }

    @Test
    void agregarExtrasConMontoNegativoFalla() {
        Evento evento = crearEventoBase("PROGRAMADO");
        EventoExtraRequestDTO dto = crearExtrasRequest("Extra luces", new BigDecimal("-1.00"), 7L, 2);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

        assertThrows(EventoException.class, () -> service.agregarExtras(10L, dto));
        verify(stockHandler, never()).validarYReservarExtras(any(Evento.class), any());
    }

    @Test
    void agregarExtrasConNotaVaciaFalla() {
        Evento evento = crearEventoBase("PROGRAMADO");
        EventoExtraRequestDTO dto = crearExtrasRequest("   ", new BigDecimal("180.00"), 7L, 2);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

        assertThrows(EventoException.class, () -> service.agregarExtras(10L, dto));
        verify(stockHandler, never()).validarYReservarExtras(any(Evento.class), any());
    }

    @Test
    void agregarExtrasNoUsaProductoPrecioUnitarioComoCargoComercial() {
        Evento evento = crearEventoBase("PROGRAMADO");
        Producto producto = crearProducto(7L, "Luces LED", "ACTIVO", new BigDecimal("999.99"));
        EventoExtraRequestDTO dto = crearExtrasRequest("Extra luces", new BigDecimal("120.00"), 7L, 2);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(productoRepository.findById(7L)).thenReturn(Optional.of(producto));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoResponseDTO response = service.agregarExtras(10L, dto);

        assertEquals(new BigDecimal("120.00"), response.getPrecioAdicional());
    }

    @Test
    void verificarDisponibilidadFallaSiFaltaHoraFin() {
        assertThrows(EventoException.class, () -> service.verificarDisponibilidad(
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                null,
                "Salon central",
                null));
    }

    @Test
    void verificarDisponibilidadFallaSiHorarioEsInvalido() {
        assertThrows(EventoException.class, () -> service.verificarDisponibilidad(
                LocalDate.now().plusDays(1),
                LocalTime.of(15, 0),
                LocalTime.of(14, 0),
                "Salon central",
                null));
    }

    @Test
    void verificarDisponibilidadRetornaFalseSiHaySolapamiento() {
        LocalDate fecha = LocalDate.now().plusDays(1);
        LocalTime horaInicio = LocalTime.of(10, 0);
        LocalTime horaFin = LocalTime.of(12, 0);

        when(eventoRepository.existeSolapamiento(eq(fecha), any(LocalTime.class), any(LocalTime.class), eq("Salon central")))
                .thenReturn(true);

        boolean disponible = service.verificarDisponibilidad(
                fecha, horaInicio, horaFin, "Salon central", null);

        assertEquals(false, disponible);
    }

    @Test
    void verificarDisponibilidadRetornaTrueSiNoHaySolapamientoExcluyendoEvento() {
        LocalDate fecha = LocalDate.now().plusDays(1);
        LocalTime horaInicio = LocalTime.of(10, 0);
        LocalTime horaFin = LocalTime.of(12, 0);

        when(eventoRepository.existeSolapamientoExcluyendoEvento(eq(15L), eq(fecha), any(LocalTime.class), any(LocalTime.class), eq("Salon central")))
                .thenReturn(false);

        boolean disponible = service.verificarDisponibilidad(
                fecha, horaInicio, horaFin, "Salon central", 15L);

        assertEquals(true, disponible);
    }

    private void stubCatalogosActivosCreacion() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(crearCliente(true)));
        when(decoracionRepository.findById(2L)).thenReturn(Optional.of(crearDecoracion("Clasica", new BigDecimal("2500.00"), "ACTIVO")));
        when(tipoEventoRepository.findById(3L)).thenReturn(Optional.of(crearTipoEvento(true)));
    }

    private EventoRequestDTO crearRequestBase() {
        EventoRequestDTO dto = new EventoRequestDTO();
        dto.setClienteId(1L);
        dto.setDecoracionId(2L);
        dto.setTipoEventoId(3L);
        dto.setFecha(LocalDate.now().plusDays(5));
        dto.setHoraInicio(LocalTime.of(10, 0));
        dto.setHoraFin(LocalTime.of(13, 0));
        dto.setUbicacion("Salon central");
        dto.setNota("Evento demo");
        dto.setPrecioAdicional(BigDecimal.ZERO);
        return dto;
    }

    private EventoExtraRequestDTO crearExtrasRequest(String notaPrecio,
                                                     BigDecimal montoAdicional,
                                                     Long productoId,
                                                     Integer cantidad) {
        EventoExtraItemRequestDTO item = new EventoExtraItemRequestDTO();
        item.setProductoId(productoId);
        item.setCantidad(cantidad);

        EventoExtraRequestDTO dto = new EventoExtraRequestDTO();
        dto.setNotaPrecio(notaPrecio);
        dto.setMontoAdicional(montoAdicional);
        dto.setItems(List.of(item));
        return dto;
    }

    private EventoUpdateDTO crearUpdateAgenda(LocalDate fecha,
                                              LocalTime horaInicio,
                                              LocalTime horaFin,
                                              String ubicacion) {
        EventoUpdateDTO dto = new EventoUpdateDTO();
        dto.setFecha(fecha);
        dto.setHoraInicio(horaInicio);
        dto.setHoraFin(horaFin);
        dto.setUbicacion(ubicacion);
        return dto;
    }

    private Evento crearEventoBase(String estadoNombre) {
        Evento evento = new Evento();
        evento.setId(10L);
        evento.setCliente(crearCliente(true));
        evento.setDecoracion(crearDecoracion("Clasica", new BigDecimal("2500.00"), "ACTIVO"));
        evento.setTipoEvento(crearTipoEvento(true));
        evento.setEstadoEvento(crearEstadoEvento(estadoNombre));
        evento.setFecha(LocalDate.now().plusDays(7));
        evento.setHoraInicio(LocalTime.of(10, 0));
        evento.setHoraFin(LocalTime.of(13, 0));
        evento.setUbicacion("Salon central");
        evento.setPrecioBase(new BigDecimal("2500.00"));
        evento.setPrecioAdicional(BigDecimal.ZERO);
        evento.setPrecioTotal(new BigDecimal("2500.00"));
        return evento;
    }

    private Cliente crearCliente(boolean activo) {
        Cliente cliente = new Cliente();
        cliente.setActivo(activo);
        cliente.setNombre("Cliente Demo");
        return cliente;
    }

    private Decoracion crearDecoracion(String nombre, BigDecimal precio, String estado) {
        Decoracion decoracion = new Decoracion();
        decoracion.setNombre(nombre);
        decoracion.setPrecio(precio);
        decoracion.setEstado(estado);
        return decoracion;
    }

    private TipoEvento crearTipoEvento(boolean activo) {
        TipoEvento tipoEvento = new TipoEvento();
        tipoEvento.setTipoNombre("BODA");
        tipoEvento.setTipoActivo(activo);
        return tipoEvento;
    }

    private EstadoEvento crearEstadoEvento(String nombre) {
        EstadoEvento estadoEvento = new EstadoEvento();
        estadoEvento.setNombre(nombre);
        return estadoEvento;
    }

    private Producto crearProducto(Long id, String nombre, String estado, BigDecimal precioUnitario) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setEstado(estado);
        producto.setPrecioUnitario(precioUnitario);

        try {
            var idField = Producto.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(producto, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }

        return producto;
    }
}
