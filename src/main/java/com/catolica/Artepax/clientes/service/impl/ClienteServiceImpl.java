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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link ClienteService}.
 * <p>
 * Handles all cliente business logic: creation with email uniqueness validation
 * and text normalisation, full updates with duplicate-email detection when the
 * email changes, soft-delete (deactivation), reactivation, and event history lookup.
 * Read operations filter out inactive clientes unless explicitly requested via
 * {@link #obtenerInactivos()}.
 * </p>
 */
@Service
public class ClienteServiceImpl implements ClienteService {

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
    @Transactional
    public ClienteResponseDTO crearCliente(ClienteRequestDTO dto) {
        String correoNormalizado = normalizarCorreo(dto.getCorreo());

        if (clienteRepository.existsByCorreoIgnoreCase(correoNormalizado)) {
            throw new IllegalArgumentException("El correo ya está en uso");
        }

        Cliente cliente = clienteMapper.toEntity(dto);
        cliente.setNombre(trimRequired(cliente.getNombre()));
        cliente.setTelefono(trimRequired(cliente.getTelefono()));
        cliente.setCorreo(correoNormalizado);
        cliente.setDireccion(trimToNull(cliente.getDireccion()));

        return clienteMapper.toDTO(clienteRepository.save(cliente));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> obtenerTodos() {
        return clienteRepository.findByActivoTrueOrderByNombreAsc()
                .stream().map(clienteMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> obtenerInactivos() {
        return clienteRepository.findByActivoFalseOrderByNombreAsc()
                .stream().map(clienteMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerPorId(Long id) {
        return clienteMapper.toDTO(clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id)));
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizar(Long id, ClienteUpdateDTO dto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));

        String correoNormalizado = normalizarCorreo(dto.getCorreo());
        String correoActual = normalizarCorreo(cliente.getCorreo());

        if (!correoNormalizado.equals(correoActual)
                && clienteRepository.existsByCorreoIgnoreCase(correoNormalizado)) {
            throw new IllegalArgumentException("El correo ya está en uso");
        }

        cliente.setNombre(trimRequired(dto.getNombre()));
        cliente.setTelefono(trimRequired(dto.getTelefono()));
        cliente.setCorreo(correoNormalizado);
        cliente.setDireccion(trimToNull(dto.getDireccion()));

        return clienteMapper.toDTO(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDTO reactivar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));
        cliente.setActivo(true);
        return clienteMapper.toDTO(clienteRepository.save(cliente));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscarPorNombre(String nombre) {
        return clienteRepository.findByNombreContainingIgnoreCaseAndActivoTrueOrderByNombreAsc(nombre)
                .stream().map(clienteMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoResponseDTO> obtenerEventosPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ClienteNotFoundException(clienteId);
        }
        return eventoRepository.findByClienteIdWithRelations(clienteId)
                .stream().map(eventoMapper::toResponseDTO).toList();
    }

    // -------------------------------------------------------------------------
    // HELPERS DE NORMALIZACIÓN
    // -------------------------------------------------------------------------

    /**
     * Trims the given string, returning {@code null} if the input is {@code null}.
     *
     * @param value the string to trim, may be null
     * @return the trimmed string, or {@code null} if input was null
     */
    private String trimRequired(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Trims the given string, returning {@code null} if the input is blank or null.
     *
     * @param value the string to trim, may be null or blank
     * @return the trimmed string, or {@code null} if input was null or blank
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Normalises an email address by trimming whitespace and converting to lowercase.
     *
     * @param correo the email to normalise, may be null or blank
     * @return the normalised email, or {@code null} if input was null or blank
     */
    private String normalizarCorreo(String correo) {
        return correo == null || correo.isBlank()
                ? null
                : correo.trim().toLowerCase();
    }
}
