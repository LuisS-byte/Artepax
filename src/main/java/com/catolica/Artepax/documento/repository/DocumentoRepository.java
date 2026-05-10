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

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
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

    List<Documento> findByDocuEstadoEnvioAndDocuIntentosEnvioLessThan(DocumentoEstadoEnvio estado, int maxIntentos);

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

    @Query(value = """
            SELECT d
            FROM Documento d
            JOIN FETCH d.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            """,
           countQuery = "SELECT COUNT(d) FROM Documento d")
    Page<Documento> findAllWithEvento(Pageable pageable);

    boolean existsByEventoIdAndTipoDocumento(Long eventoId, com.catolica.Artepax.documento.entity.DocumentoTipo tipoDocumento);
}
