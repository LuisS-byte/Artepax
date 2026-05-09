package com.catolica.Artepax.documento.repository;

import com.catolica.Artepax.documento.entity.DocumentoTipo;
import com.catolica.Artepax.documento.entity.PlantillaDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlantillaDocumentoRepository extends JpaRepository<PlantillaDocumento, Long> {
    Optional<PlantillaDocumento> findByTipoDocumentoAndActivoTrue(DocumentoTipo tipoDocumento);
}
