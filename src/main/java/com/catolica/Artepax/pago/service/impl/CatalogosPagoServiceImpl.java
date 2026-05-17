package com.catolica.Artepax.pago.service.impl;

import com.catolica.Artepax.pago.dto.*;
import com.catolica.Artepax.pago.entity.*;
import com.catolica.Artepax.pago.exception.MetodoPagoDuplicadoException;
import com.catolica.Artepax.pago.exception.MetodoPagoNotFoundException;
import com.catolica.Artepax.pago.repository.*;
import com.catolica.Artepax.pago.service.CatalogosPagoService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of {@link CatalogosPagoService} providing CRUD operations
 * over the payment catalog tables: estados, conceptos, and metodos de pago.
 * <p>
 * All list operations are simple {@code findAll()} calls. Payment method
 * management includes duplicate detection (case-insensitive) and soft-delete
 * via toggle.
 */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetodoPagoResponseDTO> listarMetodos() {
        return metodoRepo.findAll().stream().map(this::toMetodoDTO).toList();
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public MetodoPagoResponseDTO toggleMetodo(Long id) {
        MetodoPago m = metodoRepo.findById(id)
                .orElseThrow(() -> new MetodoPagoNotFoundException(id));
        m.setMetodoActivo(!Boolean.TRUE.equals(m.getMetodoActivo()));
        return toMetodoDTO(metodoRepo.save(m));
    }

    /**
     * Convierte una entidad {@link MetodoPago} a su DTO de respuesta.
     *
     * @param m la entidad de método de pago
     * @return DTO con id, nombre, descripción y estado activo
     */
    private MetodoPagoResponseDTO toMetodoDTO(MetodoPago m) {
        MetodoPagoResponseDTO dto = new MetodoPagoResponseDTO();
        dto.setId(m.getId());
        dto.setMetodoNombre(m.getMetodoNombre());
        dto.setMetodoDescripcion(m.getMetodoDescripcion());
        dto.setMetodoActivo(m.getMetodoActivo());
        return dto;
    }

    /**
     * Valida que un campo de texto no sea nulo, no esté vacío y contenga al menos una letra.
     *
     * @param valor el valor a validar
     * @param campo nombre del campo para el mensaje de error
     * @throws IllegalArgumentException si la validación falla
     */
    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank() || !valor.matches(PATRON_CONTIENE_LETRA)) {
            throw new IllegalArgumentException("El " + campo + " debe contener texto válido");
        }
    }

    /**
     * Valida que una descripción opcional contenga texto válido si no es nula o vacía.
     *
     * @param descripcion la descripción a validar (puede ser {@code null})
     * @throws IllegalArgumentException si la descripción contiene solo caracteres no alfabéticos
     */
    private void validarDescripcion(String descripcion) {
        if (descripcion != null && !descripcion.isBlank() && !descripcion.matches(PATRON_CONTIENE_LETRA)) {
            throw new IllegalArgumentException("La descripción debe contener texto válido");
        }
    }
}
