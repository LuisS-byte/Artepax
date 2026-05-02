package com.catolica.Artepax.common.controller;

import com.catolica.Artepax.common.dto.AuditoriaResponseDTO;
import com.catolica.Artepax.common.entity.Auditoria;
import com.catolica.Artepax.common.mapper.AuditoriaMapper;
import com.catolica.Artepax.common.repository.AuditoriaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;
    private final AuditoriaMapper auditoriaMapper;

    public AuditoriaController(AuditoriaRepository auditoriaRepository,
                               AuditoriaMapper auditoriaMapper) { // P7
        this.auditoriaRepository = auditoriaRepository;
        this.auditoriaMapper = auditoriaMapper;
    }
    // Reemplazar el endpoint obtenerTodo()
    @GetMapping
    public ResponseEntity<List<AuditoriaResponseDTO>> obtenerTodo(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanio) {

        // Máximo 100 registros por llamada para evitar OOM
        int tamanioSeguro = Math.min(tamanio, 100);

        Pageable pageable = PageRequest.of(pagina, tamanioSeguro,
                Sort.by("fecha").descending());

        return ResponseEntity.ok(
                auditoriaRepository.findAll(pageable)
                        .stream()
                        .map(auditoriaMapper::toDTO)
                        .toList()
        );
    }

    @GetMapping("/tabla/{tabla}")
    public ResponseEntity<List<AuditoriaResponseDTO>> porTabla(@PathVariable String tabla) {
        return ResponseEntity.ok(
                auditoriaRepository.findByTablaAfectadaOrderByFechaDesc(tabla)
                        .stream()
                        .map(auditoriaMapper::toDTO)
                        .toList()
        );
    }

    @GetMapping("/tabla/{tabla}/registro/{id}")
    public ResponseEntity<List<AuditoriaResponseDTO>> porRegistro(
            @PathVariable String tabla,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                auditoriaRepository.findByIdRegistroAndTablaAfectadaOrderByFechaDesc(id, tabla)
                        .stream()
                        .map(auditoriaMapper::toDTO)
                        .toList()
        );
    }

}