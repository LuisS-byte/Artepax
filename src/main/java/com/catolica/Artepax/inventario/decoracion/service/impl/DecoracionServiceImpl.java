package com.catolica.Artepax.inventario.decoracion.service.impl;



import com.catolica.Artepax.inventario.decoracion.dto.DecoracionRequestDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionResponseDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionUpdateDTO;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.evento.repository.TipoEventoRepository;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProductoId;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionTipoEvento;
import com.catolica.Artepax.inventario.decoracion.exception.DecoracionNotFoundException;
import com.catolica.Artepax.inventario.decoracion.mapper.DecoracionMapper;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionProductoRepository;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionRepository;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionTipoEventoRepository;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link DecoracionService} for decoration package management.
 * <p>
 * Handles creation, update, and soft-delete of decoration packages, including
 * management of the product associations through the junction entity. Validates
 * product stock availability before associating products with a decoration.
 * </p>
 */
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
    private final DecoracionTipoEventoRepository decoracionTipoEventoRepository;
    private final TipoEventoRepository tipoEventoRepository;

    /**
     * Normalizes and validates a decoration state value.
     * Defaults to {@code ACTIVO} if the input is null or blank.
     *
     * @param estado the raw state value
     * @return the normalized uppercase state
     * @throws IllegalArgumentException if the value is not ACTIVO or INACTIVO
     */
    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) return ESTADO_ACTIVO;
        String normalizado = estado.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("Estado de decoración inválido: " + estado + ". Válidos: " + ESTADOS_VALIDOS);
        }
        return normalizado;
    }

    /**
     * Normalizes a decoration name by trimming whitespace and collapsing multiple spaces.
     *
     * @param nombre the raw name to normalize
     * @return the normalized name, or {@code null} if input was null
     */
    private String normalizarNombre(String nombre) {
        return nombre == null ? null : nombre.trim().replaceAll("\\s+", " ");
    }

    /**
     * Validates that no other decoration already uses the given name (case-insensitive).
     * When {@code idActual} is provided, the check excludes the decoration being updated.
     *
     * @param nombre   the name to check for uniqueness
     * @param idActual the ID of the decoration being updated, or {@code null} for creation
     * @throws IllegalArgumentException if a duplicate name is found
     */
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
        // Normalizes the name, validates uniqueness, checks product stock,
        // validates tipo-evento compatibility, saves the decoration, creates
        // product and tipo-evento associations, and returns the result.
        String nombreNormalizado = normalizarNombre(dto.getNombre());
        validarNombreDisponible(nombreNormalizado, null);
        dto.setNombre(nombreNormalizado);

        List<TipoEvento> tipos = validarTiposCompatibles(dto.getTipoEventoIds());
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

        // asociar tipos de evento compatibles
        guardarCompatibilidades(guardada, tipos);

        List<DecoracionTipoEvento> compatibilidades =
                decoracionTipoEventoRepository.findByDecoracion_IdWithTipoEvento(guardada.getId());

        return decoracionMapper.toDTO(guardada, productos, compatibilidades);
    }

    @Override
    public List<DecoracionResponseDTO> obtenerTodas(String estado) {
        // Fetches decorations (optionally filtered by state) and eagerly loads their
        // product associations and tipo-evento compatibilities in batch.
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

        Map<Long, List<DecoracionTipoEvento>> tiposPorDecoracion =
                decoracionTipoEventoRepository.findByDecoracionIds(ids)
                        .stream()
                        .collect(Collectors.groupingBy(dte -> dte.getDecoracion().getId()));

        return decoraciones.stream()
                .map(d -> decoracionMapper.toDTO(d,
                        productosPorDecoracion.getOrDefault(d.getId(), List.of()),
                        tiposPorDecoracion.getOrDefault(d.getId(), List.of())))
                .toList();
    }

    @Override
    public DecoracionResponseDTO obtenerPorId(Long id) {
        Decoracion decoracion = decoracionRepository.findById(id)
                .orElseThrow(() -> new DecoracionNotFoundException(id));

        List<DecoracionProducto> productos =
                decoracionProductoRepository.findByDecoracionIdWithProducto(id);

        List<DecoracionTipoEvento> tipos =
                decoracionTipoEventoRepository.findByDecoracion_IdWithTipoEvento(id);

        return decoracionMapper.toDTO(decoracion, productos, tipos);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Decoracion decoracion = decoracionRepository.findById(id)
                .orElseThrow(() -> new DecoracionNotFoundException(id));

        decoracion.setEstado("INACTIVO");
        decoracionRepository.save(decoracion);
    }

    @Override
    @Transactional
    public DecoracionResponseDTO reactivar(Long id) {
        Decoracion decoracion = decoracionRepository.findById(id)
                .orElseThrow(() -> new DecoracionNotFoundException(id));

        decoracion.setEstado(ESTADO_ACTIVO);
        Decoracion guardada = decoracionRepository.save(decoracion);

        List<DecoracionProducto> productos =
                decoracionProductoRepository.findByDecoracionIdWithProducto(guardada.getId());

        List<DecoracionTipoEvento> tipos =
                decoracionTipoEventoRepository.findByDecoracion_IdWithTipoEvento(guardada.getId());

        return decoracionMapper.toDTO(guardada, productos, tipos);
    }

    @Override
    @Transactional
    public DecoracionResponseDTO actualizar(Long id, DecoracionUpdateDTO dto) {
        // Finds the existing decoration, validates name uniqueness if changed,
        // applies partial updates, optionally replaces product associations
        // and tipo-evento compatibilities.

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

        // si vienen tipos nuevos, reemplazar compatibilidades
        if (dto.getTipoEventoIds() != null) {
            reemplazarCompatibilidades(decoracion, dto.getTipoEventoIds());
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

            List<DecoracionTipoEvento> tipos =
                    decoracionTipoEventoRepository.findByDecoracion_IdWithTipoEvento(id);
            return decoracionMapper.toDTO(decoracion, nuevos, tipos);
        }

        List<DecoracionProducto> productos =
                decoracionProductoRepository.findByDecoracionIdWithProducto(id);
        List<DecoracionTipoEvento> tipos =
                decoracionTipoEventoRepository.findByDecoracion_IdWithTipoEvento(id);
        return decoracionMapper.toDTO(decoracion, productos, tipos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DecoracionResponseDTO> obtenerCompatiblesPorTipoEvento(Long tipoEventoId) {
        TipoEvento tipo = tipoEventoRepository.findById(tipoEventoId)
                .orElseThrow(() -> new IllegalArgumentException("El tipo de evento no existe."));
        if (!Boolean.TRUE.equals(tipo.getTipoActivo())) {
            throw new IllegalArgumentException("El tipo de evento está inactivo.");
        }

        return decoracionTipoEventoRepository
                .findDecoracionesActivasCompatibles(tipoEventoId)
                .stream()
                .map(d -> decoracionMapper.toDTO(d,
                        decoracionProductoRepository.findByDecoracionIdWithProducto(d.getId()),
                        decoracionTipoEventoRepository.findByDecoracion_IdWithTipoEvento(d.getId())))
                .toList();
    }

    /**
     * Validates that the given tipo-evento IDs exist, are active, and are non-empty.
     *
     * @param tipoEventoIds list of tipo-evento IDs to validate
     * @return the list of validated TipoEvento entities
     * @throws IllegalArgumentException if validation fails
     */
    private List<TipoEvento> validarTiposCompatibles(List<Long> tipoEventoIds) {
        if (tipoEventoIds == null || tipoEventoIds.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un tipo de evento compatible.");
        }
        List<Long> idsUnicos = tipoEventoIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (idsUnicos.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un tipo de evento compatible.");
        }
        List<TipoEvento> tipos = tipoEventoRepository.findAllById(idsUnicos);
        if (tipos.size() != idsUnicos.size()) {
            throw new IllegalArgumentException("Uno o más tipos de evento no existen.");
        }
        boolean hayInactivo = tipos.stream()
                .anyMatch(tipo -> !Boolean.TRUE.equals(tipo.getTipoActivo()));
        if (hayInactivo) {
            throw new IllegalArgumentException("No se pueden asociar tipos de evento inactivos.");
        }
        return tipos;
    }

    /**
     * Persists compatibility relationships between a decoration and multiple event types.
     *
     * @param decoracion the decoration entity
     * @param tipos      the list of validated tipo-evento entities
     */
    private void guardarCompatibilidades(Decoracion decoracion, List<TipoEvento> tipos) {
        List<DecoracionTipoEvento> relaciones = tipos.stream()
                .map(tipo -> new DecoracionTipoEvento(decoracion, tipo))
                .toList();
        decoracionTipoEventoRepository.saveAll(relaciones);
    }

    /**
     * Replaces all compatibility relationships for a decoration with the given IDs.
     * Validates the new IDs first, deletes existing relationships, then inserts the new ones.
     *
     * @param decoracion   the decoration entity
     * @param tipoEventoIds the new tipo-evento IDs
     */
    private void reemplazarCompatibilidades(Decoracion decoracion, List<Long> tipoEventoIds) {
        List<TipoEvento> tipos = validarTiposCompatibles(tipoEventoIds);
        decoracionTipoEventoRepository.deleteByDecoracion_Id(decoracion.getId());
        guardarCompatibilidades(decoracion, tipos);
    }

    /**
     * Validates that all products in the list have sufficient available stock
     * for the required quantity and are not in critical stock or inactive state.
     *
     * @param productos the list of product requests with IDs and required quantities
     * @throws IllegalArgumentException if any product has insufficient stock,
     *                                  is in critical stock, or is not found
     */
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
