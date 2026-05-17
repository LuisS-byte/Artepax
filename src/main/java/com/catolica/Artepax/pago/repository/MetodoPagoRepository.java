package com.catolica.Artepax.pago.repository;

import com.catolica.Artepax.pago.entity.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the {@link MetodoPago} catalog entity.
 */
@Repository
public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {

    /**
     * Finds a payment method by its exact name.
     *
     * @param nombre the method name
     * @return an {@link Optional} containing the method, or empty if not found
     */
    Optional<MetodoPago> findByMetodoNombre(String nombre);

    /**
     * Finds a payment method by its name, ignoring case.
     * Used for duplicate detection during creation.
     *
     * @param nombre the method name (case-insensitive)
     * @return an {@link Optional} containing the method, or empty if not found
     */
    Optional<MetodoPago> findByMetodoNombreIgnoreCase(String nombre);
}
