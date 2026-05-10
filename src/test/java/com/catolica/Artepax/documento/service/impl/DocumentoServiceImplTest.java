package com.catolica.Artepax.documento.service.impl;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.common.service.AuditoriaService;
import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.comunicacion.service.ComunicacionClienteService;
import com.catolica.Artepax.documento.dto.DocumentoResponseDTO;
import com.catolica.Artepax.documento.entity.Documento;
import com.catolica.Artepax.documento.entity.DocumentoEstadoEnvio;
import com.catolica.Artepax.documento.entity.DocumentoTipo;
import com.catolica.Artepax.documento.entity.PlantillaDocumento;
import com.catolica.Artepax.documento.exception.DocumentoOperacionNoPermitidaException;
import com.catolica.Artepax.documento.mapper.DocumentoMapper;
import com.catolica.Artepax.documento.repository.DocumentoRepository;
import com.catolica.Artepax.documento.repository.PlantillaDocumentoRepository;
import com.catolica.Artepax.documento.service.PdfGeneratorService;
import com.catolica.Artepax.documento.service.policy.DocumentoPolicy;
import com.catolica.Artepax.comunicacion.service.EmailService;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.evento.model.EstadoEventoNames;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.pago.entity.VistaPagosEvento;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceImplTest {

    @Mock
    private DocumentoRepository documentoRepository;
    @Mock
    private PlantillaDocumentoRepository plantillaRepository;
    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private PdfGeneratorService pdfGeneratorService;
    @Mock
    private EmailService emailService;
    @Mock
    private ConfiguracionService configuracionService;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private VistaPagosEventoRepository vistaPagosEventoRepository;
    @Mock
    private ComunicacionClienteService comunicacionClienteService;
    @Mock
    private AuditoriaService auditoriaService;

    private DocumentoServiceImpl documentoService;

    @BeforeEach
    void setUp() {
        documentoService = new DocumentoServiceImpl(
                documentoRepository,
                plantillaRepository,
                eventoRepository,
                new DocumentoMapper(),
                pdfGeneratorService,
                emailService,
                configuracionService,
                pagoRepository,
                vistaPagosEventoRepository,
                new DocumentoPolicy(),
                comunicacionClienteService,
                auditoriaService);

        lenient().when(configuracionService.getString("NEGOCIO_NOMBRE")).thenReturn("Artepax");
        lenient().when(configuracionService.getString("POLITICA_CANCELACION_MONTAJE"))
                .thenReturn("Politica de cancelacion vigente.");
        lenient().when(configuracionService.getString("NEGOCIO_CORREO")).thenReturn("contacto@artepax.com");
        lenient().when(configuracionService.getString("NEGOCIO_TELEFONO")).thenReturn("555-1234");
        lenient().when(configuracionService.getString("INSTRUCCIONES_PAGO")).thenReturn("Transferencia bancaria.");
        lenient().when(pagoRepository.sumMontoConfirmadoByEventoIdAndConceptoNombre(1L, "ANTICIPO"))
                .thenReturn(new BigDecimal("500.00"));
        lenient().when(vistaPagosEventoRepository.findByIdEvento(1L)).thenReturn(Optional.of(crearVistaPagosEvento()));

        lenient().when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> {
            Documento documento = invocation.getArgument(0);
            if (documento.getIdDocumento() == null) {
                documento.setIdDocumento(99L);
            }
            return documento;
        });
        lenient().when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void generarConfirmacionReserva_enCancelado_lanzaError() {
        when(eventoRepository.findDocumentoContextById(1L))
                .thenReturn(Optional.of(crearEvento("cliente@test.com", EstadoEventoNames.CANCELADO)));

        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA));

        verify(plantillaRepository, never()).findByTipoDocumentoAndActivoTrue(any());
    }

    @Test
    void generarConfirmacionReserva_enCotizacion_lanzaError() {
        when(eventoRepository.findDocumentoContextById(1L))
                .thenReturn(Optional.of(crearEvento("cliente@test.com", EstadoEventoNames.COTIZACION)));

        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA));
    }

    @Test
    void generarConfirmacionReserva_enProgramado_lanzaError() {
        when(eventoRepository.findDocumentoContextById(1L))
                .thenReturn(Optional.of(crearEvento("cliente@test.com", EstadoEventoNames.PROGRAMADO)));

        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA));
    }

    @Test
    void generarConfirmacionReserva_enConfirmado_permitido() {
        generarDocumentoPermitidoSinCorreo(EstadoEventoNames.CONFIRMADO);
    }

    @Test
    void generarConfirmacionReserva_enMontaje_permitido() {
        generarDocumentoPermitidoSinCorreo(EstadoEventoNames.EN_MONTAJE);
    }

    @Test
    void generarConfirmacionReserva_enFinalizado_permitido() {
        generarDocumentoPermitidoSinCorreo(EstadoEventoNames.FINALIZADO);
    }

    @Test
    void generarConfirmacionReserva_duplicado_lanzaError() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(documentoRepository.existsByEventoIdAndTipoDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(true);

        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA));

        verify(plantillaRepository, never()).findByTipoDocumentoAndActivoTrue(any());
    }

    @Test
    void generarConfirmacionReserva_enConfirmadoConCorreoValido_quedaEnviado() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.CONFIRMACION_RESERVA, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});

        DocumentoResponseDTO response = documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        assertEquals("ENVIADO", response.getDocuEstadoEnvio());
        assertEquals(1, response.getDocuIntentosEnvio());
    }

    @Test
    void generarDocumentoSinCorreo_quedaPendiente() {
        Evento evento = crearEvento(null, EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.CONFIRMACION_RESERVA, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));

        DocumentoResponseDTO response = documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        assertEquals("PENDIENTE", response.getDocuEstadoEnvio());
        assertEquals(0, response.getDocuIntentosEnvio());
        verify(pdfGeneratorService, never()).generarDesdHtml(any());
        verify(emailService, never()).enviarConAdjunto(any(), any(), any(), any(), any());
    }

    @Test
    void generarDocumentoSinCorreo_noMarcaClienteNotificado() {
        Evento evento = crearEvento(null, EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.CONFIRMACION_RESERVA, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));

        documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        assertEquals(false, evento.getClienteNotificado());
        verify(eventoRepository, never()).save(evento);
    }

    @Test
    void generarDocumentoFallaEnvio_quedaFallido() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.CONFIRMACION_RESERVA, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});
        doThrow(new RuntimeException("smtp caido")).when(emailService)
                .enviarConAdjunto(any(), any(), any(), any(), any());

        DocumentoResponseDTO response = documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        assertEquals("FALLIDO", response.getDocuEstadoEnvio());
    }

    @Test
    void generarDocumentoFallaEnvio_incrementaIntento() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.CONFIRMACION_RESERVA, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});
        doThrow(new RuntimeException("smtp caido")).when(emailService)
                .enviarConAdjunto(any(), any(), any(), any(), any());

        DocumentoResponseDTO response = documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        assertEquals(1, response.getDocuIntentosEnvio());
        assertNull(response.getDocuFechaEnvio());
    }

    @Test
    void generarDocumentoFallaEnvio_noMarcaClienteNotificado() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.CONFIRMACION_RESERVA, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});
        doThrow(new RuntimeException("smtp caido")).when(emailService)
                .enviarConAdjunto(any(), any(), any(), any(), any());

        documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        assertEquals(false, evento.getClienteNotificado());
        verify(eventoRepository, never()).save(evento);
    }

    @Test
    void generarDocumentoExitoso_marcaClienteNotificado() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.CONFIRMACION_RESERVA, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});

        documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        assertEquals(true, evento.getClienteNotificado());
        verify(eventoRepository).save(evento);
    }

    @Test
    void reenviarDocumento_conTresIntentos_lanzaError() {
        Documento documento = crearDocumentoExistente("cliente@test.com", 3, true);
        when(documentoRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(documento));

        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> documentoService.reenviarDocumento(99L));

        verify(documentoRepository, never()).save(documento);
    }

    @Test
    void reenviarDocumento_sinPlantilla_lanzaError() {
        Documento documento = crearDocumentoExistente("cliente@test.com", 1, false);
        when(documentoRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(documento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.empty());

        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> documentoService.reenviarDocumento(99L));
    }

    @Test
    void reenviarDocumento_sinCorreo_lanzaError() {
        Documento documento = crearDocumentoExistente(null, 1, true);
        when(documentoRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(documento));

        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> documentoService.reenviarDocumento(99L));
    }

    @Test
    void reenviarDocumento_exitoso_quedaReenviado() {
        Documento documento = crearDocumentoExistente("cliente@test.com", 1, true);
        when(documentoRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(documento));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});

        DocumentoResponseDTO response = documentoService.reenviarDocumento(99L);

        assertEquals("REENVIADO", response.getDocuEstadoEnvio());
        assertEquals(2, response.getDocuIntentosEnvio());
    }

    @Test
    void reenviarDocumento_exitoso_actualizaFechaEnvio() {
        Documento documento = crearDocumentoExistente("cliente@test.com", 1, true);
        when(documentoRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(documento));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});

        DocumentoResponseDTO response = documentoService.reenviarDocumento(99L);

        assertNotNull(response.getDocuFechaEnvio());
    }

    @Test
    void reenviarDocumento_exitoso_marcaClienteNotificado() {
        Documento documento = crearDocumentoExistente("cliente@test.com", 1, true);
        when(documentoRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(documento));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});

        documentoService.reenviarDocumento(99L);

        assertEquals(true, documento.getEvento().getClienteNotificado());
        verify(eventoRepository).save(documento.getEvento());
    }

    @Test
    void reenviarDocumento_fallaEnvio_quedaFallidoEIncrementaIntento() {
        Documento documento = crearDocumentoExistente("cliente@test.com", 1, true);
        when(documentoRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(documento));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});
        doThrow(new RuntimeException("smtp caido")).when(emailService)
                .enviarConAdjunto(any(), any(), any(), any(), any());

        DocumentoResponseDTO response = documentoService.reenviarDocumento(99L);

        assertEquals("FALLIDO", response.getDocuEstadoEnvio());
        assertEquals(2, response.getDocuIntentosEnvio());
        assertNull(response.getDocuFechaEnvio());
        assertEquals(false, documento.getEvento().getClienteNotificado());
    }

    @Test
    void generarDocumento_usaTipoDocumentoNameParaBuscarPlantilla() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.CONFIRMACION_RESERVA, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});

        documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        verify(plantillaRepository).findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA);
    }

    @Test
    void generarDocumento_guardaDocuTipoComoTipoDocumentoName() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.CONFIRMACION_RESERVA, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});

        DocumentoResponseDTO response = documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        assertEquals(DocumentoTipo.CONFIRMACION_RESERVA.name(), response.getDocuTipo());
    }

    // AVISO_SALDO_PENDIENTE - estados permitidos

    @Test
    void generarAvisoSaldoPendiente_enProgramado_conSaldoPendiente_permitido() {
        generarAvisoSaldoPermitidoSinCorreo(EstadoEventoNames.PROGRAMADO);
    }

    @Test
    void generarAvisoSaldoPendiente_enConfirmado_conSaldoPendiente_permitido() {
        generarAvisoSaldoPermitidoSinCorreo(EstadoEventoNames.CONFIRMADO);
    }

    @Test
    void generarAvisoSaldoPendiente_enMontaje_conSaldoPendiente_permitido() {
        generarAvisoSaldoPermitidoSinCorreo(EstadoEventoNames.EN_MONTAJE);
    }

    @Test
    void generarAvisoSaldoPendiente_enFinalizado_conSaldoPendiente_permitido() {
        generarAvisoSaldoPermitidoSinCorreo(EstadoEventoNames.FINALIZADO);
    }

    // AVISO_SALDO_PENDIENTE - estados no permitidos

    @Test
    void generarAvisoSaldoPendiente_enCotizacion_lanzaError() {
        when(eventoRepository.findDocumentoContextById(1L))
                .thenReturn(Optional.of(crearEvento("cliente@test.com", EstadoEventoNames.COTIZACION)));

        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> documentoService.generarAvisoSaldoPendiente(1L));
    }

    @Test
    void generarAvisoSaldoPendiente_enCancelacionPendiente_lanzaError() {
        when(eventoRepository.findDocumentoContextById(1L))
                .thenReturn(Optional.of(crearEvento("cliente@test.com", EstadoEventoNames.CANCELACION_PENDIENTE)));

        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> documentoService.generarAvisoSaldoPendiente(1L));
    }

    @Test
    void generarAvisoSaldoPendiente_enCancelado_lanzaError() {
        when(eventoRepository.findDocumentoContextById(1L))
                .thenReturn(Optional.of(crearEvento("cliente@test.com", EstadoEventoNames.CANCELADO)));

        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> documentoService.generarAvisoSaldoPendiente(1L));
    }

    // AVISO_SALDO_PENDIENTE - sin saldo

    @Test
    void generarAvisoSaldoPendiente_sinSaldoPendiente_lanzaError() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(vistaPagosEventoRepository.findByIdEvento(1L)).thenReturn(Optional.of(crearVistaPagosSinSaldo()));

        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> documentoService.generarAvisoSaldoPendiente(1L));
    }

    // AVISO_SALDO_PENDIENTE - correo y envío

    @Test
    void generarAvisoSaldoPendiente_sinCorreo_quedaPendiente() {
        Evento evento = crearEvento(null, EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.AVISO_SALDO_PENDIENTE, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.AVISO_SALDO_PENDIENTE))
                .thenReturn(Optional.of(plantilla));

        DocumentoResponseDTO response = documentoService.generarAvisoSaldoPendiente(1L);

        assertEquals("PENDIENTE", response.getDocuEstadoEnvio());
        assertEquals(0, response.getDocuIntentosEnvio());
        verify(pdfGeneratorService, never()).generarDesdHtml(any());
        verify(comunicacionClienteService, never()).enviarAvisoSaldoPendienteConDocumento(any(), any());
    }

    @Test
    void generarAvisoSaldoPendiente_conCorreo_enviaYQuedaEnviado() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.AVISO_SALDO_PENDIENTE, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.AVISO_SALDO_PENDIENTE))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});

        DocumentoResponseDTO response = documentoService.generarAvisoSaldoPendiente(1L);

        assertEquals("ENVIADO", response.getDocuEstadoEnvio());
        assertEquals(1, response.getDocuIntentosEnvio());
        verify(comunicacionClienteService).enviarAvisoSaldoPendienteConDocumento(eq(evento), any());
    }

    @Test
    void generarAvisoSaldoPendiente_fallaEnvio_quedaFallidoEIncrementaIntento() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.AVISO_SALDO_PENDIENTE, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.AVISO_SALDO_PENDIENTE))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});
        doThrow(new RuntimeException("smtp caido")).when(comunicacionClienteService)
                .enviarAvisoSaldoPendienteConDocumento(any(), any());

        DocumentoResponseDTO response = documentoService.generarAvisoSaldoPendiente(1L);

        assertEquals("FALLIDO", response.getDocuEstadoEnvio());
        assertEquals(1, response.getDocuIntentosEnvio());
        assertNull(response.getDocuFechaEnvio());
    }

    @Test
    void generarAvisoSaldoPendiente_exitoso_marcaClienteNotificado() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.AVISO_SALDO_PENDIENTE, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.AVISO_SALDO_PENDIENTE))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});

        documentoService.generarAvisoSaldoPendiente(1L);

        assertEquals(true, evento.getClienteNotificado());
        verify(eventoRepository).save(evento);
    }

    @Test
    void generarAvisoSaldoPendiente_fallido_noMarcaClienteNotificado() {
        Evento evento = crearEvento("cliente@test.com", EstadoEventoNames.CONFIRMADO);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.AVISO_SALDO_PENDIENTE, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.AVISO_SALDO_PENDIENTE))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});
        doThrow(new RuntimeException("smtp caido")).when(comunicacionClienteService)
                .enviarAvisoSaldoPendienteConDocumento(any(), any());

        documentoService.generarAvisoSaldoPendiente(1L);

        assertEquals(false, evento.getClienteNotificado());
        verify(eventoRepository, never()).save(evento);
    }

    private void generarAvisoSaldoPermitidoSinCorreo(String estadoEvento) {
        Evento evento = crearEvento(null, estadoEvento);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.AVISO_SALDO_PENDIENTE, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.AVISO_SALDO_PENDIENTE))
                .thenReturn(Optional.of(plantilla));

        DocumentoResponseDTO response = documentoService.generarAvisoSaldoPendiente(1L);

        assertEquals("PENDIENTE", response.getDocuEstadoEnvio());
        assertEquals(DocumentoTipo.AVISO_SALDO_PENDIENTE.name(), response.getDocuTipo());
    }

    private VistaPagosEvento crearVistaPagosSinSaldo() {
        VistaPagosEvento vista = new VistaPagosEvento();
        try {
            java.lang.reflect.Field saldoPendiente = VistaPagosEvento.class.getDeclaredField("saldoPendiente");
            saldoPendiente.setAccessible(true);
            saldoPendiente.set(vista, BigDecimal.ZERO);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return vista;
    }

    private void generarDocumentoPermitidoSinCorreo(String estadoEvento) {
        Evento evento = crearEvento(null, estadoEvento);
        PlantillaDocumento plantilla = crearPlantilla(DocumentoTipo.CONFIRMACION_RESERVA, true);

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));

        DocumentoResponseDTO response = documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        assertEquals("PENDIENTE", response.getDocuEstadoEnvio());
        assertEquals(DocumentoTipo.CONFIRMACION_RESERVA.name(), response.getDocuTipo());
    }

    private PlantillaDocumento crearPlantilla(DocumentoTipo tipo, boolean activa) {
        PlantillaDocumento plantilla = new PlantillaDocumento();
        plantilla.setIdPlantilla(7L);
        plantilla.setTipoDocumento(tipo);
        plantilla.setContenidoBase("<html>{{cliente_nombre}}</html>");
        plantilla.setActivo(activa);
        return plantilla;
    }

    private Documento crearDocumentoExistente(String correo, int intentosEnvio, boolean conPlantilla) {
        Documento documento = new Documento();
        documento.setIdDocumento(99L);
        documento.setEvento(crearEvento(correo, EstadoEventoNames.CONFIRMADO));
        documento.setPlantilla(conPlantilla ? crearPlantilla(DocumentoTipo.CONFIRMACION_RESERVA, true) : null);
        documento.setDocuTipo(DocumentoTipo.CONFIRMACION_RESERVA);
        documento.setDocuEstadoEnvio(DocumentoEstadoEnvio.FALLIDO);
        documento.setDocuIntentosEnvio(intentosEnvio);
        return documento;
    }

    private VistaPagosEvento crearVistaPagosEvento() {
        VistaPagosEvento vista = new VistaPagosEvento();
        try {
            java.lang.reflect.Field saldoPendiente = VistaPagosEvento.class.getDeclaredField("saldoPendiente");
            saldoPendiente.setAccessible(true);
            saldoPendiente.set(vista, new BigDecimal("1000.00"));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return vista;
    }

    private Evento crearEvento(String correo, String estadoNombre) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Demo");
        cliente.setCorreo(correo);

        EstadoEvento estadoEvento = new EstadoEvento();
        estadoEvento.setNombre(estadoNombre);

        TipoEvento tipoEvento = new TipoEvento();
        tipoEvento.setTipoNombre("BODA");

        Decoracion decoracion = new Decoracion();
        decoracion.setNombre("Clasica");

        Evento evento = new Evento();
        evento.setId(1L);
        evento.setCliente(cliente);
        evento.setEstadoEvento(estadoEvento);
        evento.setTipoEvento(tipoEvento);
        evento.setDecoracion(decoracion);
        evento.setFecha(LocalDate.of(2026, 5, 10));
        evento.setUbicacion("Salon central");
        evento.setPrecioTotal(new BigDecimal("1500.00"));
        evento.setClienteNotificado(false);
        return evento;
    }
}
