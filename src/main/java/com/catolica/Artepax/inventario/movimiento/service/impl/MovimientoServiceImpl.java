package com.catolica.Artepax.inventario.movimiento.service.impl;

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

import java.util.List;

@Slf4j
@Service
public class MovimientoServiceImpl implements MovimientoService {
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final StockProductoRepository stockProductoRepository;
    private final MovimientoMapper movimientoMapper;
    private final UsuarioRepository usuarioRepository;

    private final NotificacionService notificacionService;

    private static final List<String> ORIGENES_ENTRADA = List.of("REPOSICION", "AJUSTE_MANUAL");
    private static final List<String> ORIGENES_SALIDA  = List.of("AJUSTE_MANUAL", "DAÑO", "PERDIDA");

    public MovimientoServiceImpl(MovimientoInventarioRepository movimientoRepository,
                                 ProductoRepository productoRepository,
                                 StockProductoRepository stockProductoRepository,
                                 MovimientoMapper movimientoMapper,
                                 NotificacionService notificacionService,
                                 UsuarioRepository usuarioRepository) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.stockProductoRepository = stockProductoRepository;
        this.movimientoMapper = movimientoMapper;
        this.notificacionService = notificacionService;
        this.usuarioRepository = usuarioRepository;
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

        // Verificar alerta de stock después de la salida
        StockProducto stockActualizado = stockProductoRepository.findById(dto.getProductoId())
                .orElse(null);
        if (stockActualizado != null && Boolean.TRUE.equals(stockActualizado.getStockBajoAlerta())) {
            notificacionService.notificarAdmins(
                    "ALERTA_STOCK",
                    "Stock bajo alerta para el producto: " + producto.getNombre() +
                            ". Disponible: " + stockActualizado.getCantidadDisponible() +
                            " (mínimo: " + stockActualizado.getStockMinimo() + ")",
                    null
            );
        }
    }

    @Override
    @Transactional
    public void registrarAjuste(Long productoId, Integer cantidadFisica, String motivo) {
        Producto producto = obtenerProductoOFallar(productoId);
        int diferencia = cantidadFisica - producto.getCantidadTotal();

        if (diferencia == 0) return; // Sin cambios

        if (diferencia > 0) {
            // Hay más de lo que dice el sistema → ENTRADA
            producto.setCantidadTotal(cantidadFisica);
            productoRepository.save(producto);
            registrarMovimiento(producto, "ENTRADA", "AJUSTE_MANUAL",
                    diferencia, motivo != null ? motivo : "Ajuste manual — conteo físico");
        } else {
            // Hay menos de lo que dice el sistema → SALIDA
            producto.setCantidadTotal(cantidadFisica);
            productoRepository.save(producto);
            registrarMovimiento(producto, "SALIDA", "AJUSTE_MANUAL",
                    Math.abs(diferencia), motivo != null ? motivo : "Ajuste manual — conteo físico");
        }
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

    // ─── HELPERS ──────────────────────────────────────────────────────────────
    private Producto obtenerProductoOFallar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
    }

    // Reemplazar el método registrarMovimiento() existente
    private void registrarMovimiento(Producto producto, String tipo,
                                     String origen, int cantidad, String motivo) {
        MovimientoInventario m = new MovimientoInventario();
        m.setProducto(producto);
        m.setTipoMovimiento(tipo);
        m.setTipoOrigen(origen);
        m.setCantidad(cantidad);
        m.setMotivo(motivo);
        m.setRegistradoPor(obtenerUsuarioAutenticado());
        movimientoRepository.save(m);
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