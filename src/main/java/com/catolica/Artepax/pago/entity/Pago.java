package com.catolica.Artepax.pago.entity;


import com.catolica.Artepax.evento.model.Evento;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pago")
public class Pago { @Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id_pago")
private Long id;

    @Column(name = "pago_monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "pago_fecha")
    private LocalDate fecha;

    @Column(name = "pago_metodo", length = 50)
    private String metodo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private Evento evento;

    public Pago() {}

    public Long getId() { return id; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }
}
