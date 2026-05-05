package com.catolica.Artepax.inventario.movimiento.controller;

import com.catolica.Artepax.inventario.movimiento.dto.MovimientoRequestDTO;
import com.catolica.Artepax.inventario.movimiento.dto.MovimientoResponseDTO;
import com.catolica.Artepax.inventario.movimiento.service.MovimientoService;
import com.catolica.Artepax.inventario.producto.dto.StockProductoResponseDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventario")
@PreAuthorize("hasRole('ADMIN')")
public class MovimientoController {

    private final MovimientoService movimientoService;

    public MovimientoController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    // REPOSICION o AJUSTE_MANUAL
    @PostMapping("/entrada")
    public ResponseEntity<Void> entrada(@Valid @RequestBody MovimientoRequestDTO dto) {
        movimientoService.registrarEntrada(dto);
        return ResponseEntity.noContent().build();
    }

    // DAÑO, PERDIDA o AJUSTE_MANUAL
    @PostMapping("/salida")
    public ResponseEntity<Void> salida(@Valid @RequestBody MovimientoRequestDTO dto) {
        movimientoService.registrarSalida(dto);
        return ResponseEntity.noContent().build();
    }

    // Ajuste por conteo físico — calcula diferencia automáticamente
    @PostMapping("/ajuste/{productoId}")
    public ResponseEntity<Void> ajuste(
            @PathVariable Long productoId,
            @RequestBody Map<String, Object> body) {
        Integer cantidadFisica = ((Number) body.get("cantidadFisica")).intValue(); // P15: cast seguro
        String motivo = (String) body.getOrDefault("motivo", "Ajuste manual");
        movimientoService.registrarAjuste(productoId, cantidadFisica, motivo);
        return ResponseEntity.noContent().build();
    }

    // Stock real desde vista_stock_producto
    @GetMapping("/productos/{id}/stock")
    public ResponseEntity<StockProductoResponseDTO> obtenerStock(@PathVariable Long id) {
        return ResponseEntity.ok(movimientoService.obtenerStock(id));
    }

    // Historial de movimientos
    @GetMapping("/productos/{id}/movimientos")
    public ResponseEntity<List<MovimientoResponseDTO>> obtenerMovimientos(
            @PathVariable Long id) {
        return ResponseEntity.ok(movimientoService.obtenerMovimientosPorProducto(id));
    }

    @GetMapping("/movimientos")
    public ResponseEntity<Page<MovimientoResponseDTO>> obtenerTodosMovimientos(
            @PageableDefault(size = 50, sort = "fecha") Pageable pageable) {
        return ResponseEntity.ok(movimientoService.obtenerTodosMovimientos(pageable));
    }
}