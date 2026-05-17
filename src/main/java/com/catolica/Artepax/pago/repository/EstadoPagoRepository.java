package com.catolica.Artepax.pago.repository;

import com.catolica.Artepax.pago.entity.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the {@link EstadoPago} catalog entity.
 */
@Repository
public interface EstadoPagoRepository extends JpaRepository<EstadoPago, Long> {

    /**
     * Finds a payment state by its name (e.g. "PENDIENTE", "PAGADO", "CANCELADO").
     *
     * @param nombre the state name
     * @return an {@link Optional} containing the state, or empty if not found
     */
    Optional<EstadoPago> findByEstadoNombre(String nombre);
}
