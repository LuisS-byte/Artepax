package com.catolica.Artepax.common.dto;

import lombok.Data;

/**
 * DTO de respuesta para parámetros de configuración global.
 */
@Data
public class ConfiguracionResponseDTO {
    private String configClave;
    private String configValor;
    private String configTipo;
    private String configDescripcion;
}
