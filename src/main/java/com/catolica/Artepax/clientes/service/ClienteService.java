package com.catolica.Artepax.clientes.service;

import com.catolica.Artepax.clientes.dto.ClienteRequestDTO;
import com.catolica.Artepax.clientes.dto.ClienteResponseDTO;
import com.catolica.Artepax.clientes.dto.ClienteUpdateDTO;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;

import java.util.List;

public interface ClienteService {

    ClienteResponseDTO crearCliente(ClienteRequestDTO dto);

    List<ClienteResponseDTO> obtenerTodos();

    ClienteResponseDTO obtenerPorId(Long id);

    ClienteResponseDTO actualizar(Long id, ClienteUpdateDTO dto);

    void eliminar(Long id);


    List<EventoResponseDTO> obtenerEventosPorCliente(Long clienteId);

    List<ClienteResponseDTO> buscarPorNombre(String nombre);
}