package com.catolica.Artepax.inventario.movimiento.service.impl;

import com.catolica.Artepax.common.audit.ProductoAuditSnapshot;
import com.catolica.Artepax.common.service.AuditoriaService;

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

/**
 * Implementation of {@link MovimientoService} for inventory movement management.
 * <p>
 * Handles ENTRADA (stock increase), SALIDA (stock decrease), and AJUSTE
 * (physical-count-based adjustment) operations. Each movement records an audit
 * trail via {@link com.catolica.Artepax.common.service.AuditoriaService} and
 * checks low-stock thresholds to trigger admin notifications.
 * </p>
 * <p>
 * Note that this service uses {@link jakarta.transaction.Transactional @Transactional}
 * (Jakarta) instead of the Spring variant — this is a known inconsistency in the
 * codebase.
 * </p>
 */
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
        // Validates the origin type, locks the product row, increases stock,
        // creates the movement record, and audits the change.
        String origen = dto.getTipoOrigen() != null ? dto.getTipoOrigen() : "REPOSICION";

        if (!ORIGENES_ENTRADA.contains(origen)) {
            throw new IllegalArgumentException(
                    "Origen inválido para entrada: " + origen +
                            ". Válidos: " + ORIGENES_ENTRADA);
        }

        Producto producto = productoRepository.findByIdForUpdate(dto.getProductoId())
                .orElseThrow(() -> new ProductoNotFoundException(dto.getProductoId()));
        validarProductoActivo(producto);

        ProductoAuditSnapshot antes = new ProductoAuditSnapshot(
                producto.getId(), producto.getNombre(), producto.getCantidadTotal(), producto.getEstado());

        producto.setCantidadTotal(producto.getCantidadTotal() + dto.getCantidad());
        productoRepository.save(producto);

        MovimientoInventario movimiento = registrarMovimiento(producto, "ENTRADA", origen, dto.getCantidad(), dto.getMotivo());

        ProductoAuditSnapshot despues = new ProductoAuditSnapshot(
                producto.getId(), producto.getNombre(), producto.getCantidadTotal(), producto.getEstado());
        String descripcion = "Entrada: " + dto.getCantidad() + " unidades, origen: " + origen;
        auditoriaService.registrar(AJUSTAR_STOCK, PRODUCTO, producto.getId(), antes, despues, descripcion);
        auditoriaService.registrar(AJUSTAR_STOCK, MOVIMIENTO_INVENTARIO, movimiento.getId(),
                null, movimiento, "Movimiento creado: ENTRADA " + origen);

        verificarYNotificarStockBajo(producto);
    }

    @Override
    @Transactional
    public void registrarSalida(MovimientoRequestDTO dto) {
        // Validates the origin type, locks the product row, checks available stock,
        // decreases stock, creates the movement record, and audits the change.
        String origen = dto.getTipoOrigen() != null ? dto.getTipoOrigen() : "DAÑO";

        if (!ORIGENES_SALIDA.contains(origen)) {
            throw new IllegalArgumentException(
                    "Origen inválido para salida: " + origen +
                            ". Válidos: " + ORIGENES_SALIDA);
        }

        Producto producto = productoRepository.findByIdForUpdate(dto.getProductoId())
                .orElseThrow(() -> new ProductoNotFoundException(dto.getProductoId()));
        validarProductoActivo(producto);

        StockProducto stock = stockProductoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new ProductoNotFoundException(dto.getProductoId()));

        if (stock.getCantidadDisponible() < dto.getCantidad()) {
            throw new StockInsuficienteException(
                    producto.getNombre(),
                    stock.getCantidadDisponible(),
                    dto.getCantidad());
        }

        ProductoAuditSnapshot antes = new ProductoAuditSnapshot(
                producto.getId(), producto.getNombre(), producto.getCantidadTotal(), producto.getEstado());

        producto.setCantidadTotal(producto.getCantidadTotal() - dto.getCantidad());
        productoRepository.save(producto);

        MovimientoInventario movimiento = registrarMovimiento(producto, "SALIDA", origen, dto.getCantidad(), dto.getMotivo());

        ProductoAuditSnapshot despues = new ProductoAuditSnapshot(
                producto.getId(), producto.getNombre(), producto.getCantidadTotal(), producto.getEstado());
        String descripcion = "Salida: " + dto.getCantidad() + " unidades, origen: " + origen;
        auditoriaService.registrar(AJUSTAR_STOCK, PRODUCTO, producto.getId(), antes, despues, descripcion);
        auditoriaService.registrar(AJUSTAR_STOCK, MOVIMIENTO_INVENTARIO, movimiento.getId(),
                null, movimiento, "Movimiento creado: SALIDA " + origen);

        verificarYNotificarStockBajo(producto);
    }

    @Override
    @Transactional
    public void registrarAjuste(Long productoId, AjusteInventarioRequestDTO request) {
        // Locks the product, validates active reservations don't exceed the new
        // physical count, calculates the difference, and creates ENTRADA or SALIDA
        // movements accordingly.
        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .orElseThrow(() -> new ProductoNotFoundException(productoId));

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
        // Queries the stock view for the given product and maps it to a DTO.
        StockProducto stock = stockProductoRepository.findById(productoId)
                .orElseThrow(() -> new ProductoNotFoundException(productoId));
        return toStockDTO(stock);
    }

    @Override
    public Page<MovimientoResponseDTO> obtenerMovimientosPorProducto(Long productoId, Pageable pageable) {
        // Verifies the product exists, then returns a paginated list of its
        // movements with eager-loaded relations.
        if (!productoRepository.existsById(productoId)) {
            throw new ProductoNotFoundException(productoId);
        }
        return movimientoRepository.findByProductoIdWithRelationsOrderByFechaDesc(productoId, pageable)
                .map(movimientoMapper::toDTO);
    }

    @Override
    public Page<MovimientoResponseDTO> obtenerTodosMovimientos(Pageable pageable) {
        // Returns a paginated list of all movements across all products.
        return movimientoRepository.findAllWithProductoYEvento(pageable)
                .map(movimientoMapper::toDTO);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    /**
     * Validates that the product is in {@code ACTIVO} state.
     *
     * @param producto the product to check
     * @throws IllegalArgumentException if the product is not active
     */
    private void validarProductoActivo(Producto producto) {
        if (!"ACTIVO".equalsIgnoreCase(producto.getEstado())) {
            throw new IllegalArgumentException(
                    "No se pueden registrar movimientos para productos inactivos: " + producto.getNombre());
        }
    }

    /**
     * Finds a product by ID or throws an exception if not found.
     *
     * @param id the product ID
     * @return the found product
     * @throws ProductoNotFoundException if not found
     */
    private Producto obtenerProductoOFallar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
    }

    /**
     * Checks whether the product's available stock has fallen below the minimum
     * threshold and notifies admins if so.
     *
     * @param producto the product to check
     */
    private void verificarYNotificarStockBajo(Producto producto) {
        StockProducto stock = stockProductoRepository.findById(producto.getId())
                .orElse(null);
        if (stock != null && Boolean.TRUE.equals(stock.getStockBajoAlerta())) {
            try {
                notificacionService.notificarAdmins(
                        "ALERTA_STOCK",
                        "Stock bajo alerta para el producto: " + producto.getNombre() +
                                ". Disponible: " + stock.getCantidadDisponible() +
                                " (mínimo: " + stock.getStockMinimo() + ")",
                        null
                );
            } catch (Exception e) {
                log.error("Error al notificar stock bajo para producto {}: {}", producto.getNombre(), e.getMessage(), e);
            }
        }
    }

    /**
     * Creates and saves a new inventory movement record.
     *
     * @param producto the affected product
     * @param tipo     the movement type (ENTRADA or SALIDA)
     * @param origen   the origin type (REPOSICION, AJUSTE_MANUAL, DAÑO, PERDIDA)
     * @param cantidad the quantity moved
     * @param motivo   the reason for the movement
     * @return the saved movement entity
     */
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



    /**
     * Converts a {@link StockProducto} view entity to a response DTO.
     *
     * @param s the stock view entity
     * @return the populated response DTO
     */
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

    /**
     * Retrieves the currently authenticated user, or {@code null} if no valid
     * authentication context is available.
     *
     * @return the authenticated {@link Usuario}, or {@code null}
     */
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