package com.catolica.Artepax.notificacion.entity;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.evento.model.Evento;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_notificacion", nullable = false)
    private TipoNotificacion tipoNotificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_notificacion", nullable = false)
    private EstadoNotificacion estadoNotificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private Evento evento;

    @Column(name = "mensaje", nullable = false, length = 500)
    private String mensaje;

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
