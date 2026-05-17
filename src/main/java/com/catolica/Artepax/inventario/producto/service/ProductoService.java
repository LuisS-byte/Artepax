package com.catolica.Artepax.inventario.producto.service;

import com.catolica.Artepax.inventario.producto.dto.ProductoRequestDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoResponseDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoUpdateDTO;

import java.util.List;

/**
 * Service interface for {@link com.catolica.Artepax.inventario.producto.entity.Producto} operations.
 * Defines the contract for product CRUD, search, and state management.
 */
public interface ProductoService {

    /**
     * Creates a new product from the given request data.
     * Validates uniqueness by normalized name and normalizes the state field.
     *
     * @param dto the request data with product fields
     * @return the created product as a response DTO
     */
    ProductoResponseDTO crearProducto(ProductoRequestDTO dto);

    /**
     * Retrieves all products.
     *
     * @return a list of all products as response DTOs
     */
    List<ProductoResponseDTO> obtenerTodos();

    /**
     * Retrieves a single product by its ID.
     *
     * @param id the product ID
     * @return the product as a response DTO
     * @throws com.catolica.Artepax.inventario.producto.exception.ProductoNotFoundException if not found
     */
    ProductoResponseDTO obtenerPorId(Long id);

    /**
     * Updates an existing product with the given data.
     * Only non-null fields in the DTO are applied.
     *
     * @param id  the product ID to update
     * @param dto the data to apply
     * @return the updated product as a response DTO
     */
    ProductoResponseDTO actualizar(Long id, ProductoUpdateDTO dto);

    /**
     * Soft-deletes a product by setting its state to {@code INACTIVO}.
     *
     * @param id the product ID to deactivate
     */
    void eliminar(Long id);

    /**
     * Reactivates a previously deactivated product by setting its state to {@code ACTIVO}.
     *
     * @param id the product ID to reactivate
     * @return the reactivated product as a response DTO
     */
    ProductoResponseDTO reactivar(Long id);

    /**
     * Searches for products whose name contains the given string, ignoring case.
     *
     * @param nombre the search string
     * @return the list of matching products as response DTOs
     */
    List<ProductoResponseDTO> buscarPorNombre(String nombre);
}