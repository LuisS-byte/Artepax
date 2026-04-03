package com.catolica.Artepax.clientes.mapper;

import com.catolica.Artepax.clientes.dto.ClienteRequestDTO;
import com.catolica.Artepax.clientes.dto.ClienteResponseDTO;
import com.catolica.Artepax.clientes.entity.Cliente;

import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    // 🔹 DTO → Entity (crear cliente)
    public Cliente toEntity(ClienteRequestDTO dto) {
        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getNombre());
        cliente.setTelefono(dto.getTelefono());
        cliente.setCorreo(dto.getCorreo());
        cliente.setDireccion(dto.getDireccion());
        return cliente;
    }

    // 🔹 Entity → DTO (respuesta)
    public ClienteResponseDTO toDTO(Cliente cliente) {
        ClienteResponseDTO dto = new ClienteResponseDTO();
        dto.setId(cliente.getId());
        dto.setNombre(cliente.getNombre());
        dto.setTelefono(cliente.getTelefono());
        dto.setCorreo(cliente.getCorreo());
        dto.setDireccion(cliente.getDireccion());
        return dto;
    }
}