package com.catolica.Artepax.inventario.movimiento.entity;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a complete stock movement history record.
 * <p>
 * Tracks every inventory change (ENTRADA or SALIDA) with the affected product,
 * optional associated event, movement origin type, quantity, and the user who
 * registered the movement. The {@code fecha} field is auto-set by
 * {@link org.hibernate.annotations.CreationTimestamp @CreationTimestamp} at
 * persist time via JPA's {@link jakarta.persistence.PrePersist @PrePersist}
 * lifecycle callback.
 * </p>
 */
@Entity
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    @Column(name = "tipo_movimiento", nullable = false, length = 10)
    private String tipoMovimiento;

    @Column(name = "tipo_origen", nullable = false, length = 20)
    private String tipoOrigen = "REPOSICION";

    @Column(nullable = false)
    private Integer cantidad;

    @CreationTimestamp
    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(length = 255)
    private String motivo;

    public MovimientoInventario() {}

    public Long getId() { return id; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public Usuario getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(Usuario registradoPor) { this.registradoPor = registradoPor; }

    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }

    public String getTipoOrigen() { return tipoOrigen; }
    public void setTipoOrigen(String tipoOrigen) { this.tipoOrigen = tipoOrigen; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public LocalDateTime getFecha() { return fecha; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
