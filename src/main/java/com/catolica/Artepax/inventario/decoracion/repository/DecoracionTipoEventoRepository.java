package com.catolica.Artepax.inventario.decoracion.repository;

import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionTipoEvento;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionTipoEventoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for the {@link DecoracionTipoEvento} junction entity.
 * Provides queries for managing compatibility relationships between
 * decorations and event types.
 */
@Repository
public interface DecoracionTipoEventoRepository
        extends JpaRepository<DecoracionTipoEvento, DecoracionTipoEventoId> {

    /**
     * Checks if a compatibility relationship exists between a decoration
     * and an event type.
     *
     * @param decoracionId the decoration ID
     * @param tipoEventoId the tipo-evento ID
     * @return true if the pair exists
     */
    boolean existsByDecoracion_IdAndTipoEvento_Id(Long decoracionId, Long tipoEventoId);

    /**
     * Deletes all compatibility relationships for a given decoration.
     *
     * @param decoracionId the decoration ID
     */
    void deleteByDecoracion_Id(Long decoracionId);

    /**
     * Finds all compatibility relationships for a given decoration,
     * eagerly loading the associated tipo-evento.
     *
     * @param decoracionId the decoration ID
     * @return the list of compatibility relationships
     */
    @Query("""
        SELECT dte
        FROM DecoracionTipoEvento dte
        JOIN FETCH dte.tipoEvento
        WHERE dte.decoracion.id = :decoracionId
    """)
    List<DecoracionTipoEvento> findByDecoracion_IdWithTipoEvento(@Param("decoracionId") Long decoracionId);

    /**
     * Finds all compatibility relationships for the given decoration IDs,
     * eagerly loading the associated tipo-evento.
     *
     * @param ids the list of decoration IDs
     * @return the list of compatibility relationships
     */
    @Query("""
        SELECT dte
        FROM DecoracionTipoEvento dte
        JOIN FETCH dte.tipoEvento
        WHERE dte.decoracion.id IN :ids
    """)
    List<DecoracionTipoEvento> findByDecoracionIds(@Param("ids") List<Long> ids);

    /**
     * Returns all active decorations that are compatible with the given
     * event type, ordered by decoration name ascending.
     *
     * @param tipoEventoId the tipo-evento ID
     * @return the list of active compatible decorations
     */
    @Query("""
        SELECT DISTINCT dte.decoracion
        FROM DecoracionTipoEvento dte
        JOIN dte.decoracion d
        WHERE dte.tipoEvento.id = :tipoEventoId
          AND d.estado = 'ACTIVO'
        ORDER BY d.nombre ASC
    """)
    List<Decoracion> findDecoracionesActivasCompatibles(@Param("tipoEventoId") Long tipoEventoId);
}
