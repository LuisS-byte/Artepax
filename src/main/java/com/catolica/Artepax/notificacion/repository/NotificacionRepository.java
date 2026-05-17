package com.catolica.Artepax.notificacion.repository;

import com.catolica.Artepax.notificacion.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad {@link Notificacion}.
 * <p>
 * Incluye consultas personalizadas para obtener notificaciones con sus relaciones,
 * validación de propietario y verificación de idempotencia para jobs programados.
 */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /**
     * Obtiene todas las notificaciones de un usuario, ordenadas por fecha de envío
     * descendente. Incluye mediante {@code JOIN FETCH} las relaciones con tipo,
     * estado y evento para evitar consultas N+1.
     *
     * @param usuarioId identificador del usuario destino
     * @return lista de notificaciones con sus relaciones cargadas
     */
    @Query("""
            SELECT n
            FROM Notificacion n
            JOIN FETCH n.tipoNotificacion
            JOIN FETCH n.estadoNotificacion
            LEFT JOIN FETCH n.evento
            WHERE n.usuario.idUsuario = :usuarioId
            ORDER BY n.fechaEnvio DESC
            """)
    List<Notificacion> findByUsuarioIdWithRelationsOrderByFechaEnvioDesc(@Param("usuarioId") Long usuarioId);

    /**
     * Busca una notificación por su ID cargando simultáneamente el usuario
     * propietario y el estado. Usado en operaciones que requieren verificar
     * propiedad antes de modificar el estado.
     *
     * @param id identificador de la notificación
     * @return un {@code Optional} con la notificación y sus relaciones cargadas
     */
    @Query("""
            SELECT n
            FROM Notificacion n
            JOIN FETCH n.usuario
            JOIN FETCH n.estadoNotificacion
            WHERE n.id = :id
            """)
    Optional<Notificacion> findByIdWithOwnerAndEstado(@Param("id") Long id);

    /**
     * Verifica si ya existe una notificación del tipo dado para el evento indicado
     * desde la fecha/hora especificada.
     * <p>
     * Esta consulta es utilizada por los jobs programados para garantizar
     * idempotencia: evitar que se generen notificaciones duplicadas cuando
     * un mismo evento es procesado más de una vez.
     *
     * @param tipoNombre nombre del tipo de notificación (ej. {@code RECORDATORIO_EVENTO})
     * @param eventoId   identificador del evento asociado
     * @param since      límite temporal inferior; solo se consideran notificaciones
     *                   creadas a partir de esta fecha/hora
     * @return {@code true} si ya existe al menos una notificación que coincide
     */
    @Query("""
            SELECT COUNT(n) > 0
            FROM Notificacion n
            JOIN n.tipoNotificacion tn
            WHERE tn.nombreTipo = :tipoNombre
            AND n.evento.id = :eventoId
            AND n.fechaEnvio >= :since
            """)
    boolean existsByTipoAndEventoSince(@Param("tipoNombre") String tipoNombre,
                                       @Param("eventoId") Long eventoId,
                                       @Param("since") LocalDateTime since);
}
