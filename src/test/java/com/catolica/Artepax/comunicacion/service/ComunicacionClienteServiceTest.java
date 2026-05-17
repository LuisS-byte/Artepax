package com.catolica.Artepax.comunicacion.service;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.pago.entity.Pago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComunicacionClienteServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private ConfiguracionService configuracionService;

    private ComunicacionClienteService service;

    @BeforeEach
    void setUp() {
        service = new ComunicacionClienteService(emailService, configuracionService);

        lenient().when(configuracionService.getString("NEGOCIO_NOMBRE")).thenReturn("Artepax");
        lenient().when(configuracionService.getString("NEGOCIO_TELEFONO")).thenReturn("555-1234");
    }

    @Test
    void clienteComunicacionService_enviaAvisoSaldoPendiente_conCorreoValido() {
        Evento evento = crearEvento("cliente@test.com");
        byte[] pdfBytes = new byte[]{1, 2, 3};

        service.enviarAvisoSaldoPendienteConDocumento(evento, pdfBytes);

        verify(emailService).enviarConAdjunto(
                eq("cliente@test.com"),
                any(String.class),
                any(String.class),
                eq(pdfBytes),
                any(String.class));
    }

    @Test
    void clienteComunicacionService_fallaSinCorreo() {
        Evento evento = crearEvento(null);
        byte[] pdfBytes = new byte[]{1, 2, 3};

        assertThrows(IllegalArgumentException.class,
                () -> service.enviarAvisoSaldoPendienteConDocumento(evento, pdfBytes));

        verify(emailService, org.mockito.Mockito.never()).enviarConAdjunto(any(), any(), any(), any(), any());
    }

    @Test
    void clienteComunicacionService_lanzaExceptionSiEmailFalla() {
        Evento evento = crearEvento("cliente@test.com");
        byte[] pdfBytes = new byte[]{1, 2, 3};

        doThrow(new RuntimeException("smtp caido")).when(emailService)
                .enviarConAdjunto(any(), any(), any(), any(), any());

        assertThrows(RuntimeException.class,
                () -> service.enviarAvisoSaldoPendienteConDocumento(evento, pdfBytes));
    }

    @Test
    void enviarConfirmacionReserva_escapaHtmlEnNombreCliente() {
        Evento evento = crearEventoConNombre("cliente@test.com", "<script>alert(1)</script>");

        service.enviarConfirmacionReserva(evento, new byte[]{1});

        verify(emailService).enviarConAdjunto(
                eq("cliente@test.com"),
                any(String.class),
                org.mockito.ArgumentMatchers.argThat(cuerpo ->
                        cuerpo != null
                                && !cuerpo.contains("<script>")
                                && cuerpo.contains("&lt;script&gt;")),
                any(),
                any());
    }

    @Test
    void enviarConfirmacionReserva_escapaHtmlEnUbicacion() {
        Evento evento = crearEvento("cliente@test.com");
        evento.setUbicacion("Salon <b>VIP</b> & Suite");

        service.enviarConfirmacionReserva(evento, new byte[]{1});

        verify(emailService).enviarConAdjunto(
                eq("cliente@test.com"),
                any(),
                org.mockito.ArgumentMatchers.argThat(cuerpo ->
                        cuerpo != null
                                && !cuerpo.contains("<b>VIP</b>")
                                && cuerpo.contains("&lt;b&gt;VIP&lt;/b&gt;")
                                && cuerpo.contains("&amp;")),
                any(),
                any());
    }

    @Test
    void enviarRecordatorioEvento_escapaHtmlEnNombreCliente() {
        Evento evento = crearEventoConNombre("cliente@test.com", "Cliente \"test\" & amigos");

        service.enviarRecordatorioEvento(evento);

        verify(emailService).enviarSimple(
                eq("cliente@test.com"),
                any(),
                org.mockito.ArgumentMatchers.argThat(cuerpo ->
                        cuerpo != null
                                && cuerpo.contains("&quot;test&quot;")
                                && cuerpo.contains("&amp;")));
    }

    @Test
    void enviarComprobantePago_noEscapaValoresNumericos() {
        Evento evento = crearEvento("cliente@test.com");
        Pago pago = new Pago();
        pago.setMonto(new java.math.BigDecimal("1500.50"));
        pago.setFecha(java.time.LocalDateTime.of(2026, 5, 10, 14, 30));

        service.enviarComprobantePago(evento, pago);

        verify(emailService).enviarSimple(
                eq("cliente@test.com"),
                any(),
                org.mockito.ArgumentMatchers.argThat(cuerpo ->
                        cuerpo != null && cuerpo.contains("$1500.50")));
    }

    private Evento crearEventoConNombre(String correo, String nombre) {
        Evento evento = crearEvento(correo);
        evento.getCliente().setNombre(nombre);
        return evento;
    }

    private Evento crearEvento(String correo) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Demo");
        cliente.setCorreo(correo);

        EstadoEvento estado = new EstadoEvento();
        estado.setNombre("CONFIRMADO");

        TipoEvento tipoEvento = new TipoEvento();
        tipoEvento.setTipoNombre("BODA");

        Decoracion decoracion = new Decoracion();
        decoracion.setNombre("Clasica");

        Evento evento = new Evento();
        evento.setId(1L);
        evento.setCliente(cliente);
        evento.setEstadoEvento(estado);
        evento.setTipoEvento(tipoEvento);
        evento.setDecoracion(decoracion);
        evento.setFecha(LocalDate.of(2026, 5, 10));
        evento.setHoraInicio(LocalTime.of(10, 0));
        evento.setHoraFin(LocalTime.of(18, 0));
        evento.setUbicacion("Salon central");
        return evento;
    }
}
