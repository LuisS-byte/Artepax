package com.catolica.Artepax.inventario.decoracion.repository;

import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Decoracion} entities.
 * Provides standard CRUD plus queries filtered by state and case-insensitive name lookup.
 */
public interface DecoracionRepository extends JpaRepository<Decoracion, Long> {

    /**
     * Finds all decorations with the given state.
     *
     * @param estado the state to filter by (e.g. ACTIVO, INACTIVO)
     * @return the list of matching decorations
     */
    List<Decoracion> findByEstado(String estado);

    /**
     * Finds a decoration by its name, ignoring case.
     *
     * @param nombre the decoration name to search for
     * @return an {@link Optional} containing the decoration if found
     */
    Optional<Decoracion> findByNombreIgnoreCase(String nombre);
}
