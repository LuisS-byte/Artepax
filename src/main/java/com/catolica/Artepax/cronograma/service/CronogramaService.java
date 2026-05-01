package com.catolica.Artepax.cronograma.service;

import com.catolica.Artepax.cronograma.dto.*;

import java.util.List;

public interface CronogramaService {

    CronogramaResponseDTO crear(CronogramaRequestDTO dto);
    List<CronogramaResponseDTO> obtenerPorEvento(Long idEvento);
    CronogramaResponseDTO obtenerPorId(Long id);
    CronogramaResponseDTO actualizar(Long id, CronogramaUpdateDTO dto);
    CronogramaResponseDTO cambiarEstado(Long id, String nuevoEstado);
    void cancelar(Long id);
    void cancelarPorEvento(Long eventoId); // llamado por EventoService

    // CronogramaUsuario
    CronogramaUsuarioResponseDTO asignarUsuario(Long cronogramaId,
                                                CronogramaUsuarioRequestDTO dto);
    List<CronogramaUsuarioResponseDTO> obtenerUsuarios(Long cronogramaId);
    void removerUsuario(Long cronogramaId, Long usuarioId);
}
