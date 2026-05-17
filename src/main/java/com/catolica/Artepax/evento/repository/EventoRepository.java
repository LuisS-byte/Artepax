package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.Evento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad {@link Evento}.
 * <p>
 * Proporciona consultas para verificación de disponibilidad (con y sin
 * exclusión de evento), búsqueda con relaciones eager para evitar
 * LazyInitializationException, y consultas específicas para jobs
 * programados (cotizaciones vencidas, cancelaciones pendientes).
 */
public interface EventoRepository extends JpaRepository<Evento, Long> {

    /**
     * Verifica si existe un evento en la misma fecha, horario y ubicación
     * (sin considerar estado o exclusión).
     *
     * @param fecha     fecha del evento
     * @param horaInicio hora de inicio
     * @param ubicacion ubicación del evento
     * @return true si ya existe un evento con esos datos
     */
    boolean existsByFechaAndHoraInicioAndUbicacion(
        LocalDate fecha,
        LocalTime horaInicio,
        String ubicacion
    );

    /**
     * Verifica si existe otro evento activo en la misma fecha con horario solapado.
     * <p>
     * <strong>Regla de negocio — bloqueo global:</strong> Artepax solo atiende
     * un evento por bloque horario, independientemente de la ubicación. El
     * parámetro {@code ubicacion} se recibe para validación del formulario
     * (el cliente debe indicarla) pero NO se usa en el filtro de la query.
     * Si en el futuro se permiten eventos simultáneos en distintas ubicaciones,
         * debe agregarse {@code AND e.ubicacion = :ubicacion} a esta query.
     *
     * @param fecha       fecha del evento
     * @param horaInicio  inicio del bloque (con gap aplicado)
     * @param horaFin     fin del bloque (con gap aplicado)
     * @param ubicacion   se valida en el service pero NO filtra en BD
     * @return true si hay solapamiento
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Evento e
            JOIN e.estadoEvento ee
            WHERE e.fecha = :fecha
              AND ee.nombre IN ('PROGRAMADO', 'CONFIRMADO', 'EN_MONTAJE', 'CANCELACION_PENDIENTE')
              AND :horaInicio < e.horaFin
              AND :horaFin > e.horaInicio
            """)
    boolean existeSolapamiento(@Param("fecha") LocalDate fecha,
                               @Param("horaInicio") LocalTime horaInicio,
                               @Param("horaFin") LocalTime horaFin,
                               @Param("ubicacion") String ubicacion);

    /**
     * Ídem {@link #existeSolapamiento} pero excluyendo un evento específico
     * (útil al editar un evento existente para no detectarse a sí mismo).
     * <p>
     * Misma regla de bloqueo global: {@code ubicacion} no participa en el filtro.
     *
     * @param eventoIdExcluir ID del evento a excluir de la verificación
     * @param fecha           fecha del evento
     * @param horaInicio      inicio del bloque (con gap aplicado)
     * @param horaFin         fin del bloque (con gap aplicado)
     * @param ubicacion       se valida en el service pero NO filtra en BD
     * @return true si hay solapamiento
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Evento e
            JOIN e.estadoEvento ee
            WHERE e.id <> :eventoIdExcluir
              AND e.fecha = :fecha
              AND ee.nombre IN ('PROGRAMADO', 'CONFIRMADO', 'EN_MONTAJE', 'CANCELACION_PENDIENTE')
              AND :horaInicio < e.horaFin
              AND :horaFin > e.horaInicio
            """)
    boolean existeSolapamientoExcluyendoEvento(@Param("eventoIdExcluir") Long eventoIdExcluir,
                                               @Param("fecha") LocalDate fecha,
                                               @Param("horaInicio") LocalTime horaInicio,
                                               @Param("horaFin") LocalTime horaFin,
                                               @Param("ubicacion") String ubicacion);

    /**
     * Retorna los eventos de un cliente específico (sin relaciones eager).
     *
     * @param clienteId identificador del cliente
     * @return lista de eventos del cliente
     */
    List<Evento> findByClienteId(Long clienteId);

    /**
     * Retorna los eventos de un cliente con todas las relaciones cargadas eager.
     *
     * @param clienteId identificador del cliente
     * @return lista de eventos del cliente con relaciones
     */
    @Query("""
            SELECT e
            FROM Evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.decoracion
            JOIN FETCH e.tipoEvento
            JOIN FETCH e.estadoEvento
            WHERE e.cliente.id = :clienteId
            ORDER BY e.fecha DESC, e.horaInicio DESC
            """)
    List<Evento> findByClienteIdWithRelations(@Param("clienteId") Long clienteId);

    /**
     * Retorna los eventos de una fecha específica (para verificación de disponibilidad).
     *
     * @param fecha fecha a consultar
     * @return lista de eventos en esa fecha
     */
    List<Evento> findByFecha(LocalDate fecha);

    /**
     * Retorna los eventos dentro de un rango de fechas (para calendario).
     *
     * @param inicio fecha de inicio del rango
     * @param fin    fecha de fin del rango
     * @return lista de eventos en el período
     */
    List<Evento> findByFechaBetween(LocalDate inicio, LocalDate fin);

    /**
     * Retorna todos los eventos paginados con todas las relaciones cargadas eager.
     * <p>
     * Usa {@code countQuery} separada para evitar que el JOIN FETCH interfiera
     * con el conteo total de páginas.
     *
     * @param pageable parámetros de paginación y ordenamiento
     * @return página de eventos con cliente, decoración, tipo y estado
     */
    @Query(value = """
            SELECT e
            FROM Evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.decoracion
            JOIN FETCH e.tipoEvento
            JOIN FETCH e.estadoEvento
            """,
            countQuery = "SELECT COUNT(e) FROM Evento e")
    Page<Evento> findAllWithRelations(Pageable pageable);

    /**
     * Retorna los eventos en estado CANCELACION_PENDIENTE para procesamiento por jobs.
     *
     * @return lista de eventos pendientes de confirmación de cancelación
     */
    @Query("SELECT e FROM Evento e JOIN e.estadoEvento ee " +
            "WHERE ee.nombre = 'CANCELACION_PENDIENTE'")
    List<Evento> findEnCancelacionPendiente();

    /**
     * Cuenta cuántos eventos existen en un estado específico.
     *
     * @param nombre nombre del estado del evento
     * @return cantidad de eventos en ese estado
     */
    long countByEstadoEventoNombre(String nombre);

    /**
     * Retorna las cotizaciones vencidas cuya fecha de expiración es anterior a la fecha dada.
     * Utilizado por el job programado de vencimiento de cotizaciones.
     *
     * @param hoy fecha de referencia para comparar expiración
     * @return lista de cotizaciones vencidas con cliente cargado
     */
    @Query("SELECT e FROM Evento e JOIN FETCH e.cliente JOIN e.estadoEvento ee " +
            "WHERE ee.nombre = 'COTIZACION' " +
            "AND e.fechaExpiracion IS NOT NULL " +
            "AND e.fechaExpiracion < :hoy")
    List<Evento> findCotizacionesVencidas(@Param("hoy") LocalDate hoy);

    /**
     * Retorna un evento con las relaciones cliente y estado cargadas eager.
     * Utilizado por jobs para evitar LazyInitializationException.
     *
     * @param id identificador del evento
     * @return evento con cliente y estado, si existe
     */
    @Query("SELECT e FROM Evento e JOIN FETCH e.cliente JOIN FETCH e.estadoEvento WHERE e.id = :id")
    Optional<Evento> findByIdWithClienteAndEstado(@Param("id") Long id);

    /**
     * Retorna un evento con todas las relaciones cargadas eager para generación de documentos.
     *
     * @param idEvento identificador del evento
     * @return evento con relaciones completas, si existe
     */
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

    /**
     * Retorna un evento con bloqueo pesimista de escritura para operaciones
     * concurrentes (reserva de stock, transiciones de estado).
     *
     * @param id identificador del evento
     * @return evento bloqueado, si existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Evento e WHERE e.id = :id")
    Optional<Evento> findByIdForUpdate(@Param("id") Long id);
}
