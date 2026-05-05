package com.catolica.Artepax.evento.service;

import com.catolica.Artepax.evento.dto.GastoEventoRequestDTO;
import com.catolica.Artepax.evento.dto.GastoEventoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Servicio para la gestión de gastos variables atribuibles a un evento.
 * <p>
 * Los gastos solo pueden registrarse en eventos PROGRAMADO, CONFIRMADO o EN_MONTAJE,
 * y tienen dos estados: PROYECTADO y DESEMBOLSADO.
 */
public interface GastoEventoService {

    /**
     * Registra un nuevo gasto para un evento.
     *
     * @param dto datos del gasto a registrar
     * @return DTO del gasto creado
     */
    GastoEventoResponseDTO crear(GastoEventoRequestDTO dto);

    /**
     * Retorna todos los gastos asociados a un evento.
     */
    List<GastoEventoResponseDTO> obtenerPorEvento(Long eventoId);

    /**
     * Retorna un gasto por su identificador.
     */
    GastoEventoResponseDTO obtenerPorId(Long id);

    /**
     * Transición: PROYECTADO → DESEMBOLSADO.
     *
     * @param id identificador del gasto
     * @return DTO del gasto actualizado
     */
    GastoEventoResponseDTO desembolsar(Long id);

    /**
     * Elimina un gasto proyectado. No se permiten eliminar gastos desembolsados.
     */
    void eliminar(Long id);

    /**
     * Retorna todos los gastos paginados y ordenados por fecha.
     */
    Page<GastoEventoResponseDTO> obtenerTodos(Pageable pageable);
}
