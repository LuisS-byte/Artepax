package com.catolica.Artepax.evento.model;


import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Embeddable;

@Embeddable
public class EventoProductoId implements Serializable {private Long eventoId;
    private Long productoId;

    public EventoProductoId() {}

    public EventoProductoId(Long eventoId, Long productoId) {
        this.eventoId = eventoId;
        this.productoId = productoId;
    }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventoProductoId)) return false;
        EventoProductoId that = (EventoProductoId) o;
        return Objects.equals(eventoId, that.eventoId) &&
                Objects.equals(productoId, that.productoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventoId, productoId);
    }
}
