package com.catolica.Artepax.documento.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO representing a document template for API responses.
 * <p>
 * Contains the template's base HTML content, editable field descriptors,
 * and activation status.
 * </p>
 */
@Data
public class PlantillaResponseDTO {
    private Long idPlantilla;
    private String tipoDocumento;
    private String contenidoBase;
    private String camposEditables;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
