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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.catolica.Artepax.common.constant.AuditoriaAcciones.GENERAR_DOCUMENTO;
import static com.catolica.Artepax.common.constant.AuditoriaAcciones.REENVIAR_DOCUMENTO;
import static com.catolica.Artepax.common.constant.AuditoriaTablas.DOCUMENTO;

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
    private static final int MAX_INTENTOS_ENVIO = 3;
    private static final String CONCEPTO_ANTICIPO = "ANTICIPO";
    private static final String CONFIG_NEGOCIO_NOMBRE = "NEGOCIO_NOMBRE";
    private static final String CONFIG_POLITICA_CANCELACION = "POLITICA_CANCELACION_MONTAJE";
    private static final String CONFIG_NEGOCIO_CORREO = "NEGOCIO_CORREO";
    private static final String CONFIG_NEGOCIO_TELEFONO = "NEGOCIO_TELEFONO";
    private static final String CONFIG_INSTRUCCIONES_PAGO = "INSTRUCCIONES_PAGO";

    // -------------------------------------------------------------------------
    // DOCUMENTOS
    // -------------------------------------------------------------------------

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
        documento = documentoRepository.save(documento);
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
            byte[] pdfBytes = pdfGeneratorService.generarDesdHtml(contenidoGenerado);
            emailService.enviarConAdjunto(
                    correo,
                    construirAsunto(tipoDocumento),
                    construirCuerpoDocumento(evento, tipoDocumento),
                    pdfBytes,
                    construirNombreArchivo(evento));

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
            byte[] pdfBytes = pdfGeneratorService.generarDesdHtml(contenidoGenerado);
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

    @Override
    @Transactional
    public DocumentoResponseDTO reenviarDocumento(Long idDocumento) {
        Documento documento = documentoRepository.findByIdWithRelations(idDocumento)
                .orElseThrow(() -> new DocumentoNotFoundException(idDocumento));

        documentoPolicy.validarReenvioPermitido(documento);

        PlantillaDocumento plantilla = resolverPlantillaParaReenvio(documento);

        Evento evento = documento.getEvento();
        String correo = evento.getCliente().getCorreo();

        try {
            String contenidoGenerado = rellenarPlaceholders(plantilla.getContenidoBase(), evento);
            byte[] pdfBytes = pdfGeneratorService.generarDesdHtml(contenidoGenerado);
            emailService.enviarConAdjunto(
                    correo,
                    construirAsunto(documento.getDocuTipo()),
                    construirCuerpoReenvio(evento),
                    pdfBytes,
                    construirNombreArchivo(evento));

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

    private String rellenarPlaceholdersAvisoSaldo(String template, Evento evento, ResumenFinancieroDocumento resumen) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");

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
        resultado = resultado.replace("{{total_pagado}}", formatearMoneda(resumen.anticipoPagado()));
        resultado = resultado.replace("{{saldo_pendiente}}", formatearMoneda(resumen.saldoPendiente()));
        resultado = resultado.replace("{{negocio_correo}}", configuracionService.getString(CONFIG_NEGOCIO_CORREO));
        resultado = resultado.replace("{{negocio_telefono}}", configuracionService.getString(CONFIG_NEGOCIO_TELEFONO));
        resultado = resultado.replace("{{instrucciones_pago}}", configuracionService.getString(CONFIG_INSTRUCCIONES_PAGO));
        resultado = resultado.replace("{{mensaje_adicional}}", "");
        resultado = resultado.replace("{{fecha_limite_pago}}",
                evento.getFecha() != null ? evento.getFecha().minusDays(7).format(fechaFmt) : "");

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

    private String construirCuerpoCotizacion(Evento evento) {
        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        return "<p>Estimado/a <b>" + evento.getCliente().getNombre()
                + "</b>,</p><p>Adjuntamos la cotización de su evento. "
                + "Gracias por considerar a " + negocioNombre + ".</p>";
    }

    private String construirCuerpoAvisoSaldo(Evento evento) {
        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        return "<p>Estimado/a <b>" + evento.getCliente().getNombre()
                + "</b>,</p><p>Adjuntamos el aviso formal de saldo pendiente de su evento. "
                + "Gracias por su atención. <b>" + negocioNombre + "</b></p>";
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

    private DocumentoAuditSnapshot toSnapshot(Documento d) {
        return new DocumentoAuditSnapshot(
                d.getIdDocumento(),
                d.getEvento() != null ? d.getEvento().getId() : null,
                d.getDocuTipo() != null ? d.getDocuTipo().name() : null,
                d.getDocuEstadoEnvio() != null ? d.getDocuEstadoEnvio().name() : null,
                d.getDocuIntentosEnvio(),
                d.getDocuFechaEnvio());
    }

    private record ResumenFinancieroDocumento(BigDecimal anticipoPagado, BigDecimal saldoPendiente) {
    }
}
