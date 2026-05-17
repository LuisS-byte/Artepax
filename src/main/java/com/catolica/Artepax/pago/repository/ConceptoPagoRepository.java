package com.catolica.Artepax.pago.repository;

import com.catolica.Artepax.pago.entity.ConceptoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the {@link ConceptoPago} catalog entity.
 */
@Repository
public interface ConceptoPagoRepository extends JpaRepository<ConceptoPago, Long> {

    /**
     * Finds a payment concept by its exact name (e.g. "ANTICIPO").
     *
     * @param conceptoNombre the concept name
     * @return an {@link Optional} containing the concept, or empty if not found
     */
    Optional<ConceptoPago> findByConceptoNombre(String conceptoNombre);
}
