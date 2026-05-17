package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.GastoEvento;
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
public interface GastoEventoRepository extends JpaRepository<GastoEvento, Long> {
    List<GastoEvento> findByEventoId(Long eventoId);

    @Query(value = "SELECT g FROM GastoEvento g JOIN FETCH g.evento e JOIN FETCH e.cliente JOIN FETCH e.estadoEvento",
           countQuery = "SELECT COUNT(g) FROM GastoEvento g")
    Page<GastoEvento> findAllWithEvento(Pageable pageable);

    @Query("""
            SELECT g
            FROM GastoEvento g
            JOIN FETCH g.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            WHERE e.id = :eventoId
            ORDER BY g.gastoFecha DESC
            """)
    List<GastoEvento> findByEventoIdWithEvento(@Param("eventoId") Long eventoId);

    @Query("""
            SELECT g
            FROM GastoEvento g
            JOIN FETCH g.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            WHERE g.id = :id
            """)
    Optional<GastoEvento> findByIdWithEvento(@Param("id") Long id);

    @Query("""
            SELECT COALESCE(SUM(g.gastoMonto), 0)
            FROM GastoEvento g
            WHERE g.evento.id = :eventoId
              AND g.gastoEstado = :estado
            """)
    BigDecimal sumMontoByEventoIdAndEstado(
            @Param("eventoId") Long eventoId,
            @Param("estado") String estado);

    @Query(value = """
        SELECT e.id_evento AS eventoId,
               c.cli_nombre AS clienteNombre,
               ee.estado_nombre AS estadoEventoNombre,
               e.event_fechaevento AS fecha,
               e.event_horainicio AS horaInicio,
               e.event_horafin AS horaFin,
               e.event_ubicacion AS ubicacion,
               COALESCE(SUM(CASE WHEN g.gasto_estado = 'PROYECTADO' THEN g.gasto_monto ELSE 0 END), 0) AS totalProyectado,
               COALESCE(SUM(CASE WHEN g.gasto_estado = 'DESEMBOLSADO' THEN g.gasto_monto ELSE 0 END), 0) AS totalDesembolsado,
               COUNT(g.id_gasto) AS cantidadGastos,
               COALESCE(SUM(CASE WHEN g.gasto_estado = 'PROYECTADO' THEN 1 ELSE 0 END), 0) AS cantidadProyectados,
               COALESCE(SUM(CASE WHEN g.gasto_estado = 'DESEMBOLSADO' THEN 1 ELSE 0 END), 0) AS cantidadDesembolsados
        FROM evento e
        JOIN cliente c ON c.id_cliente = e.id_cliente
        JOIN estado_evento ee ON ee.id_estadoevento = e.id_estadoevento
        LEFT JOIN gasto_evento g ON g.id_evento = e.id_evento
        GROUP BY e.id_evento, c.cli_nombre, ee.estado_nombre, e.event_fechaevento,
                 e.event_horainicio, e.event_horafin, e.event_ubicacion
        ORDER BY
            CASE WHEN COALESCE(SUM(CASE WHEN g.gasto_estado = 'PROYECTADO' THEN g.gasto_monto ELSE 0 END), 0) > 0
                 THEN 0 ELSE 1 END,
            CASE ee.estado_nombre
                WHEN 'EN_MONTAJE' THEN 0
                WHEN 'CONFIRMADO' THEN 1
                WHEN 'PROGRAMADO' THEN 2
                ELSE 3
            END,
            e.event_fechaevento ASC,
            e.id_evento DESC
        """, nativeQuery = true)
    List<Object[]> findEventosControlGastosNative();
}
