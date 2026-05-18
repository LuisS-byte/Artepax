package com.catolica.Artepax.inventario.decoracion.entity;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Composite primary key for the {@link DecoracionTipoEvento} junction entity.
 * Combines the decoration ID and the tipo-evento ID to uniquely identify
 * a compatibility relationship between a decoration and an event type.
 */
@Embeddable
public class DecoracionTipoEventoId implements Serializable {

    @Column(name = "id_decoracion")
    private Long decoracionId;

    @Column(name = "id_tipo_evento")
    private Long tipoEventoId;

    protected DecoracionTipoEventoId() {}

    /**
     * Creates a composite key with both IDs.
     *
     * @param decoracionId the decoration ID
     * @param tipoEventoId the tipo-evento ID
     */
    public DecoracionTipoEventoId(Long decoracionId, Long tipoEventoId) {
        this.decoracionId = decoracionId;
        this.tipoEventoId = tipoEventoId;
    }

    public Long getDecoracionId() { return decoracionId; }
    public Long getTipoEventoId() { return tipoEventoId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DecoracionTipoEventoId that)) return false;
        return Objects.equals(decoracionId, that.decoracionId)
                && Objects.equals(tipoEventoId, that.tipoEventoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(decoracionId, tipoEventoId);
    }
}
