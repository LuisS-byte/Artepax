package com.catolica.Artepax.documento.dto;

import lombok.Data;

import java.time.LocalDateTime;

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

}
