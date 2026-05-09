package com.catolica.Artepax.documento.service.impl;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.documento.dto.DocumentoResponseDTO;
import com.catolica.Artepax.documento.entity.Documento;
import com.catolica.Artepax.documento.entity.DocumentoEstadoEnvio;
import com.catolica.Artepax.documento.entity.DocumentoTipo;
import com.catolica.Artepax.documento.entity.PlantillaDocumento;
import com.catolica.Artepax.documento.mapper.DocumentoMapper;
import com.catolica.Artepax.documento.repository.DocumentoRepository;
import com.catolica.Artepax.documento.repository.PlantillaDocumentoRepository;
import com.catolica.Artepax.comunicacion.service.EmailService;
import com.catolica.Artepax.documento.service.PdfGeneratorService;
import com.catolica.Artepax.evento.model.EstadoEvento;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
                vistaPagosEventoRepository);

        when(configuracionService.getString("NEGOCIO_NOMBRE")).thenReturn("Artepax");
        when(configuracionService.getString("POLITICA_CANCELACION_MONTAJE"))
                .thenReturn("Politica de cancelacion vigente.");
        when(pagoRepository.sumMontoConfirmadoByEventoIdAndConceptoNombre(1L, "ANTICIPO"))
                .thenReturn(new BigDecimal("500.00"));
        when(vistaPagosEventoRepository.findByIdEvento(1L)).thenReturn(Optional.of(crearVistaPagosEvento()));

        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> {
            Documento documento = invocation.getArgument(0);
            if (documento.getIdDocumento() == null) {
                documento.setIdDocumento(99L);
            }
            return documento;
        });
    }

    @Test
    void generarDocumentoMarcaEnviadoCuandoPdfYMailSalenBien() {
        Evento evento = crearEvento("cliente@test.com");
        PlantillaDocumento plantilla = crearPlantilla();

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});

        DocumentoResponseDTO response = documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        assertEquals("ENVIADO", response.getDocuEstadoEnvio());
        assertEquals(1, response.getDocuIntentosEnvio());
        assertEquals("CONFIRMACION_RESERVA", response.getDocuTipo());
        verify(documentoRepository, times(2)).save(any(Documento.class));
        verify(emailService).enviarConAdjunto(eq("cliente@test.com"), any(), any(), any(), any());
    }

    @Test
    void generarDocumentoRellenaPlaceholdersFinancierosConDatosReales() {
        Evento evento = crearEvento("cliente@test.com");
        PlantillaDocumento plantilla = crearPlantilla();
        plantilla.setContenidoBase("{{anticipo_pagado}}|{{saldo_pendiente}}|{{precio_total}}");

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});

        documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        verify(pdfGeneratorService).generarDesdHtml(eq("$500.00|$1000.00|$1500.00"));
    }

    @Test
    void generarDocumentoMarcaFallidoSiFallaElPdf() {
        Evento evento = crearEvento("cliente@test.com");
        PlantillaDocumento plantilla = crearPlantilla();

        when(eventoRepository.findDocumentoContextById(1L)).thenReturn(Optional.of(evento));
        when(plantillaRepository.findByTipoDocumentoAndActivoTrue(DocumentoTipo.CONFIRMACION_RESERVA))
                .thenReturn(Optional.of(plantilla));
        when(pdfGeneratorService.generarDesdHtml(any())).thenThrow(new RuntimeException("pdf roto"));

        DocumentoResponseDTO response = documentoService.generarDocumento(1L, DocumentoTipo.CONFIRMACION_RESERVA);

        assertEquals("FALLIDO", response.getDocuEstadoEnvio());
        assertEquals(1, response.getDocuIntentosEnvio());
        assertNull(response.getDocuFechaEnvio());
        verify(documentoRepository, times(2)).save(any(Documento.class));
        verify(emailService, never()).enviarConAdjunto(any(), any(), any(), any(), any());
    }

    @Test
    void reenviarDocumentoMarcaFallidoSiFallaElMail() {
        Documento documento = crearDocumentoExistente("cliente@test.com", 1);

        when(documentoRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(documento));
        when(pdfGeneratorService.generarDesdHtml(any())).thenReturn(new byte[]{1, 2, 3});
        doThrow(new RuntimeException("smtp caido"))
                .when(emailService)
                .enviarConAdjunto(any(), any(), any(), any(), any());

        DocumentoResponseDTO response = documentoService.reenviarDocumento(99L);

        assertEquals(DocumentoEstadoEnvio.FALLIDO, documento.getDocuEstadoEnvio());
        assertEquals(2, documento.getDocuIntentosEnvio());
        assertEquals("FALLIDO", response.getDocuEstadoEnvio());
        verify(documentoRepository).save(documento);
    }

    @Test
    void reenviarDocumentoMarcaFallidoSiFallaElPdf() {
        Documento documento = crearDocumentoExistente("cliente@test.com", 1);

        when(documentoRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(documento));
        when(pdfGeneratorService.generarDesdHtml(any())).thenThrow(new RuntimeException("pdf roto"));

        DocumentoResponseDTO response = documentoService.reenviarDocumento(99L);

        assertEquals(DocumentoEstadoEnvio.FALLIDO, documento.getDocuEstadoEnvio());
        assertEquals(2, documento.getDocuIntentosEnvio());
        assertEquals("FALLIDO", response.getDocuEstadoEnvio());
        verify(documentoRepository).save(documento);
        verify(emailService, never()).enviarConAdjunto(any(), any(), any(), any(), any());
    }

    private PlantillaDocumento crearPlantilla() {
        PlantillaDocumento plantilla = new PlantillaDocumento();
        plantilla.setIdPlantilla(7L);
        plantilla.setTipoDocumento(DocumentoTipo.CONFIRMACION_RESERVA);
        plantilla.setContenidoBase("<html>{{cliente_nombre}}</html>");
        plantilla.setActivo(true);
        return plantilla;
    }

    private Documento crearDocumentoExistente(String correo, int intentosEnvio) {
        Documento documento = new Documento();
        documento.setIdDocumento(99L);
        documento.setEvento(crearEvento(correo));
        documento.setPlantilla(crearPlantilla());
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

    private Evento crearEvento(String correo) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Demo");
        cliente.setCorreo(correo);

        EstadoEvento estadoEvento = new EstadoEvento();
        estadoEvento.setNombre("CONFIRMADO");

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
        return evento;
    }
}
