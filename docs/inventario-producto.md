# inventario/producto — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Gestiona los productos del inventario físico: nombre, cantidad total, precio unitario, stock mínimo y estado. La disponibilidad real (descontando reservas) se consulta desde la vista `vista_stock_producto`. Este módulo no expone endpoints de stock — solo CRUD de productos.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | CRUD de productos del inventario |
| Controller principal | `ProductoController` (`/api/productos`) |
| Service principal | `ProductoServiceImpl` |
| Entidades principales | `Producto` (tabla `producto`), `StockProducto` (vista `vista_stock_producto`) |
| Tablas/vistas implicadas | `producto`, `vista_stock_producto` |
| Módulos relacionados | `inventario/decoracion` (usa StockProducto para validación), `evento` (usa StockProducto para reservas) |
| Tipo de módulo | CRUD + Catálogo |

## 2. Controllers y endpoints completos

Todos requieren `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Service | Auth | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/productos` | Crear producto | `ProductoRequestDTO` | `ProductoResponseDTO` (201) | `crearProducto` | ADMIN | Valida nombre único |
| GET | `/api/productos` | Listar todos | — | `List<ProductoResponseDTO>` | `obtenerTodos` | ADMIN | Sin paginación |
| GET | `/api/productos/{id}` | Obtener uno | — | `ProductoResponseDTO` | `obtenerPorId` | ADMIN | |
| PUT | `/api/productos/{id}` | Actualizar | `ProductoUpdateDTO` | `ProductoResponseDTO` | `actualizar` | ADMIN | |
| GET | `/api/productos/buscar?nombre=` | Buscar por nombre | `nombre` (query) | `List<ProductoResponseDTO>` | `buscarPorNombre` | ADMIN | Case-insensitive, contiene |
| DELETE | `/api/productos/{id}` | Desactivar | — | `204 No Content` | `eliminar` | ADMIN | Soft delete: estado → INACTIVO |
| PATCH | `/api/productos/{id}/reactivar` | Reactivar | — | `ProductoResponseDTO` | `reactivar` | ADMIN | estado → ACTIVO |

### POST /api/productos

- **Propósito:** Crear un nuevo producto en el inventario.
- **Request body:** `ProductoRequestDTO`
- **Response:** `ProductoResponseDTO` con HTTP 201
- **Validaciones principales:**
  - `nombre` obligatorio (`@NotBlank`)
  - `cantidadTotal` obligatorio (`@NotNull`, `@Positive` — > 0)
  - Nombre único: si ya existe → `ProductoDuplicadoException` (409)
- **Excepciones posibles:**
  - `ProductoDuplicadoException` (409): "Ya existe un producto con el nombre: X"
  - `MethodArgumentNotValidException` (400): errores de validación DTO
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** listado de productos

### GET /api/productos

- **Propósito:** Listar todos los productos sin filtros.
- **Response:** `List<ProductoResponseDTO>` (sin paginación)
- **Validaciones principales:** ninguna
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/productos/{id}

- **Propósito:** Obtener un producto por ID.
- **Response:** `ProductoResponseDTO`
- **Excepciones posibles:** `ProductoNotFoundException` (404) — "Producto no encontrado con ID: X"
- **Qué debe refrescar el frontend:** NO APLICA

### PUT /api/productos/{id}

- **Propósito:** Actualizar datos de un producto.
- **Request body:** `ProductoUpdateDTO` (`nombre` requerido, resto opcional)
- **Validaciones principales:** `nombre` obligatorio (`@NotBlank`)
- **Excepciones posibles:** `ProductoNotFoundException` (404)
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** listado de productos, detalle

### GET /api/productos/buscar?nombre=X

- **Propósito:** Buscar productos cuyo nombre contenga el texto (case-insensitive).
- **Query params:** `nombre` (String, requerido)
- **Response:** `List<ProductoResponseDTO>`
- **Validaciones principales:** ninguna
- **Qué debe refrescar el frontend:** NO APLICA

### DELETE /api/productos/{id}

- **Propósito:** Desactivar un producto (soft delete).
- **Response:** `204 No Content`
- **Excepciones posibles:** `ProductoNotFoundException` (404)
- **Efectos secundarios:** `estado = "INACTIVO"`. No elimina físicamente.
- **Qué debe refrescar el frontend:** listado de productos

### PATCH /api/productos/{id}/reactivar

- **Propósito:** Reactivar un producto previamente desactivado.
- **Response:** `ProductoResponseDTO`
- **Excepciones posibles:** `ProductoNotFoundException` (404)
- **Efectos secundarios:** `estado = "ACTIVO"`
- **Qué debe refrescar el frontend:** listado de productos

## 3. DTOs request/response

### 3.1 DTOs Request

#### ProductoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| nombre | String | string | Sí (`@NotBlank`) | No | Único en BD | |
| descripcion | String | string | No | Sí | | |
| cantidadTotal | Integer | number | Sí (`@NotNull`, `@Positive`) | No | > 0 | Stock físico total |
| estado | String | string | No | Sí | | Si no se envía, default del sistema |

#### ProductoUpdateDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| nombre | String | string | Sí (`@NotBlank`) | No | | |
| descripcion | String | string | No | Sí | | |
| estado | String | string | No | Sí | | ACTIVO / INACTIVO |
| precioUnitario | BigDecimal | number | No | Sí | | Costo unitario del producto |
| stockMinimo | Integer | number | No | Sí | | Umbral para alerta de stock bajo |

### 3.2 DTOs Response

#### ProductoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| id | Long | number | No | entidad | |
| nombre | String | string | No | entidad | |
| descripcion | String | string | Sí | entidad | |
| cantidadTotal | Integer | number | No | entidad | Stock físico. NO es la disponibilidad real (ver `StockProducto`) |
| precioUnitario | BigDecimal | number | No | entidad | Costo unitario |
| stockMinimo | Integer | number | No | entidad | Umbral de alerta |
| estado | String | string | No | entidad | ACTIVO / INACTIVO |
| fechaCreacion | LocalDateTime | ISO datetime | No | entidad | |

#### StockProductoResponseDTO (NO expuesto por ProductoController)

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| idProducto | Long | number | No | vista | |
| nombre | String | string | No | vista | |
| cantidadTotal | Integer | number | No | vista | Stock físico |
| cantidadReservada | Integer | number | No | vista | Suma de reservas activas |
| cantidadDisponible | Integer | number | No | vista | `cantidadTotal - cantidadReservada` |
| stockMinimo | Integer | number | No | vista | |
| stockBajoAlerta | Boolean | boolean | No | vista | TRUE cuando `cantidadDisponible <= stockMinimo` |

**Nota:** `StockProductoResponseDTO` existe en el código pero no está expuesto en ningún endpoint del controlador de producto. Es usado internamente por `DecoracionServiceImpl` y `EventoStockHandler`. Si el frontend necesita datos de stock, debe agregarse un endpoint.

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Producto` | `producto` | Tabla operativa | Inventario físico | `produ_cantidad_total` ≥ 0, `produ_precio_unitario` ≥ 0, `produ_stock_minimo` ≥ 0 (CHECKs) |
| `StockProducto` | `vista_stock_producto` | Vista (`@Immutable`) | Disponibilidad real descontando reservas | Solo lectura. `cantidad_disponible = cantidad_total - SUM(reservas activas)`. `stock_bajo_alerta` calculado |

**Diferencia clave:** `Producto.cantidadTotal` es el stock físico. `StockProducto.cantidadDisponible` es lo que realmente está libre (físico menos reservado). Para mostrar disponibilidad real, usar `StockProducto`, no `Producto`.

## 5. Services y lógica de negocio

| Flujo | Método | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Crear | `crearProducto` | Nombre único | Crea Producto | ProductoResponseDTO |
| Listar | `obtenerTodos` | — | — | List\<ProductoResponseDTO\> |
| Obtener | `obtenerPorId` | Existencia | — | ProductoResponseDTO |
| Actualizar | `actualizar` | Existencia | Actualiza campos (nombre, descripcion, estado, precioUnitario, stockMinimo) | ProductoResponseDTO |
| Eliminar | `eliminar` | Existencia | `estado = "INACTIVO"` | void |
| Reactivar | `reactivar` | Existencia | `estado = "ACTIVO"` | ProductoResponseDTO |
| Buscar | `buscarPorNombre` | — | — | List\<ProductoResponseDTO\> |

### Flujo: crearProducto

1. Verifica que no exista otro producto con el mismo nombre (`existsByNombre`)
2. Si existe → `ProductoDuplicadoException` (409)
3. Mappea DTO a entidad, guarda, retorna DTO

### Flujo: actualizar

1. Busca producto por ID
2. Mappea campos del DTO a la entidad (`updateEntity`)
3. Guarda y retorna

**Campos actualizables:** `nombre`, `descripcion`, `estado`, `precioUnitario`, `stockMinimo`. `cantidadTotal` NO es actualizable por este endpoint (se modifica vía `movimiento_inventario`).

## 6. Estados y transiciones

Estados de Producto: `ACTIVO`, `INACTIVO`.

| Estado | Significado | Visible en frontend | Notas |
|---|---|---|---|
| ACTIVO | Disponible para uso | Sí (badge success) | |
| INACTIVO | Desactivado (soft delete) | Sí (badge secondary) | No debería usarse en nuevas decoraciones. Eventos existentes conservan la referencia |

Transiciones: DELETE → INACTIVO. PATCH reactivar → ACTIVO. PUT puede cambiar entre ambos directamente.

## 7. Excepciones y respuestas de error

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `ProductoNotFoundException` | ID no existe | 404 | "Producto no encontrado con ID: X" | Redirigir |
| `ProductoDuplicadoException` | Nombre ya existe | 409 | "Ya existe un producto con el nombre: X" | Mostrar error, sugerir otro nombre |
| `MethodArgumentNotValidException` | `@Valid` falla | 400 | "campo: mensaje" | Mostrar error en el campo |

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| Ninguno | — | — | — | ProductoServiceImpl solo usa ProductoRepository y ProductoMapper |

```text
Dependencia circular:
- Detectada: No
```

**Otros módulos que dependen de producto:**
- `inventario/decoracion` → `ProductoRepository`, `StockProductoRepository` (validación de stock)
- `evento` → `ProductoRepository` (extras), `StockProducto` (reservas vía EventoStockHandler)

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| POST crear | — | — | listado productos |
| PUT actualizar | — | — | listado productos, detalle |
| DELETE eliminar | `estado = "INACTIVO"` | — | listado productos |
| PATCH reactivar | `estado = "ACTIVO"` | — | listado productos |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Seleccionar en decoración/evento | Producto ACTIVO | Producto INACTIVO | No mostrar en dropdown |
| Editar | Siempre | — | |
| Eliminar (desactivar) | Siempre | — | Confirmar: "¿Desactivar este producto?" |
| Reactivar | Producto INACTIVO | Producto ACTIVO | Ocultar botón si ya está activo |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| nombre, descripcion, estado, precioUnitario, stockMinimo | Siempre (PUT) | — | |
| cantidadTotal | NO por este endpoint | — | Se modifica vía movimientos de inventario |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| POST crear | `GET /api/productos` |
| PUT actualizar | `GET /api/productos/{id}`, `GET /api/productos` |
| DELETE eliminar | `GET /api/productos` |
| PATCH reactivar | `GET /api/productos` |

### 10.4 Reglas importantes

- `ProductoResponseDTO.cantidadTotal` es el stock físico. Para saber cuánto está realmente disponible, se necesita la vista `vista_stock_producto` (que descuenta reservas activas). Actualmente no hay endpoint que exponga esta vista al frontend.
- Productos INACTIVOS no deben aparecer en selectores de decoración o extras de evento.
- `precioUnitario` es el costo del producto (no precio de venta). Se usa en cálculos de rentabilidad.
- `stockMinimo` define el umbral para la alerta `stockBajoAlerta` en la vista.
- No se puede crear un producto con nombre duplicado (error 409).

---

## Resumen para frontend

- CRUD estándar en `/api/productos`. DELETE es soft delete (INACTIVO). PATCH `/reactivar` para volver a ACTIVO.
- `cantidadTotal` no se edita directamente — se gestiona vía movimientos de inventario.
- `StockProducto` (vista de disponibilidad real) no tiene endpoint expuesto. Si se necesita en frontend, debe agregarse.
- Búsqueda por nombre: `GET /api/productos/buscar?nombre=X` — case-insensitive, tipo "contiene".
- Productos INACTIVOS no deben aparecer en selectores de decoraciones o extras.
- Nombre de producto es único — si se duplica, error 409.
