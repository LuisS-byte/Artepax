package com.catolica.Artepax.documento.repository;

import com.catolica.Artepax.documento.entity.Documento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findByEvento_IdOrderByDocuFechaGeneracionDesc(Long idEvento);

    List<Documento> findByDocuEstadoEnvioAndDocuIntentosEnvioLessThan(String estado, int maxIntentos);

    @Query(value = "SELECT d FROM Documento d JOIN FETCH d.evento e JOIN FETCH e.cliente JOIN FETCH e.estadoEvento",
           countQuery = "SELECT COUNT(d) FROM Documento d")
    Page<Documento> findAllWithEvento(Pageable pageable);
}
