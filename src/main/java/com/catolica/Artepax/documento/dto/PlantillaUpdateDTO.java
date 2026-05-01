package com.catolica.Artepax.documento.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class PlantillaUpdateDTO {

    // Solo se permite editar el contenido y los campos editables,
    // NO el tipo_documento (es la clave de la plantilla)
    @NotEmpty(message = "El contenido base no puede estar vacío")
    private String contenidoBase;

    private String camposEditables;

    private Boolean activo;
}
