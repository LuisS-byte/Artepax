package com.catolica.Artepax.documento.mapper;

import com.catolica.Artepax.documento.dto.DocumentoResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaResponseDTO;
import com.catolica.Artepax.documento.entity.Documento;
import com.catolica.Artepax.documento.entity.PlantillaDocumento;
import org.springframework.stereotype.Component;

@Component
public class DocumentoMapper {

    public DocumentoResponseDTO toDTO(Documento doc) {
        DocumentoResponseDTO dto = new DocumentoResponseDTO();
        dto.setIdDocumento(doc.getIdDocumento());
        dto.setIdEvento(doc.getEvento().getId());
        dto.setDocuTipo(doc.getDocuTipo());
        dto.setDocuFechaGeneracion(doc.getDocuFechaGeneracion());
        dto.setArchivoUrl(doc.getArchivoUrl());
        dto.setDocuEstadoEnvio(doc.getDocuEstadoEnvio());
        dto.setDocuFechaEnvio(doc.getDocuFechaEnvio());
        dto.setDocuIntentosEnvio(doc.getDocuIntentosEnvio());
        return dto;
    }

    public PlantillaResponseDTO toDTO(PlantillaDocumento plantilla) {
        PlantillaResponseDTO dto = new PlantillaResponseDTO();
        dto.setIdPlantilla(plantilla.getIdPlantilla());
        dto.setTipoDocumento(plantilla.getTipoDocumento());
        dto.setContenidoBase(plantilla.getContenidoBase());
        dto.setCamposEditables(plantilla.getCamposEditables());
        dto.setActivo(plantilla.getActivo());
        dto.setFechaCreacion(plantilla.getFechaCreacion());
        return dto;
    }
}
