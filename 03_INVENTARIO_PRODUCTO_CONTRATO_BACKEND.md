# Inventario — Producto — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El módulo Producto administra el catálogo base de productos físicos del inventario. Permite crear, listar, consultar, actualizar, buscar, desactivar y reactivar productos. Este módulo representa el stock físico (`cantidadTotal`) y los metadatos operativos del producto, pero no registra movimientos de inventario ni reservas de eventos.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Administrar productos físicos del inventario. |
| Controller principal | `ProductoController` |
| Service principal | `ProductoService` / `ProductoServiceImpl` |
| Entidades principales | `Producto`, `StockProducto` |
| Tablas/vistas implicadas | `producto`, `vista_stock_producto` |
| Módulos relacionados | Evento, Decoración, Movimiento de inventario |
| Tipo de módulo | CRUD / Catálogo operativo de inventario |

El frontend debe tratar este módulo como la fuente de administración de productos, no como la fuente definitiva de disponibilidad operativa. La disponibilidad real debe salir de `vista_stock_producto` cuando exista endpoint que la exponga.

## 2. Controllers y endpoints completos

Base path:

```text
/api/productos
```

Seguridad declarada en controller:

```text
@PreAuthorize("hasRole('ADMIN')")
```

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/productos` | Crear producto | `ProductoRequestDTO` | `ProductoResponseDTO` | `crearProducto()` | ADMIN | Retorna 201. |
| GET | `/api/productos` | Listar productos | No aplica | `List<ProductoResponseDTO>` | `obtenerTodos()` | ADMIN | No filtra por estado en backend. |
| GET | `/api/productos/{id}` | Obtener producto por ID | No aplica | `ProductoResponseDTO` | `obtenerPorId()` | ADMIN | Lanza 404 si no existe. |
| PUT | `/api/productos/{id}` | Actualizar producto | `ProductoUpdateDTO` | `ProductoResponseDTO` | `actualizar()` | ADMIN | Actualiza campos no nulos del DTO. |
| GET | `/api/productos/buscar?nombre={nombre}` | Buscar productos por nombre | Query param `nombre` | `List<ProductoResponseDTO>` | `buscarPorNombre()` | ADMIN | Búsqueda `contains ignore case`. |
| DELETE | `/api/productos/{id}` | Desactivar producto | No aplica | `204 No Content` | `eliminar()` | ADMIN | Baja lógica: `estado = INACTIVO`. |
| PATCH | `/api/productos/{id}/reactivar` | Reactivar producto | No aplica | `ProductoResponseDTO` | `reactivar()` | ADMIN | Cambia `estado = ACTIVO`. |

### POST `/api/productos`

- Propósito: crear un producto nuevo.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: `ProductoRequestDTO`.
- Response: `ProductoResponseDTO`.
- HTTP esperado: `201 Created`.
- Método del service: `ProductoService.crearProducto()`.
- Validaciones principales:
  - `nombre` obligatorio.
  - `cantidadTotal` obligatoria y mayor a cero.
  - no debe existir otro producto con el mismo nombre según `ProductoRepository.existsByNombre()`.
- Excepciones posibles:
  - `ProductoDuplicadoException`.
  - errores de validación DTO.
  - errores de integridad de datos.
- Efectos secundarios:
  - crea registro en `producto`.
- Qué debe refrescar el frontend:
  - listado de productos.
  - dashboard o resumen de inventario si existe.

### GET `/api/productos`

- Propósito: listar todos los productos.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<ProductoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `ProductoService.obtenerTodos()`.
- Validaciones principales: no aplica.
- Excepciones posibles:
  - error genérico si falla consulta.
- Efectos secundarios: ninguno.
- Qué debe refrescar el frontend:
  - no aplica; endpoint de lectura.

Nota frontend: el backend no filtra productos inactivos en este endpoint. Si la UI necesita separar activos/inactivos, debe usar el campo `estado` devuelto.

### GET `/api/productos/{id}`

- Propósito: obtener un producto específico.
- Path variables:
  - `id`: identificador del producto.
- Query params: no aplica.
- Request body: no aplica.
- Response: `ProductoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `ProductoService.obtenerPorId()`.
- Validaciones principales:
  - el producto debe existir.
- Excepciones posibles:
  - `ProductoNotFoundException`.
- Efectos secundarios: ninguno.
- Qué debe refrescar el frontend:
  - no aplica; endpoint de lectura.

### PUT `/api/productos/{id}`

- Propósito: actualizar datos administrativos del producto.
- Path variables:
  - `id`: identificador del producto.
- Query params: no aplica.
- Request body: `ProductoUpdateDTO`.
- Response: `ProductoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `ProductoService.actualizar()`.
- Validaciones principales:
  - `nombre` obligatorio por DTO.
  - producto debe existir.
- Excepciones posibles:
  - `ProductoNotFoundException`.
  - errores de validación DTO.
  - errores de integridad de datos.
- Efectos secundarios:
  - modifica el registro `producto`.
- Qué debe refrescar el frontend:
  - listado de productos.
  - detalle del producto.
  - vistas/resúmenes de inventario si muestran nombre, estado, precio o stock mínimo.

Nota: `ProductoMapper.updateEntity()` actualiza solo campos no nulos, pero `ProductoUpdateDTO.nombre` tiene `@NotBlank`, por lo que en la práctica el frontend debe enviar `nombre` no vacío en el PUT.

### GET `/api/productos/buscar?nombre={nombre}`

- Propósito: buscar productos por coincidencia parcial de nombre.
- Path variables: no aplica.
- Query params:
  - `nombre`: texto de búsqueda.
- Request body: no aplica.
- Response: `List<ProductoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `ProductoService.buscarPorNombre()`.
- Validaciones principales:
  - `nombre` debe venir como query param.
- Excepciones posibles:
  - error de query param faltante manejado por Spring/GlobalExceptionHandler.
- Efectos secundarios: ninguno.
- Qué debe refrescar el frontend:
  - no aplica; endpoint de lectura.

### DELETE `/api/productos/{id}`

- Propósito: desactivar producto.
- Path variables:
  - `id`: identificador del producto.
- Query params: no aplica.
- Request body: no aplica.
- Response: sin body.
- HTTP esperado: `204 No Content`.
- Método del service: `ProductoService.eliminar()`.
- Validaciones principales:
  - producto debe existir.
- Excepciones posibles:
  - `ProductoNotFoundException`.
- Efectos secundarios:
  - cambia `estado` a `INACTIVO`.
- Qué debe refrescar el frontend:
  - listado de productos.
  - detalle del producto si está abierto.
  - formularios que ofrecen productos seleccionables.

Importante: no es borrado físico. Es baja lógica.

### PATCH `/api/productos/{id}/reactivar`

- Propósito: reactivar un producto inactivo.
- Path variables:
  - `id`: identificador del producto.
- Query params: no aplica.
- Request body: no aplica.
- Response: `ProductoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `ProductoService.reactivar()`.
- Validaciones principales:
  - producto debe existir.
- Excepciones posibles:
  - `ProductoNotFoundException`.
- Efectos secundarios:
  - cambia `estado` a `ACTIVO`.
- Qué debe refrescar el frontend:
  - listado de productos.
  - detalle del producto.
  - formularios que ofrecen productos seleccionables.

## 3. DTOs request/response

### 3.1 DTOs Request

#### ProductoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `nombre` | `String` | `nombre` | Sí | No | `@NotBlank` | Nombre visible del producto. |
| `descripcion` | `String` | `descripcion` | No | Sí | No declarada | Texto descriptivo opcional. |
| `cantidadTotal` | `Integer` | `cantidadTotal` | Sí | No | `@NotNull`, `@Positive` | Stock físico inicial. Debe ser mayor a 0 al crear. |
| `estado` | `String` | `estado` | No | Sí | No declarada | Esperado funcionalmente: `ACTIVO` o `INACTIVO`. |

Campos no disponibles en creación:

```text
precioUnitario
stockMinimo
```

Aunque existen en entidad y response, no están en `ProductoRequestDTO`. En creación quedan con los valores por defecto de la entidad/BD si no se modifican por otro flujo.

#### ProductoUpdateDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `nombre` | `String` | `nombre` | Sí | No | `@NotBlank` | Debe enviarse aunque solo se quieran actualizar otros campos. |
| `descripcion` | `String` | `descripcion` | No | Sí | No declarada | Actualiza si viene no nulo. |
| `estado` | `String` | `estado` | No | Sí | No declarada | Esperado funcionalmente: `ACTIVO` o `INACTIVO`. |
| `precioUnitario` | `BigDecimal` | `precioUnitario` | No | Sí | No declarada | Actualiza costo/precio unitario del producto. |
| `stockMinimo` | `Integer` | `stockMinimo` | No | Sí | No declarada | Umbral para alerta de stock bajo. |

Nota: este DTO no incluye `cantidadTotal`. Los ajustes de stock físico no deben hacerse desde Producto; corresponden al submódulo Movimiento de inventario.

### 3.2 DTOs Response

#### ProductoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `producto.id_producto` | Identificador del producto. |
| `nombre` | `String` | `nombre` | No | `producto.produ_nombre` | Mostrar en tablas, selects y badges. |
| `descripcion` | `String` | `descripcion` | Sí | `producto.produ_descripcion` | Texto auxiliar. |
| `cantidadTotal` | `Integer` | `cantidadTotal` | No | `producto.produ_cantidad_total` | Stock físico, no disponibilidad real. |
| `precioUnitario` | `BigDecimal` | `precioUnitario` | No | `producto.produ_precio_unitario` | Usado para costos/rentabilidad. |
| `stockMinimo` | `Integer` | `stockMinimo` | No | `producto.produ_stock_minimo` | Umbral de alerta. |
| `estado` | `String` | `estado` | Sí | `producto.produ_estado` | Usar para activo/inactivo. |
| `fechaCreacion` | `LocalDateTime` | `fechaCreacion` | No | `producto.produ_creacion` | Fecha de alta. |

#### StockProductoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `idProducto` | `Long` | `idProducto` | No | `vista_stock_producto.id_producto` | Identificador del producto. |
| `nombre` | `String` | `nombre` | No | `vista_stock_producto.produ_nombre` | Nombre del producto. |
| `cantidadTotal` | `Integer` | `cantidadTotal` | No | `vista_stock_producto.cantidad_total` | Stock físico. |
| `cantidadReservada` | `Integer` | `cantidadReservada` | No | `vista_stock_producto.cantidad_reservada` | Reservas activas por eventos. |
| `cantidadDisponible` | `Integer` | `cantidadDisponible` | No | `vista_stock_producto.cantidad_disponible` | Disponibilidad operativa. |
| `stockMinimo` | `Integer` | `stockMinimo` | No | `vista_stock_producto.produ_stock_minimo` | Umbral mínimo. |
| `stockBajoAlerta` | `Boolean` | `stockBajoAlerta` | No | `vista_stock_producto.stock_bajo_alerta` | Flag visual para alerta. |

Importante: en el controller recibido no hay endpoint que retorne `StockProductoResponseDTO`. El DTO existe, pero el contrato HTTP de Producto no lo expone directamente.

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Producto` | `producto` | Tabla | Catálogo operativo de productos físicos. | Guarda stock físico, estado, precio unitario y stock mínimo. |
| `StockProducto` | `vista_stock_producto` | Vista | Resumen de disponibilidad operativa. | Solo lectura; calcula reservado, disponible y alerta. |

### Producto

Campos principales:

| Campo entidad | Columna SQL | Notas |
|---|---|---|
| `id` | `id_producto` | PK. |
| `nombre` | `produ_nombre` | Nombre único a nivel BD según documentación del esquema. |
| `descripcion` | `produ_descripcion` | Opcional. |
| `cantidadTotal` | `produ_cantidad_total` | Stock físico total. |
| `precioUnitario` | `produ_precio_unitario` | Costo/precio unitario para reportes. |
| `stockMinimo` | `produ_stock_minimo` | Umbral de alerta. |
| `estado` | `produ_estado` | Estado funcional del producto. |
| `fechaCreacion` | `produ_creacion` | Timestamp de creación. |

### StockProducto / vista_stock_producto

`StockProducto` mapea `vista_stock_producto` como entidad `@Immutable`. Debe tratarse como solo lectura. La vista resume:

```text
cantidad_total
cantidad_reservada
cantidad_disponible
stock_bajo_alerta
```

Para frontend, `cantidadTotal` no debe confundirse con `cantidadDisponible`. La disponibilidad real es `cantidadDisponible`.

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Crear producto | `crearProducto()` | Duplicado por nombre; validaciones DTO | Inserta `producto` | `ProductoResponseDTO` |
| Listar productos | `obtenerTodos()` | No aplica | Nada | Lista de `ProductoResponseDTO` |
| Obtener por ID | `obtenerPorId()` | Existencia | Nada | `ProductoResponseDTO` |
| Actualizar producto | `actualizar()` | Existencia; validaciones DTO | Campos no nulos del DTO | `ProductoResponseDTO` |
| Desactivar producto | `eliminar()` | Existencia | `estado = INACTIVO` | `void` |
| Reactivar producto | `reactivar()` | Existencia | `estado = ACTIVO` | `ProductoResponseDTO` |
| Buscar por nombre | `buscarPorNombre()` | No aplica | Nada | Lista de `ProductoResponseDTO` |

### Flujo: crear producto

1. Recibe `ProductoRequestDTO` validado.
2. Verifica si existe producto con el mismo nombre mediante `existsByNombre()`.
3. Si existe, lanza `ProductoDuplicadoException`.
4. Mapea DTO a entidad con `ProductoMapper.toEntity()`.
5. Guarda en `producto`.
6. Retorna `ProductoResponseDTO`.

### Flujo: listar productos

1. Ejecuta `productoRepository.findAll()`.
2. Mapea cada entidad con `ProductoMapper.toDTO()`.
3. Retorna lista.

No filtra por estado. El frontend recibe activos e inactivos.

### Flujo: obtener producto por ID

1. Busca por `id`.
2. Si no existe, lanza `ProductoNotFoundException`.
3. Retorna `ProductoResponseDTO`.

### Flujo: actualizar producto

1. Busca producto por `id`.
2. Si no existe, lanza `ProductoNotFoundException`.
3. Aplica `ProductoMapper.updateEntity()`.
4. Guarda entidad.
5. Retorna `ProductoResponseDTO`.

Regla importante: `cantidadTotal` no se actualiza por este endpoint. Ajustes de stock físico deben realizarse mediante Movimiento de inventario.

### Flujo: desactivar producto

1. Busca producto por `id`.
2. Si no existe, lanza `ProductoNotFoundException`.
3. Cambia `estado` a `INACTIVO`.
4. Guarda producto.
5. Retorna `204 No Content` desde controller.

### Flujo: reactivar producto

1. Busca producto por `id`.
2. Si no existe, lanza `ProductoNotFoundException`.
3. Cambia `estado` a `ACTIVO`.
4. Guarda producto.
5. Retorna `ProductoResponseDTO`.

### Flujo: buscar por nombre

1. Usa `findByNombreContainingIgnoreCase(nombre)`.
2. Mapea resultados a DTO.
3. Retorna lista.

## 6. Estados y transiciones

### Estados existentes

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `ACTIVO` | Producto disponible para operaciones nuevas. | `Producto.estado` | Sí | Debe permitirse selección en decoración/eventos si los otros módulos lo validan. |
| `INACTIVO` | Producto deshabilitado sin borrar historial. | `Producto.estado` | Sí | No debería ofrecerse para nuevas configuraciones. |

### Transiciones permitidas

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| `ACTIVO` | `INACTIVO` | `DELETE /api/productos/{id}` | Producto existe | Producto queda ocultable/bloqueable en UI. |
| `INACTIVO` | `ACTIVO` | `PATCH /api/productos/{id}/reactivar` | Producto existe | Producto vuelve a estar disponible. |
| Cualquiera | Valor enviado en PUT | `PUT /api/productos/{id}` | Producto existe; `nombre` no blank | El service acepta `estado` enviado en DTO; no hay validación explícita en service. |

No hay enum Java ni constante propia en este submódulo para `ACTIVO`/`INACTIVO`. El frontend debe tratar esos valores como contrato funcional mientras el backend no exponga catálogo.

## 7. Excepciones y respuestas de error

El sistema usa `ArtepaxException` como base para excepciones de dominio. El `GlobalExceptionHandler` responde con `ErrorResponse`:

| Campo | Significado |
|---|---|
| `codigo` | HTTP status numérico. |
| `mensaje` | Mensaje funcional o técnico saneado. |
| `errorCode` | Código opcional, puede venir `null`. |
| `timestamp` | Fecha/hora ISO local. |

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `ProductoDuplicadoException` | Crear producto con nombre existente. | `409 Conflict` | `Ya existe un producto con el nombre: {nombre}` | Mostrar error en campo nombre. |
| `ProductoNotFoundException` | Producto inexistente por ID. | `404 Not Found` | `Producto no encontrado con ID: {id}` | Mostrar “producto no encontrado” y volver al listado. |
| `ReservaActivaInsuficienteException` | Intento de ajustar stock por debajo de reservas activas. | `409 Conflict` | Indica físico y reservado activo. | Mostrar error operativo; no permitir guardar ajuste. |
| `StockInsuficienteException` | Operaciones de reserva/stock solicitan más de lo disponible. | `409 Conflict` | Indica disponible y solicitado. | Mostrar alerta de stock insuficiente. |
| `MethodArgumentNotValidException` | DTO inválido. | `400 Bad Request` | Primer error de campo con formato `campo: mensaje`. | Mostrar validación de formulario. |
| `DataIntegrityViolationException` | Violación de restricción SQL. | `409 Conflict` | `Error de integridad de datos` salvo caso especial de documentos. | Mostrar error general de integridad. |
| `AccessDeniedException` | Usuario sin rol permitido. | `403 Forbidden` | Mensaje de Spring Security. | Redirigir o bloquear acción. |
| `Exception` | Error no manejado. | `500 Internal Server Error` | `Error interno del servidor` | Mostrar error genérico. |

Nota: `ReservaActivaInsuficienteException` y `StockInsuficienteException` están en el paquete de producto, pero se usan principalmente por reglas de stock/reservas, no por los endpoints CRUD de `ProductoController`.

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| `ProductoRepository` | Repository propio | `ProductoServiceImpl` | CRUD y búsqueda de productos. | Bajo. |
| `StockProductoRepository` | Repository propio de vista | No usado por `ProductoServiceImpl` recibido | Consultar `vista_stock_producto`. | Bajo; existe pero no está expuesto por controller en esta tanda. |
| `ProductoMapper` | Mapper | `ProductoServiceImpl` | Convertir entidad/DTO. | Bajo. |
| `EventoStockHandler` | Dependencia inversa desde Evento hacia Producto | Módulo Evento | Reserva y consume stock usando productos. | Medio; cambios en producto afectan eventos. |
| `MovimientoInventario` | Relación funcional | Submódulo Movimiento | Ajusta stock físico. | Medio; cantidadTotal no debe manipularse desde frontend de producto. |
| `DecoracionProducto` | Relación funcional | Submódulo Decoración | Usa productos como composición de decoraciones. | Medio; productos inactivos no deben usarse en nuevas decoraciones. |
| `vista_stock_producto` | Vista SQL | `StockProducto` | Resumen de disponibilidad. | Bajo; solo lectura. |

Dependencia circular:

```text
Detectada: No
Detalle: ProductoServiceImpl solo depende de ProductoRepository y ProductoMapper.
Riesgo: Bajo.
Cómo se evita: El módulo Producto no inyecta servicios de Evento, Decoración o Movimiento.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Crear producto | Inserta producto físico. | Inventario / Decoración / Evento | Listado de productos; selects de productos; dashboard inventario. |
| Actualizar producto | Cambia nombre, descripción, estado, precio unitario o stock mínimo. | Inventario / Reportes / Decoración / Evento | Listado, detalle, dashboards, vistas de costos si aplica. |
| Desactivar producto | Marca `estado = INACTIVO`. | Decoración / Evento | Listado, selects de productos, formularios de decoración/evento. |
| Reactivar producto | Marca `estado = ACTIVO`. | Decoración / Evento | Listado, selects de productos. |
| Cambiar `precioUnitario` | Afecta cálculos futuros que usen costo unitario. | Reportes / Rentabilidad | Dashboard/reportes de rentabilidad si se recalculan contra producto. |
| Cambiar `stockMinimo` | Afecta alerta de stock bajo. | Inventario / Notificaciones | Dashboard de stock y alertas visuales. |

No se detectan efectos secundarios directos de notificación, auditoría o movimientos dentro de `ProductoServiceImpl` recibido.

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Crear producto | Usuario ADMIN y formulario válido. | Nombre vacío, cantidad no positiva. | “Complete los campos obligatorios.” |
| Editar producto | Producto existente. | Nombre vacío. | “El nombre es obligatorio.” |
| Desactivar producto | Producto existente y preferiblemente `ACTIVO`. | Producto inexistente. | “No se encontró el producto.” |
| Reactivar producto | Producto existente y preferiblemente `INACTIVO`. | Producto inexistente. | “No se encontró el producto.” |
| Seleccionar producto para decoración/evento | Producto `ACTIVO`. | Producto `INACTIVO`. | “Este producto está inactivo.” |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `nombre` | Crear y actualizar. | Nunca vacío. | Debe enviarse en PUT. |
| `descripcion` | Crear y actualizar. | No aplica. | Opcional. |
| `cantidadTotal` | Crear. | Actualizar producto. | Ajustes posteriores corresponden a Movimiento de inventario. |
| `estado` | Crear y actualizar; también vía endpoints de desactivar/reactivar. | No aplica. | Preferir botones de activar/desactivar sobre edición manual de estado. |
| `precioUnitario` | Actualizar. | Crear desde `ProductoRequestDTO`. | No viene en DTO de creación. |
| `stockMinimo` | Actualizar. | Crear desde `ProductoRequestDTO`. | No viene en DTO de creación. |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Crear producto | Listado de productos, selects relacionados, dashboard inventario. |
| Actualizar producto | Listado, detalle, vistas de stock, reportes si usan precio unitario. |
| Desactivar producto | Listado, filtros por estado, selects de decoración/evento. |
| Reactivar producto | Listado, filtros por estado, selects de decoración/evento. |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `ACTIVO` | Producto disponible. | Badge positivo; permitir selección operativa. |
| `INACTIVO` | Producto deshabilitado. | Badge inactivo; ocultar o deshabilitar en selects de nuevas operaciones. |
| `stockBajoAlerta = true` | Disponible menor o igual al mínimo. | Badge/alerta de stock bajo si se expone vista de stock. |

### 10.5 Reglas importantes

- No usar `cantidadTotal` como disponibilidad real si hay reservas activas.
- Para disponibilidad, usar `cantidadDisponible` desde `vista_stock_producto` cuando exista endpoint que la exponga.
- No ajustar stock físico desde la pantalla de edición de producto; usar Movimiento de inventario.
- No ofrecer productos `INACTIVO` en nuevas decoraciones o eventos.
- Después de cambiar `stockMinimo`, refrescar vistas de stock para recalcular alertas.
- Mostrar errores de negocio del backend, especialmente duplicado, no encontrado e integridad.
- No asumir que `GET /api/productos` devuelve solo activos.
- No asumir que `StockProductoResponseDTO` está expuesto por `ProductoController`; en el ZIP recibido no hay endpoint para esa respuesta.
