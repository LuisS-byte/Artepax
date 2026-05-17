package com.catolica.Artepax.notificacion.entity;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.evento.model.Evento;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa una notificación dirigida a un usuario del sistema.
 * <p>
 * Cada notificación pertenece a un usuario destino, tiene un tipo y un estado,
 * y opcionalmente puede estar asociada a un {@link Evento} para permitir
 * navegación directa desde el frontend. La fecha de envío se asigna
 * automáticamente al persistir mediante {@code @CreationTimestamp}.
 */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long id;

    /** Usuario destino de la notificación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    /** Tipo de notificación (ej. RECORDATORIO_EVENTO, CAMBIO_ESTADO). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_notificacion", nullable = false)
    private TipoNotificacion tipoNotificacion;

    /** Estado actual de la notificación (PENDIENTE, LEIDA, etc.). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_notificacion", nullable = false)
    private EstadoNotificacion estadoNotificacion;

    /** Evento opcional asociado, usado para navegación contextual desde el frontend. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private Evento evento;

    /** Contenido textual de la notificación (máximo 500 caracteres). */
    @Column(name = "mensaje", nullable = false, length = 500)
    private String mensaje;

    /** Fecha y hora en que se creó la notificación (se asigna automáticamente). */
    @Column(name = "fecha_envio", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime fechaEnvio;

    public Notificacion() {}

    public Long getId() { return id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public TipoNotificacion getTipoNotificacion() { return tipoNotificacion; }
    public void setTipoNotificacion(TipoNotificacion tipoNotificacion) { this.tipoNotificacion = tipoNotificacion; }

    public EstadoNotificacion getEstadoNotificacion() { return estadoNotificacion; }
    public void setEstadoNotificacion(EstadoNotificacion estadoNotificacion) { this.estadoNotificacion = estadoNotificacion; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
}
