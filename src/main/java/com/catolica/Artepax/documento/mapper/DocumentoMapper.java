package com.catolica.Artepax.documento.mapper;

import com.catolica.Artepax.documento.dto.DocumentoResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaResponseDTO;
import com.catolica.Artepax.documento.entity.Documento;
import com.catolica.Artepax.documento.entity.PlantillaDocumento;
import org.springframework.stereotype.Component;

/**
 * Maps between document/template entities and their corresponding DTOs.
 */
@Component
public class DocumentoMapper {

    /**
     * Converts a {@link Documento} entity to a {@link DocumentoResponseDTO}.
     * <p>
     * Safely handles nullable relationships (event, client, estadoEvento) by
     * setting the corresponding DTO fields to {@code null} when absent.
     * </p>
     *
     * @param doc the document entity, must not be {@code null}
     * @return the populated response DTO
     */
    public DocumentoResponseDTO toDTO(Documento doc) {
        DocumentoResponseDTO dto = new DocumentoResponseDTO();
        dto.setIdDocumento(doc.getIdDocumento());
        dto.setIdEvento(doc.getEvento() != null ? doc.getEvento().getId() : null);
        dto.setDocuTipo(doc.getDocuTipo() != null ? doc.getDocuTipo().name() : null);
        dto.setDocuFechaGeneracion(doc.getDocuFechaGeneracion());
        dto.setArchivoUrl(doc.getArchivoUrl());
        dto.setDocuEstadoEnvio(doc.getDocuEstadoEnvio() != null ? doc.getDocuEstadoEnvio().name() : null);
        dto.setDocuFechaEnvio(doc.getDocuFechaEnvio());
        dto.setDocuIntentosEnvio(doc.getDocuIntentosEnvio());

        if (doc.getEvento() != null && doc.getEvento().getCliente() != null) {
            dto.setClienteNombre(doc.getEvento().getCliente().getNombre());
        }

        if (doc.getEvento() != null && doc.getEvento().getEstadoEvento() != null) {
            dto.setEstadoEventoNombre(doc.getEvento().getEstadoEvento().getNombre());
        }

        return dto;
    }

    /**
     * Converts a {@link PlantillaDocumento} entity to a {@link PlantillaResponseDTO}.
     *
     * @param plantilla the template entity, must not be {@code null}
     * @return the populated response DTO
     */
    public PlantillaResponseDTO toDTO(PlantillaDocumento plantilla) {
        PlantillaResponseDTO dto = new PlantillaResponseDTO();
        dto.setIdPlantilla(plantilla.getIdPlantilla());
        dto.setTipoDocumento(plantilla.getTipoDocumento() != null ? plantilla.getTipoDocumento().name() : null);
        dto.setContenidoBase(plantilla.getContenidoBase());
        dto.setCamposEditables(plantilla.getCamposEditables());
        dto.setActivo(plantilla.getActivo());
        dto.setFechaCreacion(plantilla.getFechaCreacion());
        return dto;
    }
}
