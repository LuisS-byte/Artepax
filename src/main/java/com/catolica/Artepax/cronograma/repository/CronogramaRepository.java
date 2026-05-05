package com.catolica.Artepax.cronograma.repository;

import com.catolica.Artepax.cronograma.entity.Cronograma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio para entidades Cronograma.
 */
public interface CronogramaRepository extends JpaRepository<Cronograma, Long> {

    List<Cronograma> findByEventoId(Long eventoId);

    /**
     * Cancela todas las actividades activas de un evento (cascade cancellation).
     * No afecta actividades ya COMPLETADO o CANCELADO.
     */
    @Modifying
    @Query("UPDATE Cronograma c SET c.cronoEstado = 'CANCELADO' " +
            "WHERE c.evento.id = :eventoId " +
            "AND c.cronoEstado NOT IN ('COMPLETADO', 'CANCELADO')")
    void cancelarPorEvento(@Param("eventoId") Long eventoId);

    @Query(value = "SELECT c FROM Cronograma c JOIN FETCH c.evento e JOIN FETCH e.cliente JOIN FETCH e.estadoEvento",
           countQuery = "SELECT COUNT(c) FROM Cronograma c")
    Page<Cronograma> findAllWithEvento(Pageable pageable);
}
