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
     * El evento debe estar en estado PROGRAMADO, CONFIRMADO o EN_MONTAJE.
     *
     * @param dto datos de la actividad a crear
     * @return la actividad creada
     */
    CronogramaResponseDTO crear(CronogramaRequestDTO dto);

    /**
     * Obtiene todas las actividades de cronograma de un evento.
     *
     * @param idEvento identificador del evento
     * @return lista de actividades del evento
     */
    List<CronogramaResponseDTO> obtenerPorEvento(Long idEvento);

    /**
     * Obtiene una actividad por su identificador.
     *
     * @param id identificador de la actividad
     * @return la actividad encontrada
     */
    CronogramaResponseDTO obtenerPorId(Long id);

    /**
     * Actualiza parcialmente una actividad de cronograma.
     *
     * @param id  identificador de la actividad
     * @param dto datos a actualizar (campos no nulos)
     * @return la actividad actualizada
     */
    CronogramaResponseDTO actualizar(Long id, CronogramaUpdateDTO dto);

    /**
     * Cambia el estado de una actividad. No permite transiciones desde estados finales.
     *
     * @param id         identificador de la actividad
     * @param nuevoEstado nuevo estado (PENDIENTE, EN_PROGRESO, COMPLETADO, CANCELADO)
     * @return la actividad con el estado actualizado
     */
    CronogramaResponseDTO cambiarEstado(Long id, String nuevoEstado);

    /**
     * Cancela una actividad de cronograma.
     *
     * @param id identificador de la actividad a cancelar
     */
    void cancelar(Long id);

    /**
     * Cancela todas las actividades activas de un evento (cascade cancellation).
     * Llamado por {@code EventoServiceImpl} al confirmar cancelación de evento.
     *
     * @param eventoId identificador del evento
     */
    void cancelarPorEvento(Long eventoId);

    /**
     * Obtiene todas las actividades paginadas.
     *
     * @param pageable parámetros de paginación
     * @return página de actividades
     */
    Page<CronogramaResponseDTO> obtenerTodos(Pageable pageable);

    /**
     * Asigna un usuario a una actividad de cronograma.
     *
     * @param cronogramaId identificador de la actividad
     * @param dto          datos de asignación (usuario, pago, tipo de pago)
     * @return la asignación creada
     */
    CronogramaUsuarioResponseDTO asignarUsuario(Long cronogramaId,
                                                CronogramaUsuarioRequestDTO dto);

    /**
     * Obtiene los usuarios asignados a una actividad.
     *
     * @param cronogramaId identificador de la actividad
     * @return lista de asignaciones
     */
    List<CronogramaUsuarioResponseDTO> obtenerUsuarios(Long cronogramaId);

    /**
     * Remueve un usuario de una actividad de cronograma.
     *
     * @param cronogramaId identificador de la actividad
     * @param usuarioId    identificador del usuario a remover
     */
    void removerUsuario(Long cronogramaId, Long usuarioId);
}
