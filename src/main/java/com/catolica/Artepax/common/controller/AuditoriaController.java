package com.catolica.Artepax.common.controller;

import com.catolica.Artepax.common.dto.AuditoriaResponseDTO;
import com.catolica.Artepax.common.mapper.AuditoriaMapper;
import com.catolica.Artepax.common.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller para consulta de registros de auditoría.
 */
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;
    private final AuditoriaMapper auditoriaMapper;

    @GetMapping
    public ResponseEntity<List<AuditoriaResponseDTO>> obtenerTodo(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanio) {

        Pageable pageable = construirPageable(pagina, tamanio);

        return ResponseEntity.ok(
                auditoriaRepository.findAll(pageable)
                        .stream()
                        .map(auditoriaMapper::toDTO)
                        .toList()
        );
    }

    @GetMapping("/tabla/{tabla}")
    public ResponseEntity<List<AuditoriaResponseDTO>> porTabla(@PathVariable String tabla) {
        log.debug("Consultando auditoría para tabla {}", tabla);
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
        log.debug("Consultando auditoría para tabla={} registro={}", tabla, id);
        return ResponseEntity.ok(
                auditoriaRepository.findByIdRegistroAndTablaAfectadaOrderByFechaDesc(id, tabla)
                        .stream()
                        .map(auditoriaMapper::toDTO)
                        .toList()
        );
    }

    private Pageable construirPageable(int pagina, int tamanio) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanioSeguro = Math.min(Math.max(tamanio, 1), 100);

        return PageRequest.of(
                paginaSegura,
                tamanioSeguro,
                Sort.by(Sort.Direction.DESC, "fecha")
        );
    }
}
