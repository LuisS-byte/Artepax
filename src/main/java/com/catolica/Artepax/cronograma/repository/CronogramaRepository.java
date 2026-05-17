package com.catolica.Artepax.cronograma.repository;

import com.catolica.Artepax.cronograma.entity.Cronograma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para entidades {@link Cronograma}.
 */
public interface CronogramaRepository extends JpaRepository<Cronograma, Long> {

    /**
     * Busca actividades por id del evento, sin joins.
     *
     * @param eventoId id del evento
     * @return lista de actividades del evento
     */
    List<Cronograma> findByEventoId(Long eventoId);

    /**
     * Busca actividades por evento con joins a {@code evento}, {@code cliente} y {@code estadoEvento}.
     * Ordenado por fecha de inicio ascendente.
     *
     * @param eventoId id del evento
     * @return lista de actividades del evento con datos relacionados
     */
    @Query("""
            SELECT c
            FROM Cronograma c
            JOIN FETCH c.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            WHERE c.evento.id = :eventoId
            ORDER BY c.fechaInicio ASC
            """)
    List<Cronograma> findByEventoIdWithEvento(@Param("eventoId") Long eventoId);

    /**
     * Busca una actividad por su id con joins a {@code evento}, {@code cliente} y {@code estadoEvento}.
     *
     * @param id id de la actividad
     * @return Optional con la actividad encontrada
     */
    @Query("""
            SELECT c
            FROM Cronograma c
            JOIN FETCH c.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            WHERE c.id = :id
            """)
    Optional<Cronograma> findByIdWithEvento(@Param("id") Long id);

    /**
     * Cancela todas las actividades activas de un evento (cascade cancellation).
     * No afecta actividades ya COMPLETADO o CANCELADO.
     *
     * @param eventoId id del evento
     * @return cantidad de actividades afectadas
     */
    @Modifying
    @Query("UPDATE Cronograma c SET c.cronoEstado = 'CANCELADO' " +
            "WHERE c.evento.id = :eventoId " +
            "AND c.cronoEstado NOT IN ('COMPLETADO', 'CANCELADO')")
    int cancelarPorEvento(@Param("eventoId") Long eventoId);

    /**
     * Busca todas las actividades paginadas con joins a {@code evento}, {@code cliente} y {@code estadoEvento}.
     *
     * @param pageable parámetros de paginación
     * @return página de actividades
     */
    @Query(value = "SELECT c FROM Cronograma c JOIN FETCH c.evento e JOIN FETCH e.cliente JOIN FETCH e.estadoEvento",
           countQuery = "SELECT COUNT(c) FROM Cronograma c")
    Page<Cronograma> findAllWithEvento(Pageable pageable);
}
