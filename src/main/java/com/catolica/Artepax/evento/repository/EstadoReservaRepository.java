package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio para el catálogo de estados de reserva ({@code estado_reserva}).
 */
public interface EstadoReservaRepository extends JpaRepository<EstadoReserva, Long> {

    /**
     * Busca un estado de reserva por su nombre.
     *
     * @param estadoNombre nombre del estado (ACTIVA, LIBERADA, CONSUMIDA)
     * @return estado de reserva encontrado, si existe
     */
    Optional<EstadoReserva> findByEstadoNombre(String estadoNombre);
}
