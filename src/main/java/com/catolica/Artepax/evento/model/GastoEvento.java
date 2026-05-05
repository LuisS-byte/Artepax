package com.catolica.Artepax.evento.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.catolica.Artepax.evento.model.GastoEstadoNames.PROYECTADO;

/**
 * Gastos variables atribuibles a un evento.
 * Estados: PROYECTADO → DESEMBOLSADO.
 */
@Entity
@Table(name = "gasto_evento")
@Getter
@Setter
@NoArgsConstructor
public class GastoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gasto")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @Column(name = "gasto_concepto", nullable = false, length = 100)
    private String gastoConcepto;

    @Column(name = "gasto_monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal gastoMonto;

    @Column(name = "gasto_estado", nullable = false, length = 15)
    private String gastoEstado = PROYECTADO;

    @Column(name = "gasto_fecha", nullable = false)
    @CreationTimestamp
    private LocalDateTime gastoFecha;

    @Column(name = "gasto_nota", length = 255)
    private String gastoNota;
}
