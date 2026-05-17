package com.catolica.Artepax.clientes.mapper;

import com.catolica.Artepax.clientes.dto.ClienteRequestDTO;
import com.catolica.Artepax.clientes.dto.ClienteResponseDTO;
import com.catolica.Artepax.clientes.entity.Cliente;

import org.springframework.stereotype.Component;

/**
 * Mapper between {@link Cliente} entity and its DTOs.
 * <p>
 * Converts {@link ClienteRequestDTO} to a new {@link Cliente} entity for creation,
 * and {@link Cliente} entities to {@link ClienteResponseDTO} for API responses.
 * </p>
 */
@Component
public class ClienteMapper {

    /**
     * Converts a creation DTO into a new {@link Cliente} entity.
     * <p>
     * Only maps the basic fields (nombre, telefono, correo, direccion).
     * The caller is responsible for trimming, normalising, and persisting the entity.
     * </p>
     *
     * @param dto the creation request data
     * @return a new {@link Cliente} with fields populated from the DTO
     */
    public Cliente toEntity(ClienteRequestDTO dto) {
        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getNombre());
        cliente.setTelefono(dto.getTelefono());
        cliente.setCorreo(dto.getCorreo());
        cliente.setDireccion(dto.getDireccion());
        return cliente;
    }

    /**
     * Converts a {@link Cliente} entity into a response DTO.
     *
     * @param cliente the entity to convert
     * @return a {@link ClienteResponseDTO} with all fields including activo status
     */
    public ClienteResponseDTO toDTO(Cliente cliente) {
        ClienteResponseDTO dto = new ClienteResponseDTO();
        dto.setId(cliente.getId());
        dto.setNombre(cliente.getNombre());
        dto.setTelefono(cliente.getTelefono());
        dto.setCorreo(cliente.getCorreo());
        dto.setDireccion(cliente.getDireccion());
        dto.setActivo(cliente.getActivo());
        return dto;
    }
}