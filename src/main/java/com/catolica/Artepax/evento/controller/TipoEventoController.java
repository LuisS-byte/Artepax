package com.catolica.Artepax.evento.controller;

import com.catolica.Artepax.evento.dto.TipoEventoRequestDTO;
import com.catolica.Artepax.evento.dto.TipoEventoResponseDTO;
import com.catolica.Artepax.evento.service.TipoEventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller para gestión de tipos de evento.
 */
@RestController
@RequestMapping("/api/tipos-evento")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TipoEventoController {

    private static final Logger log = LoggerFactory.getLogger(TipoEventoController.class);

    private final TipoEventoService service;

    /**
     * Retorna todos los tipos de evento activos e inactivos.
     *
     * @return lista de tipos de evento
     */
    @GetMapping
    public List<TipoEventoResponseDTO> listar() {
        return service.listar();
    }

    /**
     * Crea un nuevo tipo de evento.
     *
     * @param dto datos del tipo de evento a crear
     * @return 201 con el tipo de evento creado
     */
    @PostMapping
    public ResponseEntity<TipoEventoResponseDTO> crear(@Valid @RequestBody TipoEventoRequestDTO dto) {
        log.info("POST /api/tipos-evento nombre={}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto.getNombre(), dto.getDescripcion()));
    }

    /**
     * Activa o desactiva un tipo de evento (toggle).
     *
     * @param id identificador del tipo de evento
     * @return tipo de evento con estado actualizado
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<TipoEventoResponseDTO> toggle(@PathVariable Long id) {
        log.info("PATCH /api/tipos-evento/{}/toggle", id);
        return ResponseEntity.ok(service.toggle(id));
    }
}
