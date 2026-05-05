package com.catolica.Artepax.cronograma.service;

import com.catolica.Artepax.cronograma.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Servicio para gestión de cronogramas de eventos.
 * Permite crear, actualizar y cambiar estado de actividades operativas,
 * así como asignar y remover usuarios de las mismas.
 */
public interface CronogramaService {

    /**
     * Crea una nueva actividad de cronograma para un evento.
     * El evento debe estar en estado CONFIRMADO o EN_MONTAJE.
     */
    CronogramaResponseDTO crear(CronogramaRequestDTO dto);

    List<CronogramaResponseDTO> obtenerPorEvento(Long idEvento);

    CronogramaResponseDTO obtenerPorId(Long id);

    CronogramaResponseDTO actualizar(Long id, CronogramaUpdateDTO dto);

    /**
     * Cambia el estado de una actividad. No permite transiciones desde estados finales.
     */
    CronogramaResponseDTO cambiarEstado(Long id, String nuevoEstado);

    void cancelar(Long id);

    /**
     * Cancela todas las actividades activas de un evento (cascade cancellation).
     * Llamado por EventoServiceImpl al confirmar cancelación de evento.
     */
    void cancelarPorEvento(Long eventoId);

    Page<CronogramaResponseDTO> obtenerTodos(Pageable pageable);

    CronogramaUsuarioResponseDTO asignarUsuario(Long cronogramaId,
                                                CronogramaUsuarioRequestDTO dto);
    List<CronogramaUsuarioResponseDTO> obtenerUsuarios(Long cronogramaId);
    void removerUsuario(Long cronogramaId, Long usuarioId);
}
