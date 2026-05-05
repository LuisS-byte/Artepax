package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.GastoEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GastoEventoRepository extends JpaRepository<GastoEvento, Long> {
    List<GastoEvento> findByEventoId(Long eventoId);

    @Query(value = "SELECT g FROM GastoEvento g JOIN FETCH g.evento e JOIN FETCH e.cliente JOIN FETCH e.estadoEvento",
           countQuery = "SELECT COUNT(g) FROM GastoEvento g")
    Page<GastoEvento> findAllWithEvento(Pageable pageable);
}
