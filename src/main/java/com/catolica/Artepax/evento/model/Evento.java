package com.catolica.Artepax.evento.model;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "evento")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evento { @Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id_evento")
private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_decoracion")
    private Decoracion decoracion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estadoevento")
    private EstadoEvento estadoEvento;

    @Column(name = "event_fechaevento", nullable = false)
    private LocalDate fecha;

    @Column(name = "event_horainicio")
    private LocalTime horaInicio;

    @Column(name = "event_horafin")
    private LocalTime horaFin;

    @Column(name = "event_ubicacion", length = 255)
    private String ubicacion;

    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    @Column(name = "event_nota", length = 255)
    private String nota;

    @Column(name = "event_creacion")
    private LocalDate fechaCreacion;

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDate.now();
        }
    }
}