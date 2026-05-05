package com.catolica.Artepax.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para registros de auditoría.
 */
@Data
public class AuditoriaResponseDTO {
    private Long id;
    private Long usuarioId;
    private String accion;
    private String tablaAfectada;
    private Long idRegistro;
    private String valorAnterior;
    private String valorNuevo;
    private String descripcion;
    private LocalDateTime fecha;
}
