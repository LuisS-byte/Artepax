# Inventario — Decoración — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El submódulo Decoración administra los paquetes o plantillas de decoración que pueden asociarse a eventos. Una decoración define nombre, descripción, precio base, estado e integración con productos requeridos mediante `decoracionxproducto`.

Una decoración no representa una reserva de stock por sí sola. Funciona como plantilla operativa. La reserva efectiva ocurre desde el módulo Evento al pasar un evento a `PROGRAMADO` o al agregar extras.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Crear, listar, consultar, actualizar y desactivar decoraciones. |
| Controller principal | `DecoracionController` |
| Service principal | `DecoracionService` / `DecoracionServiceImpl` |
| Entidades principales | `Decoracion`, `DecoracionProducto`, `DecoracionProductoId` |
| Tablas/vistas implicadas | `decoracion`, `decoracionxproducto`, `producto`, `vista_stock_producto` mediante `StockProductoRepository` |
| Módulos relacionados | Inventario Producto, Evento |
| Tipo de módulo | CRUD operativo / plantilla de negocio |

El módulo sí valida stock al crear o actualizar composición de productos. En la implementación actual no permite usar productos con stock crítico ni productos cuya disponibilidad sea inferior a la cantidad requerida.

## 2. Controllers y endpoints completos

Base path:

```text
/api/decoraciones
```

Autorización:

```text
@PreAuthorize("hasRole('ADMIN')")
```

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/decoraciones` | Crear decoración | `DecoracionRequestDTO` | `DecoracionResponseDTO` | `crearDecoracion(dto)` | ADMIN | Retorna HTTP 201. |
| GET | `/api/decoraciones?estado=` | Listar decoraciones | Query param opcional `estado` | `List<DecoracionResponseDTO>` | `obtenerTodas(estado)` | ADMIN | Si `estado` viene, filtra por estado exacto. |
| GET | `/api/decoraciones/{id}` | Obtener decoración por ID | Path variable `id` | `DecoracionResponseDTO` | `obtenerPorId(id)` | ADMIN | Lanza `DecoracionNotFoundException` si no existe. |
| PUT | `/api/decoraciones/{id}` | Actualizar decoración | `DecoracionUpdateDTO` | `DecoracionResponseDTO` | `actualizar(id, dto)` | ADMIN | Si vienen productos, reemplaza toda la composición. |
| DELETE | `/api/decoraciones/{id}` | Desactivar decoración | Path variable `id` | Sin body | `eliminar(id)` | ADMIN | Soft delete: cambia estado a `INACTIVO`. |

### POST `/api/decoraciones`

- Propósito: crear una nueva decoración con productos requeridos.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: `DecoracionRequestDTO`.
- Response: `DecoracionResponseDTO`.
- HTTP esperado: `201 Created`.
- Método del service: `DecoracionServiceImpl.crearDecoracion(dto)`.
- Validaciones principales:
  - `nombre` obligatorio.
  - `precio` obligatorio.
  - `productos` no puede estar vacío.
  - Cada producto requiere `productoId` y `cantidadRequerida > 0`.
  - El producto debe existir.
  - Debe existir stock para el producto en `StockProductoRepository`.
  - No permite productos con `stockBajoAlerta = true`.
  - No permite cantidad requerida mayor que `cantidadDisponible`.
- Excepciones posibles:
  - `MethodArgumentNotValidException` por validación DTO.
  - `ProductoNotFoundException` si el producto no existe.
  - `IllegalArgumentException` si no hay stock, si hay stock crítico o si el stock disponible es insuficiente.
  - Error de integridad de datos si la BD rechaza duplicados o restricciones.
- Efectos secundarios:
  - Inserta en `decoracion`.
  - Inserta asociaciones en `decoracionxproducto`.
- Qué debe refrescar el frontend:
  - listado de decoraciones.
  - detalle de la decoración creada.
  - formularios/selectores de eventos que usan decoraciones activas.

### GET `/api/decoraciones`

- Propósito: listar decoraciones, opcionalmente filtradas por estado.
- Path variables: no aplica.
- Query params:
  - `estado`: opcional. Ejemplo: `ACTIVO`, `INACTIVO`.
- Request body: no aplica.
- Response: `List<DecoracionResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `DecoracionServiceImpl.obtenerTodas(estado)`.
- Validaciones principales:
  - No valida semántica del estado; filtra por valor exacto si se envía.
- Excepciones posibles:
  - Error genérico si hay fallo de consulta.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - listado de decoraciones.

### GET `/api/decoraciones/{id}`

- Propósito: obtener el detalle completo de una decoración, incluyendo productos requeridos.
- Path variables:
  - `id`: ID de decoración.
- Query params: no aplica.
- Request body: no aplica.
- Response: `DecoracionResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `DecoracionServiceImpl.obtenerPorId(id)`.
- Validaciones principales:
  - La decoración debe existir.
- Excepciones posibles:
  - `DecoracionNotFoundException`.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - detalle de decoración.

### PUT `/api/decoraciones/{id}`

- Propósito: actualizar datos generales y opcionalmente reemplazar la composición de productos.
- Path variables:
  - `id`: ID de decoración.
- Query params: no aplica.
- Request body: `DecoracionUpdateDTO`.
- Response: `DecoracionResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `DecoracionServiceImpl.actualizar(id, dto)`.
- Validaciones principales:
  - `nombre` está anotado como `@NotBlank`, por lo que el request exige nombre aunque el mapper permita actualización parcial a nivel código.
  - Si `productos` viene no vacío, valida stock de todos los productos y reemplaza la composición anterior.
  - Si `productos` es `null` o lista vacía, conserva la composición existente.
- Excepciones posibles:
  - `DecoracionNotFoundException`.
  - `ProductoNotFoundException`.
  - `MethodArgumentNotValidException`.
  - `IllegalArgumentException` por stock crítico/insuficiente.
  - Error de integridad de datos.
- Efectos secundarios:
  - Actualiza `decoracion`.
  - Si vienen productos, elimina asociaciones anteriores en `decoracionxproducto` y guarda las nuevas.
- Qué debe refrescar el frontend:
  - detalle de decoración.
  - listado de decoraciones.
  - pantallas de evento que consumen decoración/precio.

### DELETE `/api/decoraciones/{id}`

- Propósito: desactivar una decoración sin borrarla físicamente.
- Path variables:
  - `id`: ID de decoración.
- Query params: no aplica.
- Request body: no aplica.
- Response: sin body.
- HTTP esperado: `204 No Content`.
- Método del service: `DecoracionServiceImpl.eliminar(id)`.
- Validaciones principales:
  - La decoración debe existir.
- Excepciones posibles:
  - `DecoracionNotFoundException`.
- Efectos secundarios:
  - Cambia `decoracion.decor_estado` a `INACTIVO`.
- Qué debe refrescar el frontend:
  - listado de decoraciones.
  - selectores de decoración en creación/edición de eventos.

## 3. DTOs request/response

### 3.1 DTOs Request

#### `DecoracionRequestDTO`

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `nombre` | `String` | `nombre` | Sí | No | `@NotBlank` | Nombre visible de la decoración. |
| `descripcion` | `String` | `descripcion` | No | Sí | No declarada | Texto descriptivo. |
| `precio` | `BigDecimal` | `precio` | Sí | No | `@NotNull` | Precio base que se copiará al evento cuando se use la decoración. |
| `estado` | `String` | `estado` | No | Sí | No declarada | Debe usarse con valores esperados como `ACTIVO`/`INACTIVO`; el backend no valida catálogo aquí. |
| `productos` | `List<DecoracionProductoRequestDTO>` | `productos` | Sí | No | `@NotEmpty` | Debe incluir al menos un producto requerido. |

#### `DecoracionUpdateDTO`

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `nombre` | `String` | `nombre` | Sí por DTO | No | `@NotBlank` | Aunque el service actualiza solo campos no nulos, la validación obliga a enviar nombre válido. |
| `descripcion` | `String` | `descripcion` | No | Sí | No declarada | Si viene, actualiza descripción. |
| `precio` | `BigDecimal` | `precio` | No | Sí | No declarada | Si viene, actualiza precio de la plantilla. No actualiza eventos ya creados. |
| `estado` | `String` | `estado` | No | Sí | No declarada | Si viene, actualiza estado. |
| `productos` | `List<DecoracionProductoRequestDTO>` | `productos` | No | Sí | Validación interna si viene no vacío | Si viene no vacío, reemplaza toda la composición anterior. |

#### `DecoracionProductoRequestDTO`

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `productoId` | `Long` | `productoId` | Sí | No | `@NotNull` | Producto requerido para la decoración. |
| `cantidadRequerida` | `Integer` | `cantidadRequerida` | Sí | No | `@NotNull`, `@Positive` | Cantidad necesaria para esta decoración. |

### 3.2 DTOs Response

#### `DecoracionResponseDTO`

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `decoracion.id_decoracion` | Identificador principal. |
| `nombre` | `String` | `nombre` | No | `decoracion.deco_nombre` | Mostrar en tablas, cards y selectores. |
| `descripcion` | `String` | `descripcion` | Sí | `decoracion.decor_descripcion` | Descripción funcional. |
| `precio` | `BigDecimal` | `precio` | No | `decoracion.decor_precio` | Precio base de la decoración. |
| `imagenUrl` | `String` | `imagenUrl` | Sí | `decoracion.decor_imagen_url` | Puede ser `null`; frontend debe usar placeholder. |
| `estado` | `String` | `estado` | Sí | `decoracion.decor_estado` | Usar para badges y filtrado. |
| `fechaCreacion` | `LocalDateTime` | `fechaCreacion` | No | `decoracion.decor_creacion` | Fecha de alta. |
| `productos` | `List<DecoracionProductoResponseDTO>` | `productos` | No | `decoracionxproducto` + `producto` | Composición de la plantilla. |

#### `DecoracionProductoResponseDTO`

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `productoId` | `Long` | `productoId` | No | `producto.id_producto` | ID del producto requerido. |
| `productoNombre` | `String` | `productoNombre` | No | `producto.produ_nombre` | Mostrar en detalle. |
| `cantidadRequerida` | `Integer` | `cantidadRequerida` | No | `decoracionxproducto.cantidad_requerida` | Cantidad por plantilla. |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Decoracion` | `decoracion` | Tabla | Plantilla comercial de decoración. | Tiene nombre, descripción, precio, imagen, estado y fecha de creación. |
| `DecoracionProducto` | `decoracionxproducto` | Relación | Composición de productos de una decoración. | PK compuesta por decoración y producto. |
| `DecoracionProductoId` | PK embebida | Relación | Identificador compuesto `decoracionId` + `productoId`. | Usado por JPA para `decoracionxproducto`. |
| `Producto` | `producto` | Tabla externa | Producto físico requerido por decoración. | Se consulta para validar existencia y mapear relaciones. |
| `StockProducto` | `vista_stock_producto` o entidad equivalente | Vista/lectura externa | Disponibilidad operativa del producto. | Se usa para validar stock crítico y cantidad disponible. |

No hay vista propia del submódulo Decoración.

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Crear decoración | `crearDecoracion(dto)` | Stock de productos, existencia de productos, validaciones DTO | Inserta decoración y asociaciones | `DecoracionResponseDTO` |
| Listar decoraciones | `obtenerTodas(estado)` | No aplica | No modifica | Lista de `DecoracionResponseDTO` |
| Obtener por ID | `obtenerPorId(id)` | Existencia de decoración | No modifica | `DecoracionResponseDTO` |
| Actualizar decoración | `actualizar(id, dto)` | Existencia, validación de stock si cambia productos | Actualiza decoración y opcionalmente reemplaza productos | `DecoracionResponseDTO` |
| Eliminar/desactivar | `eliminar(id)` | Existencia de decoración | Cambia estado a `INACTIVO` | Sin retorno |

### Flujo: crear decoración

1. Valida stock de los productos enviados mediante `validarStockProductos(dto.getProductos())`.
2. Convierte `DecoracionRequestDTO` a entidad `Decoracion`.
3. Guarda la decoración.
4. Por cada producto requerido:
   - busca el producto por ID.
   - crea `DecoracionProducto` con `DecoracionProductoId`.
   - asigna cantidad requerida.
5. Guarda todas las relaciones en `decoracionxproducto`.
6. Retorna `DecoracionResponseDTO` con productos asociados.

### Flujo: listar decoraciones

1. Si `estado` viene informado, usa `decoracionRepository.findByEstado(estado)`.
2. Si no, usa `decoracionRepository.findAll()`.
3. Consulta productos asociados con `decoracionProductoRepository.findByDecoracionIds(ids)`.
4. Agrupa productos por decoración.
5. Retorna DTOs con composición.

Observación técnica: si no hay decoraciones, `ids` será lista vacía. La query con `IN :ids` puede comportarse distinto según proveedor JPA/BD. Si en ejecución falla con lista vacía, conviene manejarlo antes de consultar productos.

### Flujo: obtener por ID

1. Busca `Decoracion` por ID.
2. Si no existe, lanza `DecoracionNotFoundException`.
3. Consulta productos asociados.
4. Retorna DTO con detalle.

### Flujo: actualizar decoración

1. Busca decoración por ID.
2. Si no existe, lanza `DecoracionNotFoundException`.
3. Actualiza campos no nulos mediante mapper.
4. Guarda la decoración.
5. Si `dto.productos` viene no nulo y no vacío:
   - valida stock.
   - elimina asociaciones anteriores.
   - crea nuevas asociaciones.
   - guarda nuevas asociaciones.
6. Retorna DTO con productos nuevos o existentes.

### Flujo: eliminar decoración

1. Busca decoración por ID.
2. Si no existe, lanza `DecoracionNotFoundException`.
3. Cambia estado a `INACTIVO`.
4. Guarda.

No hay borrado físico.

### Flujo: validar stock de productos

1. Consolida cantidades por `productoId`, sumando duplicados.
2. Consulta stock con `stockProductoRepository.findAllByIdProductoIn(productoIds)`.
3. Para cada producto:
   - si no hay stock, lanza `IllegalArgumentException`.
   - si `stockBajoAlerta = true`, lanza `IllegalArgumentException`.
   - si `cantidadDisponible < cantidadRequerida`, lanza `IllegalArgumentException`.

## 6. Estados y transiciones

### Estados existentes

El módulo no define un enum propio en código. Usa el campo textual `decoracion.estado`.

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `ACTIVO` | Decoración disponible para operación | Consultas, creación de eventos desde módulo Evento | Sí | Debe mostrarse como utilizable. |
| `INACTIVO` | Decoración desactivada | `DELETE /api/decoraciones/{id}` | Sí | No debe usarse para nuevos eventos. |

### Transiciones permitidas

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| Cualquier estado existente | Valor enviado en DTO | `PUT /api/decoraciones/{id}` | No hay validación explícita de catálogo | Actualiza `decor_estado`. |
| Cualquier estado existente | `INACTIVO` | `DELETE /api/decoraciones/{id}` | Decoración debe existir | Soft delete. |

El frontend debe tratar `DELETE` como desactivación, no como eliminación real.

## 7. Excepciones y respuestas de error

Formato general del error, según `GlobalExceptionHandler` usado en el proyecto:

```json
{
  "codigo": 400,
  "mensaje": "Mensaje del error",
  "errorCode": null,
  "timestamp": "2026-05-10T00:00:00"
}
```

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `DecoracionNotFoundException` | No existe decoración por ID | 404 | `Decoración no encontrada con ID: {id}` | Mostrar mensaje y volver/listar. |
| `MethodArgumentNotValidException` | DTO inválido | 400 | Primer error de validación, formato `campo: mensaje` | Mostrar error de formulario. |
| `ProductoNotFoundException` | Producto asociado no existe | 404 o según excepción del módulo Producto | Depende del módulo Producto | Mostrar producto inválido/no disponible. |
| `IllegalArgumentException` | Falta stock, stock crítico o stock insuficiente | 400 | Mensaje del service | Mostrar mensaje del backend. |
| `DataIntegrityViolationException` | Restricción de BD | 409 | `Error de integridad de datos` salvo caso específico de documentos | Mostrar error general de integridad. |
| `AccessDeniedException` | Usuario sin rol ADMIN | 403 | Mensaje de seguridad | Redirigir o mostrar sin permiso. |
| `Exception` | Error no controlado | 500 | `Error interno del servidor` | Mostrar error genérico. |

No se detecta excepción custom para decoración duplicada. Si la tabla tiene unique sobre nombre y se viola, llegará como `DataIntegrityViolationException` con HTTP 409 y mensaje genérico.

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| Producto | Repository externo: `ProductoRepository` | Crear/actualizar composición | Validar existencia de producto y asociarlo | Medio: falla si producto no existe. |
| Stock Producto | Repository externo: `StockProductoRepository` | `validarStockProductos` | Validar disponibilidad y stock crítico | Medio: acopla decoración con disponibilidad actual. |
| Evento | Dependencia conceptual | Eventos usan decoración como plantilla | La decoración activa alimenta creación de eventos | Medio: cambiar precio de decoración no actualiza eventos existentes. |
| Common Exception | `ArtepaxException` / Global handler | Excepciones | Formato de error HTTP | Bajo. |

```text
Dependencia circular:
- Detectada: No
- Detalle: DecoracionServiceImpl depende de ProductoRepository y StockProductoRepository, pero no se observa dependencia inversa directa hacia DecoracionService.
- Riesgo: Bajo.
- Cómo se evita o cómo debería evitarse: mantener Decoración como plantilla y Producto como fuente de stock; no inyectar EventoService dentro de DecoracionService.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Crear decoración | Crea registro en `decoracion` | Decoración | Listado y detalle de decoraciones. |
| Crear decoración | Crea relaciones en `decoracionxproducto` | Decoración / Producto | Detalle de decoración. |
| Actualizar decoración | Cambia datos base de decoración | Decoración | Listado, detalle, selectores de eventos. |
| Actualizar con productos | Borra asociaciones anteriores y crea nuevas | Decoración / Producto | Detalle de decoración. |
| Eliminar decoración | Cambia estado a `INACTIVO` | Decoración / Evento | Listado, selectores de eventos. |

No se detectan notificaciones, auditoría explícita ni jobs asociados en este submódulo.

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Crear decoración | Usuario ADMIN, formulario válido y al menos un producto | Falta nombre, precio o productos | Complete los campos obligatorios. |
| Editar decoración | Usuario ADMIN y decoración existente | Decoración no existe | La decoración ya no existe o fue eliminada. |
| Desactivar decoración | Decoración existente | Decoración no existe | No se puede desactivar una decoración inexistente. |
| Usar decoración en nuevo evento | Estado `ACTIVO` | Estado `INACTIVO` | La decoración seleccionada no está activa. |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `nombre` | Crear/editar | Nunca si se envía vacío | En update es obligatorio por validación. |
| `descripcion` | Crear/editar | No aplica | Puede ser `null`. |
| `precio` | Crear/editar | Si no se envía en creación | En creación es obligatorio. |
| `estado` | Crear/editar | No aplica | Backend no valida valores; frontend debe restringir a `ACTIVO`/`INACTIVO`. |
| `productos` | Crear/editar | Si producto está en stock crítico o no tiene disponibilidad suficiente | En update, si se envía lista no vacía reemplaza toda la composición. |
| `imagenUrl` | No editable por los DTOs actuales | Siempre desde este controller | El response lo expone, pero request/update no lo reciben. |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Crear decoración | Listado de decoraciones, detalle, selectores de evento. |
| Actualizar decoración | Listado, detalle, selectores de evento. |
| Desactivar decoración | Listado, filtros por estado, selectores de evento. |
| Cambiar productos de decoración | Detalle de decoración y cualquier vista de composición. |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `ACTIVO` | Decoración disponible | Badge activa; permitir selección en eventos. |
| `INACTIVO` | Decoración desactivada | Badge inactiva; ocultar o deshabilitar en selección. |
| Otro valor | No validado por backend | Mostrar como estado desconocido o evitar enviarlo desde UI. |

### 10.5 Reglas importantes

- No asumir que `DELETE` borra físicamente. Es soft delete a `INACTIVO`.
- No enviar estados arbitrarios desde UI; restringir a `ACTIVO`/`INACTIVO` aunque el backend no lo valide explícitamente.
- No permitir crear decoración sin productos.
- En edición, si se envía `productos` con elementos, el backend reemplaza toda la composición anterior.
- Si solo se quiere actualizar nombre, descripción, precio o estado, no enviar `productos`.
- `imagenUrl` se recibe en el response, pero no hay campo request para actualizarlo en este controller.
- Una decoración es una plantilla. La reserva real de stock ocurre en Evento, no al crear la decoración.
- El backend actual sí bloquea crear/actualizar composición con productos en stock crítico o disponibilidad insuficiente.
- Si el backend devuelve error de stock, el frontend debe mostrar literalmente el mensaje de negocio.
- No calcular disponibilidad únicamente en frontend; el backend valida contra `StockProductoRepository`.

## Resumen para frontend

1. Consumir `GET /api/decoraciones` para listado y usar `estado=ACTIVO` si solo se necesitan decoraciones disponibles.
2. `POST /api/decoraciones` requiere nombre, precio y al menos un producto.
3. Cada producto requiere `productoId` y `cantidadRequerida > 0`.
4. `PUT /api/decoraciones/{id}` exige `nombre` por DTO.
5. Si `productos` viene no vacío en update, se reemplaza toda la composición.
6. `DELETE /api/decoraciones/{id}` no borra; cambia estado a `INACTIVO`.
7. No usar decoraciones `INACTIVO` para nuevos eventos.
8. `imagenUrl` se muestra pero no se administra desde estos endpoints.
9. Los errores de stock vienen como `400` con mensaje de negocio.
10. Después de crear, editar o desactivar, refrescar listado y selectores de eventos.
