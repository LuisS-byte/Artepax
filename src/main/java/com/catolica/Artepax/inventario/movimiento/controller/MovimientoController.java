package com.catolica.Artepax.inventario.movimiento.controller;

import com.catolica.Artepax.inventario.movimiento.dto.MovimientoRequestDTO;
import com.catolica.Artepax.inventario.movimiento.dto.MovimientoResponseDTO;
import com.catolica.Artepax.inventario.movimiento.service.MovimientoService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventario")
@PreAuthorize("hasRole('ADMIN')")
public class MovimientoController {

    private final MovimientoService movimientoService;

    public MovimientoController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    // ✅ Entrada de inventario (suma stock)
    @PostMapping("/entrada")
    public ResponseEntity<String> entrada(@RequestBody MovimientoRequestDTO dto) {
        movimientoService.registrarEntrada(dto);
        return ResponseEntity.ok("Entrada registrada correctamente");
    }

    // ✅ Salida de inventario (resta stock)
    @PostMapping("/salida")
    public ResponseEntity<String> salida(@RequestBody MovimientoRequestDTO dto) {
        movimientoService.registrarSalida(dto);
        return ResponseEntity.ok("Salida registrada correctamente");
    }

    // ✅ Consultar stock actual
    @GetMapping("/productos/{id}/stock")
    public ResponseEntity<Integer> obtenerStock(@PathVariable Long id) {
        Integer stock = movimientoService.obtenerStock(id);
        return ResponseEntity.ok(stock);
    }

    // GET /inventario/productos/{id}/movimientos
    @GetMapping("/productos/{id}/movimientos")
    public ResponseEntity<List<MovimientoResponseDTO>> obtenerMovimientos(
            @PathVariable Long id) {

        List<MovimientoResponseDTO> movimientos =
                movimientoService.obtenerMovimientosPorProducto(id);
        return ResponseEntity.ok(movimientos);
    }
}