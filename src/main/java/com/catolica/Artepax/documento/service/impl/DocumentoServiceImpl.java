package com.catolica.Artepax.documento.service.impl;

import com.catolica.Artepax.common.audit.DocumentoAuditSnapshot;
import com.catolica.Artepax.common.service.AuditoriaService;
import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.comunicacion.service.ComunicacionClienteService;
import com.catolica.Artepax.comunicacion.service.EmailService;
import com.catolica.Artepax.documento.dto.DocumentoResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaUpdateDTO;
import com.catolica.Artepax.documento.entity.Documento;
import com.catolica.Artepax.documento.entity.DocumentoEstadoEnvio;
import com.catolica.Artepax.documento.entity.DocumentoTipo;
import com.catolica.Artepax.documento.entity.PlantillaDocumento;
import com.catolica.Artepax.documento.exception.DocumentoNotFoundException;
import com.catolica.Artepax.documento.exception.DocumentoOperacionNoPermitidaException;
import com.catolica.Artepax.documento.exception.PlantillaDocumentoNotFoundException;
import com.catolica.Artepax.documento.mapper.DocumentoMapper;
import com.catolica.Artepax.documento.repository.DocumentoRepository;
import com.catolica.Artepax.documento.repository.PlantillaDocumentoRepository;
import com.catolica.Artepax.documento.service.DocumentoService;
import com.catolica.Artepax.documento.service.PdfGeneratorService;
import com.catolica.Artepax.documento.service.policy.DocumentoPolicy;
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
import org.springframework.web.util.HtmlUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.catolica.Artepax.common.constant.AuditoriaAcciones.GENERAR_DOCUMENTO;
import static com.catolica.Artepax.common.constant.AuditoriaAcciones.REENVIAR_DOCUMENTO;
import static com.catolica.Artepax.common.constant.AuditoriaTablas.DOCUMENTO;

/**
 * Implementation of {@link DocumentoService} for document generation and delivery.
 * <p>
 * Documents are generated from HTML templates with placeholder substitution, rendered
 * to PDF via {@link PdfGeneratorService}, and sent to the client's email. If email
 * delivery fails, the document is persisted with status {@link DocumentoEstadoEnvio#FALLIDO}
 * instead of rolling back the transaction, allowing later retry via
 * {@link #reenviarDocumento}. This fail-soft pattern ensures the generated document
 * record and its audit trail are never lost due to a transient email issue.
 * </p>
 *
 * <h3>Document state flow</h3>
 * <pre>{@code
 * PENDIENTE ──► ENVIADO ──► FALLIDO ──► REENVIADO
 * }</pre>
 *
 * <h3>Financial document generation</h3>
 * Documents such as the outstanding balance notice ({@link DocumentoTipo#AVISO_SALDO_PENDIENTE})
 * compute the client's payment summary from both the {@code pago} table and the
 * {@code vista_pagos_evento} database view, which is read after a flush to ensure
 * data consistency within the transaction.
 */
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
    private final DocumentoPolicy documentoPolicy;
    private final ComunicacionClienteService comunicacionClienteService;
    private final AuditoriaService auditoriaService;
    private static final String CONCEPTO_ANTICIPO = "ANTICIPO";
    private static final String CONFIG_NEGOCIO_NOMBRE = "NEGOCIO_NOMBRE";
    private static final String CONFIG_POLITICA_CANCELACION = "POLITICA_CANCELACION_MONTAJE";
    private static final String CONFIG_NEGOCIO_CORREO = "NEGOCIO_CORREO";
    private static final String CONFIG_NEGOCIO_TELEFONO = "NEGOCIO_TELEFONO";
    private static final String CONFIG_INSTRUCCIONES_PAGO = "INSTRUCCIONES_PAGO";

    // -------------------------------------------------------------------------
    // DOCUMENTOS
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * <strong>Implementation details:</strong>
     * The document entity is persisted first (status {@code PENDIENTE}), then the PDF
     * is generated and emailed. If any {@link RuntimeException} occurs during PDF
     * generation, disk persistence, or email delivery, the exception is caught and the
     * document state is set to {@link DocumentoEstadoEnvio#FALLIDO} so the transaction
     * commits normally. This allows the document to be retried via {@link #reenviarDocumento}
     * without losing the generation record.
     * </p>
     */
    @Override
    @Transactional
    public DocumentoResponseDTO generarDocumento(Long idEvento, DocumentoTipo tipoDocumento) {
        Evento evento = eventoRepository.findDocumentoContextById(idEvento)
                .orElseThrow(() -> new EventoNotFoundException(idEvento));

        documentoPolicy.validarGeneracionPermitida(evento, tipoDocumento);

        if (documentoRepository.existsByEventoIdAndDocuTipo(idEvento, tipoDocumento)) {
            documentoPolicy.validarSinDocumentoDuplicado(idEvento, tipoDocumento, true);
        }

        PlantillaDocumento plantilla = plantillaRepository
                .findByTipoDocumentoAndActivoTrue(tipoDocumento)
                .orElseThrow(() -> new DocumentoOperacionNoPermitidaException(
                        "No existe plantilla activa para el tipo: " + tipoDocumento));
        documentoPolicy.validarPlantillaDisponible(plantilla);

        Documento documento = crearDocumentoPendiente(evento, plantilla, tipoDocumento);
        log.info("Documento creado: id={}, tipo={}, evento={}", documento.getIdDocumento(), tipoDocumento, idEvento);

        String correo = evento.getCliente().getCorreo();
        if (correo == null || correo.isBlank()) {
            log.warn("Evento {}: cliente sin correo registrado. El documento queda pendiente hasta contar con destinatario.", idEvento);
            auditoriaService.registrar(GENERAR_DOCUMENTO, DOCUMENTO, documento.getIdDocumento(),
                    null, toSnapshot(documento), "Documento generado sin envío (cliente sin correo)");
            return mapper.toDTO(documento);
        }

        try {
            String contenidoGenerado = rellenarPlaceholders(plantilla.getContenidoBase(), evento);
            byte[] pdfBytes = pdfGeneratorService.generarDesdeHtml(contenidoGenerado);
            String urlDescarga = guardarPdfEnDisco(pdfBytes, documento.getIdDocumento());
            documento.setArchivoUrl(urlDescarga);
            emailService.enviarConAdjunto(
                    correo,
                    construirAsunto(tipoDocumento),
                    construirCuerpoDocumento(evento, tipoDocumento),
                    pdfBytes,
                    construirNombreArchivo(tipoDocumento, evento));

            documento = actualizarEstadoEnvio(documento, DocumentoEstadoEnvio.ENVIADO, 1, LocalDateTime.now());
            marcarClienteNotificado(evento);
            log.info("Email enviado para documento id={}", documento.getIdDocumento());
            auditoriaService.registrar(GENERAR_DOCUMENTO, DOCUMENTO, documento.getIdDocumento(),
                    null, toSnapshot(documento), "Documento generado y enviado exitosamente");
        } catch (RuntimeException e) {
            documento = actualizarEstadoEnvio(documento, DocumentoEstadoEnvio.FALLIDO, 1, null);
            log.error("Error procesando documento id={}: {}", documento.getIdDocumento(), e.getMessage(), e);
            auditoriaService.registrar(GENERAR_DOCUMENTO, DOCUMENTO, documento.getIdDocumento(),
                    null, toSnapshot(documento), "Error al generar documento: " + e.getMessage());
        }

        return mapper.toDTO(documento);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Computes the financial summary from the payment repository and the
     * {@code vista_pagos_evento} database view before generating the notice.
     * Like {@link #generarDocumento}, failures during PDF generation or delivery
     * are caught and result in a {@link DocumentoEstadoEnvio#FALLIDO} state
     * rather than a rollback.
     * </p>
     */
    @Override
    @Transactional
    public DocumentoResponseDTO generarAvisoSaldoPendiente(Long eventoId) {
        Evento evento = eventoRepository.findDocumentoContextById(eventoId)
                .orElseThrow(() -> new EventoNotFoundException(eventoId));

        documentoPolicy.validarGeneracionPermitida(evento, DocumentoTipo.AVISO_SALDO_PENDIENTE);

        if (documentoRepository.existsByEventoIdAndDocuTipo(eventoId, DocumentoTipo.AVISO_SALDO_PENDIENTE)) {
            documentoPolicy.validarSinDocumentoDuplicado(eventoId, DocumentoTipo.AVISO_SALDO_PENDIENTE, true);
        }

        ResumenFinancieroDocumento resumen = obtenerResumenFinanciero(evento);
        documentoPolicy.validarSaldoPendiente(resumen.saldoPendiente());

        PlantillaDocumento plantilla = plantillaRepository
                .findByTipoDocumentoAndActivoTrue(DocumentoTipo.AVISO_SALDO_PENDIENTE)
                .orElseThrow(() -> new DocumentoOperacionNoPermitidaException(
                        "No existe plantilla activa para el tipo: AVISO_SALDO_PENDIENTE"));
        documentoPolicy.validarPlantillaDisponible(plantilla);

        Documento documento = crearDocumentoPendiente(evento, plantilla, DocumentoTipo.AVISO_SALDO_PENDIENTE);
        log.info("Aviso de saldo pendiente generado: id={}, evento={}", documento.getIdDocumento(), eventoId);

        String correo = evento.getCliente().getCorreo();
        if (correo == null || correo.isBlank()) {
            log.warn("Evento {}: cliente sin correo registrado. El documento queda pendiente.", eventoId);
            auditoriaService.registrar(GENERAR_DOCUMENTO, DOCUMENTO, documento.getIdDocumento(),
                    null, toSnapshot(documento), "Aviso saldo pendiente sin envío (cliente sin correo)");
            return mapper.toDTO(documento);
        }

        try {
            String contenidoGenerado = rellenarPlaceholdersAvisoSaldo(plantilla.getContenidoBase(), evento, resumen);
            byte[] pdfBytes = pdfGeneratorService.generarDesdeHtml(contenidoGenerado);
            String urlDescarga = guardarPdfEnDisco(pdfBytes, documento.getIdDocumento());
            documento.setArchivoUrl(urlDescarga);
            comunicacionClienteService.enviarAvisoSaldoPendienteConDocumento(evento, pdfBytes);

            documento = actualizarEstadoEnvio(documento, DocumentoEstadoEnvio.ENVIADO, 1, LocalDateTime.now());
            marcarClienteNotificado(evento);
            log.info("Aviso de saldo pendiente enviado para documento id={}", documento.getIdDocumento());
            auditoriaService.registrar(GENERAR_DOCUMENTO, DOCUMENTO, documento.getIdDocumento(),
                    null, toSnapshot(documento), "Aviso saldo pendiente generado y enviado exitosamente");
        } catch (RuntimeException e) {
            documento = actualizarEstadoEnvio(documento, DocumentoEstadoEnvio.FALLIDO, 1, null);
            log.error("Error procesando aviso de saldo pendiente id={}: {}", documento.getIdDocumento(), e.getMessage(), e);
            auditoriaService.registrar(GENERAR_DOCUMENTO, DOCUMENTO, documento.getIdDocumento(),
                    null, toSnapshot(documento), "Error al generar aviso saldo pendiente: " + e.getMessage());
        }

        return mapper.toDTO(documento);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Re-renders the PDF from the original (or current active) template and sends it
     * via email. On failure, the document state is set to {@link DocumentoEstadoEnvio#FALLIDO}
     * but the transaction commits normally so the updated intent count is preserved.
     * </p>
     */
    @Override
    @Transactional
    public DocumentoResponseDTO reenviarDocumento(Long idDocumento) {
        Documento documento = documentoRepository.findByIdWithRelations(idDocumento)
                .orElseThrow(() -> new DocumentoNotFoundException(idDocumento));

        documentoPolicy.validarReenvioPermitido(documento);

        PlantillaDocumento plantilla = resolverPlantillaParaReenvio(documento);

        Evento evento = documento.getEvento();
        String correo = evento.getCliente().getCorreo();
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El cliente no tiene correo registrado para reenviar el documento.");
        }

        try {
            String contenidoGenerado;
            if (documento.getDocuTipo() == DocumentoTipo.AVISO_SALDO_PENDIENTE) {
                ResumenFinancieroDocumento resumen = obtenerResumenFinanciero(evento);
                contenidoGenerado = rellenarPlaceholdersAvisoSaldo(plantilla.getContenidoBase(), evento, resumen);
            } else {
                contenidoGenerado = rellenarPlaceholders(plantilla.getContenidoBase(), evento);
            }
            byte[] pdfBytes = pdfGeneratorService.generarDesdeHtml(contenidoGenerado);
            String urlDescarga = guardarPdfEnDisco(pdfBytes, documento.getIdDocumento());
            documento.setArchivoUrl(urlDescarga);
            emailService.enviarConAdjunto(
                    correo,
                    construirAsunto(documento.getDocuTipo()),
                    construirCuerpoReenvio(evento),
                    pdfBytes,
                    construirNombreArchivo(documento.getDocuTipo(), evento));

            documento = actualizarEstadoEnvio(
                    documento,
                    DocumentoEstadoEnvio.REENVIADO,
                    documento.getDocuIntentosEnvio() + 1,
                    LocalDateTime.now());
            marcarClienteNotificado(evento);
            log.info("Reenvío exitoso documento id={} intento={}", idDocumento, documento.getDocuIntentosEnvio());
            auditoriaService.registrar(REENVIAR_DOCUMENTO, DOCUMENTO, idDocumento,
                    null, toSnapshot(documento), "Documento reenviado exitosamente");

        } catch (RuntimeException e) {
            documento = actualizarEstadoEnvio(
                    documento,
                    DocumentoEstadoEnvio.FALLIDO,
                    documento.getDocuIntentosEnvio() + 1,
                    null);
            log.error("Error en reenvío documento id={}: {}", idDocumento, e.getMessage(), e);
            auditoriaService.registrar(REENVIAR_DOCUMENTO, DOCUMENTO, idDocumento,
                    null, toSnapshot(documento), "Error al reenviar documento: " + e.getMessage());
            return mapper.toDTO(documento);
        }

        return mapper.toDTO(documento);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentoResponseDTO> obtenerTodos(Pageable pageable) {
        return documentoRepository.findAllWithEvento(pageable)
                .map(mapper::toDTO);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<PlantillaResponseDTO> listarPlantillas() {
        return plantillaRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PlantillaResponseDTO obtenerPlantilla(Long idPlantilla) {
        PlantillaDocumento plantilla = plantillaRepository.findById(idPlantilla)
                .orElseThrow(() -> new PlantillaDocumentoNotFoundException(idPlantilla));
        return mapper.toDTO(plantilla);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PlantillaResponseDTO actualizarPlantilla(Long idPlantilla, PlantillaUpdateDTO dto) {
        PlantillaDocumento plantilla = plantillaRepository.findById(idPlantilla)
                .orElseThrow(() -> new PlantillaDocumentoNotFoundException(idPlantilla));

        plantilla.setContenidoBase(dto.getContenidoBase().trim());
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

        resultado = resultado.replace("{{negocio_nombre}}", escapeHtmlDoc(configuracionService.getString(CONFIG_NEGOCIO_NOMBRE)));

        resultado = resultado.replace("{{cliente_nombre}}",
                escapeHtmlDoc(evento.getCliente() != null ? evento.getCliente().getNombre() : ""));

        resultado = resultado.replace("{{tipo_evento}}",
                escapeHtmlDoc(evento.getTipoEvento() != null ? evento.getTipoEvento().getTipoNombre() : ""));

        resultado = resultado.replace("{{fecha_evento}}",
                escapeHtmlDoc(evento.getFecha() != null ? evento.getFecha().format(fechaFmt) : ""));

        resultado = resultado.replace("{{hora_inicio}}",
                escapeHtmlDoc(evento.getHoraInicio() != null ? evento.getHoraInicio().format(horaFmt) : ""));

        resultado = resultado.replace("{{hora_fin}}",
                escapeHtmlDoc(evento.getHoraFin() != null ? evento.getHoraFin().format(horaFmt) : ""));

        resultado = resultado.replace("{{ubicacion}}",
                escapeHtmlDoc(evento.getUbicacion() != null ? evento.getUbicacion() : ""));

        resultado = resultado.replace("{{decoracion}}",
                escapeHtmlDoc(evento.getDecoracion() != null ? evento.getDecoracion().getNombre() : ""));

        resultado = resultado.replace("{{precio_total}}",
                formatearMoneda(evento.getPrecioTotal()));

        resultado = resultado.replace("{{fecha_generacion}}",
                escapeHtmlDoc(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));

        resultado = resultado.replace("{{anticipo_pagado}}", formatearMoneda(resumenFinanciero.anticipoPagado()));
        resultado = resultado.replace("{{total_pagado}}", formatearMoneda(resumenFinanciero.totalPagado()));
        resultado = resultado.replace("{{saldo_pendiente}}", formatearMoneda(resumenFinanciero.saldoPendiente()));
        resultado = resultado.replace("{{condiciones_cancelacion}}",
                escapeHtmlDoc(configuracionService.getString(CONFIG_POLITICA_CANCELACION)));

        resultado = resultado.replace("{{negocio_correo}}", escapeHtmlDoc(configuracionService.getString(CONFIG_NEGOCIO_CORREO)));
        resultado = resultado.replace("{{negocio_telefono}}", escapeHtmlDoc(configuracionService.getString(CONFIG_NEGOCIO_TELEFONO)));

        resultado = resultado.replace("{{texto_adicional}}", "");
        return resultado;
    }

    private String rellenarPlaceholdersAvisoSaldo(String template, Evento evento, ResumenFinancieroDocumento resumen) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");

        String resultado = template;

        resultado = resultado.replace("{{negocio_nombre}}", escapeHtmlDoc(configuracionService.getString(CONFIG_NEGOCIO_NOMBRE)));
        resultado = resultado.replace("{{cliente_nombre}}",
                escapeHtmlDoc(evento.getCliente() != null ? evento.getCliente().getNombre() : ""));
        resultado = resultado.replace("{{tipo_evento}}",
                escapeHtmlDoc(evento.getTipoEvento() != null ? evento.getTipoEvento().getTipoNombre() : ""));
        resultado = resultado.replace("{{fecha_evento}}",
                escapeHtmlDoc(evento.getFecha() != null ? evento.getFecha().format(fechaFmt) : ""));
        resultado = resultado.replace("{{hora_inicio}}",
                escapeHtmlDoc(evento.getHoraInicio() != null ? evento.getHoraInicio().format(horaFmt) : ""));
        resultado = resultado.replace("{{hora_fin}}",
                escapeHtmlDoc(evento.getHoraFin() != null ? evento.getHoraFin().format(horaFmt) : ""));
        resultado = resultado.replace("{{ubicacion}}",
                escapeHtmlDoc(evento.getUbicacion() != null ? evento.getUbicacion() : ""));
        resultado = resultado.replace("{{decoracion}}",
                escapeHtmlDoc(evento.getDecoracion() != null ? evento.getDecoracion().getNombre() : ""));
        resultado = resultado.replace("{{precio_total}}",
                formatearMoneda(evento.getPrecioTotal()));
        resultado = resultado.replace("{{fecha_generacion}}",
                escapeHtmlDoc(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        resultado = resultado.replace("{{total_pagado}}", formatearMoneda(resumen.totalPagado()));
        resultado = resultado.replace("{{saldo_pendiente}}", formatearMoneda(resumen.saldoPendiente()));
        resultado = resultado.replace("{{negocio_correo}}", escapeHtmlDoc(configuracionService.getString(CONFIG_NEGOCIO_CORREO)));
        resultado = resultado.replace("{{negocio_telefono}}", escapeHtmlDoc(configuracionService.getString(CONFIG_NEGOCIO_TELEFONO)));
        resultado = resultado.replace("{{instrucciones_pago}}", escapeHtmlDoc(configuracionService.getString(CONFIG_INSTRUCCIONES_PAGO)));
        resultado = resultado.replace("{{mensaje_adicional}}", "");
        resultado = resultado.replace("{{fecha_limite_pago}}",
                escapeHtmlDoc(evento.getFecha() != null ? evento.getFecha().minusDays(7).format(fechaFmt) : ""));
        resultado = resultado.replace("{{condiciones_cancelacion}}",
                escapeHtmlDoc(configuracionService.getString(CONFIG_POLITICA_CANCELACION)));

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

    private String construirNombreArchivo(DocumentoTipo tipo, Evento evento) {
        String prefijo = switch (tipo) {
            case COTIZACION_EVENTO -> "cotizacion_evento";
            case CONFIRMACION_RESERVA -> "confirmacion_reserva";
            case AVISO_SALDO_PENDIENTE -> "aviso_saldo_pendiente";
            case SALDO_PENDIENTE -> "saldo_pendiente";
        };
        return prefijo + "_" + evento.getId() + ".pdf";
    }

    private PlantillaDocumento resolverPlantillaParaReenvio(Documento documento) {
        PlantillaDocumento plantilla = documento.getPlantilla();
        if (plantilla != null) {
            documentoPolicy.validarPlantillaDisponible(plantilla);
            return plantilla;
        }

        PlantillaDocumento plantillaActiva = plantillaRepository
                .findByTipoDocumentoAndActivoTrue(documento.getDocuTipo())
                .orElseThrow(() -> new DocumentoOperacionNoPermitidaException(
                        "No existe plantilla activa para el tipo: " + documento.getDocuTipo().name()));
        documento.setPlantilla(plantillaActiva);
        return plantillaActiva;
    }

    private void marcarClienteNotificado(Evento evento) {
        evento.setClienteNotificado(true);
        eventoRepository.save(evento);
    }

    private String construirAsunto(DocumentoTipo tipoDocumento) {
        return switch (tipoDocumento) {
            case COTIZACION_EVENTO -> "Cotización del evento - Artepax";
            case AVISO_SALDO_PENDIENTE -> "Aviso de saldo pendiente - " + configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
            default -> "Confirmación de reserva - Artepax";
        };
    }

    private String construirCuerpoDocumento(Evento evento, DocumentoTipo tipoDocumento) {
        return switch (tipoDocumento) {
            case COTIZACION_EVENTO -> construirCuerpoCotizacion(evento);
            case AVISO_SALDO_PENDIENTE -> construirCuerpoAvisoSaldo(evento);
            default -> construirCuerpoConfirmacion(evento);
        };
    }

    private String construirCuerpoConfirmacion(Evento evento) {
        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        return "<p>Estimado/a <b>" + escapeHtmlDoc(evento.getCliente().getNombre())
                + "</b>,</p><p>Adjuntamos el documento de confirmación de su evento. "
                + "Gracias por confiar en " + escapeHtmlDoc(negocioNombre) + ".</p>";
    }

    private String construirCuerpoReenvio(Evento evento) {
        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        return "<p>Estimado/a <b>" + escapeHtmlDoc(evento.getCliente().getNombre())
                + "</b>,</p><p>Le reenviamos el documento adjunto. "
                + "Gracias por confiar en " + escapeHtmlDoc(negocioNombre) + ".</p>";
    }

    private String construirCuerpoCotizacion(Evento evento) {
        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        return "<p>Estimado/a <b>" + escapeHtmlDoc(evento.getCliente().getNombre())
                + "</b>,</p><p>Adjuntamos la cotización de su evento. "
                + "Gracias por considerar a " + escapeHtmlDoc(negocioNombre) + ".</p>";
    }

    private String construirCuerpoAvisoSaldo(Evento evento) {
        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        return "<p>Estimado/a <b>" + escapeHtmlDoc(evento.getCliente().getNombre())
                + "</b>,</p><p>Adjuntamos el aviso formal de saldo pendiente de su evento. "
                + "Gracias por su atención. <b>" + escapeHtmlDoc(negocioNombre) + "</b></p>";
    }

    private ResumenFinancieroDocumento obtenerResumenFinanciero(Evento evento) {
        BigDecimal anticipoPagado = pagoRepository
                .sumMontoConfirmadoByEventoIdAndConceptoNombre(evento.getId(), CONCEPTO_ANTICIPO);

        BigDecimal totalPagado = pagoRepository.sumMontoConfirmadoByEventoId(evento.getId());

        BigDecimal saldoPendiente = vistaPagosEventoRepository.findByIdEvento(evento.getId())
                .map(VistaPagosEvento::getSaldoPendiente)
                .orElseGet(() -> {
                    BigDecimal precioTotal = evento.getPrecioTotal() != null ? evento.getPrecioTotal() : BigDecimal.ZERO;
                    return precioTotal.subtract(totalPagado).max(BigDecimal.ZERO);
                });

        return new ResumenFinancieroDocumento(anticipoPagado, totalPagado, saldoPendiente);
    }

    private String formatearMoneda(BigDecimal monto) {
        BigDecimal valor = monto != null ? monto : BigDecimal.ZERO;
        return "$" + valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String escapeHtmlDoc(String value) {
        if (value == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(value);
    }

    /**
     * Persiste el PDF generado en disco local y retorna la URL de descarga.
     * Si falla la escritura, solo loguea la advertencia sin interrumpir el flujo.
     */
    private String guardarPdfEnDisco(byte[] pdfBytes, Long documentoId) {
        try {
            Path directorio = Path.of("storage", "documentos");
            Files.createDirectories(directorio);
            Path archivo = directorio.resolve(documentoId + ".pdf");
            Files.write(archivo, pdfBytes);
            log.info("PDF guardado en disco: {}", archivo.toAbsolutePath());
            return "/api/documentos/" + documentoId + "/descarga";
        } catch (Exception e) {
            log.warn("No se pudo guardar el PDF en disco para documento {}: {}", documentoId, e.getMessage());
            return null;
        }
    }

    private DocumentoAuditSnapshot toSnapshot(Documento d) {
        return new DocumentoAuditSnapshot(
                d.getIdDocumento(),
                d.getEvento() != null ? d.getEvento().getId() : null,
                d.getDocuTipo() != null ? d.getDocuTipo().name() : null,
                d.getDocuEstadoEnvio() != null ? d.getDocuEstadoEnvio().name() : null,
                d.getDocuIntentosEnvio(),
                d.getDocuFechaEnvio());
    }

    private record ResumenFinancieroDocumento(BigDecimal anticipoPagado, BigDecimal totalPagado, BigDecimal saldoPendiente) {
    }
}
