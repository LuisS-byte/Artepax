package com.catolica.Artepax.cronograma.entity;

import com.catolica.Artepax.evento.model.Evento;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cronograma")
public class Cronograma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cronograma")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @Column(name = "actividad", nullable = false, length = 255)
    private String actividad;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Column(name = "crono_estado", nullable = false, length = 15)
    private String cronoEstado = "PENDIENTE";

    @Column(name = "crono_observacion", length = 255)
    private String cronoObservacion;

    public Cronograma() {}

    public Long getId() { return id; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

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
}
