package com.catolica.Artepax.pago.service.impl;

import com.catolica.Artepax.pago.dto.*;
import com.catolica.Artepax.pago.entity.*;
import com.catolica.Artepax.pago.exception.MetodoPagoDuplicadoException;
import com.catolica.Artepax.pago.exception.MetodoPagoNotFoundException;
import com.catolica.Artepax.pago.repository.*;
import com.catolica.Artepax.pago.service.CatalogosPagoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogosPagoServiceImpl implements CatalogosPagoService {

    private static final String PATRON_CONTIENE_LETRA = ".*[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ].*";

    private final EstadoPagoRepository estadoRepo;
    private final ConceptoPagoRepository conceptoRepo;
    private final MetodoPagoRepository metodoRepo;

    public CatalogosPagoServiceImpl(EstadoPagoRepository estadoRepo,
                                    ConceptoPagoRepository conceptoRepo,
                                    MetodoPagoRepository metodoRepo) {
        this.estadoRepo = estadoRepo;
        this.conceptoRepo = conceptoRepo;
        this.metodoRepo = metodoRepo;
    }

    @Override
    public List<EstadoPagoResponseDTO> listarEstados() {
        return estadoRepo.findAll().stream().map(e -> {
            EstadoPagoResponseDTO dto = new EstadoPagoResponseDTO();
            dto.setId(e.getId());
            dto.setEstadoNombre(e.getEstadoNombre());
            dto.setEstadoDescripcion(e.getEstadoDescripcion());
            return dto;
        }).toList();
    }

    @Override
    public List<ConceptoPagoResponseDTO> listarConceptos() {
        return conceptoRepo.findAll().stream().map(c -> {
            ConceptoPagoResponseDTO dto = new ConceptoPagoResponseDTO();
            dto.setId(c.getId());
            dto.setConceptoNombre(c.getConceptoNombre());
            dto.setConceptoDescripcion(c.getConceptoDescripcion());
            return dto;
        }).toList();
    }

    @Override
    public List<MetodoPagoResponseDTO> listarMetodos() {
        return metodoRepo.findAll().stream().map(this::toMetodoDTO).toList();
    }

    @Override
    public MetodoPagoResponseDTO crearMetodo(String nombre, String descripcion) {
        validarTexto(nombre, "nombre");
        validarDescripcion(descripcion);

        String nombreNormalizado = nombre.trim();

        metodoRepo.findByMetodoNombreIgnoreCase(nombreNormalizado)
                .ifPresent(existing -> {
                    throw new MetodoPagoDuplicadoException(nombre);
                });

        MetodoPago m = new MetodoPago();
        m.setMetodoNombre(nombreNormalizado);
        m.setMetodoDescripcion(descripcion);
        m.setMetodoActivo(true);
        return toMetodoDTO(metodoRepo.save(m));
    }

    @Override
    public MetodoPagoResponseDTO toggleMetodo(Long id) {
        MetodoPago m = metodoRepo.findById(id)
                .orElseThrow(() -> new MetodoPagoNotFoundException(id));
        m.setMetodoActivo(!Boolean.TRUE.equals(m.getMetodoActivo()));
        return toMetodoDTO(metodoRepo.save(m));
    }

    private MetodoPagoResponseDTO toMetodoDTO(MetodoPago m) {
        MetodoPagoResponseDTO dto = new MetodoPagoResponseDTO();
        dto.setId(m.getId());
        dto.setMetodoNombre(m.getMetodoNombre());
        dto.setMetodoDescripcion(m.getMetodoDescripcion());
        dto.setMetodoActivo(m.getMetodoActivo());
        return dto;
    }

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank() || !valor.matches(PATRON_CONTIENE_LETRA)) {
            throw new IllegalArgumentException("El " + campo + " debe contener texto válido");
        }
    }

    private void validarDescripcion(String descripcion) {
        if (descripcion != null && !descripcion.isBlank() && !descripcion.matches(PATRON_CONTIENE_LETRA)) {
            throw new IllegalArgumentException("La descripción debe contener texto válido");
        }
    }
}
