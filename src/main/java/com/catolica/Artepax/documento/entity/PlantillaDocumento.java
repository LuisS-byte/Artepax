package com.catolica.Artepax.documento.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "plantilla_documento")
@Getter
@Setter
@NoArgsConstructor
public class PlantillaDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPlantilla;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, unique = true, length = 50)
    private DocumentoTipo tipoDocumento;

    @Column(name = "contenido_base", nullable = false, columnDefinition = "TEXT")
    private String contenidoBase;

    @Column(name = "campos_editables", columnDefinition = "TEXT")
    private String camposEditables;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;


}
