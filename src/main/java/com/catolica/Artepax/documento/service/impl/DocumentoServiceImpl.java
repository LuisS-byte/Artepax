package com.catolica.Artepax.documento.service.impl;
import com.catolica.Artepax.documento.dto.DocumentoResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaUpdateDTO;
import com.catolica.Artepax.documento.entity.Documento;
import com.catolica.Artepax.documento.entity.DocumentoEstadoEnvio;
import com.catolica.Artepax.documento.entity.DocumentoTipo;
import com.catolica.Artepax.documento.entity.PlantillaDocumento;
import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.documento.exception.DocumentoNotFoundException;
import com.catolica.Artepax.documento.exception.DocumentoOperacionNoPermitidaException;
import com.catolica.Artepax.documento.exception.PlantillaDocumentoNotFoundException;
import com.catolica.Artepax.documento.mapper.DocumentoMapper;
import com.catolica.Artepax.documento.repository.DocumentoRepository;
import com.catolica.Artepax.documento.repository.PlantillaDocumentoRepository;
import com.catolica.Artepax.documento.service.DocumentoService;
import com.catolica.Artepax.comunicacion.service.EmailService;
import com.catolica.Artepax.documento.service.PdfGeneratorService;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.exception.EventoNotFoundException;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.pago.entity.VistaPagosEvento;
import com.catolica.Artepax.pago.repository.PagoRepository;
import com.catolica.Artepax.pago.repository.VistaPagosEventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentoServiceImpl implements DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final PlantillaDocumentoRepository plantillaRepository;
    private final EventoRepository eventoRepository;
    private final DocumentoMapper mapper;
    private final PdfGeneratorService pdfGeneratorService;
    private final EmailService emailService;
    private final ConfiguracionService configuracionService;
    private final PagoRepository pagoRepository;
    private final VistaPagosEventoRepository vistaPagosEventoRepository;
    private static final int MAX_INTENTOS_ENVIO = 3;
    private static final String CONCEPTO_ANTICIPO = "ANTICIPO";
    private static final String CONFIG_NEGOCIO_NOMBRE = "NEGOCIO_NOMBRE";
    private static final String CONFIG_POLITICA_CANCELACION = "POLITICA_CANCELACION_MONTAJE";

    // -------------------------------------------------------------------------
    // DOCUMENTOS
    // -------------------------------------------------------------------------

    @Override
    public DocumentoResponseDTO generarDocumento(Long idEvento, DocumentoTipo tipoDocumento) {
        Evento evento = eventoRepository.findDocumentoContextById(idEvento)
                .orElseThrow(() -> new EventoNotFoundException(idEvento));

        PlantillaDocumento plantilla = plantillaRepository
                .findByTipoDocumentoAndActivoTrue(tipoDocumento)
                .orElseThrow(() -> new DocumentoOperacionNoPermitidaException(
                        "No existe plantilla activa para el tipo: " + tipoDocumento));

        Documento documento = crearDocumentoPendiente(evento, plantilla, tipoDocumento);
        log.info("Documento generado: id={}, tipo={}, evento={}", documento.getIdDocumento(), tipoDocumento, idEvento);

        String correo = evento.getCliente().getCorreo();
        if (correo == null || correo.isBlank()) {
            log.warn("Evento {}: cliente sin correo registrado. El documento queda pendiente hasta contar con destinatario.", idEvento);
            return mapper.toDTO(documento);
        }

        try {
            String contenidoGenerado = rellenarPlaceholders(plantilla.getContenidoBase(), evento);
            byte[] pdfBytes = pdfGeneratorService.generarDesdHtml(contenidoGenerado);
            emailService.enviarConAdjunto(
                    correo,
                    "Confirmación de reserva - Artepax",
                    construirCuerpoConfirmacion(evento),
                    pdfBytes,
                    construirNombreArchivo(evento));

            documento = actualizarEstadoEnvio(documento, DocumentoEstadoEnvio.ENVIADO, 1, LocalDateTime.now());
            log.info("Email enviado para documento id={}", documento.getIdDocumento());
        } catch (RuntimeException e) {
            documento = actualizarEstadoEnvio(documento, DocumentoEstadoEnvio.FALLIDO, 1, null);
            log.error("Error procesando documento id={}: {}", documento.getIdDocumento(), e.getMessage(), e);
        }

        return mapper.toDTO(documento);
    }

    @Override
    public DocumentoResponseDTO reenviarDocumento(Long idDocumento) {
        Documento documento = documentoRepository.findByIdWithRelations(idDocumento)
                .orElseThrow(() -> new DocumentoNotFoundException(idDocumento));

        String correo = documento.getEvento().getCliente().getCorreo();
        if (correo == null || correo.isBlank()) {
            throw new DocumentoOperacionNoPermitidaException(
                    "No se puede reenviar: el cliente no tiene correo registrado.");
        }

        if (documento.getDocuIntentosEnvio() >= MAX_INTENTOS_ENVIO) {
            throw new DocumentoOperacionNoPermitidaException(
                    "Se alcanzó el límite de " + MAX_INTENTOS_ENVIO + " intentos de envío.");
        }

        // Regenerar el PDF desde la plantilla y el evento — así evitamos depender de Cloudinary
        PlantillaDocumento plantilla = documento.getPlantilla();
        if (plantilla == null) {
            throw new DocumentoOperacionNoPermitidaException("El documento no tiene plantilla asociada.");
        }

        Evento evento = documento.getEvento();

        try {
            String contenidoGenerado = rellenarPlaceholders(plantilla.getContenidoBase(), evento);
            byte[] pdfBytes = pdfGeneratorService.generarDesdHtml(contenidoGenerado);
            emailService.enviarConAdjunto(
                    correo,
                    "Reenvío de documento - Artepax",
                    construirCuerpoReenvio(evento),
                    pdfBytes,
                    construirNombreArchivo(evento));

            documento = actualizarEstadoEnvio(
                    documento,
                    DocumentoEstadoEnvio.REENVIADO,
                    documento.getDocuIntentosEnvio() + 1,
                    LocalDateTime.now());
            log.info("Reenvío exitoso documento id={} intento={}", idDocumento, documento.getDocuIntentosEnvio());

        } catch (RuntimeException e) {
            documento = actualizarEstadoEnvio(
                    documento,
                    DocumentoEstadoEnvio.FALLIDO,
                    documento.getDocuIntentosEnvio() + 1,
                    null);
            log.error("Error en reenvío documento id={}: {}", idDocumento, e.getMessage(), e);
            return mapper.toDTO(documento);
        }

        return mapper.toDTO(documento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> listarPorEvento(Long idEvento) {
        if (!eventoRepository.existsById(idEvento)) {
            throw new EventoNotFoundException(idEvento);
        }
        return documentoRepository
                .findByEventoIdWithRelations(idEvento)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentoResponseDTO> obtenerTodos(Pageable pageable) {
        return documentoRepository.findAllWithEvento(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoResponseDTO obtenerDocumento(Long idDocumento) {
        Documento documento = documentoRepository.findByIdWithRelations(idDocumento)
                .orElseThrow(() -> new DocumentoNotFoundException(idDocumento));
        return mapper.toDTO(documento);
    }

    // -------------------------------------------------------------------------
    // PLANTILLAS
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<PlantillaResponseDTO> listarPlantillas() {
        return plantillaRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlantillaResponseDTO obtenerPlantilla(Long idPlantilla) {
        PlantillaDocumento plantilla = plantillaRepository.findById(idPlantilla)
                .orElseThrow(() -> new PlantillaDocumentoNotFoundException(idPlantilla));
        return mapper.toDTO(plantilla);
    }

    @Override
    @Transactional
    public PlantillaResponseDTO actualizarPlantilla(Long idPlantilla, PlantillaUpdateDTO dto) {
        PlantillaDocumento plantilla = plantillaRepository.findById(idPlantilla)
                .orElseThrow(() -> new PlantillaDocumentoNotFoundException(idPlantilla));

        plantilla.setContenidoBase(dto.getContenidoBase());
        if (dto.getCamposEditables() != null) {
            plantilla.setCamposEditables(dto.getCamposEditables());
        }
        if (dto.getActivo() != null) {
            plantilla.setActivo(dto.getActivo());
        }

        log.info("Plantilla actualizada: id={}, tipo={}", idPlantilla, plantilla.getTipoDocumento());
        return mapper.toDTO(plantillaRepository.save(plantilla));
    }

    // -------------------------------------------------------------------------
    // HELPERS PRIVADOS
    // -------------------------------------------------------------------------

    /**
     * Rellena los placeholders {{campo}} de la plantilla HTML con los datos reales del evento.
     * Placeholders soportados según NOTAS_DB.md sección 3.18.
     */
    private String rellenarPlaceholders(String template, Evento evento) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");
        ResumenFinancieroDocumento resumenFinanciero = obtenerResumenFinanciero(evento);

        String resultado = template;

        resultado = resultado.replace("{{negocio_nombre}}", configuracionService.getString(CONFIG_NEGOCIO_NOMBRE));

        resultado = resultado.replace("{{cliente_nombre}}",
                evento.getCliente() != null ? evento.getCliente().getNombre() : "");

        resultado = resultado.replace("{{tipo_evento}}",
                evento.getTipoEvento() != null ? evento.getTipoEvento().getTipoNombre() : "");

        resultado = resultado.replace("{{fecha_evento}}",
                evento.getFecha() != null ? evento.getFecha().format(fechaFmt) : "");

        resultado = resultado.replace("{{hora_inicio}}",
                evento.getHoraInicio() != null ? evento.getHoraInicio().format(horaFmt) : "");

        resultado = resultado.replace("{{hora_fin}}",
                evento.getHoraFin() != null ? evento.getHoraFin().format(horaFmt) : "");

        resultado = resultado.replace("{{ubicacion}}",
                evento.getUbicacion() != null ? evento.getUbicacion() : "");

        resultado = resultado.replace("{{decoracion}}",
                evento.getDecoracion() != null ? evento.getDecoracion().getNombre() : "");

        resultado = resultado.replace("{{precio_total}}",
                formatearMoneda(evento.getPrecioTotal()));

        resultado = resultado.replace("{{fecha_generacion}}",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        resultado = resultado.replace("{{anticipo_pagado}}", formatearMoneda(resumenFinanciero.anticipoPagado()));
        resultado = resultado.replace("{{saldo_pendiente}}", formatearMoneda(resumenFinanciero.saldoPendiente()));
        resultado = resultado.replace("{{condiciones_cancelacion}}",
                configuracionService.getString(CONFIG_POLITICA_CANCELACION));

        resultado = resultado.replace("{{texto_adicional}}", "");
        return resultado;
    }

    private Documento crearDocumentoPendiente(Evento evento, PlantillaDocumento plantilla, DocumentoTipo tipoDocumento) {
        Documento documento = new Documento();
        documento.setEvento(evento);
        documento.setPlantilla(plantilla);
        documento.setDocuTipo(tipoDocumento);
        documento.setDocuEstadoEnvio(DocumentoEstadoEnvio.PENDIENTE);
        documento.setDocuIntentosEnvio(0);
        return documentoRepository.save(documento);
    }

    private Documento actualizarEstadoEnvio(
            Documento documento,
            DocumentoEstadoEnvio estado,
            int intentosEnvio,
            LocalDateTime fechaEnvio) {
        documento.setDocuEstadoEnvio(estado);
        documento.setDocuIntentosEnvio(intentosEnvio);
        documento.setDocuFechaEnvio(fechaEnvio);
        return documentoRepository.save(documento);
    }

    private String construirNombreArchivo(Evento evento) {
        return "confirmacion_evento_" + evento.getId() + ".pdf";
    }

    private String construirCuerpoConfirmacion(Evento evento) {
        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        return "<p>Estimado/a <b>" + evento.getCliente().getNombre()
                + "</b>,</p><p>Adjuntamos el documento de confirmación de su evento. "
                + "Gracias por confiar en " + negocioNombre + ".</p>";
    }

    private String construirCuerpoReenvio(Evento evento) {
        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        return "<p>Estimado/a <b>" + evento.getCliente().getNombre()
                + "</b>,</p><p>Le reenviamos el documento adjunto. "
                + "Gracias por confiar en " + negocioNombre + ".</p>";
    }

    private ResumenFinancieroDocumento obtenerResumenFinanciero(Evento evento) {
        BigDecimal anticipoPagado = pagoRepository
                .sumMontoConfirmadoByEventoIdAndConceptoNombre(evento.getId(), CONCEPTO_ANTICIPO);

        BigDecimal saldoPendiente = vistaPagosEventoRepository.findByIdEvento(evento.getId())
                .map(VistaPagosEvento::getSaldoPendiente)
                .orElseGet(() -> {
                    BigDecimal totalPagado = pagoRepository.sumMontoConfirmadoByEventoId(evento.getId());
                    BigDecimal precioTotal = evento.getPrecioTotal() != null ? evento.getPrecioTotal() : BigDecimal.ZERO;
                    return precioTotal.subtract(totalPagado).max(BigDecimal.ZERO);
                });

        return new ResumenFinancieroDocumento(anticipoPagado, saldoPendiente);
    }

    private String formatearMoneda(BigDecimal monto) {
        BigDecimal valor = monto != null ? monto : BigDecimal.ZERO;
        return "$" + valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private record ResumenFinancieroDocumento(BigDecimal anticipoPagado, BigDecimal saldoPendiente) {
    }
}
