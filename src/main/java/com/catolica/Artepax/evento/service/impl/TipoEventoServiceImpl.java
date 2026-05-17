package com.catolica.Artepax.evento.service.impl;

import com.catolica.Artepax.evento.dto.TipoEventoResponseDTO;
import com.catolica.Artepax.evento.exception.TipoEventoException;
import com.catolica.Artepax.evento.exception.TipoEventoNotFoundException;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.evento.repository.TipoEventoRepository;
import com.catolica.Artepax.evento.service.TipoEventoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio de tipos de evento.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TipoEventoServiceImpl implements TipoEventoService {

    private static final String PATRON_CONTIENE_LETRA = ".*[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ].*";

    private final TipoEventoRepository repo;

    @Override
    @Transactional(readOnly = true)
    public List<TipoEventoResponseDTO> listar() {
        return repo.findAllByOrderByTipoNombreAsc().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public TipoEventoResponseDTO crear(String nombre, String descripcion) {
        String nombreNormalizado = nombre.trim();
        String descripcionNormalizada = (descripcion == null || descripcion.isBlank())
                ? null
                : descripcion.trim();

        validarTexto(nombreNormalizado, "nombre");
        validarDescripcion(descripcionNormalizada);

        if (repo.existsByTipoNombreIgnoreCase(nombreNormalizado)) {
            throw new TipoEventoException("Ya existe un tipo de evento con el nombre: " + nombreNormalizado);
        }

        TipoEvento t = new TipoEvento();
        t.setTipoNombre(nombreNormalizado);
        t.setTipoDescripcion(descripcionNormalizada);
        t.setTipoActivo(true);
        TipoEvento saved = repo.save(t);
        log.info("Tipo de evento creado: id={}, nombre={}", saved.getId(), nombreNormalizado);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public TipoEventoResponseDTO toggle(Long id) {
        TipoEvento t = repo.findById(id)
                .orElseThrow(() -> new TipoEventoNotFoundException(id));
        t.setTipoActivo(!t.getTipoActivo());
        TipoEvento saved = repo.save(t);
        log.info("Tipo de evento toggleado: id={}, activo={}", id, saved.getTipoActivo());
        return toDTO(saved);
    }

    private TipoEventoResponseDTO toDTO(TipoEvento t) {
        TipoEventoResponseDTO dto = new TipoEventoResponseDTO();
        dto.setId(t.getId());
        dto.setTipoNombre(t.getTipoNombre());
        dto.setTipoDescripcion(t.getTipoDescripcion());
        dto.setTipoActivo(t.getTipoActivo());
        return dto;
    }

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank() || !valor.matches(PATRON_CONTIENE_LETRA)) {
            throw new TipoEventoException("El " + campo + " debe contener texto válido");
        }
    }

    private void validarDescripcion(String descripcion) {
        if (descripcion != null && !descripcion.isBlank() && !descripcion.matches(PATRON_CONTIENE_LETRA)) {
            throw new TipoEventoException("La descripción debe contener texto válido");
        }
    }
}
