package com.catolica.Artepax.notificacion.dto;

import java.time.LocalDateTime;

public class NotificacionResponseDTO {
    private Long id;
    private String mensaje;
    private String tipoNotificacion;
    private String estadoNotificacion;
    private Long eventoId;
    private LocalDateTime fechaEnvio;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getTipoNotificacion() { return tipoNotificacion; }
    public void setTipoNotificacion(String tipoNotificacion) { this.tipoNotificacion = tipoNotificacion; }

    public String getEstadoNotificacion() { return estadoNotificacion; }
    public void setEstadoNotificacion(String estadoNotificacion) { this.estadoNotificacion = estadoNotificacion; }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
}