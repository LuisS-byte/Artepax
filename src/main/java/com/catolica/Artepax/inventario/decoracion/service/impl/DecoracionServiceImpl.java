package com.catolica.Artepax.inventario.decoracion.service.impl;



import com.catolica.Artepax.inventario.decoracion.dto.DecoracionRequestDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionResponseDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionUpdateDTO;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProductoId;
import com.catolica.Artepax.inventario.decoracion.exception.DecoracionNotFoundException;
import com.catolica.Artepax.inventario.decoracion.mapper.DecoracionMapper;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionProductoRepository;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionRepository;
import com.catolica.Artepax.inventario.decoracion.service.DecoracionService;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.entity.StockProducto;
import com.catolica.Artepax.inventario.producto.exception.ProductoNotFoundException;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import com.catolica.Artepax.inventario.producto.repository.StockProductoRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DecoracionServiceImpl implements DecoracionService {

    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String ESTADO_INACTIVO = "INACTIVO";
    private static final Set<String> ESTADOS_VALIDOS = Set.of(ESTADO_ACTIVO, ESTADO_INACTIVO);

    private final DecoracionRepository decoracionRepository;
    private final DecoracionProductoRepository decoracionProductoRepository;
    private final ProductoRepository productoRepository;
    private final StockProductoRepository stockProductoRepository;
    private final DecoracionMapper decoracionMapper;

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) return ESTADO_ACTIVO;
        String normalizado = estado.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("Estado de decoración inválido: " + estado + ". Válidos: " + ESTADOS_VALIDOS);
        }
        return normalizado;
    }

    private String normalizarNombre(String nombre) {
        return nombre == null ? null : nombre.trim().replaceAll("\\s+", " ");
    }

    private void validarNombreDisponible(String nombre, Long idActual) {
        decoracionRepository.findByNombreIgnoreCase(nombre)
                .filter(d -> idActual == null || !d.getId().equals(idActual))
                .ifPresent(d -> {
                    throw new IllegalArgumentException("Ya existe una decoración con el nombre: " + nombre);
                });
    }

    @Override
    @Transactional
    public DecoracionResponseDTO crearDecoracion(DecoracionRequestDTO dto) {
        String nombreNormalizado = normalizarNombre(dto.getNombre());
        validarNombreDisponible(nombreNormalizado, null);
        dto.setNombre(nombreNormalizado);

        validarStockProductos(dto.getProductos());

        // guardar decoracion
        Decoracion decoracion = decoracionMapper.toEntity(dto);
        decoracion.setEstado(normalizarEstado(decoracion.getEstado()));
        Decoracion guardada = decoracionRepository.save(decoracion);

        // asociar productos
        List<DecoracionProducto> productos = dto.getProductos().stream().map(p -> {

            Producto producto = productoRepository.findById(p.getProductoId())
                    .orElseThrow(() -> new ProductoNotFoundException(p.getProductoId()));

            if (!ESTADO_ACTIVO.equalsIgnoreCase(producto.getEstado())) {
                throw new IllegalArgumentException(
                        "El producto '" + producto.getNombre() + "' está inactivo y no puede usarse en una decoración");
            }

            DecoracionProducto dp = new DecoracionProducto();
            dp.setId(new DecoracionProductoId(guardada.getId(), producto.getId()));
            dp.setDecoracion(guardada);
            dp.setProducto(producto);
            dp.setCantidadRequerida(p.getCantidadRequerida());

            return dp;

        }).toList();

        decoracionProductoRepository.saveAll(productos);

        return decoracionMapper.toDTO(guardada, productos);
    }

    @Override
    public List<DecoracionResponseDTO> obtenerTodas(String estado) {
        List<Decoracion> decoraciones = estado != null
                ? decoracionRepository.findByEstado(estado)
                : decoracionRepository.findAll();

        List<Long> ids = decoraciones.stream().map(Decoracion::getId).toList();

        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, List<DecoracionProducto>> productosPorDecoracion =
                decoracionProductoRepository.findByDecoracionIds(ids)
                        .stream()
                        .collect(Collectors.groupingBy(dp -> dp.getDecoracion().getId()));

        return decoraciones.stream()
                .map(d -> decoracionMapper.toDTO(d,
                        productosPorDecoracion.getOrDefault(d.getId(), List.of())))
                .toList();
    }

    @Override
    public DecoracionResponseDTO obtenerPorId(Long id) {
        Decoracion decoracion = decoracionRepository.findById(id)
                .orElseThrow(() -> new DecoracionNotFoundException(id));

        List<DecoracionProducto> productos =
                decoracionProductoRepository.findByDecoracionIdWithProducto(id);

        return decoracionMapper.toDTO(decoracion, productos);
    }

    @Override
    @Transactional
    public DecoracionResponseDTO actualizar(Long id, DecoracionUpdateDTO dto) {

        Decoracion decoracion = decoracionRepository.findById(id)
                .orElseThrow(() -> new DecoracionNotFoundException(id));

        if (dto.getNombre() != null) {
            String nombreNormalizado = normalizarNombre(dto.getNombre());
            validarNombreDisponible(nombreNormalizado, id);
            dto.setNombre(nombreNormalizado);
        }

        decoracionMapper.updateEntity(decoracion, dto);

        if (dto.getEstado() != null) {
            decoracion.setEstado(normalizarEstado(dto.getEstado()));
        }

        decoracionRepository.save(decoracion);

        // si vienen productos nuevos, reemplazar los existentes
        if (dto.getProductos() != null && !dto.getProductos().isEmpty()) {
            validarStockProductos(dto.getProductos());

            // borrar asociaciones anteriores
            List<DecoracionProducto> anteriores =
                    decoracionProductoRepository.findByDecoracionId(id);
            decoracionProductoRepository.deleteAll(anteriores);

            // guardar nuevas asociaciones
            List<DecoracionProducto> nuevos = dto.getProductos().stream().map(p -> {

                Producto producto = productoRepository.findById(p.getProductoId())
                        .orElseThrow(() -> new ProductoNotFoundException(p.getProductoId()));

                if (!ESTADO_ACTIVO.equalsIgnoreCase(producto.getEstado())) {
                    throw new IllegalArgumentException(
                            "El producto '" + producto.getNombre() + "' está inactivo y no puede usarse en una decoración");
                }

                DecoracionProducto dp = new DecoracionProducto();
                dp.setId(new DecoracionProductoId(id, producto.getId()));
                dp.setDecoracion(decoracion);
                dp.setProducto(producto);
                dp.setCantidadRequerida(p.getCantidadRequerida());

                return dp;

            }).toList();

            decoracionProductoRepository.saveAll(nuevos);

            return decoracionMapper.toDTO(decoracion, nuevos);
        }

        List<DecoracionProducto> productos =
                decoracionProductoRepository.findByDecoracionIdWithProducto(id);
        return decoracionMapper.toDTO(decoracion, productos);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Decoracion decoracion = decoracionRepository.findById(id)
                .orElseThrow(() -> new DecoracionNotFoundException(id));

        decoracion.setEstado("INACTIVO");
        decoracionRepository.save(decoracion);
    }

    private void validarStockProductos(List<com.catolica.Artepax.inventario.decoracion.dto.DecoracionProductoRequestDTO> productos) {
        Map<Long, Integer> cantidadesRequeridas = productos.stream()
                .collect(Collectors.toMap(
                        com.catolica.Artepax.inventario.decoracion.dto.DecoracionProductoRequestDTO::getProductoId,
                        com.catolica.Artepax.inventario.decoracion.dto.DecoracionProductoRequestDTO::getCantidadRequerida,
                        Integer::sum));

        List<Long> productoIds = cantidadesRequeridas.keySet().stream().toList();
        Map<Long, StockProducto> stockPorProductoId = stockProductoRepository.findAllByIdProductoIn(productoIds)
                .stream()
                .collect(Collectors.toMap(StockProducto::getIdProducto, stock -> stock));

        for (Map.Entry<Long, Integer> entry : cantidadesRequeridas.entrySet()) {
            StockProducto stock = stockPorProductoId.get(entry.getKey());
            if (stock == null) {
                throw new IllegalArgumentException("No se encontró stock para el producto con ID: " + entry.getKey());
            }
            if (Boolean.TRUE.equals(stock.getStockBajoAlerta())) {
                throw new IllegalArgumentException(
                        "No se puede usar el producto '" + stock.getNombre() + "' porque está en stock crítico");
            }
            if (stock.getCantidadDisponible() == null || stock.getCantidadDisponible() < entry.getValue()) {
                throw new IllegalArgumentException(
                        "Stock insuficiente para el producto '" + stock.getNombre() + "'. Disponible: "
                                + stock.getCantidadDisponible() + ", requerido: " + entry.getValue());
            }
        }
    }
}
