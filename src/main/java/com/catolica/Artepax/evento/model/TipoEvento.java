package com.catolica.Artepax.evento.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tipo_evento")
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

    public TipoEvento() {}

    public Long getId() { return id; }

    public String getTipoNombre() { return tipoNombre; }
    public void setTipoNombre(String tipoNombre) { this.tipoNombre = tipoNombre; }

    public String getTipoDescripcion() { return tipoDescripcion; }
    public void setTipoDescripcion(String tipoDescripcion) { this.tipoDescripcion = tipoDescripcion; }

    public Boolean getTipoActivo() { return tipoActivo; }
    public void setTipoActivo(Boolean tipoActivo) { this.tipoActivo = tipoActivo; }
}
