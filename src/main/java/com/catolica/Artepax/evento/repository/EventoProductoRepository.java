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

    @Query(value = """
            SELECT ep.id_producto,
                   p.produ_nombre,
                   ep.cantidad_reservada,
                   p.produ_precio_unitario,
                   er.estado_nombre,
                   CASE WHEN dxp.id_producto IS NOT NULL THEN 'DECORACION' ELSE 'EXTRA' END AS origen
            FROM evento_producto ep
            JOIN producto p ON p.id_producto = ep.id_producto
            JOIN estado_reserva er ON er.id_estado_reserva = ep.id_estado_reserva
            LEFT JOIN decoracionxproducto dxp
                ON dxp.id_producto = ep.id_producto
                AND dxp.id_decoracion = (SELECT id_decoracion FROM evento WHERE id_evento = :eventoId)
            WHERE ep.id_evento = :eventoId
            ORDER BY origen, p.produ_nombre
            """, nativeQuery = true)
    List<Object[]> findProductosByEventoId(@Param("eventoId") Long eventoId);

    long countByProductoIdAndEstadoReservaEstadoNombre(Long productoId, String estadoNombre);

    @Query("""
            SELECT COALESCE(SUM(ep.cantidadReservada), 0)
            FROM EventoProducto ep
            WHERE ep.producto.id = :productoId
              AND ep.estadoReserva.estadoNombre = 'ACTIVA'
            """)
    Integer sumCantidadReservadaActivaByProductoId(@Param("productoId") Long productoId);
}
