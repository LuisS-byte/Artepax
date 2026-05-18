package com.catolica.Artepax.inventario.decoracion.dto;

/**
 * Simplified DTO for exposing event type data within decoration responses.
 * Carries only the essential fields: id, name, and active status.
 */
public class TipoEventoSimpleDTO {

    private Long id;
    private String nombre;
    private Boolean activo;

    public TipoEventoSimpleDTO() {}

    /**
     * Creates a fully populated simple DTO.
     *
     * @param id     the tipo-evento ID
     * @param nombre the display name
     * @param activo whether the event type is active
     */
    public TipoEventoSimpleDTO(Long id, String nombre, Boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.activo = activo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
