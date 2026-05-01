package com.catolica.Artepax.pago.service;


import com.catolica.Artepax.pago.dto.ConceptoPagoResponseDTO;
import com.catolica.Artepax.pago.dto.EstadoPagoResponseDTO;
import com.catolica.Artepax.pago.dto.MetodoPagoResponseDTO;

import java.util.List;

public interface CatalogosPagoService {
    // EstadoPago
    List<EstadoPagoResponseDTO> listarEstados();

    // ConceptoPago
    List<ConceptoPagoResponseDTO> listarConceptos();

    // MetodoPago
    List<MetodoPagoResponseDTO> listarMetodos();
    MetodoPagoResponseDTO crearMetodo(String nombre, String descripcion);
    MetodoPagoResponseDTO toggleMetodo(Long id);
}