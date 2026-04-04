package com.catolica.Artepax.inventario.decoracion.service;


import com.catolica.Artepax.inventario.decoracion.dto.DecoracionRequestDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionResponseDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionUpdateDTO;

import java.util.List;
public interface DecoracionService {

    DecoracionResponseDTO crearDecoracion(DecoracionRequestDTO dto);
    List<DecoracionResponseDTO> obtenerTodas();
    DecoracionResponseDTO obtenerPorId(Long id);
    DecoracionResponseDTO actualizar(Long id, DecoracionUpdateDTO dto);
    void eliminar(Long id);
}
