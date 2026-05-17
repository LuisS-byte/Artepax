package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.EstadoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repositorio para el catálogo de estados del evento ({@code estado_evento}).
 */
public interface EstadoEventoRepository extends JpaRepository<EstadoEvento, Long> {

    /**
     * Busca un estado por su nombre.
     *
     * @param nombre nombre del estado (COTIZACION, PROGRAMADO, etc.)
     * @return estado encontrado, si existe
     */
    Optional<EstadoEvento> findByNombre(String nombre);
}