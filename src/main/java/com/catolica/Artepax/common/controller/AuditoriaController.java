package com.catolica.Artepax.common.controller;

import com.catolica.Artepax.common.dto.AuditoriaResponseDTO;
import com.catolica.Artepax.common.entity.Auditoria;
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

    public AuditoriaController(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
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
                        .map(this::toDTO)
                        .toList()
        );
    }

    @GetMapping("/tabla/{tabla}")
    public ResponseEntity<List<AuditoriaResponseDTO>> porTabla(@PathVariable String tabla) {
        return ResponseEntity.ok(
                auditoriaRepository.findByTablaAfectadaOrderByFechaDesc(tabla)
                        .stream()
                        .map(this::toDTO)
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
                        .map(this::toDTO)
                        .toList()
        );
    }

    private AuditoriaResponseDTO toDTO(Auditoria a) {
        AuditoriaResponseDTO dto = new AuditoriaResponseDTO();
        dto.setId(a.getId());
        dto.setUsuarioId(a.getUsuario() != null ? a.getUsuario().getIdUsuario() : null);
        dto.setAccion(a.getAccion());
        dto.setTablaAfectada(a.getTablaAfectada());
        dto.setIdRegistro(a.getIdRegistro());
        dto.setValorAnterior(a.getValorAnterior());
        dto.setValorNuevo(a.getValorNuevo());
        dto.setDescripcion(a.getDescripcion());
        dto.setFecha(a.getFecha());
        return dto;
    }
}