package com.catolica.Artepax.inventario.producto.controller;

import com.catolica.Artepax.inventario.producto.dto.ProductoRequestDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoResponseDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoUpdateDTO;
import com.catolica.Artepax.inventario.producto.service.ProductoService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@PreAuthorize("hasRole('ADMIN')")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // ✅ Crear producto
    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crearProducto(
            @Valid @RequestBody ProductoRequestDTO requestDTO) {

        ProductoResponseDTO producto = productoService.crearProducto(requestDTO);
        return ResponseEntity.status(201).body(producto);
    }

    // ✅ Obtener todos los productos
    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> obtenerTodos() {
        List<ProductoResponseDTO> productos = productoService.obtenerTodos();
        return ResponseEntity.ok(productos);
    }

    // ✅ Obtener producto por ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerPorId(
            @PathVariable Long id) {

        ProductoResponseDTO producto = productoService.obtenerPorId(id);
        return ResponseEntity.ok(producto);
    }

    // ✅ Actualizar producto
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoUpdateDTO updateDTO) {

        ProductoResponseDTO actualizado = productoService.actualizar(id, updateDTO);
        return ResponseEntity.ok(actualizado);
    }


    // GET /productos/buscar?nombre=globo
    @GetMapping("/buscar")
    public ResponseEntity<List<ProductoResponseDTO>> buscarPorNombre(
            @RequestParam String nombre) {

        List<ProductoResponseDTO> productos = productoService.buscarPorNombre(nombre);
        return ResponseEntity.ok(productos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<ProductoResponseDTO> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.reactivar(id));
}
}