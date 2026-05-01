package com.catolica.Artepax.evento.dto;

public class TipoEventoResponseDTO {
    private Long id;
    private String tipoNombre;
    private String tipoDescripcion;
    private Boolean tipoActivo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipoNombre() { return tipoNombre; }
    public void setTipoNombre(String tipoNombre) { this.tipoNombre = tipoNombre; }

    public String getTipoDescripcion() { return tipoDescripcion; }
    public void setTipoDescripcion(String tipoDescripcion) { this.tipoDescripcion = tipoDescripcion; }

    public Boolean getTipoActivo() { return tipoActivo; }
    public void setTipoActivo(Boolean tipoActivo) { this.tipoActivo = tipoActivo; }
}
