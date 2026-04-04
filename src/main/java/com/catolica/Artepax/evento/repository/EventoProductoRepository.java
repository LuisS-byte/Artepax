package com.catolica.Artepax.evento.repository;


import com.catolica.Artepax.evento.model.EventoProducto;
import com.catolica.Artepax.evento.model.EventoProductoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface EventoProductoRepository extends JpaRepository<EventoProducto, EventoProductoId> {

    List<EventoProducto> findByEventoId(Long eventoId);
}