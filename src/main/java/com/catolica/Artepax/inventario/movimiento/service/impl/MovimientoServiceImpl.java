package com.catolica.Artepax.inventario.movimiento.service.impl;

import com.catolica.Artepax.common.audit.ProductoAuditSnapshot;
import com.catolica.Artepax.common.service.AuditoriaService;
import com.catolica.Artepax.evento.model.ReservaEstados;
import com.catolica.Artepax.evento.repository.EventoProductoRepository;
import com.catolica.Artepax.inventario.movimiento.dto.AjusteInventarioRequestDTO;
import com.catolica.Artepax.inventario.movimiento.dto.MovimientoRequestDTO;
import com.catolica.Artepax.inventario.movimiento.dto.MovimientoResponseDTO;
import com.catolica.Artepax.inventario.movimiento.entity.MovimientoInventario;
import com.catolica.Artepax.inventario.movimiento.mapper.MovimientoMapper;
import com.catolica.Artepax.inventario.movimiento.repository.MovimientoInventarioRepository;
import com.catolica.Artepax.inventario.movimiento.service.MovimientoService;
import com.catolica.Artepax.inventario.producto.dto.StockProductoResponseDTO;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.entity.StockProducto;
import com.catolica.Artepax.inventario.producto.exception.ProductoNotFoundException;
import com.catolica.Artepax.inventario.producto.exception.ReservaActivaInsuficienteException;
import com.catolica.Artepax.inventario.producto.exception.StockInsuficienteException;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import com.catolica.Artepax.inventario.producto.repository.StockProductoRepository;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import jakarta.transaction.Transactional;
import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.catolica.Artepax.common.constant.AuditoriaAcciones.AJUSTAR_STOCK;
import static com.catolica.Artepax.common.constant.AuditoriaTablas.MOVIMIENTO_INVENTARIO;
import static com.catolica.Artepax.common.constant.AuditoriaTablas.PRODUCTO;

@Slf4j
@Service
public class MovimientoServiceImpl implements MovimientoService {
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final StockProductoRepository stockProductoRepository;
    private final MovimientoMapper movimientoMapper;
    private final UsuarioRepository usuarioRepository;
    private final EventoProductoRepository eventoProductoRepository;
    private final AuditoriaService auditoriaService;

    private final NotificacionService notificacionService;

    private static final List<String> ORIGENES_ENTRADA = List.of("REPOSICION", "AJUSTE_MANUAL");
    private static final List<String> ORIGENES_SALIDA  = List.of("AJUSTE_MANUAL", "DAÑO", "PERDIDA");

    public MovimientoServiceImpl(MovimientoInventarioRepository movimientoRepository,
                                 ProductoRepository productoRepository,
                                 StockProductoRepository stockProductoRepository,
                                 MovimientoMapper movimientoMapper,
                                 NotificacionService notificacionService,
                                 UsuarioRepository usuarioRepository,
                                 EventoProductoRepository eventoProductoRepository,
                                 AuditoriaService auditoriaService) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.stockProductoRepository = stockProductoRepository;
        this.movimientoMapper = movimientoMapper;
        this.notificacionService = notificacionService;
        this.usuarioRepository = usuarioRepository;
        this.eventoProductoRepository = eventoProductoRepository;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public void registrarEntrada(MovimientoRequestDTO dto) {
        String origen = dto.getTipoOrigen() != null ? dto.getTipoOrigen() : "REPOSICION";

        if (!ORIGENES_ENTRADA.contains(origen)) {
            throw new IllegalArgumentException(
                    "Origen inválido para entrada: " + origen +
                            ". Válidos: " + ORIGENES_ENTRADA);
        }

        Producto producto = obtenerProductoOFallar(dto.getProductoId());
        producto.setCantidadTotal(producto.getCantidadTotal() + dto.getCantidad());
        productoRepository.save(producto);

        registrarMovimiento(producto, "ENTRADA", origen, dto.getCantidad(), dto.getMotivo());

        verificarYNotificarStockBajo(producto);
    }

    @Override
    @Transactional
    public void registrarSalida(MovimientoRequestDTO dto) {
        String origen = dto.getTipoOrigen() != null ? dto.getTipoOrigen() : "DAÑO";

        if (!ORIGENES_SALIDA.contains(origen)) {
            throw new IllegalArgumentException(
                    "Origen inválido para salida: " + origen +
                            ". Válidos: " + ORIGENES_SALIDA);
        }

        Producto producto = obtenerProductoOFallar(dto.getProductoId());

        StockProducto stock = stockProductoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new ProductoNotFoundException(dto.getProductoId()));

        if (stock.getCantidadDisponible() < dto.getCantidad()) {
            throw new StockInsuficienteException(
                    producto.getNombre(),
                    stock.getCantidadDisponible(),
                    dto.getCantidad());
        }

        producto.setCantidadTotal(producto.getCantidadTotal() - dto.getCantidad());
        productoRepository.save(producto);

        registrarMovimiento(producto, "SALIDA", origen, dto.getCantidad(), dto.getMotivo());

        verificarYNotificarStockBajo(producto);
    }

    @Override
    @Transactional
    public void registrarAjuste(Long productoId, AjusteInventarioRequestDTO request) {
        Producto producto = obtenerProductoOFallar(productoId);

        if (!"ACTIVO".equalsIgnoreCase(producto.getEstado())) {
            throw new IllegalArgumentException("No se puede ajustar stock de un producto inactivo: " + producto.getNombre());
        }

        Integer cantidadReservadaActiva = eventoProductoRepository.sumCantidadReservadaActivaByProductoId(productoId);
        if (cantidadReservadaActiva == null) {
            cantidadReservadaActiva = 0;
        }

        if (request.cantidadFisica() < cantidadReservadaActiva) {
            throw new ReservaActivaInsuficienteException(
                    producto.getNombre(), request.cantidadFisica(), cantidadReservadaActiva);
        }

        int cantidadActual = producto.getCantidadTotal();
        int diferencia = request.cantidadFisica() - cantidadActual;

        if (diferencia == 0) {
            log.info("Ajuste manual para producto {}: sin cambios (físico = sistema = {})", productoId, cantidadActual);
            return;
        }

        ProductoAuditSnapshot antes = new ProductoAuditSnapshot(
                producto.getId(), producto.getNombre(), cantidadActual, producto.getEstado());

        String motivo = request.motivo() != null ? request.motivo() : "Ajuste manual — conteo físico";

        if (diferencia > 0) {
            producto.setCantidadTotal(request.cantidadFisica());
            productoRepository.save(producto);
            MovimientoInventario movimiento = registrarMovimiento(producto, "ENTRADA", "AJUSTE_MANUAL", diferencia, motivo);
            log.info("Ajuste ENTRADA producto {}: +{} (sistema {} -> físico {})", productoId, diferencia, cantidadActual, request.cantidadFisica());

            ProductoAuditSnapshot despues = new ProductoAuditSnapshot(
                    producto.getId(), producto.getNombre(), producto.getCantidadTotal(), producto.getEstado());
            auditoriaService.registrar(AJUSTAR_STOCK, PRODUCTO, producto.getId(), antes, despues,
                    motivo + " | " + diferencia + " unidades ENTRADA");
            auditoriaService.registrar(AJUSTAR_STOCK, MOVIMIENTO_INVENTARIO, movimiento.getId(),
                    null, movimiento, "Movimiento creado: ENTRADA AJUSTE_MANUAL");
        } else {
            producto.setCantidadTotal(request.cantidadFisica());
            productoRepository.save(producto);
            MovimientoInventario movimiento = registrarMovimiento(producto, "SALIDA", "AJUSTE_MANUAL", Math.abs(diferencia), motivo);
            log.info("Ajuste SALIDA producto {}: -{} (sistema {} -> físico {})", productoId, Math.abs(diferencia), cantidadActual, request.cantidadFisica());

            ProductoAuditSnapshot despues = new ProductoAuditSnapshot(
                    producto.getId(), producto.getNombre(), producto.getCantidadTotal(), producto.getEstado());
            auditoriaService.registrar(AJUSTAR_STOCK, PRODUCTO, producto.getId(), antes, despues,
                    motivo + " | " + Math.abs(diferencia) + " unidades SALIDA");
            auditoriaService.registrar(AJUSTAR_STOCK, MOVIMIENTO_INVENTARIO, movimiento.getId(),
                    null, movimiento, "Movimiento creado: SALIDA AJUSTE_MANUAL");
        }

        verificarYNotificarStockBajo(producto);
    }

    @Override
    public StockProductoResponseDTO obtenerStock(Long productoId) {
        StockProducto stock = stockProductoRepository.findById(productoId)
                .orElseThrow(() -> new ProductoNotFoundException(productoId));
        return toStockDTO(stock);
    }

    @Override
    public List<MovimientoResponseDTO> obtenerMovimientosPorProducto(Long productoId) {
        if (!productoRepository.existsById(productoId)) {
            throw new ProductoNotFoundException(productoId);
        }
        return movimientoRepository.findByProductoId(productoId)
                .stream().map(movimientoMapper::toDTO).toList();
    }

    @Override
    public Page<MovimientoResponseDTO> obtenerTodosMovimientos(Pageable pageable) {
        return movimientoRepository.findAllWithProductoYEvento(pageable)
                .map(movimientoMapper::toDTO);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────
    private Producto obtenerProductoOFallar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
    }

    private void verificarYNotificarStockBajo(Producto producto) {
        StockProducto stock = stockProductoRepository.findById(producto.getId())
                .orElse(null);
        if (stock != null && Boolean.TRUE.equals(stock.getStockBajoAlerta())) {
            notificacionService.notificarAdmins(
                    "ALERTA_STOCK",
                    "Stock bajo alerta para el producto: " + producto.getNombre() +
                            ". Disponible: " + stock.getCantidadDisponible() +
                            " (mínimo: " + stock.getStockMinimo() + ")",
                    null
            );
        }
    }

    // Reemplazar el método registrarMovimiento() existente
    private MovimientoInventario registrarMovimiento(Producto producto, String tipo,
                                      String origen, int cantidad, String motivo) {
        MovimientoInventario m = new MovimientoInventario();
        m.setProducto(producto);
        m.setTipoMovimiento(tipo);
        m.setTipoOrigen(origen);
        m.setCantidad(cantidad);
        m.setMotivo(motivo);
        m.setRegistradoPor(obtenerUsuarioAutenticado());
        return movimientoRepository.save(m);
    }



    private StockProductoResponseDTO toStockDTO(StockProducto s) {
        StockProductoResponseDTO dto = new StockProductoResponseDTO();
        dto.setIdProducto(s.getIdProducto());
        dto.setNombre(s.getNombre());
        dto.setCantidadTotal(s.getCantidadTotal());
        dto.setCantidadReservada(s.getCantidadReservada());
        dto.setCantidadDisponible(s.getCantidadDisponible());
        dto.setStockMinimo(s.getStockMinimo());
        dto.setStockBajoAlerta(s.getStockBajoAlerta());
        return dto;
    }

    private Usuario obtenerUsuarioAutenticado() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            String correo = auth.getName();
            return usuarioRepository.findByCorreo(correo).orElse(null);
        } catch (Exception e) {
            log.warn("No se pudo obtener el usuario autenticado para el movimiento: {}", e.getMessage());
            return null;
        }
    }

}