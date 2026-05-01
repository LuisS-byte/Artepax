package com.catolica.Artepax.inventario.producto.service;

import com.catolica.Artepax.inventario.producto.dto.ProductoRequestDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoResponseDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoUpdateDTO;

import java.util.List;

public interface ProductoService {


    ProductoResponseDTO crearProducto(ProductoRequestDTO dto);
    List<ProductoResponseDTO> obtenerTodos();
    ProductoResponseDTO obtenerPorId(Long id);
    ProductoResponseDTO actualizar(Long id, ProductoUpdateDTO dto);
    void eliminar(Long id);
    ProductoResponseDTO reactivar(Long id); // ← cambiar void por ProductoResponseDTO
    List<ProductoResponseDTO> buscarPorNombre(String nombre);
}