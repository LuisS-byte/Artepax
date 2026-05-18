package com.catolica.Artepax.inventario.decoracion.entity;

import com.catolica.Artepax.evento.model.TipoEvento;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Junction entity linking a decoration package to a compatible event type.
 * <p>
 * Uses a composite key ({@link DecoracionTipoEventoId}) and stores the
 * creation timestamp. Each row represents a valid compatibility pairing
 * between a decoration and an event type.
 * </p>
 */
@Entity
@Table(name = "decoracion_tipo_evento")
public class DecoracionTipoEvento {

    @EmbeddedId
    private DecoracionTipoEventoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("decoracionId")
    @JoinColumn(name = "id_decoracion")
    private Decoracion decoracion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tipoEventoId")
    @JoinColumn(name = "id_tipo_evento")
    private TipoEvento tipoEvento;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    protected DecoracionTipoEvento() {}

    /**
     * Creates a new compatibility relationship between a decoration and an event type.
     *
     * @param decoracion the decoration
     * @param tipoEvento the compatible event type
     */
    public DecoracionTipoEvento(Decoracion decoracion, TipoEvento tipoEvento) {
        this.decoracion = decoracion;
        this.tipoEvento = tipoEvento;
        this.id = new DecoracionTipoEventoId(decoracion.getId(), tipoEvento.getId());
    }

    public DecoracionTipoEventoId getId() { return id; }
    public Decoracion getDecoracion() { return decoracion; }
    public TipoEvento getTipoEvento() { return tipoEvento; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
