package com.catolica.Artepax.pago.repository;

import com.catolica.Artepax.pago.entity.VistaPagosEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VistaPagosEventoRepository extends JpaRepository<VistaPagosEvento, Long> {

    Optional<VistaPagosEvento> findByIdEvento(Long idEvento);
}
