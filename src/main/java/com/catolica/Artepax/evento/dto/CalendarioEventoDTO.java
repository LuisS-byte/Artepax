package com.catolica.Artepax.evento.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO que representa un evento en la vista de calendario del dashboard.
 * <p>
 * Proviene de la vista de base de datos {@code vista_eventos_periodo} e incluye
 * datos denormalizados del cliente, decoración, estado, pagos y cronograma.
 */
@Data
public class CalendarioEventoDTO {
    private Long idEvento;
    private LocalDate fechaEvento;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String ubicacion;
    private String estado;
    private String tipoEvento;
    private String clienteNombre;
    private String clienteTelefono;
    private String clienteCorreo;
    private String decoracion;
    private String decoracionImagen;
    private BigDecimal precioTotal;
    private Boolean clienteNotificado;
    private LocalDateTime fechaConfirmacion;
    private LocalDate fechaExpiracion;
    private LocalDateTime fechaCreacion;
    private BigDecimal totalCobrado;
    private BigDecimal saldoPendiente;
    private Boolean cobroCompleto;
    private Boolean tieneCronograma;
}
