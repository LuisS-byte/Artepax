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

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

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

    // Suma total de pagos PAGADO para validar sobrepago
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.evento.id = :eventoId " +
            "AND p.estadoPago.estadoNombre = 'PAGADO'")
    BigDecimal sumMontoConfirmadoByEventoId(Long eventoId);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.evento.id = :eventoId " +
            "AND p.id <> :pagoId " +
            "AND p.estadoPago.estadoNombre = 'PAGADO'")
    BigDecimal sumPagosConfirmadosPorEventoExcluyendoPago(@Param("eventoId") Long eventoId,
                                                          @Param("pagoId") Long pagoId);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.evento.id = :eventoId " +
            "AND p.estadoPago.estadoNombre = 'PAGADO' " +
            "AND p.conceptoPago.conceptoNombre = :conceptoNombre")
    BigDecimal sumMontoConfirmadoByEventoIdAndConceptoNombre(Long eventoId, String conceptoNombre);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.evento.id = :eventoId " +
            "AND p.estadoPago.estadoNombre IN ('PAGADO', 'PENDIENTE')")
    BigDecimal sumMontoActivoByEventoId(Long eventoId);

    List<Pago> findByEventoIdAndEstadoPago_EstadoNombreIn(Long eventoId, List<String> estados);

    // Listado global paginado con JOIN para evitar N+1
    @Query(value = """
            SELECT p
            FROM Pago p
            JOIN FETCH p.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            """,
           countQuery = "SELECT COUNT(p) FROM Pago p")
    Page<Pago> findAllWithEvento(Pageable pageable);

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
