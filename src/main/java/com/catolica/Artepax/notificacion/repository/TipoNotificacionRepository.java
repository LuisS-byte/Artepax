package com.catolica.Artepax.notificacion.repository;

import com.catolica.Artepax.notificacion.entity.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad {@link TipoNotificacion}.
 * <p>
 * Proporciona acceso al catálogo de tipos de notificación, incluyendo
 * la búsqueda por nombre de tipo para asignación dinámica.
 */
@Repository
public interface TipoNotificacionRepository extends JpaRepository<TipoNotificacion, Long> {

    /**
     * Busca un tipo de notificación por su nombre.
     *
     * @param nombreTipo el nombre del tipo (ej. {@code RECORDATORIO_EVENTO})
     * @return un {@code Optional} con la entidad encontrada, o vacío si no existe
     */
    Optional<TipoNotificacion> findByNombreTipo(String nombreTipo);
}
