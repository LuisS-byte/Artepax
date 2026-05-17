package com.catolica.Artepax.documento.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for updating a document template.
 * <p>
 * Only the base content, editable fields, and active flag can be modified.
 * The document type ({@code tipoDocumento}) is the template's key and cannot be changed.
 * </p>
 */
@Data
public class PlantillaUpdateDTO {

    // Solo se permite editar el contenido y los campos editables,
    // NO el tipo_documento (es la clave de la plantilla)
    @NotBlank(message = "El contenido base no puede estar vacío")
    private String contenidoBase;

    private String camposEditables;

    private Boolean activo;
}
