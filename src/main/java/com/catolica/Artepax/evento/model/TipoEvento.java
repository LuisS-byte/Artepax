package com.catolica.Artepax.evento.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo editable de tipos de evento.
 * Permite desactivar tipos sin borrar historial.
 */
@Entity
@Table(name = "tipo_evento")
@Getter
@Setter
@NoArgsConstructor
public class TipoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_evento")
    private Long id;

    @Column(name = "tipo_nombre", nullable = false, length = 50)
    private String tipoNombre;

    @Column(name = "tipo_descripcion", length = 255)
    private String tipoDescripcion;

    @Column(name = "tipo_activo", nullable = false)
    private Boolean tipoActivo = true;
}
