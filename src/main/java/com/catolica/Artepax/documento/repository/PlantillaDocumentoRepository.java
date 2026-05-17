package com.catolica.Artepax.documento.repository;

import com.catolica.Artepax.documento.entity.DocumentoTipo;
import com.catolica.Artepax.documento.entity.PlantillaDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link PlantillaDocumento} entity operations.
 */
@Repository
public interface PlantillaDocumentoRepository extends JpaRepository<PlantillaDocumento, Long> {

    /**
     * Finds an active template by its document type.
     *
     * @param tipoDocumento the document type
     * @return an {@link Optional} containing the active template, or empty if none found
     */
    Optional<PlantillaDocumento> findByTipoDocumentoAndActivoTrue(DocumentoTipo tipoDocumento);
}
