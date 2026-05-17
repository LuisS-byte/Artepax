package com.catolica.Artepax.cronograma.entity;

import com.catolica.Artepax.evento.model.Evento;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Actividad operativa asociada a un evento dentro del cronograma.
 * <p>
 * Flujo de estados: PENDIENTE → EN_PROGRESO → COMPLETADO (o CANCELADO).
 * Las actividades están disponibles desde que el evento alcanza el estado CONFIRMADO.
 * La cancelación en cascada se dispara desde el evento.
 * </p>
 */
@Entity
@Table(name = "cronograma")
public class Cronograma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cronograma")
    private Long id;

    /** Evento al que pertenece esta actividad. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    /** Nombre o descripción de la actividad. */
    @Column(name = "actividad", nullable = false, length = 255)
    private String actividad;

    /** Fecha y hora de inicio planificada. */
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    /** Fecha y hora de fin planificada. */
    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    /** Estado actual de la actividad: PENDIENTE, EN_PROGRESO, COMPLETADO o CANCELADO. */
    @Column(name = "crono_estado", nullable = false, length = 15)
    private String cronoEstado = CronogramaEstados.PENDIENTE;

    /** Observación opcional asociada al estado (ej. motivo de cancelación). */
    @Column(name = "crono_observacion", length = 255)
    private String cronoObservacion;

    public Cronograma() {}

    /**
     * Retorna el identificador único de la actividad.
     *
     * @return id de la actividad
     */
    public Long getId() { return id; }

    /**
     * Retorna el evento al que pertenece esta actividad.
     *
     * @return evento asociado
     */
    public Evento getEvento() { return evento; }

    /**
     * Asigna el evento al que pertenece esta actividad.
     *
     * @param evento evento asociado
     */
    public void setEvento(Evento evento) { this.evento = evento; }

    /**
     * Retorna el nombre o descripción de la actividad.
     *
     * @return actividad
     */
    public String getActividad() { return actividad; }

    /**
     * Asigna el nombre o descripción de la actividad.
     *
     * @param actividad nombre de la actividad
     */
    public void setActividad(String actividad) { this.actividad = actividad; }

    /**
     * Retorna la fecha y hora de inicio planificada.
     *
     * @return fecha de inicio
     */
    public LocalDateTime getFechaInicio() { return fechaInicio; }

    /**
     * Asigna la fecha y hora de inicio planificada.
     *
     * @param fechaInicio fecha de inicio
     */
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    /**
     * Retorna la fecha y hora de fin planificada.
     *
     * @return fecha de fin
     */
    public LocalDateTime getFechaFin() { return fechaFin; }

    /**
     * Asigna la fecha y hora de fin planificada.
     *
     * @param fechaFin fecha de fin
     */
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    /**
     * Retorna el estado actual de la actividad.
     *
     * @return PENDIENTE, EN_PROGRESO, COMPLETADO o CANCELADO
     */
    public String getCronoEstado() { return cronoEstado; }

    /**
     * Asigna el estado de la actividad.
     *
     * @param cronoEstado nuevo estado
     */
    public void setCronoEstado(String cronoEstado) { this.cronoEstado = cronoEstado; }

    /**
     * Retorna la observación asociada al estado.
     *
     * @return observación o {@code null} si no hay
     */
    public String getCronoObservacion() { return cronoObservacion; }

    /**
     * Asigna una observación asociada al estado.
     *
     * @param cronoObservacion observación (ej. motivo de cancelación)
     */
    public void setCronoObservacion(String cronoObservacion) { this.cronoObservacion = cronoObservacion; }
}
