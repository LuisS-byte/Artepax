# inventario/decoracion — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Gestiona los paquetes de decoración (conjuntos de productos) que se ofrecen a los clientes. Cada decoración define un precio base y una lista de productos con cantidades requeridas. El precio base se copia al evento al crearlo.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | CRUD de decoraciones y su composición de productos |
| Controller principal | `DecoracionController` (`/api/decoraciones`) |
| Service principal | `DecoracionServiceImpl` |
| Entidades principales | `Decoracion`, `DecoracionProducto` (tabla puente `decoracionxproducto`) |
| Tablas/vistas implicadas | `decoracion`, `decoracionxproducto`, `producto`, `stock_producto` |
| Módulos relacionados | `inventario/producto` (StockProducto, Producto) |
| Tipo de módulo | CRUD con validación de stock |

## 2. Controllers y endpoints completos

Todos requieren `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Service | Auth | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/decoraciones` | Crear decoración | `DecoracionRequestDTO` | `DecoracionResponseDTO` (201) | `crearDecoracion` | ADMIN | Valida stock de todos los productos |
| GET | `/api/decoraciones` | Listar todas | `?estado=` (opcional) | `List<DecoracionResponseDTO>` | `obtenerTodas` | ADMIN | Filtro opcional por estado |
| GET | `/api/decoraciones/{id}` | Obtener una | — | `DecoracionResponseDTO` | `obtenerPorId` | ADMIN | Incluye productos |
| PUT | `/api/decoraciones/{id}` | Actualizar | `DecoracionUpdateDTO` | `DecoracionResponseDTO` | `actualizar` | ADMIN | Si vienen productos, reemplaza todos |
| DELETE | `/api/decoraciones/{id}` | Desactivar | — | `204 No Content` | `eliminar` | ADMIN | Soft delete: estado → INACTIVO |

### POST /api/decoraciones

- **Propósito:** Crear una decoración con su lista de productos asociados.
- **Request body:** `DecoracionRequestDTO`
- **Response:** `DecoracionResponseDTO` con HTTP 201
- **Validaciones principales:**
  - `nombre` obligatorio (`@NotBlank`)
  - `precio` obligatorio (`@NotNull`)
  - `productos` no vacío (`@NotEmpty`)
  - Por cada producto: `productoId` obligatorio, `cantidadRequerida > 0`
  - Stock validado contra `StockProducto`: rechaza si stock crítico (`stockBajoAlerta`) o insuficiente
- **Excepciones posibles:**
  - `IllegalArgumentException` (400): "No se puede usar el producto X porque está en stock crítico"
  - `IllegalArgumentException` (400): "Stock insuficiente para el producto X. Disponible: Y, requerido: Z"
  - `ProductoNotFoundException` (404)
- **Efectos secundarios:** ninguna notificación ni auditoría
- **Qué debe refrescar el frontend:** listado de decoraciones

### GET /api/decoraciones

- **Propósito:** Listar decoraciones, opcionalmente filtradas por estado.
- **Query params:** `estado` (String, opcional) — por ejemplo `?estado=ACTIVO`
- **Response:** `List<DecoracionResponseDTO>` con productos incluidos
- **Validaciones principales:** ninguna
- **Excepciones posibles:** ninguna
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/decoraciones/{id}

- **Propósito:** Obtener una decoración con sus productos.
- **Response:** `DecoracionResponseDTO`
- **Excepciones posibles:** `DecoracionNotFoundException` (404) — "Decoración no encontrada con ID: X"
- **Qué debe refrescar el frontend:** NO APLICA

### PUT /api/decoraciones/{id}

- **Propósito:** Actualizar datos de la decoración. Si se envían productos, se reemplaza toda la lista (borra asociaciones anteriores y crea nuevas).
- **Request body:** `DecoracionUpdateDTO` (todos los campos opcionales excepto `nombre` requerido)
- **Validaciones principales:**
  - Si `productos` no es null ni vacío → valida stock de cada producto (misma lógica que en crear)
  - Los productos anteriores se eliminan y se crean los nuevos
- **Excepciones posibles:**
  - `DecoracionNotFoundException` (404)
  - `IllegalArgumentException` (400): errores de stock
  - `ProductoNotFoundException` (404)
- **Efectos secundarios:** Reemplazo total de `decoracionxproducto`
- **Qué debe refrescar el frontend:** detalle de la decoración, listado de decoraciones

### DELETE /api/decoraciones/{id}

- **Propósito:** Desactivar una decoración (soft delete).
- **Response:** `204 No Content`
- **Validaciones principales:** ninguna (solo existencia)
- **Excepciones posibles:** `DecoracionNotFoundException` (404)
- **Efectos secundarios:** `estado = "INACTIVO"`. No elimina las asociaciones con productos.
- **Qué debe refrescar el frontend:** listado de decoraciones

## 3. DTOs request/response

### 3.1 DTOs Request

#### DecoracionRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| nombre | String | string | Sí (`@NotBlank`) | No | | |
| descripcion | String | string | No | Sí | | |
| precio | BigDecimal | number | Sí (`@NotNull`) | No | | Precio de venta de la decoración |
| estado | String | string | No | Sí | | Si no se envía, el mapper define default |
| productos | List\<DecoracionProductoRequestDTO\> | array | Sí (`@NotEmpty`) | No | Al menos 1 producto | |

#### DecoracionProductoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| productoId | Long | number | Sí (`@NotNull`) | No | Producto debe existir | |
| cantidadRequerida | Integer | number | Sí (`@NotNull`, `@Positive`) | No | > 0 | Cantidad necesaria para esta decoración |

#### DecoracionUpdateDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| nombre | String | string | Sí (`@NotBlank`) | No | | |
| descripcion | String | string | No | Sí | | |
| precio | BigDecimal | number | No | Sí | | |
| estado | String | string | No | Sí | | |
| productos | List\<DecoracionProductoRequestDTO\> | array | No | Sí | Si se envía, reemplaza TODOS los productos existentes | Si es null o vacío, mantiene los actuales |

### 3.2 DTOs Response

#### DecoracionResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| id | Long | number | No | entidad | |
| nombre | String | string | No | entidad | |
| descripcion | String | string | Sí | entidad | |
| precio | BigDecimal | number | No | entidad | Este valor se copia a `evento.precioBase` al crear evento |
| imagenUrl | String | string (URL) | Sí | entidad (`decor_imagen_url`) | |
| estado | String | string | No | entidad | ACTIVO / INACTIVO |
| fechaCreacion | LocalDateTime | ISO datetime | No | entidad | |
| productos | List\<DecoracionProductoResponseDTO\> | array | No | tabla puente | |

#### DecoracionProductoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| productoId | Long | number | No | producto.id | |
| productoNombre | String | string | No | producto.nombre | Denormalizado del producto |
| cantidadRequerida | Integer | number | No | decoracionxproducto | |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Decoracion` | `decoracion` | Tabla operativa | Paquete de decoración | `decor_precio` ≥ 0 (CHECK). `decor_estado`: ACTIVO / INACTIVO |
| `DecoracionProducto` | `decoracionxproducto` | Relación (puente) | Composición de productos por decoración | PK compuesta: `(id_decoracion, id_producto)`. `cantidad_requerida > 0` (CHECK) |
| — | `producto` | Referencia | Producto asociado | Solo lectura desde este módulo |
| — | `stock_producto` | Vista (entity) | Stock disponible | Usado solo para validación al crear/actualizar |

**Nota DB:** El precio de la decoración (`decor_precio`) debe ser ≥ 0 según CHECK `chk_decoracion_precio`. No hay validación en el DTO — la DB la impone.

## 5. Services y lógica de negocio

| Flujo | Método | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Crear | `crearDecoracion` | Stock de productos (no crítico, suficiente), nombre/precio/productos obligatorios | Crea Decoracion + DecoracionProducto | DecoracionResponseDTO |
| Listar | `obtenerTodas` | — | — | List\<DecoracionResponseDTO\> |
| Obtener | `obtenerPorId` | Existencia | — | DecoracionResponseDTO |
| Actualizar | `actualizar` | Stock si hay productos nuevos. Nombre obligatorio | Actualiza Decoracion. Si hay productos: borra todos y crea nuevos | DecoracionResponseDTO |
| Eliminar | `eliminar` | Existencia | `estado = "INACTIVO"` (soft delete) | void |

### Flujo: crearDecoracion

1. `validarStockProductos(dto.getProductos())` — por cada producto:
   - Consulta `StockProducto` por ID
   - Rechaza si `stockBajoAlerta = true` → "No se puede usar el producto X porque está en stock crítico"
   - Rechaza si `cantidadDisponible < cantidadRequerida` → "Stock insuficiente..."
2. Mappea DTO a entidad Decoracion
3. Guarda Decoracion
4. Por cada producto en el DTO: busca Producto, crea DecoracionProducto con `cantidadRequerida`
5. Guarda todos los DecoracionProducto
6. Retorna DTO con productos incluidos

### Flujo: actualizar

1. Busca Decoracion por ID
2. Actualiza campos desde DTO (nombre, descripcion, precio, estado)
3. Si `dto.getProductos() != null && !isEmpty()`:
   - `validarStockProductos(dto.getProductos())` — misma validación que en crear
   - Elimina TODAS las asociaciones anteriores (`deleteAll`)
   - Crea nuevas asociaciones
4. Retorna DTO

### validarStockProductos (método privado)

- Agrupa cantidades por `productoId` (suma si hay duplicados)
- Consulta `StockProductoRepository.findAllByIdProductoIn(ids)`
- Por cada producto requerido:
  - Si no tiene stock → error
  - Si `stockBajoAlerta = true` → error (stock crítico)
  - Si `cantidadDisponible < requerida` → error (insuficiente)

## 6. Estados y transiciones

Estados de Decoracion: `ACTIVO`, `INACTIVO`.

| Estado | Significado | Visible en frontend | Notas |
|---|---|---|---|
| ACTIVO | Disponible para nuevos eventos | Sí (badge success) | |
| INACTIVO | Desactivado (soft delete) | Sí (badge secondary) | No debería aparecer en selectores de nuevo evento |

No hay transiciones complejas. El DELETE solo cambia a INACTIVO. El PUT puede cambiar entre ACTIVO/INACTIVO directamente.

## 7. Excepciones y respuestas de error

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `DecoracionNotFoundException` | ID no existe | 404 | "Decoración no encontrada con ID: X" | Redirigir |
| `ProductoNotFoundException` | productoId no existe | 404 | "Producto no encontrado: X" | Verificar catálogo de productos |
| `IllegalArgumentException` | Stock crítico | 400 | "No se puede usar el producto X porque está en stock crítico" | Mostrar mensaje, sugerir otro producto |
| `IllegalArgumentException` | Stock insuficiente | 400 | "Stock insuficiente para el producto X. Disponible: Y, requerido: Z" | Mostrar mensaje con números |
| `IllegalArgumentException` | Producto sin stock | 400 | "No se encontró stock para el producto con ID: X" | Verificar producto |
| `MethodArgumentNotValidException` | `@Valid` falla | 400 | "campo: mensaje" | Mostrar error en el campo |

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| `inventario/producto` (ProductoRepository) | Repository | `crearDecoracion`, `actualizar` | Validar existencia de producto | Bajo |
| `inventario/producto` (StockProductoRepository) | Repository | `validarStockProductos` | Validar stock disponible, crítico | Medio: depende de vista `stock_producto` |

```text
Dependencia circular:
- Detectada: No
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| POST crear | Crea filas en `decoracionxproducto` | inventario | listado decoraciones |
| PUT actualizar (con productos) | Elimina y recrea filas en `decoracionxproducto` | inventario | detalle decoración |
| DELETE eliminar | `estado = "INACTIVO"` | — | listado decoraciones |

**Importante:** Cambiar el precio de una decoración NO actualiza eventos existentes. El `precioBase` del evento se copia al crear y no se sincroniza.

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Seleccionar en nuevo evento | Decoracion ACTIVA | Decoracion INACTIVA | No mostrar en dropdown |
| Editar | Siempre | — | |
| Eliminar (desactivar) | Siempre | — | Confirmar: "¿Desactivar esta decoración?" |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| Todos | Siempre | — | No hay restricciones de estado para edición |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| POST crear | `GET /api/decoraciones` |
| PUT actualizar | `GET /api/decoraciones/{id}`, `GET /api/decoraciones` |
| DELETE eliminar | `GET /api/decoraciones` |

### 10.4 Reglas importantes

- La validación de stock en crear/actualizar usa `StockProducto` (vista con disponibilidad real). Si un producto está en stock crítico, no se puede usar en ninguna decoración.
- Al actualizar una decoración con nueva lista de productos, se ELIMINAN todas las asociaciones anteriores y se crean las nuevas. Si el frontend quiere agregar un producto sin borrar los existentes, debe enviar la lista completa incluyendo los que ya tenía.
- El precio de la decoración se copia al evento al crearlo. Cambiar el precio después NO afecta eventos existentes.
- Decoraciones INACTIVAS no deben aparecer en selectores de creación de evento. El backend valida que la decoración esté ACTIVA al crear/editar evento (`validarDecoracionActiva` en EventoServiceImpl).

---

## Resumen para frontend

- CRUD estándar en `/api/decoraciones`. El DELETE es soft delete (INACTIVO).
- Cada decoración tiene una lista de productos con `cantidadRequerida`. Se validan contra stock real al crear/actualizar.
- Al actualizar productos, se reemplaza toda la lista. Enviar la lista completa (no solo el nuevo).
- Stock crítico o insuficiente bloquea la creación/actualización. El backend devuelve mensajes descriptivos.
- Decoraciones INACTIVAS no deben aparecer en selectores de eventos.
- El precio de la decoración es el `precioBase` que se copia al evento.
