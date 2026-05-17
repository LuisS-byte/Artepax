package com.catolica.Artepax.notificacion.repository;

import com.catolica.Artepax.notificacion.entity.EstadoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad {@link EstadoNotificacion}.
 * <p>
 * Proporciona acceso al catálogo de estados de notificación, incluyendo
 * la búsqueda por nombre de estado para asignación dinámica.
 */
@Repository
public interface EstadoNotificacionRepository extends JpaRepository<EstadoNotificacion, Long> {

    /**
     * Busca un estado de notificación por su nombre.
     *
     * @param nombreEstado el nombre del estado (ej. {@code PENDIENTE}, {@code LEIDA})
     * @return un {@code Optional} con la entidad encontrada, o vacío si no existe
     */
    Optional<EstadoNotificacion> findByNombreEstado(String nombreEstado);
}
