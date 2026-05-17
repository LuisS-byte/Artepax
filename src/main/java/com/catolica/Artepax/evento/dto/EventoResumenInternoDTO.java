package com.catolica.Artepax.evento.dto;

import com.catolica.Artepax.pago.dto.ResumenPagoEventoDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO de resumen interno de un evento para uso administrativo.
 * <p>
 * Agrupa datos básicos del evento, el resumen de pagos, el resumen de gastos
 * y la rentabilidad calculada en una sola respuesta.
 */
@Data
public class EventoResumenInternoDTO {
    private Long idEvento;
    private String estadoEvento;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String ubicacion;
    private String clienteNombre;
    private String decoracionNombre;
    private String tipoEventoNombre;
    private String nota;
    private String notaPrecio;
    private BigDecimal precioBase;
    private BigDecimal precioAdicional;
    private BigDecimal precioTotal;
    private LocalDateTime fechaConfirmacion;
    private ResumenPagoEventoDTO resumenPagos;
    private ResumenGastoEventoDTO resumenGastos;
    private RentabilidadEventoDTO rentabilidad;
}
