package com.catolica.Artepax.common.mapper;


import com.catolica.Artepax.common.dto.AuditoriaResponseDTO;
import com.catolica.Artepax.common.entity.Auditoria;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaMapper {

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
