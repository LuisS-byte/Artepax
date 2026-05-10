package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoReservaRepository extends JpaRepository<EstadoReserva, Long> {

    Optional<EstadoReserva> findByEstadoNombre(String estadoNombre);
}
