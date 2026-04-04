package com.catolica.Artepax.cronograma.service;

import com.catolica.Artepax.cronograma.dto.CronogramaRequestDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaResponseDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaUpdateDTO;

import java.util.List;

public interface CronogramaService {

    CronogramaResponseDTO crear(CronogramaRequestDTO dto);

    List<CronogramaResponseDTO> obtenerPorEvento(Long idEvento);

    CronogramaResponseDTO obtenerPorId(Long id);

    CronogramaResponseDTO actualizar(Long id, CronogramaUpdateDTO dto);

    void eliminar(Long id);
}
