package com.catolica.Artepax.clientes.service.impl;

import com.catolica.Artepax.clientes.dto.ClienteRequestDTO;
import com.catolica.Artepax.clientes.dto.ClienteResponseDTO;
import com.catolica.Artepax.clientes.dto.ClienteUpdateDTO;
import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.clientes.exception.ClienteNotFoundException;
import com.catolica.Artepax.clientes.mapper.ClienteMapper;
import com.catolica.Artepax.clientes.repository.ClienteRepository;
import com.catolica.Artepax.clientes.service.ClienteService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    public ClienteServiceImpl(ClienteRepository clienteRepository, ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }

    // ✅ Crear cliente
    @Override
    public ClienteResponseDTO crearCliente(ClienteRequestDTO dto) {

        // 🔥 Validar correo único
        if (clienteRepository.existsByCorreo(dto.getCorreo())) {
            throw new RuntimeException("El correo ya está en uso");
        }

        Cliente cliente = clienteMapper.toEntity(dto);
        Cliente guardado = clienteRepository.save(cliente);

        return clienteMapper.toDTO(guardado);
    }

    // ✅ Obtener todos
    @Override
    public List<ClienteResponseDTO> obtenerTodos() {
        return clienteRepository.findAll()
                .stream()
                .map(clienteMapper::toDTO)
                .toList();
    }

    // ✅ Obtener por ID
    @Override
    public ClienteResponseDTO obtenerPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));

        return clienteMapper.toDTO(cliente);
    }

    // ✅ Actualizar
    @Override
    public ClienteResponseDTO actualizar(Long id, ClienteUpdateDTO dto) {

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));

        if (dto.getCorreo() != null &&
                clienteRepository.existsByCorreo(dto.getCorreo()) &&
                !cliente.getCorreo().equals(dto.getCorreo())) {

            throw new RuntimeException("El correo ya está en uso");
        }

        // 🔥 actualizar campos
        cliente.setNombre(dto.getNombre());
        cliente.setTelefono(dto.getTelefono());
        cliente.setDireccion(dto.getDireccion());
        cliente.setCorreo(dto.getCorreo());

        Cliente actualizado = clienteRepository.save(cliente);

        return clienteMapper.toDTO(actualizado);
    }

    // ✅ Eliminar
    @Override
    public void eliminar(Long id) {

        if (!clienteRepository.existsById(id)) {
            throw new ClienteNotFoundException(id);
        }

        clienteRepository.deleteById(id);
    }
}