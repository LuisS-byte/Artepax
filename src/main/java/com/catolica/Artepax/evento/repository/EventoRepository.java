package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    boolean existsByFechaAndHoraInicioAndUbicacion(
        LocalDate fecha,
        LocalTime horaInicio,
        String ubicacion
    );

    @Query("""
            SELECT COUNT(e) > 0
            FROM Evento e
            JOIN e.estadoEvento ee
            WHERE e.fecha = :fecha
              AND LOWER(TRIM(e.ubicacion)) = LOWER(TRIM(:ubicacion))
              AND ee.nombre IN ('PROGRAMADO', 'CONFIRMADO', 'EN_MONTAJE', 'CANCELACION_PENDIENTE')
              AND :horaInicio < e.horaFin
              AND :horaFin > e.horaInicio
            """)
    boolean existeSolapamiento(@Param("fecha") LocalDate fecha,
                               @Param("horaInicio") LocalTime horaInicio,
                               @Param("horaFin") LocalTime horaFin,
                               @Param("ubicacion") String ubicacion);

    @Query("""
            SELECT COUNT(e) > 0
            FROM Evento e
            JOIN e.estadoEvento ee
            WHERE e.id <> :eventoIdExcluir
              AND e.fecha = :fecha
              AND LOWER(TRIM(e.ubicacion)) = LOWER(TRIM(:ubicacion))
              AND ee.nombre IN ('PROGRAMADO', 'CONFIRMADO', 'EN_MONTAJE', 'CANCELACION_PENDIENTE')
              AND :horaInicio < e.horaFin
              AND :horaFin > e.horaInicio
            """)
    boolean existeSolapamientoExcluyendoEvento(@Param("eventoIdExcluir") Long eventoIdExcluir,
                                               @Param("fecha") LocalDate fecha,
                                               @Param("horaInicio") LocalTime horaInicio,
                                               @Param("horaFin") LocalTime horaFin,
                                               @Param("ubicacion") String ubicacion);

    List<Evento> findByClienteId(Long clienteId);

    // para disponibilidad
    List<Evento> findByFecha(LocalDate fecha);

    // para calendario por rango de fechas
    List<Evento> findByFechaBetween(LocalDate inicio, LocalDate fin);

    @Query("""
            SELECT e
            FROM Evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.decoracion
            JOIN FETCH e.tipoEvento
            JOIN FETCH e.estadoEvento
            """)
    List<Evento> findAllWithRelations();

    @Query("SELECT e FROM Evento e JOIN e.estadoEvento ee " +
            "WHERE ee.nombre = 'CANCELACION_PENDIENTE'")
    List<Evento> findEnCancelacionPendiente();

    // Para job de cotizaciones vencidas
    @Query("SELECT e FROM Evento e JOIN e.estadoEvento ee " +
            "WHERE ee.nombre = 'COTIZACION' " +
            "AND e.fechaExpiracion IS NOT NULL " +
            "AND e.fechaExpiracion < :hoy")
    List<Evento> findCotizacionesVencidas(@Param("hoy") LocalDate hoy);

    @Query("""
            SELECT e
            FROM Evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.decoracion
            JOIN FETCH e.tipoEvento
            JOIN FETCH e.estadoEvento
            WHERE e.id = :idEvento
            """)
    Optional<Evento> findDocumentoContextById(@Param("idEvento") Long idEvento);
}
