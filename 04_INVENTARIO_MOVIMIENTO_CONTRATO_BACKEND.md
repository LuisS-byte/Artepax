# Inventario — Movimiento — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El submódulo Movimiento administra los cambios operativos de stock físico de productos. Permite registrar entradas, salidas, ajustes por conteo físico, consultar stock calculado desde la vista `vista_stock_producto` y consultar el historial de movimientos.

No administra el catálogo de productos completo; eso corresponde al submódulo Producto. Tampoco administra reservas de eventos directamente; solo las consulta para evitar ajustes físicos por debajo de reservas activas.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Registrar movimientos físicos de inventario y consultar historial/stock operativo. |
| Controller principal | `MovimientoController` |
| Service principal | `MovimientoService` / `MovimientoServiceImpl` |
| Entidades principales | `MovimientoInventario`, `Producto`, `StockProducto` |
| Tablas/vistas implicadas | `movimiento_inventario`, `producto`, `vista_stock_producto`, `evento_producto` |
| Módulos relacionados | Producto, Evento/Reservas, Notificación, Auditoría, Usuario/Auth |
| Tipo de módulo | Operativo |

## 2. Controllers y endpoints completos

Base path: `/api/inventario`

Seguridad: el controller tiene `@PreAuthorize("hasRole('ADMIN')")`. Todos los endpoints requieren rol `ADMIN`.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/inventario/entrada` | Registrar entrada de stock | `MovimientoRequestDTO` | `204 No Content` | `registrarEntrada(dto)` | ADMIN | Orígenes válidos: `REPOSICION`, `AJUSTE_MANUAL`. |
| POST | `/api/inventario/salida` | Registrar salida de stock | `MovimientoRequestDTO` | `204 No Content` | `registrarSalida(dto)` | ADMIN | Orígenes válidos: `AJUSTE_MANUAL`, `DAÑO`, `PERDIDA`. |
| POST | `/api/inventario/ajuste/{productoId}` | Ajustar stock por conteo físico | `AjusteInventarioRequestDTO` | `204 No Content` | `registrarAjuste(productoId, request)` | ADMIN | Calcula automáticamente entrada o salida según diferencia. |
| GET | `/api/inventario/productos/{id}/stock` | Obtener stock operativo de producto | No aplica | `StockProductoResponseDTO` | `obtenerStock(id)` | ADMIN | Lee desde `vista_stock_producto`. |
| GET | `/api/inventario/productos/{id}/movimientos` | Obtener historial por producto | No aplica | `List<MovimientoResponseDTO>` | `obtenerMovimientosPorProducto(id)` | ADMIN | Valida existencia del producto. |
| GET | `/api/inventario/movimientos` | Obtener historial global paginado | Pageable | `Page<MovimientoResponseDTO>` | `obtenerTodosMovimientos(pageable)` | ADMIN | Default: size 50, sort `fecha`. |

### POST `/api/inventario/entrada`

- Propósito: registrar incremento de stock físico.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: `MovimientoRequestDTO`.
- Response: vacío.
- HTTP esperado: `204 No Content`.
- Método del service: `registrarEntrada(dto)`.
- Validaciones principales:
  - `productoId` obligatorio.
  - `cantidad` obligatoria y mayor a 0.
  - `tipoOrigen` debe estar en `REPOSICION` o `AJUSTE_MANUAL`.
  - Producto debe existir.
- Excepciones posibles:
  - `MethodArgumentNotValidException` → `400`.
  - `IllegalArgumentException` por origen inválido → `400`.
  - `ProductoNotFoundException` → `404`.
- Efectos secundarios:
  - Incrementa `producto.cantidadTotal`.
  - Crea fila en `movimiento_inventario` con `tipoMovimiento = ENTRADA`.
  - Registra usuario autenticado si puede resolverlo por correo.
  - Verifica alerta de stock bajo después del movimiento.
- Qué debe refrescar el frontend:
  - Stock del producto.
  - Historial de movimientos del producto.
  - Listado global de movimientos.
  - Dashboard o alertas de inventario si existen.
  - Notificaciones, si se muestra centro de notificaciones.

### POST `/api/inventario/salida`

- Propósito: registrar decremento de stock físico por daño, pérdida o ajuste manual.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: `MovimientoRequestDTO`.
- Response: vacío.
- HTTP esperado: `204 No Content`.
- Método del service: `registrarSalida(dto)`.
- Validaciones principales:
  - `productoId` obligatorio.
  - `cantidad` obligatoria y mayor a 0.
  - `tipoOrigen` debe estar en `AJUSTE_MANUAL`, `DAÑO` o `PERDIDA`.
  - Producto debe existir.
  - Stock disponible desde `vista_stock_producto` debe ser suficiente.
- Excepciones posibles:
  - `MethodArgumentNotValidException` → `400`.
  - `IllegalArgumentException` por origen inválido → `400`.
  - `ProductoNotFoundException` → `404`.
  - `StockInsuficienteException` → `409`.
- Efectos secundarios:
  - Decrementa `producto.cantidadTotal`.
  - Crea fila en `movimiento_inventario` con `tipoMovimiento = SALIDA`.
  - Registra usuario autenticado si puede resolverlo por correo.
  - Verifica alerta de stock bajo después del movimiento.
- Qué debe refrescar el frontend:
  - Stock del producto.
  - Historial de movimientos del producto.
  - Listado global de movimientos.
  - Dashboard o alertas de inventario si existen.
  - Notificaciones, si se genera alerta.

### POST `/api/inventario/ajuste/{productoId}`

- Propósito: ajustar el stock del sistema al conteo físico real.
- Path variables:
  - `productoId`: id del producto a ajustar.
- Query params: no aplica.
- Request body: `AjusteInventarioRequestDTO`.
- Response: vacío.
- HTTP esperado: `204 No Content`.
- Método del service: `registrarAjuste(productoId, request)`.
- Validaciones principales:
  - `cantidadFisica` obligatoria y no negativa.
  - `motivo` obligatorio.
  - Producto debe existir.
  - Producto debe estar `ACTIVO`.
  - `cantidadFisica` no puede ser menor que la cantidad reservada activa.
- Excepciones posibles:
  - `MethodArgumentNotValidException` → `400`.
  - `ProductoNotFoundException` → `404`.
  - `IllegalArgumentException` si el producto está inactivo → `400`.
  - `ReservaActivaInsuficienteException` → `409`.
- Efectos secundarios:
  - Si no hay diferencia, no modifica stock ni registra movimiento.
  - Si la diferencia es positiva, actualiza stock y registra movimiento `ENTRADA / AJUSTE_MANUAL`.
  - Si la diferencia es negativa, actualiza stock y registra movimiento `SALIDA / AJUSTE_MANUAL`.
  - Registra auditoría sobre `PRODUCTO` y `MOVIMIENTO_INVENTARIO` cuando hay cambio.
  - Verifica alerta de stock bajo después del ajuste.
- Qué debe refrescar el frontend:
  - Stock del producto.
  - Detalle del producto.
  - Historial de movimientos del producto.
  - Auditoría, si se muestra.
  - Notificaciones, si se genera alerta.

### GET `/api/inventario/productos/{id}/stock`

- Propósito: obtener stock operativo calculado para un producto.
- Path variables:
  - `id`: id del producto.
- Query params: no aplica.
- Request body: no aplica.
- Response: `StockProductoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `obtenerStock(id)`.
- Validaciones principales:
  - Debe existir una fila en `vista_stock_producto` para el producto.
- Excepciones posibles:
  - `ProductoNotFoundException` → `404`.
- Efectos secundarios: no tiene.
- Qué debe refrescar el frontend:
  - No aplica; es consulta.

### GET `/api/inventario/productos/{id}/movimientos`

- Propósito: obtener historial de movimientos de un producto específico.
- Path variables:
  - `id`: id del producto.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<MovimientoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `obtenerMovimientosPorProducto(id)`.
- Validaciones principales:
  - Producto debe existir en `producto`.
- Excepciones posibles:
  - `ProductoNotFoundException` → `404`.
- Efectos secundarios: no tiene.
- Qué debe refrescar el frontend:
  - No aplica; es consulta.

### GET `/api/inventario/movimientos`

- Propósito: obtener historial global paginado de movimientos de inventario.
- Path variables: no aplica.
- Query params:
  - Parámetros estándar de `Pageable`: `page`, `size`, `sort`.
- Request body: no aplica.
- Response: `Page<MovimientoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `obtenerTodosMovimientos(pageable)`.
- Validaciones principales: no aplica.
- Excepciones posibles:
  - Errores genéricos de paginación o persistencia → según `GlobalExceptionHandler`.
- Efectos secundarios: no tiene.
- Qué debe refrescar el frontend:
  - No aplica; es consulta.

## 3. DTOs request/response

### 3.1 DTOs Request

#### `MovimientoRequestDTO`

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `productoId` | `Long` | `productoId` | Sí | No | `@NotNull` | Producto afectado. |
| `cantidad` | `Integer` | `cantidad` | Sí | No | `@NotNull`, `@Positive` | Debe ser mayor a 0. |
| `motivo` | `String` | `motivo` | No | Sí | Sin anotación | Conviene enviarlo para trazabilidad. |
| `tipoOrigen` | `String` | `tipoOrigen` | No | Sí | Validación manual por service | Default en DTO: `REPOSICION`. Para salida, si viene null el service usa `DAÑO`. |

Valores válidos según operación:

| Operación | Valores válidos de `tipoOrigen` | Default real |
|---|---|---|
| Entrada | `REPOSICION`, `AJUSTE_MANUAL` | `REPOSICION` |
| Salida | `AJUSTE_MANUAL`, `DAÑO`, `PERDIDA` | `DAÑO` si llega null al service |

Nota: el DTO inicializa `tipoOrigen = "REPOSICION"`. Para salidas, el frontend debe enviar explícitamente `DAÑO`, `PERDIDA` o `AJUSTE_MANUAL`; de lo contrario, puede llegar `REPOSICION` y el service lo rechazará como origen inválido para salida.

#### `AjusteInventarioRequestDTO`

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `cantidadFisica` | `Integer` | `cantidadFisica` | Sí | No | `@NotNull`, `@Min(0)` | Conteo físico real. Puede ser 0. |
| `motivo` | `String` | `motivo` | Sí | No | `@NotBlank` | Obligatorio para trazabilidad. |

### 3.2 DTOs Response

#### `MovimientoResponseDTO`

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `movimiento_inventario.id_movimiento` | Identificador del movimiento. |
| `productoId` | `Long` | `productoId` | No | `movimiento.producto.id` | Producto afectado. |
| `eventoId` | `Long` | `eventoId` | Sí | `movimiento.evento.id` | Solo existe para movimientos asociados a evento. |
| `tipoMovimiento` | `String` | `tipoMovimiento` | No | `movimiento.tipoMovimiento` | `ENTRADA` o `SALIDA`. |
| `tipoOrigen` | `String` | `tipoOrigen` | No | `movimiento.tipoOrigen` | `REPOSICION`, `AJUSTE_MANUAL`, `DAÑO`, `PERDIDA`, `EVENTO` si lo generan otros flujos. |
| `cantidad` | `Integer` | `cantidad` | No | `movimiento.cantidad` | Siempre positiva; el signo lo define `tipoMovimiento`. |
| `fecha` | `LocalDateTime` | `fecha` | No | `movimiento.fecha` | Fecha de creación del movimiento. |
| `motivo` | `String` | `motivo` | Sí | `movimiento.motivo` | Texto operativo. |
| `productoNombre` | `String` | `productoNombre` | Sí | `movimiento.producto.nombre` | Nombre del producto. |
| `eventoNombre` | `String` | `eventoNombre` | Sí | `movimiento.evento.cliente.nombre` | En código se llena con el nombre del cliente del evento, no con un nombre propio de evento. |

#### `StockProductoResponseDTO`

Este DTO pertenece al submódulo Producto, pero este controller lo retorna en `GET /api/inventario/productos/{id}/stock`.

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `idProducto` | `Long` | `idProducto` | No | `vista_stock_producto.id_producto` | Producto consultado. |
| `nombre` | `String` | `nombre` | No | `vista_stock_producto.produ_nombre` | Nombre del producto. |
| `cantidadTotal` | `Integer` | `cantidadTotal` | No | `producto.produ_cantidad_total` vía vista | Stock físico total. |
| `cantidadReservada` | `Integer` | `cantidadReservada` | No | vista SQL | Reservas activas. |
| `cantidadDisponible` | `Integer` | `cantidadDisponible` | No | vista SQL | Disponible operativo. |
| `stockMinimo` | `Integer` | `stockMinimo` | No | `producto.produ_stock_minimo` vía vista | Umbral mínimo. |
| `stockBajoAlerta` | `Boolean` | `stockBajoAlerta` | No | vista SQL | Indicador para badge/alerta. |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `MovimientoInventario` | `movimiento_inventario` | Tabla operativa | Historial de entradas, salidas, ajustes y movimientos asociados a eventos. | Registra producto, evento opcional, usuario opcional, tipo, origen, cantidad, fecha y motivo. |
| `Producto` | `producto` | Tabla | Stock físico base. | `cantidadTotal` se incrementa/decrementa directamente desde este módulo. |
| `StockProducto` | `vista_stock_producto` | Vista solo lectura | Stock disponible real considerando reservas activas. | Usada para validar salidas y devolver stock al frontend. |
| `EventoProducto` | `evento_producto` | Relación / reserva | Reservas activas por evento. | Consultada en ajuste para no permitir stock físico menor a reservas activas. |
| `Usuario` | `usuario` | Tabla | Usuario autenticado que registró el movimiento. | Se resuelve desde `SecurityContextHolder` por correo. Si falla, queda `null`. |
| `Evento` | `evento` | Tabla | Contexto opcional del movimiento. | Movimientos creados desde este controller no asignan evento; otros flujos pueden hacerlo. |

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Registrar entrada | `registrarEntrada(dto)` | Origen válido, producto existente | Incrementa `producto.cantidadTotal`; crea movimiento `ENTRADA` | `void` |
| Registrar salida | `registrarSalida(dto)` | Origen válido, producto existente, stock disponible suficiente | Decrementa `producto.cantidadTotal`; crea movimiento `SALIDA` | `void` |
| Ajuste por conteo físico | `registrarAjuste(productoId, request)` | Producto activo, cantidad física >= reservas activas | Ajusta `producto.cantidadTotal`; crea movimiento de entrada/salida si hay diferencia; audita | `void` |
| Obtener stock | `obtenerStock(productoId)` | Existe fila en `vista_stock_producto` | No modifica | `StockProductoResponseDTO` |
| Historial por producto | `obtenerMovimientosPorProducto(productoId)` | Producto existe | No modifica | `List<MovimientoResponseDTO>` |
| Historial global | `obtenerTodosMovimientos(pageable)` | No aplica | No modifica | `Page<MovimientoResponseDTO>` |

### Flujo: registrar entrada

1. Determina `tipoOrigen`: usa el valor del DTO o `REPOSICION` si llega null.
2. Valida que el origen esté en `REPOSICION` o `AJUSTE_MANUAL`.
3. Busca el producto.
4. Incrementa `producto.cantidadTotal`.
5. Guarda el producto.
6. Registra movimiento con `tipoMovimiento = ENTRADA`.
7. Asocia usuario autenticado si puede resolverlo por correo.
8. Verifica y notifica stock bajo si la vista indica `stockBajoAlerta = true`.

### Flujo: registrar salida

1. Determina `tipoOrigen`: usa el valor del DTO o `DAÑO` si llega null.
2. Valida que el origen esté en `AJUSTE_MANUAL`, `DAÑO` o `PERDIDA`.
3. Busca el producto.
4. Consulta `vista_stock_producto`.
5. Si `cantidadDisponible < cantidad`, lanza `StockInsuficienteException`.
6. Decrementa `producto.cantidadTotal`.
7. Guarda el producto.
8. Registra movimiento con `tipoMovimiento = SALIDA`.
9. Verifica y notifica stock bajo si corresponde.

### Flujo: registrar ajuste

1. Busca el producto.
2. Rechaza productos inactivos.
3. Consulta reservas activas del producto en `evento_producto`.
4. Rechaza si `cantidadFisica < cantidadReservadaActiva`.
5. Calcula `diferencia = cantidadFisica - cantidadActual`.
6. Si la diferencia es 0, no hace cambios.
7. Si la diferencia es positiva:
   - Actualiza `cantidadTotal`.
   - Crea movimiento `ENTRADA / AJUSTE_MANUAL`.
   - Registra auditoría de producto y movimiento.
8. Si la diferencia es negativa:
   - Actualiza `cantidadTotal`.
   - Crea movimiento `SALIDA / AJUSTE_MANUAL`.
   - Registra auditoría de producto y movimiento.
9. Verifica y notifica stock bajo.

## 6. Estados y transiciones

Este módulo no maneja estados de ciclo de vida propios, pero sí maneja valores categóricos.

### Tipos de movimiento

| Valor | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `ENTRADA` | Incremento de stock físico | Movimientos de reposición o ajuste positivo | Sí | Siempre con cantidad positiva. |
| `SALIDA` | Decremento de stock físico | Daño, pérdida, ajuste negativo, evento | Sí | Siempre con cantidad positiva. |

### Tipos de origen

| Valor | Significado | Permitido en entrada | Permitido en salida | Notas frontend |
|---|---|---|---|---|
| `REPOSICION` | Entrada por reposición | Sí | No | Usar en formulario de entrada. |
| `AJUSTE_MANUAL` | Diferencia por conteo físico | Sí | Sí | Normalmente usar endpoint de ajuste, no entrada/salida manual. |
| `DAÑO` | Salida por daño | No | Sí | Usar en formulario de salida. |
| `PERDIDA` | Salida por pérdida | No | Sí | Usar en formulario de salida. |
| `EVENTO` | Salida generada por evento | No en este controller | No en este controller | Puede aparecer en historial si lo genera el flujo de finalización de evento. |

### Transiciones permitidas

No hay transición de estado. Las operaciones modifican cantidades de producto y generan movimientos históricos.

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| Stock actual | Stock actual + cantidad | `POST /entrada` | Origen válido; producto existe | Movimiento `ENTRADA`; posible alerta. |
| Stock actual | Stock actual - cantidad | `POST /salida` | Origen válido; stock disponible suficiente | Movimiento `SALIDA`; posible alerta. |
| Stock sistema | Stock físico informado | `POST /ajuste/{productoId}` | Producto activo; físico >= reservas activas | Movimiento `ENTRADA` o `SALIDA`; auditoría; posible alerta. |

## 7. Excepciones y respuestas de error

Formato común de error:

```json
{
  "codigo": 400,
  "mensaje": "mensaje de error",
  "errorCode": null,
  "timestamp": "2026-05-10T00:00:00"
}
```

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `MethodArgumentNotValidException` | Falla validación de DTO | 400 | Primer campo inválido, formato `campo: mensaje` | Mostrar debajo del campo o como alerta de formulario. |
| `IllegalArgumentException` | Origen inválido o producto inactivo en ajuste | 400 | Mensaje del service | Mostrar mensaje del backend. |
| `ProductoNotFoundException` | Producto inexistente o sin fila en vista de stock | 404 | `Producto no encontrado con ID: {id}` | Mostrar no encontrado y refrescar catálogo. |
| `StockInsuficienteException` | Salida excede `cantidadDisponible` | 409 | `Stock insuficiente para '{nombre}'. Disponible: X, Solicitado: Y` | Bloquear operación y refrescar stock. |
| `ReservaActivaInsuficienteException` | Ajuste físico menor que reservas activas | 409 | Indica físico y reservado activo | Explicar que no puede reducir por debajo de reservas. |
| `AccessDeniedException` | Usuario sin rol ADMIN | 403 | Mensaje de acceso denegado | Redirigir o mostrar permiso insuficiente. |
| `Exception` | Error no manejado | 500 | `Error interno del servidor` | Mostrar error genérico y registrar incidencia. |

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| Producto | Repository / entidad / excepción | `ProductoRepository`, `Producto`, `ProductoNotFoundException`, `StockInsuficienteException` | Buscar y modificar stock físico. | Alto: modifica `cantidadTotal`. |
| Producto / Stock | Vista SQL / repository | `StockProductoRepository` | Validar disponibilidad y devolver stock calculado. | Medio: depende de que la vista esté alineada con reservas. |
| Evento / Reservas | Repository | `EventoProductoRepository` | Validar reservas activas antes de ajuste. | Medio: evita inconsistencias con eventos programados/confirmados. |
| Usuario/Auth | Repository + SecurityContext | `UsuarioRepository`, `SecurityContextHolder` | Registrar quién hizo el movimiento. | Bajo: si falla, movimiento queda con usuario null. |
| Notificación | Service | `NotificacionService` | Alertar stock bajo. | Medio: notificación está dentro del flujo transaccional. |
| Auditoría | Service | `AuditoriaService` | Auditar ajustes manuales. | Medio: solo ajuste audita explícitamente en este módulo. |
| Common exceptions | Global handler | `ArtepaxException`, `GlobalExceptionHandler` | Formato común de errores. | Bajo. |

Dependencia circular:

```text
Detectada: No evidente en este módulo.
Detalle: MovimientoServiceImpl depende de repositories/servicios externos, pero no se observa que esos servicios dependan del módulo Movimiento.
Riesgo: Bajo.
Cómo se evita o cómo debería evitarse: Mantener los efectos de evento hacia inventario mediante handlers o servicios dedicados, no inyectando MovimientoService en EventoService si no es necesario.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Registrar entrada | Incrementa `producto.cantidadTotal` | Producto / Stock | Stock producto, listado producto, movimientos, dashboard inventario. |
| Registrar entrada | Crea `movimiento_inventario` | Movimiento | Historial del producto y listado global. |
| Registrar entrada | Puede generar notificación `ALERTA_STOCK` | Notificación | Centro de notificaciones. |
| Registrar salida | Decrementa `producto.cantidadTotal` | Producto / Stock | Stock producto, listado producto, movimientos, dashboard inventario. |
| Registrar salida | Crea `movimiento_inventario` | Movimiento | Historial del producto y listado global. |
| Registrar salida | Puede generar notificación `ALERTA_STOCK` | Notificación | Centro de notificaciones. |
| Registrar ajuste | Cambia `producto.cantidadTotal` si hay diferencia | Producto / Stock | Stock producto, detalle producto, movimientos. |
| Registrar ajuste | Crea movimiento de entrada/salida si hay diferencia | Movimiento | Historial producto y listado global. |
| Registrar ajuste | Registra auditoría | Auditoría | Vista de auditoría si existe. |
| Registrar ajuste | Puede generar notificación `ALERTA_STOCK` | Notificación | Centro de notificaciones. |
| Consultar stock | Ninguno | No aplica | No aplica. |
| Consultar movimientos | Ninguno | No aplica | No aplica. |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Registrar entrada | Producto seleccionado, cantidad > 0, origen `REPOSICION` o `AJUSTE_MANUAL` | Sin producto, cantidad inválida, origen no permitido | `Complete producto, cantidad y origen válido para entrada.` |
| Registrar salida | Producto seleccionado, cantidad > 0, origen `DAÑO`, `PERDIDA` o `AJUSTE_MANUAL` | Cantidad mayor al disponible o origen inválido | Mostrar mensaje del backend sobre stock insuficiente u origen inválido. |
| Ajustar stock | Producto activo, cantidad física >= 0, motivo no vacío | Producto inactivo o físico menor que reservas activas | `No se puede ajustar por debajo de reservas activas.` |
| Ver historial | Producto existente | Producto inexistente | `Producto no encontrado.` |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `productoId` | Al crear entrada/salida | Después de enviar | No debe cambiarse durante submit. |
| `cantidad` | Entrada/salida | Después de enviar | Debe ser mayor a 0. |
| `tipoOrigen` | Entrada/salida | Después de enviar | Mostrar opciones según tipo de operación. |
| `motivo` | Entrada/salida/ajuste | Después de enviar | En ajuste es obligatorio. |
| `cantidadFisica` | Ajuste | Después de enviar | Puede ser 0, pero no menor que reservas activas. |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Entrada | Stock del producto, movimientos del producto, listado global, dashboard inventario, notificaciones. |
| Salida | Stock del producto, movimientos del producto, listado global, dashboard inventario, notificaciones. |
| Ajuste | Stock del producto, detalle producto, movimientos del producto, listado global, auditoría, notificaciones. |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `ENTRADA` | Incremento físico | Badge positivo/neutro. |
| `SALIDA` | Decremento físico | Badge de advertencia. |
| `REPOSICION` | Reposición de stock | Etiqueta de origen. |
| `AJUSTE_MANUAL` | Ajuste por conteo físico | Etiqueta de origen. |
| `DAÑO` | Pérdida por daño | Etiqueta de riesgo. |
| `PERDIDA` | Pérdida extravío | Etiqueta de riesgo. |
| `stockBajoAlerta = true` | Disponible <= mínimo | Mostrar alerta visual de stock bajo. |

### 10.5 Reglas importantes

- No enviar `REPOSICION` en salidas; el service lo rechaza.
- Para salida, enviar explícitamente `DAÑO`, `PERDIDA` o `AJUSTE_MANUAL`.
- No calcular disponibilidad solo con `cantidadTotal`; usar el endpoint de stock o la vista ya expuesta por backend.
- Después de entrada, salida o ajuste, refrescar stock y movimientos.
- En ajuste, la cantidad física puede ser 0, pero el backend impedirá bajar por debajo de reservas activas.
- Mostrar mensajes de negocio del backend para `StockInsuficienteException` y `ReservaActivaInsuficienteException`.
- El historial puede traer `eventoId` y `eventoNombre` nulos.
- `eventoNombre` actualmente corresponde al nombre del cliente del evento, no a un título de evento.
- Las operaciones mutantes retornan `204 No Content`; el frontend debe refrescar manualmente los datos necesarios.

## Resumen para frontend

- Todos los endpoints están bajo `/api/inventario` y requieren rol `ADMIN`.
- Las entradas y salidas devuelven `204`, no un DTO actualizado.
- El frontend debe refrescar stock y movimientos después de cada operación mutante.
- Para entradas, los orígenes válidos son `REPOSICION` y `AJUSTE_MANUAL`.
- Para salidas, los orígenes válidos son `DAÑO`, `PERDIDA` y `AJUSTE_MANUAL`.
- El ajuste por conteo físico calcula automáticamente si debe ser entrada o salida.
- El ajuste no permite dejar el stock físico por debajo de reservas activas.
- La disponibilidad real viene de `vista_stock_producto`, no de `producto.cantidadTotal`.
- Las alertas de stock bajo se generan después de entrada, salida y ajuste si la vista indica `stockBajoAlerta`.
- Los movimientos pueden tener `eventoId` nulo; el frontend debe soportarlo.

## Observaciones técnicas detectadas

| Punto | Impacto | Recomendación |
|---|---|---|
| Mensaje de alerta de stock bajo duplica `Disponible` en `MovimientoServiceImpl`. | Bajo; afecta texto visible de notificación. | Corregir string para no repetir la frase. |
| `MovimientoRequestDTO` tiene default `tipoOrigen = REPOSICION`, pero salida usa default `DAÑO` solo si llega null. | Medio; si frontend omite origen en salida, probablemente se enviará `REPOSICION` y será rechazado. | En frontend, siempre enviar origen explícito según acción. |
| Solo el ajuste registra auditoría explícita. | Medio si se requiere trazabilidad completa de entradas/salidas. | Definir si entrada/salida manual deben auditarse también. |
| Notificación de stock bajo está dentro de la transacción. | Bajo/medio; si notificación falla, puede afectar la operación. | Evaluar best-effort en fase posterior si se requiere. |
