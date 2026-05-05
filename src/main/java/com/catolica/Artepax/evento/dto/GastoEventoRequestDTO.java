package com.catolica.Artepax.evento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

import static com.catolica.Artepax.evento.model.GastoEstadoNames.PROYECTADO;

/**
 * DTO de entrada para crear un gasto asociado a un evento.
 */
@Data
public class GastoEventoRequestDTO {

    @NotNull(message = "El evento es obligatorio")
    private Long eventoId;

    @NotBlank(message = "El concepto es obligatorio")
    private String gastoConcepto;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal gastoMonto;

    private String gastoEstado = PROYECTADO;
    private String gastoNota;
}
