package com.catolica.Artepax.documento.controller;


import com.catolica.Artepax.documento.dto.DocumentoResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaUpdateDTO;
import com.catolica.Artepax.documento.service.DocumentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DocumentoController {

    private final DocumentoService documentoService;

    // -------------------------------------------------------------------------
    // DOCUMENTOS
    // -------------------------------------------------------------------------

    /**
     * POST /api/documentos/evento/{idEvento}/generar?tipo=CONFIRMACION_RESERVA
     * Genera un documento para el evento usando la plantilla del tipo indicado.
     */
    @PostMapping("/evento/{idEvento}/generar")
    public ResponseEntity<DocumentoResponseDTO> generarDocumento(
            @PathVariable Long idEvento,
            @RequestParam String tipo) {
        return ResponseEntity.ok(documentoService.generarDocumento(idEvento, tipo));
    }

    /**
     * GET /api/documentos/evento/{idEvento}
     * Lista todos los documentos generados para un evento.
     */
    @GetMapping("/evento/{idEvento}")
    public ResponseEntity<List<DocumentoResponseDTO>> listarPorEvento(
            @PathVariable Long idEvento) {
        return ResponseEntity.ok(documentoService.listarPorEvento(idEvento));
    }

    /**
     * GET /api/documentos/{id}
     * Obtiene un documento por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentoResponseDTO> obtenerDocumento(
            @PathVariable Long id) {
        return ResponseEntity.ok(documentoService.obtenerDocumento(id));
    }

    /**
     * POST /api/documentos/{id}/reenviar
     * Reenvía el documento al correo del cliente.
     */
    @PostMapping("/{id}/reenviar")
    public ResponseEntity<DocumentoResponseDTO> reenviarDocumento(
            @PathVariable Long id) {
        return ResponseEntity.ok(documentoService.reenviarDocumento(id));
    }

    // -------------------------------------------------------------------------
    // PLANTILLAS
    // -------------------------------------------------------------------------

    /**
     * GET /api/documentos/plantillas
     * Lista todas las plantillas disponibles.
     */
    @GetMapping("/plantillas")
    public ResponseEntity<List<PlantillaResponseDTO>> listarPlantillas() {
        return ResponseEntity.ok(documentoService.listarPlantillas());
    }

    /**
     * GET /api/documentos/plantillas/{id}
     * Obtiene una plantilla por ID.
     */
    @GetMapping("/plantillas/{id}")
    public ResponseEntity<PlantillaResponseDTO> obtenerPlantilla(
            @PathVariable Long id) {
        return ResponseEntity.ok(documentoService.obtenerPlantilla(id));
    }

    /**
     * PUT /api/documentos/plantillas/{id}
     * Actualiza el contenido HTML de una plantilla.
     */
    @PutMapping("/plantillas/{id}")
    public ResponseEntity<PlantillaResponseDTO> actualizarPlantilla(
            @PathVariable Long id,
            @Valid @RequestBody PlantillaUpdateDTO dto) {
        return ResponseEntity.ok(documentoService.actualizarPlantilla(id, dto));
    }

    @GetMapping
    public ResponseEntity<Page<DocumentoResponseDTO>> obtenerTodos(
            @PageableDefault(size = 50, sort = "docuFechaGeneracion") Pageable pageable) {
        return ResponseEntity.ok(documentoService.obtenerTodos(pageable));
    }
}
