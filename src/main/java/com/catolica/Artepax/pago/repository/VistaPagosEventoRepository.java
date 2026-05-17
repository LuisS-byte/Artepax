package com.catolica.Artepax.pago.repository;

import com.catolica.Artepax.pago.entity.VistaPagosEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for the read-only {@link VistaPagosEvento} database view entity.
 * <p>
 * This view aggregates payment data per event. Since the entity is {@link org.hibernate.annotations.Immutable},
 * write operations are not supported.
 */
public interface VistaPagosEventoRepository extends JpaRepository<VistaPagosEvento, Long> {

    /**
     * Finds the payment summary view row for a given event.
     *
     * @param idEvento the event ID
     * @return an {@link Optional} containing the view row, or empty if the event
     *         has no payment data yet
     */
    Optional<VistaPagosEvento> findByIdEvento(Long idEvento);
}
