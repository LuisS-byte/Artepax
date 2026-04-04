package com.catolica.Artepax.evento.service.impl;

import com.catolica.Artepax.evento.dto.EventoRequestDTO;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;
import com.catolica.Artepax.evento.dto.EventoUpdateDTO;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.mapper.EventoMapper;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.evento.model.EventoProducto;
import com.catolica.Artepax.evento.model.EventoProductoId;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.repository.EstadoEventoRepository;
import com.catolica.Artepax.evento.repository.EventoProductoRepository;
import com.catolica.Artepax.evento.service.EventoService;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.clientes.repository.ClienteRepository;

import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionRepository;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionProductoRepository;

import com.catolica.Artepax.inventario.movimiento.entity.MovimientoInventario;
import com.catolica.Artepax.inventario.movimiento.repository.MovimientoInventarioRepository;

import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.exception.StockInsuficienteException;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;

import com.catolica.Artepax.pago.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventoServiceImpl implements EventoService {

    private final EventoRepository eventoRepository;
    private final ClienteRepository clienteRepository;
    private final DecoracionRepository decoracionRepository;
    private final DecoracionProductoRepository decoracionProductoRepository;
    private final EstadoEventoRepository estadoEventoRepository;
    private final EventoProductoRepository eventoProductoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final EventoMapper eventoMapper;
    private final PagoService pagoService;

    @Override
    @Transactional
    public EventoResponseDTO crearEvento(EventoRequestDTO dto) {
        // validar cliente
        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new EventoException("Cliente no encontrado"));

        // validar decoracion
        Decoracion decoracion = decoracionRepository.findById(dto.getDecoracionId())
                .orElseThrow(() -> new EventoException("Decoracion no encontrada"));

        // validar fecha
        if (dto.getFecha().isBefore(LocalDate.now())) {
            throw new EventoException("No se puede crear un evento en el pasado");
        }

        // validar conflicto de horario
        boolean existe = eventoRepository.existsByFechaAndHoraInicioAndUbicacion(
                dto.getFecha(), dto.getHoraInicio(), dto.getUbicacion()
        );
        if (existe) {
            throw new EventoException("Ya existe un evento en esa fecha, hora y ubicación");
        }

        // obtener productos requeridos por la decoracion
        List<DecoracionProducto> productosRequeridos = decoracionProductoRepository
                .findByDecoracionId(decoracion.getId());

        // validar stock de cada producto ANTES de guardar nada
        for (DecoracionProducto dp : productosRequeridos) {
            Producto producto = dp.getProducto();
            if (producto.getCantidadTotal() < dp.getCantidadRequerida()) {
                throw new StockInsuficienteException(
                        producto.getNombre(),
                        producto.getCantidadTotal(),
                        dp.getCantidadRequerida()
                );
            }
        }

        // buscar estado PROGRAMADO
        EstadoEvento estadoProgramado = estadoEventoRepository.findByNombre("PROGRAMADO")
                .orElseThrow(() -> new EventoException("Estado PROGRAMADO no encontrado en BD"));

        // crear y guardar evento
        Evento evento = eventoMapper.toEntity(dto, cliente, decoracion);
        evento.setEstadoEvento(estadoProgramado);
        Evento guardado = eventoRepository.save(evento);

        // reservar productos y generar movimientos de SALIDA
        for (DecoracionProducto dp : productosRequeridos) {
            Producto producto = dp.getProducto();

            // registrar reserva en evento_producto
            EventoProducto eventoProducto = new EventoProducto();
            eventoProducto.setId(new EventoProductoId(guardado.getId(), producto.getId()));
            eventoProducto.setEvento(guardado);
            eventoProducto.setProducto(producto);
            eventoProducto.setCantidadReservada(dp.getCantidadRequerida());
            eventoProductoRepository.save(eventoProducto);

            // descontar stock
            producto.setCantidadTotal(producto.getCantidadTotal() - dp.getCantidadRequerida());
            productoRepository.save(producto);

            // registrar movimiento de SALIDA
            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setProducto(producto);
            movimiento.setTipoMovimiento("SALIDA");
            movimiento.setCantidad(dp.getCantidadRequerida());
            movimiento.setMotivo("Reserva para evento ID: " + guardado.getId());
            movimientoRepository.save(movimiento);
        }

        // al final de crearEvento, después de guardar los movimientos:
        pagoService.generarPagoAutomatico(guardado);
        return eventoMapper.toResponseDTO(guardado);
    }

    @Override
    public List<EventoResponseDTO> obtenerEventos() {
        return eventoRepository.findAll()
                .stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EventoResponseDTO obtenerEventoPorId(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new EventoException("Evento no encontrado con ID: " + id));
        return eventoMapper.toResponseDTO(evento);
    }

    @Override
    @Transactional
    public EventoResponseDTO actualizarEvento(Long id, EventoUpdateDTO dto) {

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new EventoException("Evento no encontrado con ID: " + id));

        Cliente cliente = null;
        if (dto.getClienteId() != null) {
            cliente = clienteRepository.findById(dto.getClienteId())
                    .orElseThrow(() -> new EventoException("Cliente no encontrado"));
        }

        Decoracion decoracion = null;
        if (dto.getDecoracionId() != null) {
            decoracion = decoracionRepository.findById(dto.getDecoracionId())
                    .orElseThrow(() -> new EventoException("Decoracion no encontrada"));
        }

        if (dto.getFecha() != null && dto.getFecha().isBefore(LocalDate.now())) {
            throw new EventoException("No se puede actualizar a una fecha pasada");
        }

        if (dto.getFecha() != null || dto.getHoraInicio() != null || dto.getUbicacion() != null) {
            LocalDate fecha = dto.getFecha() != null ? dto.getFecha() : evento.getFecha();
            var horaInicio = dto.getHoraInicio() != null ? dto.getHoraInicio() : evento.getHoraInicio();
            var ubicacion = dto.getUbicacion() != null ? dto.getUbicacion() : evento.getUbicacion();

            boolean existeConflicto = eventoRepository.existsByFechaAndHoraInicioAndUbicacion(
                    fecha, horaInicio, ubicacion
            );

            if (existeConflicto && !(evento.getFecha().equals(fecha)
                    && evento.getHoraInicio().equals(horaInicio)
                    && evento.getUbicacion().equals(ubicacion))) {
                throw new EventoException("Conflicto con otro evento existente");
            }
        }

        eventoMapper.updateEntityFromDTO(evento, dto, cliente, decoracion);
        Evento actualizado = eventoRepository.save(evento);
        return eventoMapper.toResponseDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminarEvento(Long id) {

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new EventoException("Evento no encontrado con ID: " + id));

        // solo devolver stock si el evento NO ha finalizado
        String estadoActual = evento.getEstadoEvento().getNombre();

        if (!estadoActual.equals("FINALIZADO") && !estadoActual.equals("CANCELADO")) {

            List<EventoProducto> reservas = eventoProductoRepository.findByEventoId(id);

            for (EventoProducto reserva : reservas) {
                Producto producto = reserva.getProducto();

                producto.setCantidadTotal(
                        producto.getCantidadTotal() + reserva.getCantidadReservada()
                );
                productoRepository.save(producto);

                MovimientoInventario movimiento = new MovimientoInventario();
                movimiento.setProducto(producto);
                movimiento.setTipoMovimiento("ENTRADA");
                movimiento.setCantidad(reserva.getCantidadReservada());
                movimiento.setMotivo("Devolución por cancelación de evento ID: " + id);
                movimientoRepository.save(movimiento);
            }
        }

        EstadoEvento estadoCancelado = estadoEventoRepository.findByNombre("CANCELADO")
                .orElseThrow(() -> new EventoException("Estado CANCELADO no encontrado en BD"));

        evento.setEstadoEvento(estadoCancelado);
        evento.setMotivoCancelacion("Evento cancelado");
        eventoRepository.save(evento);
    }

    @Override
    @Transactional
    public void cancelarEvento(Long id, String motivo) {

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new EventoException("Evento no encontrado con ID: " + id));

        // solo se puede cancelar si está PROGRAMADO
        String estadoActual = evento.getEstadoEvento().getNombre();
        if (!estadoActual.equals("PROGRAMADO")) {
            throw new EventoException("Solo se pueden cancelar eventos en estado PROGRAMADO");
        }

        // devolver stock porque no se ha realizado el evento
        List<EventoProducto> reservas = eventoProductoRepository.findByEventoId(id);

        for (EventoProducto reserva : reservas) {
            Producto producto = reserva.getProducto();

            producto.setCantidadTotal(
                    producto.getCantidadTotal() + reserva.getCantidadReservada()
            );
            productoRepository.save(producto);

            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setProducto(producto);
            movimiento.setTipoMovimiento("ENTRADA");
            movimiento.setCantidad(reserva.getCantidadReservada());
            movimiento.setMotivo("Devolución por cancelación de evento ID: " + id);
            movimientoRepository.save(movimiento);
        }

        EstadoEvento estadoCancelado = estadoEventoRepository.findByNombre("CANCELADO")
                .orElseThrow(() -> new EventoException("Estado CANCELADO no encontrado en BD"));

        evento.setEstadoEvento(estadoCancelado);
        evento.setMotivoCancelacion(motivo);
        eventoRepository.save(evento);
    }

    @Override
    @Transactional
    public void finalizarEvento(Long id) {

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new EventoException("Evento no encontrado con ID: " + id));

        // solo se puede finalizar si está PROGRAMADO
        String estadoActual = evento.getEstadoEvento().getNombre();
        if (!estadoActual.equals("PROGRAMADO")) {
            throw new EventoException("Solo se pueden finalizar eventos en estado PROGRAMADO");
        }

        // no se devuelve stock porque el evento ya ocurrió
        EstadoEvento estadoFinalizado = estadoEventoRepository.findByNombre("FINALIZADO")
                .orElseThrow(() -> new EventoException("Estado FINALIZADO no encontrado en BD"));

        evento.setEstadoEvento(estadoFinalizado);
        eventoRepository.save(evento);
    }

    @Override
    public boolean verificarDisponibilidad(LocalDate fecha, LocalTime horaInicio, String ubicacion) {
        return !eventoRepository.existsByFechaAndHoraInicioAndUbicacion(
                fecha, horaInicio, ubicacion
        );
    }

    @Override
    public List<EventoResponseDTO> obtenerCalendario(LocalDate inicio, LocalDate fin) {
        if (inicio.isAfter(fin)) {
            throw new EventoException("La fecha de inicio no puede ser mayor a la fecha fin");
        }
        return eventoRepository.findByFechaBetween(inicio, fin)
                .stream()
                .map(eventoMapper::toResponseDTO)
                .toList();
    }
}