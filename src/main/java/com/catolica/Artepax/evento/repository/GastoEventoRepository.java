package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.GastoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GastoEventoRepository extends JpaRepository<GastoEvento, Long> {
    List<GastoEvento> findByEventoId(Long eventoId);
}
