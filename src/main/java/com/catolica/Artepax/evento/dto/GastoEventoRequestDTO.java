package com.catolica.Artepax.evento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = "El concepto no puede superar 100 caracteres")
    private String gastoConcepto;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal gastoMonto;

    private String gastoEstado = PROYECTADO;
    @Size(max = 255, message = "La nota no puede superar 255 caracteres")
    private String gastoNota;
}
