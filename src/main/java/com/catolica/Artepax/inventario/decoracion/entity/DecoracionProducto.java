package com.catolica.Artepax.inventario.decoracion.entity;



import com.catolica.Artepax.inventario.producto.entity.Producto;
import jakarta.persistence.*;

/**
 * Junction entity linking a decoration package to a product.
 * <p>
 * Uses a composite key ({@link DecoracionProductoId}) and stores the
 * required quantity of the product for the decoration. Each row represents
 * a single product within a decoration bundle.
 * </p>
 */
@Entity
@Table(name = "decoracionxproducto")
public class DecoracionProducto {


    @EmbeddedId
    private DecoracionProductoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("decoracionId")
    @JoinColumn(name = "id_decoracion")
    private Decoracion decoracion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productoId")
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @Column(name = "cantidad_requerida", nullable = false)
    private Integer cantidadRequerida;

    public DecoracionProducto() {}

    public DecoracionProductoId getId() { return id; }
    public void setId(DecoracionProductoId id) { this.id = id; }

    public Decoracion getDecoracion() { return decoracion; }
    public void setDecoracion(Decoracion decoracion) { this.decoracion = decoracion; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Integer getCantidadRequerida() { return cantidadRequerida; }
    public void setCantidadRequerida(Integer cantidadRequerida) { this.cantidadRequerida = cantidadRequerida; }
}
