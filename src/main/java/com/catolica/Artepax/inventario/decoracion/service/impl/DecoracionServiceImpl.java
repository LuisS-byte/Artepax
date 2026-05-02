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
import com.catolica.Artepax.inventario.producto.exception.ProductoNotFoundException;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DecoracionServiceImpl implements DecoracionService {

    private final DecoracionRepository decoracionRepository;
    private final DecoracionProductoRepository decoracionProductoRepository;
    private final ProductoRepository productoRepository;
    private final DecoracionMapper decoracionMapper;

    @Override
    @Transactional
    public DecoracionResponseDTO crearDecoracion(DecoracionRequestDTO dto) {

        // guardar decoracion
        Decoracion decoracion = decoracionMapper.toEntity(dto);
        Decoracion guardada = decoracionRepository.save(decoracion);

        // asociar productos
        List<DecoracionProducto> productos = dto.getProductos().stream().map(p -> {

            Producto producto = productoRepository.findById(p.getProductoId())
                    .orElseThrow(() -> new ProductoNotFoundException(p.getProductoId()));

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
                .orElseThrow(() -> new DecoracionNotFoundException(id)); // P4

        List<DecoracionProducto> productos =
                decoracionProductoRepository.findByDecoracionId(id);

        return decoracionMapper.toDTO(decoracion, productos);
    }

    @Override
    @Transactional
    public DecoracionResponseDTO actualizar(Long id, DecoracionUpdateDTO dto) {

        Decoracion decoracion = decoracionRepository.findById(id)
                .orElseThrow(() -> new DecoracionNotFoundException(id)); // P4

        decoracionMapper.updateEntity(decoracion, dto);
        decoracionRepository.save(decoracion);

        // si vienen productos nuevos, reemplazar los existentes
        if (dto.getProductos() != null && !dto.getProductos().isEmpty()) {

            // borrar asociaciones anteriores
            List<DecoracionProducto> anteriores =
                    decoracionProductoRepository.findByDecoracionId(id);
            decoracionProductoRepository.deleteAll(anteriores);

            // guardar nuevas asociaciones
            List<DecoracionProducto> nuevos = dto.getProductos().stream().map(p -> {

                Producto producto = productoRepository.findById(p.getProductoId())
                        .orElseThrow(() -> new ProductoNotFoundException(p.getProductoId()));

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
                decoracionProductoRepository.findByDecoracionId(id);
        return decoracionMapper.toDTO(decoracion, productos);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Decoracion decoracion = decoracionRepository.findById(id)
                .orElseThrow(() -> new DecoracionNotFoundException(id)); // P4

        decoracion.setEstado("INACTIVO");
        decoracionRepository.save(decoracion);
    }
}