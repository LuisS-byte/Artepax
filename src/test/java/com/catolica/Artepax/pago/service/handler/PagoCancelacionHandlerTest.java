package com.catolica.Artepax.pago.service.handler;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import com.catolica.Artepax.pago.entity.EstadoPago;
import com.catolica.Artepax.pago.entity.Pago;
import com.catolica.Artepax.pago.exception.EstadoPagoNoConfiguradoException;
import com.catolica.Artepax.pago.repository.EstadoPagoRepository;
import com.catolica.Artepax.pago.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoCancelacionHandlerTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private EstadoPagoRepository estadoPagoRepository;

    @Mock
    private NotificacionService notificacionService;

    private PagoCancelacionHandler handler;

    private Evento evento;
    private EstadoPago estadoPendiente;
    private EstadoPago estadoPagado;
    private EstadoPago estadoCancelado;
    private EstadoPago estadoDevolucion;

    @BeforeEach
    void setUp() {
        handler = new PagoCancelacionHandler(pagoRepository, estadoPagoRepository, notificacionService);

        Cliente cliente = new Cliente();
        cliente.setNombre("Juan Perez");

        evento = new Evento();
        evento.setId(10L);
        evento.setCliente(cliente);

        estadoPendiente = new EstadoPago();
        estadoPendiente.setEstadoNombre("PENDIENTE");

        estadoPagado = new EstadoPago();
        estadoPagado.setEstadoNombre("PAGADO");

        estadoCancelado = new EstadoPago();
        estadoCancelado.setEstadoNombre("CANCELADO");

        estadoDevolucion = new EstadoPago();
        estadoDevolucion.setEstadoNombre("DEVOLUCION_PENDIENTE");
    }

    @Test
    void sinPagosNoModificaNada() {
        when(pagoRepository.findByEventoIdAndEstadoPago_EstadoNombreIn(eq(10L), anyList()))
                .thenReturn(List.of());

        handler.procesarPagosPorCancelacion(evento, "Cliente desistio");

        verify(pagoRepository, never()).saveAll(any());
        verify(notificacionService, never()).notificarAdmins(any(), any(), any());
    }

    @Test
    void pagoPendienteSeCancela() {
        Pago pago = crearPago(1L, estadoPendiente, new BigDecimal("500.00"));
        when(pagoRepository.findByEventoIdAndEstadoPago_EstadoNombreIn(eq(10L), anyList()))
                .thenReturn(List.of(pago));
        when(estadoPagoRepository.findByEstadoNombre("CANCELADO"))
                .thenReturn(Optional.of(estadoCancelado));
        when(pagoRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        handler.procesarPagosPorCancelacion(evento, "Cliente desistio");

        assertEquals("CANCELADO", pago.getEstadoPago().getEstadoNombre());
        verify(notificacionService, never()).notificarAdmins(any(), any(), any());
    }

    @Test
    void pagoPagadoPasaADevolucionPendiente() {
        Pago pago = crearPago(2L, estadoPagado, new BigDecimal("800.00"));
        when(pagoRepository.findByEventoIdAndEstadoPago_EstadoNombreIn(eq(10L), anyList()))
                .thenReturn(List.of(pago));
        when(estadoPagoRepository.findByEstadoNombre("CANCELADO"))
                .thenReturn(Optional.of(estadoCancelado));
        when(estadoPagoRepository.findByEstadoNombre("DEVOLUCION_PENDIENTE"))
                .thenReturn(Optional.of(estadoDevolucion));
        when(pagoRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        handler.procesarPagosPorCancelacion(evento, "Cliente desistio");

        assertEquals("DEVOLUCION_PENDIENTE", pago.getEstadoPago().getEstadoNombre());

        ArgumentCaptor<String> mensajeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).notificarAdmins(eq("CANCELACION_EVENTO"), mensajeCaptor.capture(), eq(evento));
        assertTrue(mensajeCaptor.getValue().contains("800.00"));
    }

    @Test
    void pagosMixtosPendienteCanceladoPagadoDevolucion() {
        Pago pagoPendiente = crearPago(1L, estadoPendiente, new BigDecimal("300.00"));
        Pago pagoPagado = crearPago(2L, estadoPagado, new BigDecimal("700.00"));
        when(pagoRepository.findByEventoIdAndEstadoPago_EstadoNombreIn(eq(10L), anyList()))
                .thenReturn(List.of(pagoPendiente, pagoPagado));
        when(estadoPagoRepository.findByEstadoNombre("CANCELADO"))
                .thenReturn(Optional.of(estadoCancelado));
        when(estadoPagoRepository.findByEstadoNombre("DEVOLUCION_PENDIENTE"))
                .thenReturn(Optional.of(estadoDevolucion));
        when(pagoRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        handler.procesarPagosPorCancelacion(evento, "Cliente desistio");

        assertEquals("CANCELADO", pagoPendiente.getEstadoPago().getEstadoNombre());
        assertEquals("DEVOLUCION_PENDIENTE", pagoPagado.getEstadoPago().getEstadoNombre());

        ArgumentCaptor<String> mensajeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).notificarAdmins(eq("CANCELACION_EVENTO"), mensajeCaptor.capture(), eq(evento));
        assertTrue(mensajeCaptor.getValue().contains("700.00"));
    }

    @Test
    void faltaEstadoDevolucionLanzaExcepcion() {
        Pago pago = crearPago(1L, estadoPagado, new BigDecimal("500.00"));
        when(pagoRepository.findByEventoIdAndEstadoPago_EstadoNombreIn(eq(10L), anyList()))
                .thenReturn(List.of(pago));
        when(estadoPagoRepository.findByEstadoNombre("CANCELADO"))
                .thenReturn(Optional.of(estadoCancelado));
        when(estadoPagoRepository.findByEstadoNombre("DEVOLUCION_PENDIENTE"))
                .thenReturn(Optional.empty());

        assertThrows(EstadoPagoNoConfiguradoException.class,
                () -> handler.procesarPagosPorCancelacion(evento, "motivo"));

        verify(pagoRepository, never()).saveAll(any());
    }

    @Test
    void multiplesPagadosNotificaSumaTotal() {
        Pago p1 = crearPago(1L, estadoPagado, new BigDecimal("400.00"));
        Pago p2 = crearPago(2L, estadoPagado, new BigDecimal("600.00"));
        when(pagoRepository.findByEventoIdAndEstadoPago_EstadoNombreIn(eq(10L), anyList()))
                .thenReturn(List.of(p1, p2));
        when(estadoPagoRepository.findByEstadoNombre("CANCELADO"))
                .thenReturn(Optional.of(estadoCancelado));
        when(estadoPagoRepository.findByEstadoNombre("DEVOLUCION_PENDIENTE"))
                .thenReturn(Optional.of(estadoDevolucion));
        when(pagoRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        handler.procesarPagosPorCancelacion(evento, "motivo");

        ArgumentCaptor<String> mensajeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).notificarAdmins(eq("CANCELACION_EVENTO"), mensajeCaptor.capture(), eq(evento));
        assertTrue(mensajeCaptor.getValue().contains("1000.00"));
    }

    private Pago crearPago(Long id, EstadoPago estado, BigDecimal monto) {
        Pago pago = new Pago();
        try {
            var idField = Pago.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(pago, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        pago.setEstadoPago(estado);
        pago.setMonto(monto);
        pago.setEvento(evento);
        return pago;
    }
}
