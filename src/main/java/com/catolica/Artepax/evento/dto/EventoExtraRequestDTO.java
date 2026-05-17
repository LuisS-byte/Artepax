package com.catolica.Artepax.evento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de entrada para agregar productos extra a un evento operativo.
 * <p>
 * Incluye la lista de ítems extra, la nota de precio y el monto adicional
 * que incrementa el precio total del evento.
 */
@Data
public class EventoExtraRequestDTO {

    @Valid
    @NotEmpty(message = "Debe indicar al menos un producto extra")
    private List<EventoExtraItemRequestDTO> items;

    @NotBlank(message = "La nota de precio es obligatoria")
    private String notaPrecio;

    @NotNull(message = "El monto adicional es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto adicional debe ser mayor a 0")
    private BigDecimal montoAdicional;
}
