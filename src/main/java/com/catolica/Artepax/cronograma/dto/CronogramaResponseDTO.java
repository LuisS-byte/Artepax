package com.catolica.Artepax.cronograma.dto;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para una actividad del cronograma.
 * Incluye datos denormalizados del evento (cliente, estado) para evitar joins adicionales.
 */
public class CronogramaResponseDTO {

    private Long id;
    private Long idEvento;
    private String actividad;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String cronoEstado;
    private String cronoObservacion;
    private String clienteNombre;
    private String estadoEventoNombre;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdEvento() { return idEvento; }
    public void setIdEvento(Long idEvento) { this.idEvento = idEvento; }

    public String getActividad() { return actividad; }
    public void setActividad(String actividad) { this.actividad = actividad; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public String getCronoEstado() { return cronoEstado; }
    public void setCronoEstado(String cronoEstado) { this.cronoEstado = cronoEstado; }

    public String getCronoObservacion() { return cronoObservacion; }
    public void setCronoObservacion(String cronoObservacion) { this.cronoObservacion = cronoObservacion; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getEstadoEventoNombre() { return estadoEventoNombre; }
    public void setEstadoEventoNombre(String estadoEventoNombre) { this.estadoEventoNombre = estadoEventoNombre; }
}
