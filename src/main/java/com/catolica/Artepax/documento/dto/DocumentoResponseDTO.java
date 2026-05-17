package com.catolica.Artepax.documento.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO representing a document for API responses.
 * <p>
 * Carries all relevant document data including type, generation date, file URL,
 * delivery status, and associated event/client information.
 * </p>
 */
@Data
public class DocumentoResponseDTO {
    private Long idDocumento;
    private Long idEvento;
    private String docuTipo;
    private LocalDateTime docuFechaGeneracion;
    private String archivoUrl;
    private String docuEstadoEnvio;
    private LocalDateTime docuFechaEnvio;
    private Integer docuIntentosEnvio;
    private String clienteNombre;
    private String estadoEventoNombre;

}
