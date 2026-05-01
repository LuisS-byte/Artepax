package com.catolica.Artepax.evento.service.impl;
import com.catolica.Artepax.evento.dto.TipoEventoResponseDTO;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.evento.repository.TipoEventoRepository;
import com.catolica.Artepax.evento.service.TipoEventoService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TipoEventoServiceImpl implements TipoEventoService {

    private final TipoEventoRepository repo;

    public TipoEventoServiceImpl(TipoEventoRepository repo) {
        this.repo = repo;
    }

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
        return toDTO(repo.save(t));
    }

    @Override
    public TipoEventoResponseDTO toggle(Long id) {
        TipoEvento t = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de evento no encontrado: " + id));
        t.setTipoActivo(!t.getTipoActivo());
        return toDTO(repo.save(t));
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