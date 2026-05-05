package com.catolica.Artepax.evento.dto;

import lombok.Data;

/**
 * DTO de salida para la representación de un tipo de evento.
 */
@Data
public class TipoEventoResponseDTO {

    private Long id;
    private String tipoNombre;
    private String tipoDescripcion;
    private Boolean tipoActivo;
}
