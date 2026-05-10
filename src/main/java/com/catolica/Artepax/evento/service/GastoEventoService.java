package com.catolica.Artepax.evento.service;

import com.catolica.Artepax.evento.dto.GastoEventoRequestDTO;
import com.catolica.Artepax.evento.dto.GastoEventoResponseDTO;
import com.catolica.Artepax.evento.dto.ResumenGastoEventoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Servicio para la gestion de gastos variables atribuibles a un evento.
 * <p>
 * Los gastos solo pueden registrarse en eventos PROGRAMADO, CONFIRMADO o EN_MONTAJE,
 * y tienen dos estados: PROYECTADO y DESEMBOLSADO.
 */
public interface GastoEventoService {

    GastoEventoResponseDTO crear(GastoEventoRequestDTO dto);

    List<GastoEventoResponseDTO> obtenerPorEvento(Long eventoId);

    GastoEventoResponseDTO obtenerPorId(Long id);

    /**
     * Transicion: PROYECTADO -> DESEMBOLSADO.
     */
    GastoEventoResponseDTO desembolsar(Long id);

    /**
     * Elimina un gasto proyectado. No se permiten eliminar gastos desembolsados.
     */
    void eliminar(Long id);

    Page<GastoEventoResponseDTO> obtenerTodos(Pageable pageable);

    ResumenGastoEventoDTO getResumen(Long eventoId);
}
