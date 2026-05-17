package com.catolica.Artepax.notificacion.repository;

import com.catolica.Artepax.notificacion.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

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

    @Query("""
            SELECT n
            FROM Notificacion n
            JOIN FETCH n.usuario
            JOIN FETCH n.estadoNotificacion
            WHERE n.id = :id
            """)
    Optional<Notificacion> findByIdWithOwnerAndEstado(@Param("id") Long id);

    /**
     * Verifica si ya existe una notificación del tipo dado para el evento dado
     * desde la fecha/hora indicada. Usado por jobs programados para evitar
     * notificaciones duplicadas (idempotencia).
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
