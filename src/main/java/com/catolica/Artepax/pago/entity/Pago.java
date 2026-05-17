package com.catolica.Artepax.pago.entity;

import com.catolica.Artepax.evento.model.Evento;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a payment made for an event within the system.
 * <p>
 * A payment is linked to a specific {@link Evento} and goes through a state lifecycle:
 * <ul>
 *   <li>{@code PENDIENTE} — initial state when created, no confirmation date yet</li>
 *   <li>{@code PAGADO} — confirmed payment, {@link #fecha} is set at confirmation time</li>
 *   <li>{@code CANCELADO} — payment was cancelled before being confirmed</li>
 *   <li>{@code DEVOLUCION_PENDIENTE} — payment was marked for refund after event cancellation</li>
 * </ul>
 * The {@link #monto} field holds the payment amount and {@link #fecha} is {@code null} while the
 * payment is in {@code PENDIENTE} state.
 */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_pago", nullable = false)
    private EstadoPago estadoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_concepto_pago", nullable = false)
    private ConceptoPago conceptoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_metodo_pago", nullable = false)
    private MetodoPago metodoPago;

    /** Payment amount. */
    @Column(name = "pago_monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    /** Confirmation timestamp. {@code null} while the payment is in {@code PENDIENTE} state. */
    @Column(name = "pago_fecha")
    private LocalDateTime fecha;

    public Pago() {}

    public Long getId() { return id; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public EstadoPago getEstadoPago() { return estadoPago; }
    public void setEstadoPago(EstadoPago estadoPago) { this.estadoPago = estadoPago; }

    public ConceptoPago getConceptoPago() { return conceptoPago; }
    public void setConceptoPago(ConceptoPago conceptoPago) { this.conceptoPago = conceptoPago; }

    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
