package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoEventoRepository extends JpaRepository<TipoEvento, Long> {

    boolean existsByTipoNombreIgnoreCase(String tipoNombre);

    List<TipoEvento> findAllByOrderByTipoNombreAsc();
}
