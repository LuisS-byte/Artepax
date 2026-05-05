package com.catolica.Artepax.evento.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo de estados del ciclo de vida del evento.
 * Las transiciones válidas son responsabilidad del backend.
 */
@Entity
@Table(name = "estado_evento")
@Getter
@Setter
@NoArgsConstructor
public class EstadoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estadoevento")
    private Long id;

    @Column(name = "estado_nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "estado_descripcion", length = 255)
    private String descripcion;
}
