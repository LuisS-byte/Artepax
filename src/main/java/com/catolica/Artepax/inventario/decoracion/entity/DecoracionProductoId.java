package com.catolica.Artepax.inventario.decoracion.entity;


import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Embeddable;

@Embeddable
public class DecoracionProductoId implements Serializable {
    private Long decoracionId;
    private Long productoId;

    public DecoracionProductoId() {}

    public DecoracionProductoId(Long decoracionId, Long productoId) {
        this.decoracionId = decoracionId;
        this.productoId = productoId;
    }

    public Long getDecoracionId() { return decoracionId; }
    public void setDecoracionId(Long decoracionId) { this.decoracionId = decoracionId; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DecoracionProductoId)) return false;
        DecoracionProductoId that = (DecoracionProductoId) o;
        return Objects.equals(decoracionId, that.decoracionId) &&
                Objects.equals(productoId, that.productoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(decoracionId, productoId);
    }
}