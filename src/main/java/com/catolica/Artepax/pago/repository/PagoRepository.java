package com.catolica.Artepax.pago.repository;


import com.catolica.Artepax.pago.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByEventoId(Long eventoId);

    // Suma total de pagos PAGADO para validar sobrepago
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.evento.id = :eventoId " +
            "AND p.estadoPago.estadoNombre = 'PAGADO'")
    BigDecimal sumMontoConfirmadoByEventoId(Long eventoId);
}