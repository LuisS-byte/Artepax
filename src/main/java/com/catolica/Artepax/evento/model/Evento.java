package com.catolica.Artepax.evento.model;

import com.catolica.Artepax.clientes.entity.Cliente;
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
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @Column(name = "id_decoracion")
    private Long decoracionId;

    @Column(name = "event_fechaevento", nullable = false)
    private LocalDate fecha;

    @Column(name = "event_horainicio")
    private LocalTime horaInicio;

    @Column(name = "event_horafin")
    private LocalTime horaFin;

    @Column(name = "event_ubicacion")
    private String ubicacion;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    @Column(name = "id_estadoevento", nullable = false)
    private Long estadoEventoId;

    @Column(name = "event_nota")
    private String nota;

    @Column(name = "event_creacion")
    private LocalDate fechaCreacion;
}