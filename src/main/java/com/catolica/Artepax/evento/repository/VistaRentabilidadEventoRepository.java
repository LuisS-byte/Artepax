package com.catolica.Artepax.evento.repository;


import com.catolica.Artepax.evento.model.VistaRentabilidadEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio para la vista de base de datos {@code vista_rentabilidad_evento}.
 * <p>
 * Proporciona acceso a los datos de rentabilidad precalculados por evento.
 */
public interface VistaRentabilidadEventoRepository extends JpaRepository<VistaRentabilidadEvento, Long> {

    /**
     * Retorna los datos de rentabilidad de un evento.
     *
     * @param idEvento identificador del evento
     * @return rentabilidad del evento, si existe
     */
    Optional<VistaRentabilidadEvento> findByIdEvento(Long idEvento);
}
