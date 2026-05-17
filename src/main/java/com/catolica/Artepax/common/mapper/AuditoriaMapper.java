package com.catolica.Artepax.common.mapper;


import com.catolica.Artepax.common.dto.AuditoriaResponseDTO;
import com.catolica.Artepax.common.entity.Auditoria;
import org.springframework.stereotype.Component;

/**
 * Mapper que convierte entidades {@link Auditoria} a {@link AuditoriaResponseDTO}.
 * Extrae el ID del usuario de la relación ManyToOne (puede ser null si es un
 * registro del sistema sin usuario autenticado).
 */
@Component
public class AuditoriaMapper {

    /**
     * Convierte una entidad {@link Auditoria} a su DTO de respuesta.
     *
     * @param a la entidad de auditoría a convertir
     * @return DTO con los datos del registro de auditoría
     */
    public AuditoriaResponseDTO toDTO(Auditoria a) {
        AuditoriaResponseDTO dto = new AuditoriaResponseDTO();
        dto.setId(a.getId());
        dto.setUsuarioId(a.getUsuario() != null ? a.getUsuario().getIdUsuario() : null);
        dto.setAccion(a.getAccion());
        dto.setTablaAfectada(a.getTablaAfectada());
        dto.setIdRegistro(a.getIdRegistro());
        dto.setValorAnterior(a.getValorAnterior());
        dto.setValorNuevo(a.getValorNuevo());
        dto.setDescripcion(a.getDescripcion());
        dto.setFecha(a.getFecha());
        return dto;
    }
}
