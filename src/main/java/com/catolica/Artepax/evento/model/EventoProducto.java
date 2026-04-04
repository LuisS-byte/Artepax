package com.catolica.Artepax.evento.model;

import com.catolica.Artepax.inventario.producto.entity.Producto;
import jakarta.persistence.*;

@Entity
@Table(name = "evento_producto")
public class EventoProducto {
    @EmbeddedId
    private EventoProductoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventoId")
    @JoinColumn(name = "id_evento")
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productoId")
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @Column(name = "cantidad_reservada", nullable = false)
    private Integer cantidadReservada;

    public EventoProducto() {}

    public EventoProductoId getId() { return id; }
    public void setId(EventoProductoId id) { this.id = id; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Integer getCantidadReservada() { return cantidadReservada; }
    public void setCantidadReservada(Integer cantidadReservada) { this.cantidadReservada = cantidadReservada; }

}
