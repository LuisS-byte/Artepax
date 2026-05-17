package com.catolica.Artepax.inventario.producto.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

/**
 * Read-only entity mapped to the database view {@code vista_stock_producto}.
 * <p>
 * Computes available stock as {@code cantidad_total - cantidad_reservada} and
 * provides a low-stock alert flag. This entity is {@link jakarta.persistence.Immutable @Immutable}
 * and cannot be modified through JPA.
 * </p>
 */
@Entity
@Immutable
@Table(name = "vista_stock_producto")
public class StockProducto {

    @Id
    @Column(name = "id_producto")
    private Long idProducto;

    @Column(name = "produ_nombre")
    private String nombre;

    @Column(name = "cantidad_total")
    private Integer cantidadTotal;

    @Column(name = "cantidad_reservada")
    private Integer cantidadReservada;

    @Column(name = "cantidad_disponible")
    private Integer cantidadDisponible;

    @Column(name = "produ_stock_minimo")
    private Integer stockMinimo;

    @Column(name = "stock_bajo_alerta")
    private Boolean stockBajoAlerta;

    public Long getIdProducto() { return idProducto; }
    public String getNombre() { return nombre; }
    public Integer getCantidadTotal() { return cantidadTotal; }
    public Integer getCantidadReservada() { return cantidadReservada; }
    public Integer getCantidadDisponible() { return cantidadDisponible; }
    public Integer getStockMinimo() { return stockMinimo; }
    public Boolean getStockBajoAlerta() { return stockBajoAlerta; }
}