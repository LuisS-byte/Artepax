# evento/gasto — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Gestión de gastos variables atribuibles a eventos: registro, transición PROYECTADO → DESEMBOLSADO, eliminación (solo proyectados) y resumen.

| Elemento | Detalle |
|---|---|
| Controller | `GastoEventoController` (`/api/gastos`) |
| Service | `GastoEventoServiceImpl` |
| Entidad | `GastoEvento` (tabla `gasto_evento`) |
| Tipo de módulo | Operativo |

## 2. Endpoints

Todos requieren `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Notas |
|---|---|---|---|---|---|
| POST | `/api/gastos` | Crear gasto | `GastoEventoRequestDTO` | `GastoEventoResponseDTO` (201) | Solo PROGRAMADO/CONFIRMADO/EN_MONTAJE |
| GET | `/api/gastos/evento/{eventoId}` | Gastos por evento | — | `List<GastoEventoResponseDTO>` | |
| GET | `/api/gastos/evento/{eventoId}/resumen` | Resumen financiero | — | `ResumenGastoEventoDTO` | |
| GET | `/api/gastos/{id}` | Obtener uno | — | `GastoEventoResponseDTO` | |
| PATCH | `/api/gastos/{id}/desembolsar` | Desembolsar | — | `GastoEventoResponseDTO` | PROYECTADO → DESEMBOLSADO |
| DELETE | `/api/gastos/{id}` | Eliminar | — | `204 No Content` | Solo PROYECTADO |
| GET | `/api/gastos` | Todos paginado | Pageable | `Page<GastoEventoResponseDTO>` | Default: size=50, sort=gastoFecha |

### POST /api/gastos

- **Request:** `{ "eventoId": 1, "gastoConcepto": "Transporte", "gastoMonto": 150.00, "gastoEstado": "PROYECTADO", "gastoNota": "Flete" }`
- **Validaciones:** `eventoId`, `gastoConcepto`, `gastoMonto` obligatorios. `gastoMonto > 0`. Evento debe estar en PROGRAMADO, CONFIRMADO o EN_MONTAJE.
- **Errores:** `GastoEventoException` (400): "Solo se pueden registrar gastos en eventos PROGRAMADO, CONFIRMADO o EN_MONTAJE. Estado actual: X"

### PATCH /api/gastos/{id}/desembolsar

- Transición PROYECTADO → DESEMBOLSADO.
- **Errores:** `GastoEventoException` (400): "Solo se pueden desembolsar gastos en estado PROYECTADO"

### DELETE /api/gastos/{id}

- Solo elimina gastos PROYECTADO. No permite eliminar DESEMBOLSADO.
- Es eliminación física (no soft delete).

## 3. DTOs

#### GastoEventoRequestDTO

| Campo | Tipo | Requerido | Validaciones | Notas |
|---|---|---|---|---|
| eventoId | Long | Sí (`@NotNull`) | | |
| gastoConcepto | String | Sí (`@NotBlank`) | | |
| gastoMonto | BigDecimal | Sí (`@DecimalMin("0.01")`) | > 0 | |
| gastoEstado | String | No (default: "PROYECTADO") | | PROYECTADO o DESEMBOLSADO |
| gastoNota | String | No | | |

#### GastoEventoResponseDTO

| Campo | Tipo | Nullable | Notas |
|---|---|---|---|
| id | Long | No | |
| eventoId | Long | No | |
| gastoConcepto | String | No | |
| gastoMonto | BigDecimal | No | |
| gastoEstado | String | No | PROYECTADO / DESEMBOLSADO |
| gastoFecha | LocalDateTime | No | |
| gastoNota | String | Sí | |
| clienteNombre | String | No | Denormalizado |
| estadoEventoNombre | String | No | Denormalizado |

#### ResumenGastoEventoDTO (record)

| Campo | Tipo | Notas |
|---|---|---|
| totalProyectado | BigDecimal | Suma PROYECTADO |
| totalDesembolsado | BigDecimal | Suma DESEMBOLSADO |
| totalPendiente | BigDecimal | Igual a totalProyectado |

## 4. Estados

| Estado | Significado | Visible en frontend |
|---|---|---|
| PROYECTADO | Estimado, no pagado | Badge warning |
| DESEMBOLSADO | Ya salió del bolsillo | Badge success |

Transición: PROYECTADO → DESEMBOLSADO (única). DESEMBOLSADO es terminal (no se puede eliminar ni revertir).

## 5. Reglas para frontend

- Solo crear gastos en eventos PROGRAMADO, CONFIRMADO o EN_MONTAJE.
- `gastoEstado` default es PROYECTADO si no se envía.
- Gastos DESEMBOLSADO no se pueden eliminar.
- El resumen (`/evento/{id}/resumen`) devuelve totales agregados.

---

## Resumen para frontend

- 7 endpoints. CRUD con transición PROYECTADO → DESEMBOLSADO.
- DELETE solo para PROYECTADO.
- Resumen financiero disponible en `/evento/{id}/resumen`.
- Estados: PROYECTADO (editable/eliminable), DESEMBOLSADO (terminal).
