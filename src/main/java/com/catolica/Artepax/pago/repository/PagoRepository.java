package com.catolica.Artepax.pago.repository;


import com.catolica.Artepax.pago.entity.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("""
            SELECT p
            FROM Pago p
            JOIN FETCH p.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            JOIN FETCH p.estadoPago
            JOIN FETCH p.conceptoPago
            JOIN FETCH p.metodoPago
            WHERE e.id = :eventoId
            """)
    List<Pago> findByEventoIdWithRelations(@Param("eventoId") Long eventoId);

    // Suma total de pagos PAGADO para validar sobrepago
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.evento.id = :eventoId " +
            "AND p.estadoPago.estadoNombre = 'PAGADO'")
    BigDecimal sumMontoConfirmadoByEventoId(Long eventoId);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.evento.id = :eventoId " +
            "AND p.id <> :pagoId " +
            "AND p.estadoPago.estadoNombre = 'PAGADO'")
    BigDecimal sumPagosConfirmadosPorEventoExcluyendoPago(@Param("eventoId") Long eventoId,
                                                          @Param("pagoId") Long pagoId);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.evento.id = :eventoId " +
            "AND p.estadoPago.estadoNombre = 'PAGADO' " +
            "AND p.conceptoPago.conceptoNombre = :conceptoNombre")
    BigDecimal sumMontoConfirmadoByEventoIdAndConceptoNombre(Long eventoId, String conceptoNombre);

    // Listado global paginado con JOIN para evitar N+1
    @Query(value = """
            SELECT p
            FROM Pago p
            JOIN FETCH p.evento e
            JOIN FETCH e.cliente
            JOIN FETCH e.estadoEvento
            """,
           countQuery = "SELECT COUNT(p) FROM Pago p")
    Page<Pago> findAllWithEvento(Pageable pageable);
}
