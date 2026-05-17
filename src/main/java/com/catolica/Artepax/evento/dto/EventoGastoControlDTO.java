package com.catolica.Artepax.evento.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO que representa el control de gastos de un evento para el panel administrativo.
 * <p>
 * Incluye totales agregados (proyectado, desembolsado, pendiente), cantidades
 * de gastos por estado e indicador de si permite registrar nuevos gastos
 * según el estado del evento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoGastoControlDTO {

    private Long eventoId;
    private String clienteNombre;
    private String estadoEventoNombre;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String ubicacion;

    private BigDecimal totalProyectado;
    private BigDecimal totalDesembolsado;
    private BigDecimal totalPendiente;

    private Long cantidadGastos;
    private Long cantidadProyectados;
    private Long cantidadDesembolsados;

    private Boolean permiteRegistrarGasto;
}
