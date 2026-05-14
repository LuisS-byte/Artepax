# inventario/movimiento — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Gestiona el historial de movimientos de inventario (entradas, salidas, ajustes) y expone el stock real desde `vista_stock_producto`. Cada movimiento actualiza `producto.cantidad_total` y queda registrado en `movimiento_inventario`. También verifica stock bajo y envía notificaciones.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Movimientos de stock y consulta de disponibilidad real |
| Controller principal | `MovimientoController` (`/api/inventario`) |
| Service principal | `MovimientoServiceImpl` |
| Entidades principales | `MovimientoInventario` (tabla `movimiento_inventario`), `StockProducto` (vista `vista_stock_producto`) |
| Tablas/vistas implicadas | `movimiento_inventario`, `producto`, `vista_stock_producto`, `evento_producto` |
| Módulos relacionados | `inventario/producto`, `evento`, `notificacion`, `common` (auditoría) |
| Tipo de módulo | Operativo + Reporte |

## 2. Controllers y endpoints completos

Todos requieren `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Service | Auth | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/inventario/entrada` | Registrar entrada | `MovimientoRequestDTO` | `204 No Content` | `registrarEntrada` | ADMIN | REPOSICION o AJUSTE_MANUAL |
| POST | `/api/inventario/salida` | Registrar salida | `MovimientoRequestDTO` | `204 No Content` | `registrarSalida` | ADMIN | DAÑO, PERDIDA o AJUSTE_MANUAL |
| POST | `/api/inventario/ajuste/{productoId}` | Ajuste por conteo físico | `AjusteInventarioRequestDTO` | `204 No Content` | `registrarAjuste` | ADMIN | Calcula diferencia automática |
| GET | `/api/inventario/productos/{id}/stock` | Consultar stock real | — | `StockProductoResponseDTO` | `obtenerStock` | ADMIN | Vista `vista_stock_producto` |
| GET | `/api/inventario/productos/{id}/movimientos` | Historial por producto | — | `List<MovimientoResponseDTO>` | `obtenerMovimientosPorProducto` | ADMIN | |
| GET | `/api/inventario/movimientos` | Todos los movimientos | Pageable | `Page<MovimientoResponseDTO>` | `obtenerTodosMovimientos` | ADMIN | Default: size=50, sort=fecha |

### POST /api/inventario/entrada

- **Propósito:** Registrar una entrada de stock (reposición o ajuste manual positivo).
- **Request body:** `MovimientoRequestDTO`
- **Response:** `204 No Content`
- **Validaciones principales:**
  - `productoId` obligatorio (`@NotNull`)
  - `cantidad > 0` (`@Positive`)
  - `tipoOrigen` debe ser `REPOSICION` o `AJUSTE_MANUAL` (default: REPOSICION)
- **Excepciones posibles:**
  - `ProductoNotFoundException` (404)
  - `IllegalArgumentException` (400): "Origen inválido para entrada: X. Válidos: [REPOSICION, AJUSTE_MANUAL]"
- **Efectos secundarios:**
  - Incrementa `producto.cantidadTotal`
  - Crea registro en `movimiento_inventario`
  - Si el producto queda en stock bajo → notificación `ALERTA_STOCK` a admins
  - Asocia el movimiento al usuario autenticado
- **Qué debe refrescar el frontend:** stock del producto (`GET /api/inventario/productos/{id}/stock`), historial de movimientos, listado de productos

### POST /api/inventario/salida

- **Propósito:** Registrar una salida de stock (daño, pérdida o ajuste manual negativo).
- **Request body:** `MovimientoRequestDTO`
- **Response:** `204 No Content`
- **Validaciones principales:**
  - `tipoOrigen` debe ser `DAÑO`, `PERDIDA` o `AJUSTE_MANUAL` (default: DAÑO)
  - Stock disponible suficiente: `cantidadDisponible >= cantidad` en la vista
- **Excepciones posibles:**
  - `ProductoNotFoundException` (404)
  - `IllegalArgumentException` (400): "Origen inválido para salida: X"
  - `StockInsuficienteException` (400): "Stock insuficiente para el producto X. Disponible: Y, requerido: Z"
- **Efectos secundarios:**
  - Decrementa `producto.cantidadTotal`
  - Crea registro en `movimiento_inventario`
  - Verifica y notifica stock bajo si aplica
- **Qué debe refrescar el frontend:** stock, historial, listado de productos

### POST /api/inventario/ajuste/{productoId}

- **Propósito:** Ajustar stock por conteo físico. El sistema calcula automáticamente la diferencia.
- **Path variables:** `productoId` (Long)
- **Request body:** `AjusteInventarioRequestDTO` (record: `cantidadFisica`, `motivo`)
- **Response:** `204 No Content`
- **Validaciones principales:**
  - `cantidadFisica >= 0` (`@Min(0)`)
  - `motivo` obligatorio (`@NotBlank`)
  - Producto debe estar ACTIVO
  - `cantidadFisica` no puede ser menor que la cantidad reservada activa
- **Excepciones posibles:**
  - `ProductoNotFoundException` (404)
  - `IllegalArgumentException` (400): "No se puede ajustar stock de un producto inactivo: X"
  - `ReservaActivaInsuficienteException` (400): si `cantidadFisica < cantidadReservadaActiva`
- **Efectos secundarios:**
  - Si `diferencia == 0`: no hace nada, solo loguea
  - Si `diferencia > 0`: entrada por AJUSTE_MANUAL
  - Si `diferencia < 0`: salida por AJUSTE_MANUAL
  - Registra auditoría doble (producto + movimiento)
  - Verifica y notifica stock bajo
- **Qué debe refrescar el frontend:** stock, historial, listado de productos

### GET /api/inventario/productos/{id}/stock

- **Propósito:** Consultar stock real desde la vista `vista_stock_producto`.
- **Path variables:** `id` (Long) — ID del producto
- **Response:** `StockProductoResponseDTO`
- **Excepciones posibles:** `ProductoNotFoundException` (404)
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/inventario/productos/{id}/movimientos

- **Propósito:** Historial de movimientos de un producto específico.
- **Path variables:** `id` (Long)
- **Response:** `List<MovimientoResponseDTO>`
- **Excepciones posibles:** `ProductoNotFoundException` (404) — si el producto no existe
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/inventario/movimientos

- **Propósito:** Listado paginado de todos los movimientos con producto y evento cargados.
- **Query params:** `Pageable` (default: size=50, sort=fecha)
- **Response:** `Page<MovimientoResponseDTO>`
- **Qué debe refrescar el frontend:** NO APLICA

## 3. DTOs request/response

### 3.1 DTOs Request

#### MovimientoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| productoId | Long | number | Sí (`@NotNull`) | No | Producto debe existir | |
| cantidad | Integer | number | Sí (`@NotNull`, `@Positive`) | No | > 0 | Siempre positivo (el tipo define entrada/salida) |
| motivo | String | string | No | Sí | | |
| tipoOrigen | String | string | No | Sí (default: "REPOSICION") | Entrada: REPOSICION, AJUSTE_MANUAL. Salida: DAÑO, PERDIDA, AJUSTE_MANUAL | |

#### AjusteInventarioRequestDTO (record)

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| cantidadFisica | Integer | number | Sí (`@NotNull`, `@Min(0)`) | No | >= 0 | Cantidad contada físicamente |
| motivo | String | string | Sí (`@NotBlank`) | No | | Razón del ajuste |

### 3.2 DTOs Response

#### StockProductoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| idProducto | Long | number | No | vista | |
| nombre | String | string | No | vista | |
| cantidadTotal | Integer | number | No | vista | Stock físico |
| cantidadReservada | Integer | number | No | vista | Suma reservas activas |
| cantidadDisponible | Integer | number | No | vista | `total - reservada` |
| stockMinimo | Integer | number | No | vista | Umbral de alerta |
| stockBajoAlerta | Boolean | boolean | No | vista | TRUE → badge warning en frontend |

#### MovimientoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| id | Long | number | No | entidad | |
| productoId | Long | number | No | entidad.producto | |
| eventoId | Long | number | Sí | entidad.evento | Solo si `tipoOrigen = EVENTO` |
| tipoMovimiento | String | string | No | entidad | ENTRADA / SALIDA |
| tipoOrigen | String | string | No | entidad | REPOSICION, AJUSTE_MANUAL, DAÑO, PERDIDA, EVENTO |
| cantidad | Integer | number | No | entidad | |
| fecha | LocalDateTime | ISO datetime | No | entidad | |
| motivo | String | string | Sí | entidad | |
| productoNombre | String | string | No | entidad.producto | Denormalizado |
| eventoNombre | String | string | Sí | entidad.evento | Solo si asociado a evento |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `MovimientoInventario` | `movimiento_inventario` | Tabla histórica | Trazabilidad de stock | `tipo_movimiento`: ENTRADA/SALIDA. `tipo_origen`: EVENTO, REPOSICION, AJUSTE_MANUAL, DAÑO, PERDIDA |
| `StockProducto` | `vista_stock_producto` | Vista (`@Immutable`) | Disponibilidad real | Solo lectura. `cantidad_disponible` es el stock libre |
| `Producto` | `producto` | Tabla operativa | Stock físico | `cantidad_total` se modifica con cada movimiento |

**Orígenes válidos por tipo de movimiento:**

| Endpoint | tipoMovimiento | tipoOrigen permitidos |
|---|---|---|
| POST entrada | ENTRADA | REPOSICION, AJUSTE_MANUAL |
| POST salida | SALIDA | DAÑO, PERDIDA, AJUSTE_MANUAL |
| POST ajuste | ENTRADA o SALIDA (automático) | AJUSTE_MANUAL |
| Interno (evento) | SALIDA | EVENTO (no expuesto al frontend) |

## 5. Services y lógica de negocio

| Flujo | Método | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Entrada | `registrarEntrada` | Origen válido, producto existe | `cantidadTotal += cantidad`, crea movimiento, notifica stock bajo | void |
| Salida | `registrarSalida` | Origen válido, stock suficiente | `cantidadTotal -= cantidad`, crea movimiento, notifica stock bajo | void |
| Ajuste | `registrarAjuste` | Producto activo, cantidadFisica >= reservas activas, diferencia != 0 | `cantidadTotal = cantidadFisica`, crea movimiento, auditoría, notifica stock bajo | void |
| Stock | `obtenerStock` | Producto existe | — | StockProductoResponseDTO |
| Historial | `obtenerMovimientosPorProducto` | Producto existe | — | List\<MovimientoResponseDTO\> |
| Todos | `obtenerTodosMovimientos` | — | — | Page\<MovimientoResponseDTO\> |

### Flujo: registrarEntrada

1. Valida `tipoOrigen` ∈ {REPOSICION, AJUSTE_MANUAL}
2. Busca producto
3. `producto.cantidadTotal += cantidad`
4. Crea `MovimientoInventario` con `tipoMovimiento=ENTRADA`, asocia usuario autenticado
5. Verifica stock bajo → notifica si aplica

### Flujo: registrarSalida

1. Valida `tipoOrigen` ∈ {DAÑO, PERDIDA, AJUSTE_MANUAL}
2. Busca producto
3. Consulta `StockProducto` — valida `cantidadDisponible >= cantidad`
4. `producto.cantidadTotal -= cantidad`
5. Crea `MovimientoInventario` con `tipoMovimiento=SALIDA`
6. Verifica stock bajo → notifica si aplica

### Flujo: registrarAjuste

1. Busca producto, valida que esté ACTIVO
2. Consulta `eventoProductoRepository.sumCantidadReservadaActivaByProductoId` — suma de reservas en estados ACTIVA
3. Valida `cantidadFisica >= cantidadReservadaActiva` (no se puede ajustar por debajo de lo reservado)
4. Calcula `diferencia = cantidadFisica - cantidadTotal`
5. Si diferencia == 0 → no hace nada
6. Si diferencia > 0 → entrada por AJUSTE_MANUAL
7. Si diferencia < 0 → salida por AJUSTE_MANUAL (valor absoluto)
8. Registra auditoría doble (PRODUCTO + MOVIMIENTO_INVENTARIO)
9. Verifica stock bajo

### verificarYNotificarStockBajo (privado)

- Después de cada entrada/salida/ajuste
- Consulta `StockProducto` por ID
- Si `stockBajoAlerta == true` → `notificacionService.notificarAdmins("ALERTA_STOCK", mensaje, null)`

## 6. Estados y transiciones

No aplica. Este módulo no maneja estados propios.

## 7. Excepciones y respuestas de error

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `ProductoNotFoundException` | productoId no existe | 404 | "Producto no encontrado con ID: X" | Redirigir |
| `IllegalArgumentException` | Origen inválido | 400 | "Origen inválido para entrada/salida: X. Válidos: [...]" | Corregir tipoOrigen |
| `IllegalArgumentException` | Producto inactivo en ajuste | 400 | "No se puede ajustar stock de un producto inactivo: X" | Reactivar producto primero |
| `StockInsuficienteException` | Salida > disponible | 400 | "Stock insuficiente para el producto X. Disponible: Y, requerido: Z" | Mostrar mensaje con números |
| `ReservaActivaInsuficienteException` | Ajuste < reservado | 400 | Mensaje descriptivo con nombre, cantidadFisica, cantidadReservada | No permitir ajuste por debajo de reservas |
| `MethodArgumentNotValidException` | `@Valid` falla | 400 | "campo: mensaje" | Mostrar error en el campo |

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| `inventario/producto` | Repository | Todos los flujos | Leer/modificar Producto, consultar StockProducto | Alto |
| `evento` (EventoProductoRepository) | Repository | `registrarAjuste` | Sumar reservas activas para validar ajuste mínimo | Medio |
| `notificacion` | Service | `verificarYNotificarStockBajo` | Notificar ALERTA_STOCK | Bajo |
| `common` (AuditoriaService) | Service | `registrarAjuste` | Registrar auditoría de ajuste | Bajo |
| `authentication/user` | Repository | `registrarMovimiento` | Asociar usuario autenticado al movimiento | Bajo |

```text
Dependencia circular:
- Detectada: No
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| POST entrada | Incrementa `producto.cantidadTotal`, crea movimiento, notifica si stock bajo | producto, notificacion | stock, historial, listado productos, notificaciones |
| POST salida | Decrementa `producto.cantidadTotal`, crea movimiento, notifica si stock bajo | producto, notificacion | stock, historial, listado productos, notificaciones |
| POST ajuste | Reemplaza `producto.cantidadTotal`, crea movimiento, doble auditoría, notifica si stock bajo | producto, common, notificacion | stock, historial, listado productos, auditoría, notificaciones |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Entrada | Siempre | — | |
| Salida | Stock disponible >= cantidad | Stock insuficiente | "Stock insuficiente. Disponible: X" |
| Ajuste | Producto ACTIVO, cantidadFisica >= reservas | Producto INACTIVO, cantidadFisica < reservas | "No se puede ajustar por debajo de las reservas activas (X unidades)" |

### 10.2 Tipos de origen por acción

| Acción | tipoOrigen | Default |
|---|---|---|
| Entrada | REPOSICION, AJUSTE_MANUAL | REPOSICION |
| Salida | DAÑO, PERDIDA, AJUSTE_MANUAL | DAÑO |
| Ajuste | AJUSTE_MANUAL (fijo) | — |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Cualquier POST (entrada/salida/ajuste) | `GET /api/inventario/productos/{id}/stock`, `GET /api/inventario/productos/{id}/movimientos`, `GET /api/productos` |
| Ajuste | Además: `GET /api/auditoria` (si hay vista de auditoría) |

### 10.4 Reglas importantes

- `StockProductoResponseDTO.cantidadDisponible` es el número real para validaciones de stock. No usar `ProductoResponseDTO.cantidadTotal`.
- El ajuste por conteo físico calcula la diferencia automáticamente. El frontend solo envía `cantidadFisica` (lo que se contó) y el `motivo`.
- No se puede ajustar el stock por debajo de la cantidad reservada activa. Si hay 5 unidades reservadas, `cantidadFisica` debe ser >= 5.
- Los movimientos de tipo EVENTO (salida por finalizar evento) son generados internamente por `EventoStockHandler`. El frontend no los crea directamente.
- Cada movimiento de entrada/salida/ajuste verifica automáticamente si el producto quedó en stock bajo y envía notificación si aplica.

---

## Resumen para frontend

- 3 endpoints POST para modificar stock: entrada, salida, ajuste. Todos retornan 204.
- 3 endpoints GET: stock real (vista), historial por producto, todos los movimientos paginados.
- El ajuste calcula la diferencia automáticamente — solo enviar `cantidadFisica` y `motivo`.
- Stock real se consulta en `GET /api/inventario/productos/{id}/stock`. Usar `cantidadDisponible` para validaciones.
- No se puede ajustar stock por debajo de las reservas activas.
- Cada operación verifica stock bajo y puede generar notificación automática.
- Los movimientos de tipo EVENTO son generados por el sistema, no por el frontend.
