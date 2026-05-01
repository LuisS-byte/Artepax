package com.catolica.Artepax.evento.service;

import com.catolica.Artepax.evento.dto.TipoEventoResponseDTO;
import java.util.List;
public interface TipoEventoService {
    List<TipoEventoResponseDTO> listar();
    TipoEventoResponseDTO crear(String nombre, String descripcion);
    TipoEventoResponseDTO toggle(Long id);
}
