package com.catolica.Artepax.evento.service;

import com.catolica.Artepax.evento.dto.GastoEventoRequestDTO;
import com.catolica.Artepax.evento.dto.GastoEventoResponseDTO;
import java.util.List;

public interface GastoEventoService {
    GastoEventoResponseDTO crear(GastoEventoRequestDTO dto);
    List<GastoEventoResponseDTO> obtenerPorEvento(Long eventoId);
    GastoEventoResponseDTO obtenerPorId(Long id);
    GastoEventoResponseDTO desembolsar(Long id);
    void eliminar(Long id);
}
