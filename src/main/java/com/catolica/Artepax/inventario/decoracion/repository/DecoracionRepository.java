package com.catolica.Artepax.inventario.decoracion.repository;

import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecoracionRepository extends JpaRepository<Decoracion, Long> {

    List<Decoracion> findByEstado(String estado);
}
