package com.catolica.Artepax.evento.model;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Immutable
@Table(name = "vista_eventos_periodo")
public class VistaEventosPeriodo {

    @Id
    @Column(name = "id_evento")
    private Long idEvento;

    @Column(name = "cli_nombre")
    private String clienteNombre;

    @Column(name = "cli_telefono")
    private String clienteTelefono;

    @Column(name = "cli_correo")
    private String clienteCorreo;

    @Column(name = "tipo_evento")
    private String tipoEvento;

    @Column(name = "estado")
    private String estado;

    @Column(name = "decoracion")
    private String decoracion;

    @Column(name = "decoracion_imagen")
    private String decoracionImagen;

    @Column(name = "event_fechaevento")
    private LocalDate fechaEvento;

    @Column(name = "event_horainicio")
    private LocalTime horaInicio;

    @Column(name = "event_horafin")
    private LocalTime horaFin;

    @Column(name = "event_ubicacion")
    private String ubicacion;

    @Column(name = "event_precio_base")
    private BigDecimal precioBase;

    @Column(name = "event_precio_adicional")
    private BigDecimal precioAdicional;

    @Column(name = "event_precio_total")
    private BigDecimal precioTotal;

    @Column(name = "event_cliente_notificado")
    private Boolean clienteNotificado;

    @Column(name = "event_fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(name = "event_fecha_expiracion")
    private LocalDate fechaExpiracion;

    @Column(name = "event_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "total_cobrado")
    private BigDecimal totalCobrado;

    @Column(name = "saldo_pendiente")
    private BigDecimal saldoPendiente;

    @Column(name = "cobro_completo")
    private Boolean cobroCompleto;

    @Column(name = "tiene_cronograma")
    private Boolean tieneCronograma;

    // Getters
    public Long getIdEvento() { return idEvento; }
    public String getClienteNombre() { return clienteNombre; }
    public String getClienteTelefono() { return clienteTelefono; }
    public String getClienteCorreo() { return clienteCorreo; }
    public String getTipoEvento() { return tipoEvento; }
    public String getEstado() { return estado; }
    public String getDecoracion() { return decoracion; }
    public String getDecoracionImagen() { return decoracionImagen; }
    public LocalDate getFechaEvento() { return fechaEvento; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public String getUbicacion() { return ubicacion; }
    public BigDecimal getPrecioBase() { return precioBase; }
    public BigDecimal getPrecioAdicional() { return precioAdicional; }
    public BigDecimal getPrecioTotal() { return precioTotal; }
    public Boolean getClienteNotificado() { return clienteNotificado; }
    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }
    public LocalDate getFechaExpiracion() { return fechaExpiracion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public BigDecimal getTotalCobrado() { return totalCobrado; }
    public BigDecimal getSaldoPendiente() { return saldoPendiente; }
    public Boolean getCobroCompleto() { return cobroCompleto; }
    public Boolean getTieneCronograma() { return tieneCronograma; }
}
