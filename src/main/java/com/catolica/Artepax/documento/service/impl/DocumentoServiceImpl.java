package com.catolica.Artepax.documento.service.impl;
import com.catolica.Artepax.documento.dto.DocumentoResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaUpdateDTO;
import com.catolica.Artepax.documento.entity.Documento;
import com.catolica.Artepax.documento.entity.PlantillaDocumento;
import com.catolica.Artepax.documento.mapper.DocumentoMapper;
import com.catolica.Artepax.documento.repository.DocumentoRepository;
import com.catolica.Artepax.documento.repository.PlantillaDocumentoRepository;
import com.catolica.Artepax.documento.service.DocumentoService;
import com.catolica.Artepax.documento.service.EmailService;
import com.catolica.Artepax.documento.service.PdfGeneratorService;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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
    private static final int MAX_INTENTOS_ENVIO = 3;

    // -------------------------------------------------------------------------
    // DOCUMENTOS
    // -------------------------------------------------------------------------

    @Override
    @Transactional(noRollbackFor = RuntimeException.class) // P12: si el email falla el documento queda guardado
    public DocumentoResponseDTO generarDocumento(Long idEvento, String tipoDocumento) {
        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado: " + idEvento));

        PlantillaDocumento plantilla = plantillaRepository
                .findByTipoDocumentoAndActivoTrue(tipoDocumento)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe plantilla activa para el tipo: " + tipoDocumento));

        // Verificar que el cliente tiene correo antes de intentar envío futuro
        if (evento.getCliente().getCorreo() == null || evento.getCliente().getCorreo().isBlank()) {
            log.warn("Evento {}: cliente sin correo registrado. El documento se generará pero no podrá enviarse.", idEvento);
        }

        // Rellenar placeholders de la plantilla
        String contenidoGenerado = rellenarPlaceholders(plantilla.getContenidoBase(), evento);

        // Guardar el documento en PENDIENTE primero — así siempre queda persistido
        Documento documento = new Documento();
        documento.setEvento(evento);
        documento.setPlantilla(plantilla);
        documento.setDocuTipo(tipoDocumento);
        documento.setDocuEstadoEnvio("PENDIENTE");
        documento.setDocuIntentosEnvio(0);

        Documento guardado = documentoRepository.save(documento); // P12: commit garantizado por noRollbackFor
        log.info("Documento generado: id={}, tipo={}, evento={}", guardado.getIdDocumento(), tipoDocumento, idEvento);

        // Generar PDF desde el HTML con placeholders reemplazados
        byte[] pdfBytes = pdfGeneratorService.generarDesdHtml(contenidoGenerado);

        // Enviar por email si el cliente tiene correo
        String correo = evento.getCliente().getCorreo();
        if (correo != null && !correo.isBlank()) {
            String asunto = "Confirmación de reserva - Artepax";
            String cuerpoEmail = "<p>Estimado/a <b>" + evento.getCliente().getNombre()
                    + "</b>,</p><p>Adjuntamos el documento de confirmación de su evento. "
                    + "Gracias por confiar en Artepax.</p>";
            String nombreArchivo = "confirmacion_evento_" + evento.getId() + ".pdf";

            try {
                emailService.enviarConAdjunto(correo, asunto, cuerpoEmail, pdfBytes, nombreArchivo);
                guardado.setDocuEstadoEnvio("ENVIADO");
                guardado.setDocuFechaEnvio(LocalDateTime.now());
                guardado.setDocuIntentosEnvio(1);
                log.info("Email enviado para documento id={}", guardado.getIdDocumento());
            } catch (RuntimeException e) {
                guardado.setDocuEstadoEnvio("FALLIDO");
                guardado.setDocuIntentosEnvio(1);
                log.error("Error enviando email para documento id={}: {}", guardado.getIdDocumento(), e.getMessage());
                // No relanzamos — el documento queda en FALLIDO y puede reintentarse con /reenviar
            }

            documentoRepository.save(guardado); // P12: persiste ENVIADO o FALLIDO
        } else {
            log.warn("Evento {}: cliente sin correo, PDF generado pero no enviado.", idEvento);
        }

        return mapper.toDTO(guardado);
    }

    @Override
    @Transactional
    public DocumentoResponseDTO reenviarDocumento(Long idDocumento) {
        Documento documento = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado: " + idDocumento));

        String correo = documento.getEvento().getCliente().getCorreo();
        if (correo == null || correo.isBlank()) {
            throw new IllegalStateException(
                    "No se puede reenviar: el cliente no tiene correo registrado.");
        }

        if (documento.getDocuIntentosEnvio() >= MAX_INTENTOS_ENVIO) {
            throw new IllegalStateException(
                    "Se alcanzó el límite de " + MAX_INTENTOS_ENVIO + " intentos de envío.");
        }

        // Regenerar el PDF desde la plantilla y el evento — así evitamos depender de Cloudinary
        PlantillaDocumento plantilla = documento.getPlantilla();
        if (plantilla == null) {
            throw new IllegalStateException("El documento no tiene plantilla asociada.");
        }

        Evento evento = documento.getEvento();
        String contenidoGenerado = rellenarPlaceholders(plantilla.getContenidoBase(), evento);
        byte[] pdfBytes = pdfGeneratorService.generarDesdHtml(contenidoGenerado);

        try {
            String asunto = "Reenvío de documento - Artepax";
            String cuerpoEmail = "<p>Estimado/a <b>" + evento.getCliente().getNombre()
                    + "</b>,</p><p>Le reenviamos el documento adjunto. "
                    + "Gracias por confiar en Artepax.</p>";
            String nombreArchivo = "confirmacion_evento_" + evento.getId() + ".pdf";

            emailService.enviarConAdjunto(correo, asunto, cuerpoEmail, pdfBytes, nombreArchivo);

            documento.setDocuEstadoEnvio("REENVIADO");
            documento.setDocuFechaEnvio(LocalDateTime.now());
            documento.setDocuIntentosEnvio(documento.getDocuIntentosEnvio() + 1);

            log.info("Reenvío exitoso documento id={} intento={}", idDocumento, documento.getDocuIntentosEnvio());

        } catch (RuntimeException e) {
            documento.setDocuEstadoEnvio("FALLIDO");
            documento.setDocuIntentosEnvio(documento.getDocuIntentosEnvio() + 1);
            documentoRepository.save(documento);
            log.error("Error en reenvío documento id={}: {}", idDocumento, e.getMessage());
            throw e;
        }

        return mapper.toDTO(documentoRepository.save(documento));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> listarPorEvento(Long idEvento) {
        if (!eventoRepository.existsById(idEvento)) {
            throw new IllegalArgumentException("Evento no encontrado: " + idEvento);
        }
        return documentoRepository
                .findByEvento_IdOrderByDocuFechaGeneracionDesc(idEvento)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoResponseDTO obtenerDocumento(Long idDocumento) {
        Documento documento = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado: " + idDocumento));
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
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PlantillaResponseDTO obtenerPlantilla(Long idPlantilla) {
        PlantillaDocumento plantilla = plantillaRepository.findById(idPlantilla)
                .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));
        return mapper.toDTO(plantilla);
    }

    @Override
    @Transactional
    public PlantillaResponseDTO actualizarPlantilla(Long idPlantilla, PlantillaUpdateDTO dto) {
        PlantillaDocumento plantilla = plantillaRepository.findById(idPlantilla)
                .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));

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

        String resultado = template;

        resultado = resultado.replace("{{negocio_nombre}}", "Artepax");

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
                evento.getPrecioTotal() != null ? "$" + evento.getPrecioTotal().toString() : "$0.00");

        resultado = resultado.replace("{{fecha_generacion}}",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        resultado = resultado.replace("{{anticipo_pagado}}", "");
        resultado = resultado.replace("{{saldo_pendiente}}", "");
        resultado = resultado.replace("{{condiciones_cancelacion}}",
                "En caso de cancelación con menos de 48 horas de anticipación, " +
                        "se aplicará una penalización del 20% sobre el precio total.");

        resultado = resultado.replace("{{texto_adicional}}", "");
        return resultado;
    }
}
