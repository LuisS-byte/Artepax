package com.catolica.Artepax.documento.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlantillaResponseDTO {
    private Long idPlantilla;
    private String tipoDocumento;
    private String contenidoBase;
    private String camposEditables;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
