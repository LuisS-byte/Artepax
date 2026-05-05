package com.catolica.Artepax.evento.service.impl;

import com.catolica.Artepax.evento.dto.TipoEventoResponseDTO;
import com.catolica.Artepax.evento.exception.TipoEventoException;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.evento.repository.TipoEventoRepository;
import com.catolica.Artepax.evento.service.TipoEventoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación del servicio de tipos de evento.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TipoEventoServiceImpl implements TipoEventoService {

    private final TipoEventoRepository repo;

    @Override
    public List<TipoEventoResponseDTO> listar() {
        return repo.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public TipoEventoResponseDTO crear(String nombre, String descripcion) {
        TipoEvento t = new TipoEvento();
        t.setTipoNombre(nombre);
        t.setTipoDescripcion(descripcion);
        t.setTipoActivo(true);
        TipoEvento saved = repo.save(t);
        log.info("Tipo de evento creado: id={}, nombre={}", saved.getId(), nombre);
        return toDTO(saved);
    }

    @Override
    public TipoEventoResponseDTO toggle(Long id) {
        TipoEvento t = repo.findById(id)
                .orElseThrow(() -> new TipoEventoException("Tipo de evento no encontrado: " + id));
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
}
