package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.EstadoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EstadoEventoRepository extends JpaRepository<EstadoEvento, Long> {
    Optional<EstadoEvento> findByNombre(String nombre);
}