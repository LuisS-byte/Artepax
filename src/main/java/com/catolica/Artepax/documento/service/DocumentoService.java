package com.catolica.Artepax.documento.service;

import com.catolica.Artepax.documento.dto.DocumentoResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaUpdateDTO;

import java.util.List;
public interface DocumentoService {

    // --- Documentos ---

    /** Genera un documento para el evento usando la plantilla del tipo indicado. */
    DocumentoResponseDTO generarDocumento(Long idEvento, String tipoDocumento);

    /** Reenvía el documento al correo del cliente. */
    DocumentoResponseDTO reenviarDocumento(Long idDocumento);

    /** Lista todos los documentos de un evento. */
    List<DocumentoResponseDTO> listarPorEvento(Long idEvento);

    /** Obtiene un documento por su ID. */
    DocumentoResponseDTO obtenerDocumento(Long idDocumento);

    // --- Plantillas ---

    /** Lista todas las plantillas. */
    List<PlantillaResponseDTO> listarPlantillas();

    /** Obtiene una plantilla por ID. */
    PlantillaResponseDTO obtenerPlantilla(Long idPlantilla);

    /** Actualiza el contenido de una plantilla. */
    PlantillaResponseDTO actualizarPlantilla(Long idPlantilla, PlantillaUpdateDTO dto);
}
