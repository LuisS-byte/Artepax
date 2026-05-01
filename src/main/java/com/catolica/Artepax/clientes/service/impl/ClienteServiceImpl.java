package com.catolica.Artepax.clientes.service.impl;

import com.catolica.Artepax.clientes.dto.ClienteRequestDTO;
import com.catolica.Artepax.clientes.dto.ClienteResponseDTO;
import com.catolica.Artepax.clientes.dto.ClienteUpdateDTO;
import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.clientes.exception.ClienteNotFoundException;
import com.catolica.Artepax.clientes.mapper.ClienteMapper;
import com.catolica.Artepax.clientes.repository.ClienteRepository;
import com.catolica.Artepax.clientes.service.ClienteService;

import com.catolica.Artepax.evento.dto.EventoResponseDTO;
import com.catolica.Artepax.evento.mapper.EventoMapper;
import com.catolica.Artepax.evento.repository.EventoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteServiceImpl  implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final EventoRepository eventoRepository;
    private final EventoMapper eventoMapper;

    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              ClienteMapper clienteMapper,
                              EventoRepository eventoRepository,
                              EventoMapper eventoMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.eventoRepository = eventoRepository;
        this.eventoMapper = eventoMapper;
    }

    @Override
    public ClienteResponseDTO crearCliente(ClienteRequestDTO dto) {
        if (clienteRepository.existsByCorreo(dto.getCorreo())) {
            throw new IllegalArgumentException("El correo ya está en uso");
        }
        return clienteMapper.toDTO(clienteRepository.save(clienteMapper.toEntity(dto)));
    }

    @Override
    public List<ClienteResponseDTO> obtenerTodos() {
        // Solo retorna activos
        return clienteRepository.findByActivoTrue()
                .stream().map(clienteMapper::toDTO).toList();
    }

    @Override
    public List<ClienteResponseDTO> obtenerInactivos() {
        return clienteRepository.findByActivoFalse()
                .stream().map(clienteMapper::toDTO).toList();
    }

    @Override
    public ClienteResponseDTO obtenerPorId(Long id) {
        return clienteMapper.toDTO(clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id)));
    }

    @Override
    public ClienteResponseDTO actualizar(Long id, ClienteUpdateDTO dto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));

        if (dto.getCorreo() != null
                && !dto.getCorreo().equals(cliente.getCorreo())
                && clienteRepository.existsByCorreo(dto.getCorreo())) {
            throw new IllegalArgumentException("El correo ya está en uso");
        }

        cliente.setNombre(dto.getNombre());
        cliente.setTelefono(dto.getTelefono());
        cliente.setDireccion(dto.getDireccion());
        cliente.setCorreo(dto.getCorreo());

        return clienteMapper.toDTO(clienteRepository.save(cliente));
    }

    @Override
    public void eliminar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));
        // Soft delete — nunca DELETE físico
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    @Override
    public ClienteResponseDTO reactivar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));
        cliente.setActivo(true);
        return clienteMapper.toDTO(clienteRepository.save(cliente));
    }

    @Override
    public List<ClienteResponseDTO> buscarPorNombre(String nombre) {
        return clienteRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre)
                .stream().map(clienteMapper::toDTO).toList();
    }

    @Override
    public List<EventoResponseDTO> obtenerEventosPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ClienteNotFoundException(clienteId);
        }
        return eventoRepository.findByClienteId(clienteId)
                .stream().map(eventoMapper::toResponseDTO).toList();
    }
}