package com.catolica.Artepax.pago.service.impl;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.service.EventoService;
import com.catolica.Artepax.evento.service.handler.EventoPagoValidator;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import com.catolica.Artepax.pago.dto.PagoRequestDTO;
import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.entity.EstadoPago;
import com.catolica.Artepax.pago.entity.MetodoPago;
import com.catolica.Artepax.pago.entity.Pago;
import com.catolica.Artepax.pago.exception.PagoException;
import com.catolica.Artepax.pago.mapper.PagoMapper;
import com.catolica.Artepax.pago.repository.ConceptoPagoRepository;
import com.catolica.Artepax.pago.repository.EstadoPagoRepository;
import com.catolica.Artepax.pago.repository.MetodoPagoRepository;
import com.catolica.Artepax.pago.repository.PagoRepository;
import com.catolica.Artepax.pago.repository.VistaPagosEventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import org.mockito.InOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private EstadoPagoRepository estadoPagoRepository;
    @Mock
    private MetodoPagoRepository metodoPagoRepository;
    @Mock
    private ConceptoPagoRepository conceptoPagoRepository;
    @Mock
    private EventoService eventoService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private VistaPagosEventoRepository vistaPagosEventoRepository;
    @Mock
    private EventoPagoValidator eventoPagoValidator;
    @Mock
    private ConfiguracionService configuracionService;

    private PagoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PagoServiceImpl(
                pagoRepository,
                new PagoMapper(),
                eventoRepository,
                estadoPagoRepository,
                metodoPagoRepository,
                conceptoPagoRepository,
                eventoService,
                notificacionService,
                vistaPagosEventoRepository,
                eventoPagoValidator,
                configuracionService);

    }

    @Test
    void obtenerPorEventoUsaQueryConFetchRelations() {
        when(pagoRepository.findByEventoIdWithRelations(4L)).thenReturn(List.of());

        service.obtenerPorEvento(4L);

        verify(pagoRepository).findByEventoIdWithRelations(4L);
    }

    @Test
    void registrarPagoConMetodoInactivoLanzaErrorDeNegocio() {
        PagoRequestDTO dto = crearPagoRequest();
        Evento evento = crearEvento("PROGRAMADO", "1000.00");
        EstadoPago estadoPendiente = crearEstadoPago("PENDIENTE");
        MetodoPago metodoInactivo = crearMetodoPago(false);

        when(eventoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(evento));
        when(pagoRepository.sumMontoActivoByEventoId(10L)).thenReturn(BigDecimal.ZERO);
        when(estadoPagoRepository.findByEstadoNombre("PENDIENTE")).thenReturn(Optional.of(estadoPendiente));
        when(metodoPagoRepository.findById(3L)).thenReturn(Optional.of(metodoInactivo));

        assertThrows(PagoException.class, () -> service.registrarPago(dto));

        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void confirmarPagoValidoLoPasaAPagadoYLlenaFecha() {
        Pago pago = spy(crearPagoPendiente(1L, "PROGRAMADO", "1000.00", "300.00"));
        Evento evento = pago.getEvento();
        EstadoPago estadoPagado = crearEstadoPago("PAGADO");

        when(pagoRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(pago));
        when(eventoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(evento));
        when(pagoRepository.sumPagosConfirmadosPorEventoExcluyendoPago(10L, 1L))
                .thenReturn(new BigDecimal("400.00"));
        when(estadoPagoRepository.findByEstadoNombre("PAGADO")).thenReturn(Optional.of(estadoPagado));
        when(eventoPagoValidator.tieneAnticipoSuficiente(evento)).thenReturn(true);

        PagoResponseDTO response = service.confirmarPago(1L);

        assertEquals("PAGADO", pago.getEstadoPago().getEstadoNombre());
        assertNotNull(pago.getFecha());
        assertEquals("PAGADO", response.getEstadoPago());
        assertNotNull(response.getFecha());
        verify(pago).setEstadoPago(estadoPagado);
        verify(pago).setFecha(any(LocalDateTime.class));
        InOrder orden = inOrder(pagoRepository, eventoRepository, notificacionService, eventoService);
        orden.verify(pagoRepository).findByIdWithRelations(1L);
        orden.verify(eventoRepository).findByIdForUpdate(10L);
        orden.verify(pagoRepository).sumPagosConfirmadosPorEventoExcluyendoPago(10L, 1L);
        orden.verify(pagoRepository).save(pago);
        orden.verify(pagoRepository).flush();
        orden.verify(notificacionService).notificarAdmins(eq("PAGO_RECIBIDO"), any(String.class), eq(evento));
        orden.verify(eventoService).confirmarDesdeAnticipo(10L);
        orden.verify(eventoService).finalizarDesdeCobroCompleto(10L);
        orden.verify(pagoRepository).findByIdWithRelations(1L);
    }

    @Test
    void confirmarPagoYaPagadoLanzaError() {
        Pago pago = spy(crearPagoConEstado(1L, "PAGADO", "PROGRAMADO", "1000.00", "300.00"));
        EstadoPago estadoOriginal = pago.getEstadoPago();

        when(pagoRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(pago));

        assertThrows(PagoException.class, () -> service.confirmarPago(1L));

        assertSame(estadoOriginal, pago.getEstadoPago());
        assertNull(pago.getFecha());
        verify(pago, never()).setEstadoPago(any(EstadoPago.class));
        verify(pago, never()).setFecha(any(LocalDateTime.class));
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void confirmarPagoCanceladoLanzaError() {
        Pago pago = spy(crearPagoConEstado(1L, "CANCELADO", "PROGRAMADO", "1000.00", "300.00"));
        EstadoPago estadoOriginal = pago.getEstadoPago();

        when(pagoRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(pago));

        assertThrows(PagoException.class, () -> service.confirmarPago(1L));

        assertSame(estadoOriginal, pago.getEstadoPago());
        assertNull(pago.getFecha());
        verify(pago, never()).setEstadoPago(any(EstadoPago.class));
        verify(pago, never()).setFecha(any(LocalDateTime.class));
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void confirmarPagoCuandoEventoEstaCanceladoLanzaError() {
        Pago pago = spy(crearPagoPendiente(1L, "CANCELADO", "1000.00", "300.00"));
        assertFalloSinConfirmacionParcialPorEstadoEvento(pago);
    }

    @Test
    void confirmarPagoCuandoEventoEstaCancelacionPendienteLanzaError() {
        Pago pago = spy(crearPagoPendiente(1L, "CANCELACION_PENDIENTE", "1000.00", "300.00"));
        assertFalloSinConfirmacionParcialPorEstadoEvento(pago);
    }

    @Test
    void confirmarPagoCuandoEventoEstaFinalizadoLanzaError() {
        Pago pago = spy(crearPagoPendiente(1L, "FINALIZADO", "1000.00", "300.00"));
        assertFalloSinConfirmacionParcialPorEstadoEvento(pago);
    }

    @Test
    void confirmarPagoConEstadoEventoNoReconocidoLanzaError() {
        Pago pago = spy(crearPagoPendiente(1L, "PAUSADO", "1000.00", "300.00"));
        assertFalloSinConfirmacionParcialPorEstadoEvento(pago);
    }

    @Test
    void confirmarPagoQueExcedePrecioTotalLanzaError() {
        Pago pago = spy(crearPagoPendiente(1L, "PROGRAMADO", "1000.00", "250.00"));
        Evento evento = pago.getEvento();
        EstadoPago estadoOriginal = pago.getEstadoPago();

        when(pagoRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(pago));
        when(eventoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(evento));
        when(pagoRepository.sumPagosConfirmadosPorEventoExcluyendoPago(10L, 1L))
                .thenReturn(new BigDecimal("900.01"));

        assertThrows(PagoException.class, () -> service.confirmarPago(1L));

        assertSame(estadoOriginal, pago.getEstadoPago());
        assertNull(pago.getFecha());
        verify(pago, never()).setEstadoPago(any(EstadoPago.class));
        verify(pago, never()).setFecha(any(LocalDateTime.class));
        verify(pagoRepository, never()).save(any(Pago.class));
        verify(notificacionService, never()).notificarAdmins(any(String.class), any(String.class), any(Evento.class));
    }

    @Test
    void confirmarSegundoPagoPendienteQueExcedeElTotalFalla() {
        Pago segundoPago = spy(crearPagoPendiente(2L, "PROGRAMADO", "1000.00", "250.00"));
        Evento evento = segundoPago.getEvento();
        EstadoPago estadoOriginal = segundoPago.getEstadoPago();

        when(pagoRepository.findByIdWithRelations(2L)).thenReturn(Optional.of(segundoPago));
        when(eventoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(evento));
        when(pagoRepository.sumPagosConfirmadosPorEventoExcluyendoPago(10L, 2L))
                .thenReturn(new BigDecimal("800.00"));

        assertThrows(PagoException.class, () -> service.confirmarPago(2L));

        assertSame(estadoOriginal, segundoPago.getEstadoPago());
        assertNull(segundoPago.getFecha());
        verify(segundoPago, never()).setEstadoPago(any(EstadoPago.class));
        verify(segundoPago, never()).setFecha(any(LocalDateTime.class));
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void confirmarPagoQueNoExcedeSaldoPendienteEstaPermitido() {
        Pago pago = spy(crearPagoPendiente(3L, "CONFIRMADO", "1000.00", "200.00"));
        Evento evento = pago.getEvento();
        EstadoPago estadoPagado = crearEstadoPago("PAGADO");

        when(pagoRepository.findByIdWithRelations(3L)).thenReturn(Optional.of(pago));
        when(eventoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(evento));
        when(pagoRepository.sumPagosConfirmadosPorEventoExcluyendoPago(10L, 3L))
                .thenReturn(new BigDecimal("800.00"));
        when(estadoPagoRepository.findByEstadoNombre("PAGADO")).thenReturn(Optional.of(estadoPagado));

        service.confirmarPago(3L);

        assertEquals("PAGADO", pago.getEstadoPago().getEstadoNombre());
        assertNotNull(pago.getFecha());
        verify(pagoRepository).save(pago);
        verify(eventoPagoValidator, never()).tieneAnticipoSuficiente(any(Evento.class));
        verify(eventoService, never()).confirmarDesdeAnticipo(any(Long.class));
    }

    private void assertFalloSinConfirmacionParcialPorEstadoEvento(Pago pago) {
        Evento evento = pago.getEvento();
        EstadoPago estadoOriginal = pago.getEstadoPago();

        when(pagoRepository.findByIdWithRelations(pago.getId())).thenReturn(Optional.of(pago));
        when(eventoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(evento));

        assertThrows(PagoException.class, () -> service.confirmarPago(pago.getId()));

        assertSame(estadoOriginal, pago.getEstadoPago());
        assertNull(pago.getFecha());
        verify(pago, never()).setEstadoPago(any(EstadoPago.class));
        verify(pago, never()).setFecha(any(LocalDateTime.class));
        verify(pagoRepository, never()).save(any(Pago.class));
        verify(notificacionService, never()).notificarAdmins(any(String.class), any(String.class), any(Evento.class));
    }

    private PagoRequestDTO crearPagoRequest() {
        PagoRequestDTO dto = new PagoRequestDTO();
        dto.setEventoId(10L);
        dto.setMonto(new BigDecimal("100.00"));
        dto.setMetodoPagoId(3L);
        dto.setConceptoPagoId(4L);
        return dto;
    }

    private Pago crearPagoPendiente(Long pagoId, String estadoEvento, String precioTotal, String montoPago) {
        return crearPagoConEstado(pagoId, "PENDIENTE", estadoEvento, precioTotal, montoPago);
    }

    private Pago crearPagoConEstado(Long pagoId,
                                    String estadoPagoNombre,
                                    String estadoEventoNombre,
                                    String precioTotal,
                                    String montoPago) {
        Pago pago = new Pago();
        pago.setEvento(crearEvento(estadoEventoNombre, precioTotal));
        pago.setEstadoPago(crearEstadoPago(estadoPagoNombre));
        pago.setMonto(new BigDecimal(montoPago));
        pago.setFecha(null);

        try {
            var idField = Pago.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(pago, pagoId);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }

        return pago;
    }

    private Evento crearEvento(String estadoNombre, String precioTotal) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Demo");

        EstadoEvento estadoEvento = new EstadoEvento();
        estadoEvento.setNombre(estadoNombre);

        Evento evento = new Evento();
        evento.setId(10L);
        evento.setCliente(cliente);
        evento.setEstadoEvento(estadoEvento);
        evento.setFecha(LocalDate.now().plusDays(1));
        evento.setPrecioTotal(new BigDecimal(precioTotal));
        return evento;
    }

    private EstadoPago crearEstadoPago(String nombre) {
        EstadoPago estadoPago = new EstadoPago();
        estadoPago.setEstadoNombre(nombre);
        return estadoPago;
    }

    private MetodoPago crearMetodoPago(boolean activo) {
        MetodoPago metodoPago = new MetodoPago();
        metodoPago.setMetodoActivo(activo);
        return metodoPago;
    }
}
