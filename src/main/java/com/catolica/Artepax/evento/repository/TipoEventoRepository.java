package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para el catálogo de tipos de evento ({@code tipo_evento}).
 */
@Repository
public interface TipoEventoRepository extends JpaRepository<TipoEvento, Long> {

    /**
     * Verifica si existe un tipo de evento con el nombre dado (ignorando mayúsculas).
     *
     * @param tipoNombre nombre del tipo de evento a verificar
     * @return true si ya existe un tipo con ese nombre
     */
    boolean existsByTipoNombreIgnoreCase(String tipoNombre);

    /**
     * Retorna todos los tipos de evento ordenados por nombre ascendente.
     *
     * @return lista ordenada de tipos de evento
     */
    List<TipoEvento> findAllByOrderByTipoNombreAsc();
}
