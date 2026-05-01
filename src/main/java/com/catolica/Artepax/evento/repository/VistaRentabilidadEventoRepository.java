package com.catolica.Artepax.evento.repository;


import com.catolica.Artepax.evento.model.VistaRentabilidadEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VistaRentabilidadEventoRepository extends JpaRepository<VistaRentabilidadEvento, Long> {
    Optional<VistaRentabilidadEvento> findByIdEvento(Long idEvento);
}
