package com.catolica.Artepax.evento.repository;


import com.catolica.Artepax.evento.model.EventoProducto;
import com.catolica.Artepax.evento.model.EventoProductoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventoProductoRepository extends JpaRepository<EventoProducto, EventoProductoId> {

    @Query("""
            SELECT ep
            FROM EventoProducto ep
            WHERE ep.evento.id = :eventoId
              AND ep.producto.id = :productoId
              AND ep.estadoReserva.estadoNombre = :estadoNombre
            """)
    Optional<EventoProducto> findByEventoIdAndProductoIdAndEstadoReservaNombre(
            @Param("eventoId") Long eventoId,
            @Param("productoId") Long productoId,
            @Param("estadoNombre") String estadoNombre);

    @Query("""
            SELECT ep
            FROM EventoProducto ep
            WHERE ep.evento.id = :eventoId
              AND ep.estadoReserva.estadoNombre = :estadoNombre
            """)
    List<EventoProducto> findByEventoIdAndEstadoReservaNombre(
            @Param("eventoId") Long eventoId,
            @Param("estadoNombre") String estadoNombre);

    @Query("""
            SELECT ep
            FROM EventoProducto ep
            WHERE ep.evento.id = :eventoId
              AND ep.producto.id IN :productoIds
              AND ep.estadoReserva.estadoNombre = :estadoNombre
            """)
    List<EventoProducto> findByEventoIdAndProductoIdsAndEstadoReservaNombre(
            @Param("eventoId") Long eventoId,
            @Param("productoIds") Collection<Long> productoIds,
            @Param("estadoNombre") String estadoNombre);

    @Query("""
            SELECT ep.producto.id AS productoId,
                   SUM(ep.cantidadReservada) AS cantidadReservada
            FROM EventoProducto ep
            WHERE ep.producto.id IN :productoIds
              AND ep.estadoReserva.estadoNombre = :estadoNombre
            GROUP BY ep.producto.id
            """)
    List<ReservaStockProducto> sumCantidadReservadaByProductoIdsAndEstadoReservaNombre(
            @Param("productoIds") Collection<Long> productoIds,
            @Param("estadoNombre") String estadoNombre);

    long countByProductoIdAndEstadoReservaEstadoNombre(Long productoId, String estadoNombre);

    @Query("""
            SELECT COALESCE(SUM(ep.cantidadReservada), 0)
            FROM EventoProducto ep
            WHERE ep.producto.id = :productoId
              AND ep.estadoReserva.estadoNombre = 'ACTIVA'
            """)
    Integer sumCantidadReservadaActivaByProductoId(@Param("productoId") Long productoId);
}
