# Tasks: Endpoints Globales GET /api/{modulo}

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~230 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
400-line budget risk: Low

## Phase 1: Preparación de DTOs (transversal)

- [ ] 1.1 🔴 `PagoResponseDTO` — Agregar `clienteNombre` (String) y `estadoEventoNombre` (String) con getters/setters (~12 líneas)
- [ ] 1.2 🔴 `GastoEventoResponseDTO` — Agregar `clienteNombre` y `estadoEventoNombre` con getters/setters (~12 líneas)
- [ ] 1.3 🔴 `CronogramaResponseDTO` — Agregar `clienteNombre` y `estadoEventoNombre` con getters/setters (~12 líneas)
- [ ] 1.4 🔴 `DocumentoResponseDTO` — Agregar `clienteNombre` y `estadoEventoNombre` con getters/setters (~12 líneas)
- [ ] 1.5 🔴 `MovimientoResponseDTO` — Agregar `productoNombre` (String) y `eventoNombre` (String, nullable) con getters/setters (~12 líneas)

## Phase 2: Queries en Repositories

- [ ] 2.1 🔴 `PagoRepository` — Agregar `@Query` con `JOIN FETCH p.evento e JOIN FETCH e.cliente JOIN FETCH e.estadoEvento` + `countQuery` para Pageable (~8 líneas)
- [ ] 2.2 🔴 `GastoEventoRepository` — Agregar query con `JOIN FETCH g.evento e JOIN FETCH e.cliente JOIN FETCH e.estadoEvento` + countQuery (~8 líneas)
- [ ] 2.3 🔴 `CronogramaRepository` — Agregar query con `JOIN FETCH c.evento e JOIN FETCH e.cliente JOIN FETCH e.estadoEvento` + countQuery (~8 líneas)
- [ ] 2.4 🔴 `DocumentoRepository` — Agregar query con `JOIN FETCH d.evento e JOIN FETCH e.cliente JOIN FETCH e.estadoEvento` + countQuery (~8 líneas)
- [ ] 2.5 🔴 `MovimientoInventarioRepository` — Agregar query con `JOIN FETCH m.producto LEFT JOIN FETCH m.evento e LEFT JOIN FETCH e.cliente LEFT JOIN FETCH e.estadoEvento` + countQuery (~10 líneas)

## Phase 3: Mappers — extender toDTO()

- [ ] 3.1 🔴 `PagoMapper.toDTO()` — Agregar mapeo de `pago.getEvento().getCliente().getNombre()` y `pago.getEvento().getEstadoEvento().getNombre()` (~4 líneas)
- [ ] 3.2 🔴 `GastoEventoServiceImpl.toDTO()` — Agregar mapeo de `g.getEvento().getCliente().getNombre()` y `g.getEvento().getEstadoEvento().getNombre()` (~4 líneas)
- [ ] 3.3 🔴 `CronogramaMapper.toDTO()` — Agregar mapeo de `cronograma.getEvento().getCliente().getNombre()` y `cronograma.getEvento().getEstadoEvento().getNombre()` (~4 líneas)
- [ ] 3.4 🔴 `DocumentoMapper.toDTO(Documento)` — Agregar mapeo de `doc.getEvento().getCliente().getNombre()` y `doc.getEvento().getEstadoEvento().getNombre()` (~4 líneas)
- [ ] 3.5 🔴 `MovimientoMapper.toDTO()` — Agregar `movimiento.getProducto().getNombre()` y `movimiento.getEvento() != null ? movimiento.getEvento().getCliente().getNombre() : null` (~5 líneas)

## Phase 4: Service — método obtenerTodos()

- [ ] 4.1 🔴 `PagoService` + `PagoServiceImpl` — Agregar `Page<PagoResponseDTO> obtenerTodos(Pageable pageable)`, delegar al repo + mapper (~15 líneas)
- [ ] 4.2 🔴 `GastoEventoService` + `GastoEventoServiceImpl` — Agregar `Page<GastoEventoResponseDTO> obtenerTodos(Pageable pageable)` (~15 líneas)
- [ ] 4.3 🔴 `CronogramaService` + `CronogramaServiceImpl` — Agregar `Page<CronogramaResponseDTO> obtenerTodos(Pageable pageable)` (~15 líneas)
- [ ] 4.4 🔴 `DocumentoService` + `DocumentoServiceImpl` — Agregar `Page<DocumentoResponseDTO> obtenerTodos(Pageable pageable)` (~15 líneas)
- [ ] 4.5 🔴 `MovimientoService` + `MovimientoServiceImpl` — Agregar `Page<MovimientoResponseDTO> obtenerTodos(Pageable pageable)` (~15 líneas)

## Phase 5: Controller — endpoint GET con paginación

- [ ] 5.1 🔴 `PagoController` — `GET /api/pagos` con `@RequestParam page/size`, `PageRequest.of(page, size, Sort.by("fecha").descending())` → `pagoService.obtenerTodos(pageable)` (~12 líneas)
- [ ] 5.2 🔴 `GastoEventoController` — `GET /api/gastos` con paginación, orden por `gastoFecha` desc (~12 líneas)
- [ ] 5.3 🔴 `CronogramaController` — `GET /api/cronogramas` con paginación, orden por `fechaInicio` desc (~12 líneas)
- [ ] 5.4 🔴 `DocumentoController` — `GET /api/documentos` con paginación, orden por `docuFechaGeneracion` desc (~12 líneas)
- [ ] 5.5 🔴 `MovimientoController` — `GET /api/inventario/movimientos` con paginación, orden por `fecha` desc (~12 líneas)

## Phase 6: Verificación

- [ ] 6.1 🟡 Compilar proyecto — `./mvnw compile` sin errores
- [ ] 6.2 🟡 Verificar imports — cada archivo modificado importa `Page`, `Pageable`, `PageRequest`, `Sort`

### Dependency Order

```
Phase 1 (DTOs) → Phase 2 (Repos) → Phase 3 (Mappers) → Phase 4 (Services) → Phase 5 (Controllers) → Phase 6 (Verify)
```

Tasks within each phase are independent and can be done in parallel.
