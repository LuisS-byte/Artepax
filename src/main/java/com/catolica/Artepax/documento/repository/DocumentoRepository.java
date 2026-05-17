package com.catolica.Artepax.documento.repository;

import com.catolica.Artepax.documento.entity.DocumentoEstadoEnvio;
import com.catolica.Artepax.documento.entity.Documento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Documento} entity operations.
 * <p>
 * Provides queries that eagerly fetch related entities (event, client, event status,
 * event type, decoration, template) to avoid N+1 issues when building document DTOs.
 * </p>
 */
@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    /**
     * Finds all documents for a given event with related entities eagerly fetched.
     *
     * @param idEvento the event ID
     * @return list of documents ordered by generation date descending
     */
    @Query("""
            SELECT d
            FROM Documento d
            JOIN FETCH d.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            WHERE e.id = :idEvento
            ORDER BY d.docuFechaGeneracion DESC
            """)
    List<Documento> findByEventoIdWithRelations(@Param("idEvento") Long idEvento);

    /**
     * Finds documents in a given state that have not exceeded the maximum retry attempts.
     *
     * @param estado      the delivery state to filter by
     * @param maxIntentos the maximum number of send attempts (exclusive)
     * @return list of documents matching the criteria
     */
    List<Documento> findByDocuEstadoEnvioAndDocuIntentosEnvioLessThan(DocumentoEstadoEnvio estado, int maxIntentos);

    /**
     * Finds a single document by ID with all related entities eagerly fetched.
     *
     * @param idDocumento the document ID
     * @return an {@link Optional} containing the document if found, or empty otherwise
     */
    @Query("""
            SELECT d
            FROM Documento d
            JOIN FETCH d.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            JOIN FETCH e.tipoEvento
            JOIN FETCH e.decoracion
            LEFT JOIN FETCH d.plantilla
            WHERE d.idDocumento = :idDocumento
            """)
    Optional<Documento> findByIdWithRelations(@Param("idDocumento") Long idDocumento);

    /**
     * Returns a paginated list of all documents with event and client data eagerly fetched.
     *
     * @param pageable pagination parameters
     * @return a page of documents
     */
    @Query(value = """
            SELECT d
            FROM Documento d
            JOIN FETCH d.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            """,
           countQuery = "SELECT COUNT(d) FROM Documento d")
    Page<Documento> findAllWithEvento(Pageable pageable);

    /**
     * Checks whether a document of the given type already exists for the specified event.
     *
     * @param eventoId the event ID
     * @param docuTipo the document type
     * @return {@code true} if a document of that type exists for the event
     */
    boolean existsByEventoIdAndDocuTipo(Long eventoId, com.catolica.Artepax.documento.entity.DocumentoTipo docuTipo);
}
