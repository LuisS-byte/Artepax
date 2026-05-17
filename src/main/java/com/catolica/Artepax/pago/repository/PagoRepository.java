package com.catolica.Artepax.pago.repository;

import com.catolica.Artepax.pago.entity.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Pago} entity operations.
 * <p>
 * Provides custom JPQL queries with {@code JOIN FETCH} to avoid N+1 queries
 * on payment-event-client relationships, and aggregation queries for payment
 * totals used in overpayment validation and state transitions.
 */
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    /**
     * Finds all payments for a given event, eagerly loading all relationships.
     *
     * @param eventoId the event ID
     * @return list of payments with fully loaded relations
     */
    @Query("""
            SELECT p
            FROM Pago p
            JOIN FETCH p.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            JOIN FETCH p.estadoPago
            JOIN FETCH p.conceptoPago
            JOIN FETCH p.metodoPago
            WHERE e.id = :eventoId
            """)
    List<Pago> findByEventoIdWithRelations(@Param("eventoId") Long eventoId);

    /**
     * Finds a single payment by ID, eagerly loading all relationships.
     *
     * @param id the payment ID
     * @return an {@link Optional} containing the payment with relations, or empty if not found
     */
    @Query("""
            SELECT p
            FROM Pago p
            JOIN FETCH p.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            JOIN FETCH p.estadoPago
            JOIN FETCH p.conceptoPago
            JOIN FETCH p.metodoPago
            WHERE p.id = :id
            """)
    Optional<Pago> findByIdWithRelations(@Param("id") Long id);

    /**
     * Sums the total amount of all confirmed (PAGADO) payments for an event.
     * Used to validate that new payments do not exceed the event's total price.
     *
     * @param eventoId the event ID
     * @return the summed amount, or {@link BigDecimal#ZERO} if none
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.evento.id = :eventoId " +
            "AND p.estadoPago.estadoNombre = 'PAGADO'")
    BigDecimal sumMontoConfirmadoByEventoId(Long eventoId);

    /**
     * Sums the total amount of confirmed payments for an event, excluding a specific payment.
     * Used when confirming a payment to check the total against the event price without counting
     * the payment being confirmed.
     *
     * @param eventoId the event ID
     * @param pagoId   the payment ID to exclude from the sum
     * @return the summed amount excluding the specified payment, or {@link BigDecimal#ZERO}
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.evento.id = :eventoId " +
            "AND p.id <> :pagoId " +
            "AND p.estadoPago.estadoNombre = 'PAGADO'")
    BigDecimal sumPagosConfirmadosPorEventoExcluyendoPago(@Param("eventoId") Long eventoId,
                                                          @Param("pagoId") Long pagoId);

    /**
     * Sums the total amount of confirmed payments for an event filtered by payment concept name.
     * Used to compute how much has been paid under a specific concept (e.g. ANTICIPO).
     *
     * @param eventoId       the event ID
     * @param conceptoNombre the concept name to filter by (e.g. "ANTICIPO")
     * @return the summed amount for the given concept, or {@link BigDecimal#ZERO}
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.evento.id = :eventoId " +
            "AND p.estadoPago.estadoNombre = 'PAGADO' " +
            "AND p.conceptoPago.conceptoNombre = :conceptoNombre")
    BigDecimal sumMontoConfirmadoByEventoIdAndConceptoNombre(Long eventoId, String conceptoNombre);

    /**
     * Sums the total amount of all active payments (PAGADO or PENDIENTE) for an event.
     * Used to validate that a new payment registration does not exceed the event's total price.
     *
     * @param eventoId the event ID
     * @return the summed active amount, or {@link BigDecimal#ZERO}
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.evento.id = :eventoId " +
            "AND p.estadoPago.estadoNombre IN ('PAGADO', 'PENDIENTE')")
    BigDecimal sumMontoActivoByEventoId(Long eventoId);

    /**
     * Finds all payments for an event that match any of the given state names.
     * Used during cancellation to retrieve PENDIENTE and PAGADO payments.
     *
     * @param eventoId the event ID
     * @param estados  list of state names to match (e.g. ["PENDIENTE", "PAGADO"])
     * @return the matching payments
     */
    List<Pago> findByEventoIdAndEstadoPago_EstadoNombreIn(Long eventoId, List<String> estados);

    /**
     * Returns a paginated list of all payments with event and client data eagerly loaded,
     * avoiding N+1 queries on the listing page.
     *
     * @param pageable pagination parameters
     * @return a page of payments with event and client relations loaded
     */
    @Query(value = """
            SELECT p
            FROM Pago p
            JOIN FETCH p.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            """,
           countQuery = "SELECT COUNT(p) FROM Pago p")
    Page<Pago> findAllWithEvento(Pageable pageable);

    /**
     * Executes a native SQL query that combines event, client, and payment view data
     * to build the collection (cobranza) listing.
     * <p>
     * Returns raw {@code Object[]} rows, one per event in active states
     * (PROGRAMADO, CONFIRMADO, EN_MONTAJE, CANCELACION_PENDIENTE, PENDIENTE_COBRO),
     * with payment totals from {@code vista_pagos_evento} and a subquery counting
     * pending payments.
     *
     * @return list of object arrays with columns matching {@link com.catolica.Artepax.pago.dto.EventoCobranzaDTO}
     */
    @Query(value = """
            SELECT e.id_evento AS eventoId,
                   c.cli_nombre AS clienteNombre,
                   ee.estado_nombre AS estadoEventoNombre,
                   e.event_precio_total AS precioTotal,
                   COALESCE(vp.total_cobrado, 0) AS totalCobrado,
                   COALESCE(vp.saldo_pendiente, e.event_precio_total) AS saldoPendiente,
                   COALESCE(vp.cobro_completo, false) AS cobroCompleto,
                   COALESCE(vp.tiene_devolucion_pendiente, false) AS tieneDevolucionPendiente,
                   COALESCE(vp.total_devolucion_pendiente, 0) AS totalDevolucionPendiente,
                   COALESCE((SELECT COUNT(p2.id_pago) FROM public.pago p2
                             WHERE p2.id_evento = e.id_evento
                             AND p2.id_estado_pago = (SELECT id_estado_pago FROM public.estado_pago WHERE estado_nombre = 'PENDIENTE')), 0) AS cantidadPagosPendientes
            FROM public.evento e
            JOIN public.cliente c ON c.id_cliente = e.id_cliente
            JOIN public.estado_evento ee ON ee.id_estadoevento = e.id_estadoevento
            LEFT JOIN public.vista_pagos_evento vp ON vp.id_evento = e.id_evento
            WHERE ee.estado_nombre IN ('PROGRAMADO', 'CONFIRMADO', 'EN_MONTAJE', 'CANCELACION_PENDIENTE', 'PENDIENTE_COBRO')
            """,
            nativeQuery = true)
    List<Object[]> findEventosCobranzaRaw();
}
