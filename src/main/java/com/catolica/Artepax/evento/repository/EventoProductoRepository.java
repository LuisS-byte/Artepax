package com.catolica.Artepax.evento.repository;


import com.catolica.Artepax.evento.model.EventoProducto;
import com.catolica.Artepax.evento.model.EventoProductoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la tabla puente {@code evento_producto} que vincula eventos
 * con productos y gestiona las reservas de stock.
 * <p>
 * Cada registro representa una reserva de un producto para un evento en un
 * estado específico (ACTIVA, LIBERADA, CONSUMIDA). La disponibilidad de stock
 * se calcula restando las reservas activas del total de productos.
 */
public interface EventoProductoRepository extends JpaRepository<EventoProducto, EventoProductoId> {

    /**
     * Busca una reserva específica de un producto en un evento por su estado.
     *
     * @param eventoId     identificador del evento
     * @param productoId   identificador del producto
     * @param estadoNombre estado de la reserva (ACTIVA, LIBERADA, CONSUMIDA)
     * @return la reserva encontrada, si existe
     */
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

    /**
     * Retorna todas las reservas de un evento filtradas por estado.
     *
     * @param eventoId     identificador del evento
     * @param estadoNombre estado de la reserva (ACTIVA, LIBERADA, CONSUMIDA)
     * @return lista de reservas del evento en el estado dado
     */
    @Query("""
            SELECT ep
            FROM EventoProducto ep
            WHERE ep.evento.id = :eventoId
              AND ep.estadoReserva.estadoNombre = :estadoNombre
            """)
    List<EventoProducto> findByEventoIdAndEstadoReservaNombre(
            @Param("eventoId") Long eventoId,
            @Param("estadoNombre") String estadoNombre);

    /**
     * Retorna las reservas activas de un evento para un conjunto específico de productos.
     *
     * @param eventoId     identificador del evento
     * @param productoIds  identificadores de los productos a buscar
     * @param estadoNombre estado de la reserva (ACTIVA, LIBERADA, CONSUMIDA)
     * @return lista de reservas coincidentes
     */
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

    /**
     * Agrupa y suma las cantidades reservadas de productos activos por producto.
     * Utilizado para calcular disponibilidad de stock.
     *
     * @param productoIds  identificadores de los productos
     * @param estadoNombre estado de la reserva (ACTIVA)
     * @return proyección con productoId y cantidadReservada agregada
     */
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

    /**
     * Consulta nativa que retorna todos los productos asociados a un evento
     * con datos denormalizados: nombre, cantidad reservada, precio unitario,
     * estado de reserva y origen (DECORACION si pertenece a la decoración
     * del evento, EXTRA si fue agregado como extra).
     *
     * @param eventoId identificador del evento
     * @return lista de arreglos con datos de producto por evento
     */
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

    /**
     * Cuenta cuántas reservas activas existen para un producto en un estado dado.
     *
     * @param productoId   identificador del producto
     * @param estadoNombre estado de la reserva
     * @return cantidad de reservas en ese estado
     */
    long countByProductoIdAndEstadoReservaEstadoNombre(Long productoId, String estadoNombre);

    /**
     * Suma la cantidad total reservada de un producto en estado ACTIVA.
     *
     * @param productoId identificador del producto
     * @return suma de cantidades reservadas activas, 0 si no hay reservas
     */
    @Query("""
            SELECT COALESCE(SUM(ep.cantidadReservada), 0)
            FROM EventoProducto ep
            WHERE ep.producto.id = :productoId
              AND ep.estadoReserva.estadoNombre = 'ACTIVA'
            """)
    Integer sumCantidadReservadaActivaByProductoId(@Param("productoId") Long productoId);
}
