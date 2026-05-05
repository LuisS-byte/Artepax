package com.catolica.Artepax.evento.service;

import com.catolica.Artepax.evento.dto.TipoEventoResponseDTO;
import java.util.List;

/**
 * Servicio para la gestión del catálogo de tipos de evento.
 */
public interface TipoEventoService {

    /**
     * Retorna todos los tipos de evento existentes.
     */
    List<TipoEventoResponseDTO> listar();

    /**
     * Crea un nuevo tipo de evento activo.
     *
     * @param nombre nombre del tipo
     * @param descripcion descripción opcional
     * @return DTO del tipo creado
     */
    TipoEventoResponseDTO crear(String nombre, String descripcion);

    /**
     * Activa o desactiva un tipo de evento existente.
     *
     * @param id identificador del tipo
     * @return DTO del tipo actualizado
     */
    TipoEventoResponseDTO toggle(Long id);
}
