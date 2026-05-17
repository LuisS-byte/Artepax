package com.catolica.Artepax.documento.service;

import com.catolica.Artepax.documento.dto.DocumentoResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaUpdateDTO;
import com.catolica.Artepax.documento.entity.DocumentoTipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
/**
 * Service interface for document and template management.
 * <p>
 * Defines operations for generating, retrieving, resending documents, and
 * managing HTML-based document templates.
 * </p>
 */
public interface DocumentoService {

    /**
     * Generates a document of the specified type for an event.
     * <p>
     * The document is persisted immediately, then an attempt is made to
     * render the associated PDF and send it via email. If email delivery
     * fails, the document is saved with {@link DocumentoEstadoEnvio#FALLIDO}
     * so it can be retried later via {@link #reenviarDocumento}.
     * </p>
     *
     * @param idEvento      the event ID
     * @param tipoDocumento the document type
     * @return the generated document DTO
     */
    DocumentoResponseDTO generarDocumento(Long idEvento, DocumentoTipo tipoDocumento);

    /**
     * Re-sends an existing document to the client's email.
     * <p>
     * Validates that the resend is permitted (maximum attempts not exceeded,
     * client email present, template available) before generating the PDF and
     * sending it again.
     * </p>
     *
     * @param idDocumento the document ID to resend
     * @return the updated document DTO
     */
    DocumentoResponseDTO reenviarDocumento(Long idDocumento);

    /**
     * Lists all documents generated for a specific event.
     *
     * @param idEvento the event ID
     * @return list of document DTOs ordered by generation date descending
     */
    List<DocumentoResponseDTO> listarPorEvento(Long idEvento);

    /**
     * Retrieves a single document by its ID.
     *
     * @param idDocumento the document ID
     * @return the document DTO
     * @throws DocumentoNotFoundException if no document exists with the given ID
     */
    DocumentoResponseDTO obtenerDocumento(Long idDocumento);

    /**
     * Returns a paginated list of all documents in the system.
     *
     * @param pageable pagination parameters
     * @return a page of document DTOs
     */
    Page<DocumentoResponseDTO> obtenerTodos(Pageable pageable);

    /**
     * Lists all available document templates.
     *
     * @return list of template DTOs
     */
    List<PlantillaResponseDTO> listarPlantillas();

    /**
     * Retrieves a single template by its ID.
     *
     * @param idPlantilla the template ID
     * @return the template DTO
     * @throws PlantillaDocumentoNotFoundException if no template exists with the given ID
     */
    PlantillaResponseDTO obtenerPlantilla(Long idPlantilla);

    /**
     * Updates an existing template's content, editable fields, and active flag.
     *
     * @param idPlantilla the template ID
     * @param dto         the update data
     * @return the updated template DTO
     */
    PlantillaResponseDTO actualizarPlantilla(Long idPlantilla, PlantillaUpdateDTO dto);

    /**
     * Generates an outstanding balance notice document for an event.
     * <p>
     * Validates that the event has a positive pending balance before generating
     * the document and sending the notice to the client.
     * </p>
     *
     * @param eventoId the event ID
     * @return the generated document DTO
     */
    DocumentoResponseDTO generarAvisoSaldoPendiente(Long eventoId);
}
