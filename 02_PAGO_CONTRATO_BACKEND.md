# Pago — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El módulo **Pago** administra los pagos asociados a eventos, sus catálogos operativos y el resumen financiero por evento. Permite registrar pagos, confirmarlos, anularlos, consultarlos por evento, consultar un resumen de cobros y administrar métodos de pago.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Registrar, confirmar, anular y consultar pagos de eventos. También expone catálogos de estado, concepto y método de pago. |
| Controller principal | `PagoController` bajo `/api/pagos` |
| Controller de catálogos | `CatalogosPagoController` bajo `/api/catalogos` |
| Service principal | `PagoService` / `PagoServiceImpl` |
| Service de catálogos | `CatalogosPagoService` / `CatalogosPagoServiceImpl` |
| Handler relevante | `PagoCancelacionHandler` |
| Entidades principales | `Pago`, `EstadoPago`, `ConceptoPago`, `MetodoPago`, `VistaPagosEvento` |
| Tablas/vistas implicadas | `pago`, `estado_pago`, `concepto_pago`, `metodo_pago`, `vista_pagos_evento` |
| Módulos relacionados | Evento, Notificación, Configuración indirecta por validación de anticipo, Catálogos |
| Tipo de módulo | Operativo + Catálogo + Resumen financiero |

El módulo **no** administra directamente el estado del evento, salvo por un efecto indirecto: al confirmar un pago puede activar la verificación de anticipo y llamar a `EventoService.confirmarDesdeAnticipo(...)` si el evento está en `PROGRAMADO` y alcanza el anticipo mínimo.

## 2. Controllers y endpoints completos

### 2.1 Endpoints de pagos

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| `POST` | `/api/pagos` | Registrar pago | `PagoRequestDTO` | `PagoResponseDTO` | `registrarPago(dto)` | `ADMIN` | Crea pago en estado `PENDIENTE`. Retorna HTTP `201`. |
| `PATCH` | `/api/pagos/{id}/confirmar` | Confirmar pago | N/A | `PagoResponseDTO` | `confirmarPago(id)` | `ADMIN` | Cambia `PENDIENTE` a `PAGADO`, asigna fecha y notifica. |
| `PATCH` | `/api/pagos/{id}/anular` | Anular pago | N/A | `PagoResponseDTO` | `anularPago(id)` | `ADMIN` | Solo anula pagos `PENDIENTE`. |
| `GET` | `/api/pagos/evento/{eventoId}` | Listar pagos de un evento | N/A | `List<PagoResponseDTO>` | `obtenerPorEvento(eventoId)` | `ADMIN` | Usa JOIN FETCH para evitar N+1. |
| `GET` | `/api/pagos/evento/{eventoId}/resumen` | Resumen financiero del evento | N/A | `ResumenPagoEventoDTO` | `getResumenPagos(eventoId)` | `ADMIN` | Usa `vista_pagos_evento`; si no hay fila en la vista, construye fallback desde `evento`. |
| `GET` | `/api/pagos` | Listar pagos paginados | `Pageable` | `Page<PagoResponseDTO>` | `obtenerTodos(pageable)` | `ADMIN` | Default `size=50`, `sort=id`. |

### POST `/api/pagos`

- Propósito: registrar un nuevo pago asociado a un evento.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: `PagoRequestDTO`.
- Response: `PagoResponseDTO`.
- HTTP esperado: `201 Created`.
- Método del service: `PagoServiceImpl.registrarPago(dto)`.
- Validaciones principales:
  - `eventoId`, `monto`, `metodoPagoId` y `conceptoPagoId` son obligatorios.
  - `monto` debe ser mayor o igual a `0.01`.
  - El evento se busca con bloqueo pesimista mediante `eventoRepository.findByIdForUpdate(...)`.
  - Se bloquea registro de pagos si el evento está en `CANCELADO`, `CANCELACION_PENDIENTE`, `FINALIZADO` o `COTIZACION`.
  - El total activo del evento (`PAGADO + PENDIENTE`) más el nuevo pago no puede exceder `evento.precioTotal`.
  - El método de pago debe existir y estar activo.
  - El concepto de pago debe existir.
- Excepciones posibles:
  - `EventoNotFoundException` si el evento no existe.
  - `PagoException` por estado inválido, sobrepago o método inactivo.
  - `EstadoPagoNoConfiguradoException` si falta `PENDIENTE`.
  - `MetodoPagoNotFoundException` si no existe el método.
  - `ConceptoPagoNotFoundException` si no existe el concepto.
- Efectos secundarios:
  - Crea registro en `pago` con estado `PENDIENTE`.
  - No notifica.
  - No cambia el estado del evento.
- Qué debe refrescar el frontend:
  - Lista de pagos del evento.
  - Resumen de pagos del evento.
  - Detalle/resumen interno del evento si muestra pagos.

**Nota de consistencia:** `COTIZACION` bloquea correctamente tanto el registro como la confirmación de pagos. No se aceptan pagos en cotización porque es solo un presupuesto, no un compromiso del cliente. Los pagos se habilitan a partir de `PROGRAMADO`.

### PATCH `/api/pagos/{id}/confirmar`

- Propósito: confirmar un pago pendiente.
- Path variables: `id` del pago.
- Query params: no aplica.
- Request body: no aplica.
- Response: `PagoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `PagoServiceImpl.confirmarPago(id)`.
- Validaciones principales:
  - El pago debe existir.
  - Solo se puede confirmar si el pago está en `PENDIENTE`.
  - El evento se bloquea con `findByIdForUpdate(...)`.
  - El estado del evento debe estar dentro de: `PROGRAMADO`, `CONFIRMADO`, `EN_MONTAJE`.
  - Los pagos confirmados excluyendo el pago actual, más el pago actual, no pueden exceder `evento.precioTotal`.
  - Debe existir el estado de pago `PAGADO`.
- Excepciones posibles:
  - `PagoNotFoundException`.
  - `PagoException` por estado de pago inválido, estado de evento inválido o sobrepago.
  - `EventoNotFoundException`.
  - `EstadoPagoNoConfiguradoException`.
- Efectos secundarios:
  - Cambia el pago a `PAGADO`.
  - Asigna `pago.fecha = LocalDateTime.now()`.
  - Notifica a administradores con tipo `PAGO_RECIBIDO`.
  - Si el evento está en `PROGRAMADO` y alcanza anticipo mínimo, llama a `EventoService.confirmarDesdeAnticipo(...)`.
- Qué debe refrescar el frontend:
  - Lista de pagos del evento.
  - Resumen de pagos del evento.
  - Detalle del evento.
  - Listado/calendario de eventos si el estado pudo cambiar a `CONFIRMADO`.
  - Notificaciones.

### PATCH `/api/pagos/{id}/anular`

- Propósito: anular un pago pendiente.
- Path variables: `id` del pago.
- Query params: no aplica.
- Request body: no aplica.
- Response: `PagoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `PagoServiceImpl.anularPago(id)`.
- Validaciones principales:
  - El pago debe existir.
  - Solo se pueden anular pagos en estado `PENDIENTE`.
  - Debe existir el estado `CANCELADO` en `estado_pago`.
- Excepciones posibles:
  - `PagoNotFoundException`.
  - `PagoException` si el pago no está en `PENDIENTE`.
  - `EstadoPagoNoConfiguradoException`.
- Efectos secundarios:
  - Cambia el estado del pago a `CANCELADO`.
  - No notifica.
  - No cambia el estado del evento.
- Qué debe refrescar el frontend:
  - Lista de pagos del evento.
  - Resumen de pagos del evento.
  - Detalle/resumen financiero del evento.

### GET `/api/pagos/evento/{eventoId}`

- Propósito: obtener todos los pagos de un evento.
- Path variables: `eventoId`.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<PagoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `PagoServiceImpl.obtenerPorEvento(eventoId)`.
- Validaciones principales: no valida existencia del evento; retorna la lista que encuentre por `eventoId`.
- Excepciones posibles: errores no manejados de persistencia.
- Efectos secundarios: no tiene.
- Qué debe refrescar el frontend: no aplica; es endpoint de lectura.

### GET `/api/pagos/evento/{eventoId}/resumen`

- Propósito: obtener resumen financiero del evento.
- Path variables: `eventoId`.
- Query params: no aplica.
- Request body: no aplica.
- Response: `ResumenPagoEventoDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `PagoServiceImpl.getResumenPagos(eventoId)`.
- Validaciones principales:
  - Busca `vista_pagos_evento` por `idEvento`.
  - Si no existe fila en la vista, busca el evento por ID y arma un resumen con valores cero.
- Excepciones posibles:
  - `EventoNotFoundException` si no existe fila en la vista y tampoco existe el evento.
- Efectos secundarios: no tiene.
- Qué debe refrescar el frontend: no aplica; es endpoint de lectura.

**Nota:** la entidad `VistaPagosEvento` ya mapea `total_devolucion_pendiente` y `tiene_devolucion_pendiente`. `ResumenPagoEventoDTO` ahora los expone.

### GET `/api/pagos`

- Propósito: obtener listado global paginado de pagos.
- Path variables: no aplica.
- Query params: parámetros estándar de `Pageable`: `page`, `size`, `sort`.
- Request body: no aplica.
- Response: `Page<PagoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `PagoServiceImpl.obtenerTodos(pageable)`.
- Validaciones principales: no aplica.
- Excepciones posibles: errores no manejados de persistencia.
- Efectos secundarios: no tiene.
- Qué debe refrescar el frontend: no aplica; es endpoint de lectura.

### 2.2 Endpoints de catálogos de pago

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| `GET` | `/api/catalogos/estados-pago` | Listar estados de pago | N/A | `List<EstadoPagoResponseDTO>` | `listarEstados()` | `ADMIN` | Catálogo de estados. |
| `GET` | `/api/catalogos/conceptos-pago` | Listar conceptos de pago | N/A | `List<ConceptoPagoResponseDTO>` | `listarConceptos()` | `ADMIN` | Catálogo de conceptos. |
| `GET` | `/api/catalogos/metodos-pago` | Listar métodos de pago | N/A | `List<MetodoPagoResponseDTO>` | `listarMetodos()` | `ADMIN` | Incluye activos e inactivos. |
| `POST` | `/api/catalogos/metodos-pago` | Crear método de pago | `MetodoPagoCreateDTO` | `MetodoPagoResponseDTO` | `crearMetodo(nombre, descripcion)` | `ADMIN` | Retorna `200 OK`, no `201`. |
| `PATCH` | `/api/catalogos/metodos-pago/{id}/toggle` | Activar/desactivar método de pago | N/A | `MetodoPagoResponseDTO` | `toggleMetodo(id)` | `ADMIN` | Cambia `metodoActivo` al valor contrario. |

## 3. DTOs request/response

### 3.1 DTOs Request

#### PagoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `eventoId` | `Long` | `eventoId` | Sí | No | `@NotNull` | ID del evento al que se asociará el pago. |
| `monto` | `BigDecimal` | `monto` | Sí | No | `@NotNull`, `@DecimalMin("0.01")` | Debe ser mayor a cero. El backend valida sobrepago. |
| `metodoPagoId` | `Long` | `metodoPagoId` | Sí | No | `@NotNull` | Debe corresponder a un método existente y activo. |
| `conceptoPagoId` | `Long` | `conceptoPagoId` | Sí | No | `@NotNull` | Debe corresponder a un concepto existente. |

#### MetodoPagoCreateDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `nombre` | `String` | `nombre` | Sí | No | `@NotBlank`, `@Size(2,100)`, `@Pattern` letras y espacios | No se aceptan nombres solo numéricos. |
| `descripcion` | `String` | `descripcion` | No | Sí | `@Size(max=500)`; validación manual si no está en blanco | Si se envía, debe contener texto válido con al menos una letra. |

### 3.2 DTOs Response

#### PagoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `pago.id` | Identificador del pago. |
| `monto` | `BigDecimal` | `monto` | No | `pago.monto` | Monto del pago. |
| `fecha` | `LocalDateTime` | `fecha` | Sí | `pago.fecha` | Nulo mientras el pago está `PENDIENTE`; se asigna al confirmar. |
| `eventoId` | `Long` | `eventoId` | Sí | `pago.evento.id` | Evento asociado. |
| `estadoPago` | `String` | `estadoPago` | Sí | `pago.estadoPago.estadoNombre` | Usar para badge y acciones. |
| `conceptoPago` | `String` | `conceptoPago` | Sí | `pago.conceptoPago.conceptoNombre` | Ej. `ANTICIPO`, `ABONO`, etc. |
| `metodoPago` | `String` | `metodoPago` | Sí | `pago.metodoPago.metodoNombre` | Medio de pago. |
| `clienteNombre` | `String` | `clienteNombre` | Sí | `pago.evento.cliente.nombre` | Útil en listado global. |
| `estadoEventoNombre` | `String` | `estadoEventoNombre` | Sí | `pago.evento.estadoEvento.nombre` | Útil para bloquear acciones desde UI. |

#### ResumenPagoEventoDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `idEvento` | `Long` | `idEvento` | No | `vista_pagos_evento.id_evento` o fallback | Identificador del evento. |
| `precioTotal` | `BigDecimal` | `precioTotal` | No | `vista_pagos_evento.precio_total` o `evento.precioTotal` | Total del evento. |
| `totalCobrado` | `BigDecimal` | `totalCobrado` | No | `vista_pagos_evento.total_cobrado` | Suma de pagos `PAGADO`. |
| `saldoPendiente` | `BigDecimal` | `saldoPendiente` | No | `vista_pagos_evento.saldo_pendiente` | `precioTotal - totalCobrado`. |
| `cobroCompleto` | `Boolean` | `cobroCompleto` | No | `vista_pagos_evento.cobro_completo` | Indica si total cobrado cubre el precio total. |
| `totalDevolucionPendiente` | `BigDecimal` | `totalDevolucionPendiente` | No | `vista_pagos_evento.total_devolucion_pendiente` | Suma de pagos en `DEVOLUCION_PENDIENTE`. |
| `tieneDevolucionPendiente` | `Boolean` | `tieneDevolucionPendiente` | No | `vista_pagos_evento.tiene_devolucion_pendiente` | Flag de si hay devolución pendiente. |

#### EstadoPagoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `estado_pago.id_estado_pago` | ID del estado. |
| `estadoNombre` | `String` | `estadoNombre` | No | `estado_pago.estado_nombre` | Usar para badges y reglas. |
| `estadoDescripcion` | `String` | `estadoDescripcion` | Sí | `estado_pago.estado_descripcion` | Texto descriptivo. |

#### ConceptoPagoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `concepto_pago.id_concepto_pago` | ID del concepto. |
| `conceptoNombre` | `String` | `conceptoNombre` | No | `concepto_pago.concepto_nombre` | Usar en selects. |
| `conceptoDescripcion` | `String` | `conceptoDescripcion` | Sí | `concepto_pago.concepto_descripcion` | Texto descriptivo. |

#### MetodoPagoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `metodo_pago.id_metodo_pago` | ID del método. |
| `metodoNombre` | `String` | `metodoNombre` | No | `metodo_pago.metodo_nombre` | Mostrar en select/listado. |
| `metodoDescripcion` | `String` | `metodoDescripcion` | Sí | `metodo_pago.metodo_descripcion` | Descripción. |
| `metodoActivo` | `Boolean` | `metodoActivo` | No | `metodo_pago.metodo_activo` | El frontend debe ocultar o deshabilitar inactivos para nuevos pagos. |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Pago` | `pago` | Tabla operativa | Registro de pagos por evento. | Relaciona evento, estado, concepto y método. |
| `EstadoPago` | `estado_pago` | Catálogo | Estados del ciclo de vida del pago. | Incluye al menos `PENDIENTE`, `PAGADO`, `CANCELADO`, `DEVOLUCION_PENDIENTE` según cambios recientes. |
| `ConceptoPago` | `concepto_pago` | Catálogo | Propósito del pago. | Ej. anticipo, abono, saldo, penalización. |
| `MetodoPago` | `metodo_pago` | Catálogo editable | Medio de pago. | Tiene flag `metodoActivo`. |
| `VistaPagosEvento` | `vista_pagos_evento` | Vista solo lectura | Resumen de cobros por evento. | Mapea también devolución pendiente, y el DTO de resumen ahora la expone. |

### Vista `vista_pagos_evento`

| Columna | Significado | Uso actual |
|---|---|---|
| `id_evento` | Evento resumido | ID del resumen. |
| `precio_total` | Total acordado del evento | `ResumenPagoEventoDTO.precioTotal`. |
| `total_cobrado` | Suma de pagos `PAGADO` | `ResumenPagoEventoDTO.totalCobrado`. |
| `saldo_pendiente` | `precio_total - total_cobrado` | `ResumenPagoEventoDTO.saldoPendiente`. |
| `cobro_completo` | Si `total_cobrado >= precio_total` | `ResumenPagoEventoDTO.cobroCompleto`. |
| `total_devolucion_pendiente` | Suma de pagos en `DEVOLUCION_PENDIENTE` | `ResumenPagoEventoDTO.totalDevolucionPendiente`. |
| `tiene_devolucion_pendiente` | Flag de devolución pendiente | `ResumenPagoEventoDTO.tieneDevolucionPendiente`. |

La vista debe tratarse como resumen de solo lectura. La lógica de cancelación y cambios de estado de pago usa directamente la tabla `pago` y `estado_pago`, no la vista.

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Registrar pago | `registrarPago(dto)` | Evento, estado del evento, sobrepago, método activo, concepto, estado `PENDIENTE` | Crea `Pago` en `PENDIENTE` | `PagoResponseDTO` |
| Confirmar pago | `confirmarPago(id)` | Pago existe, pago en `PENDIENTE`, evento en estado permitido, sobrepago, estado `PAGADO` | Pago pasa a `PAGADO`, asigna fecha | `PagoResponseDTO` |
| Anular pago | `anularPago(id)` | Pago existe, pago en `PENDIENTE`, estado `CANCELADO` | Pago pasa a `CANCELADO` | `PagoResponseDTO` |
| Obtener pagos por evento | `obtenerPorEvento(eventoId)` | No valida evento explícitamente | No modifica | `List<PagoResponseDTO>` |
| Obtener todos | `obtenerTodos(pageable)` | No aplica | No modifica | `Page<PagoResponseDTO>` |
| Resumen de pagos | `getResumenPagos(eventoId)` | Vista o fallback a evento | No modifica | `ResumenPagoEventoDTO` |
| Listar estados | `listarEstados()` | No aplica | No modifica | `List<EstadoPagoResponseDTO>` |
| Listar conceptos | `listarConceptos()` | No aplica | No modifica | `List<ConceptoPagoResponseDTO>` |
| Listar métodos | `listarMetodos()` | No aplica | No modifica | `List<MetodoPagoResponseDTO>` |
| Crear método | `crearMetodo(nombre, descripcion)` | Texto válido, no duplicado | Crea `MetodoPago` activo | `MetodoPagoResponseDTO` |
| Toggle método | `toggleMetodo(id)` | Método existe | Invierte `metodoActivo` | `MetodoPagoResponseDTO` |
| Procesar pagos por cancelación | `PagoCancelacionHandler.procesarPagosPorCancelacion(evento, motivo)` | Estados `CANCELADO` y `DEVOLUCION_PENDIENTE` configurados | `PENDIENTE -> CANCELADO`, `PAGADO -> DEVOLUCION_PENDIENTE` | `BigDecimal totalDevolucion` |

### Flujo: registrar pago

1. Busca el evento por `eventoId` con bloqueo pesimista (`findByIdForUpdate`).
2. Valida el estado del evento.
3. Calcula `sumMontoActivoByEventoId`, que suma pagos `PAGADO + PENDIENTE`.
4. Rechaza si el monto activo más el nuevo monto excede `evento.precioTotal`.
5. Busca estado `PENDIENTE`.
6. Busca método de pago y valida que esté activo.
7. Busca concepto de pago.
8. Crea el pago con fecha `null` y estado `PENDIENTE`.
9. Guarda y retorna `PagoResponseDTO`.

### Flujo: confirmar pago

1. Busca el pago por ID.
2. Valida que esté en `PENDIENTE`.
3. Busca y bloquea el evento asociado.
4. Valida que el estado del evento esté permitido para confirmar pagos.
5. Calcula total confirmado excluyendo el pago actual.
6. Rechaza si confirmar el pago excede el precio total del evento.
7. Cambia estado a `PAGADO`.
8. Asigna fecha actual.
9. Guarda el pago.
10. Notifica a administradores con tipo `PAGO_RECIBIDO`.
11. Si el evento está en `PROGRAMADO`, verifica anticipo mínimo y puede llamar a `EventoService.confirmarDesdeAnticipo(...)`.
12. Retorna `PagoResponseDTO`.

### Flujo: anular pago

1. Busca el pago por ID.
2. Valida que esté en `PENDIENTE`.
3. Busca estado `CANCELADO`.
4. Cambia el estado del pago a `CANCELADO`.
5. Guarda y retorna `PagoResponseDTO`.

### Flujo: cancelación de evento y pagos

Este flujo lo ejecuta `EventoServiceImpl.confirmarCancelacion(...)` mediante `PagoCancelacionHandler`.

1. Busca pagos del evento en estados `PENDIENTE` o `PAGADO`.
2. Si no hay pagos, retorna `BigDecimal.ZERO`.
3. Busca estado `CANCELADO`.
4. Si hay pagos `PAGADO`, busca estado `DEVOLUCION_PENDIENTE`.
5. Cambia pagos `PENDIENTE` a `CANCELADO`.
6. Cambia pagos `PAGADO` a `DEVOLUCION_PENDIENTE`.
7. Guarda todos los pagos.
8. Si existe monto pendiente de devolución, notifica a administradores con tipo `CANCELACION_EVENTO`.
9. Retorna el total pendiente de devolución.

## 6. Estados y transiciones

### Estados existentes

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `PENDIENTE` | Pago registrado pero no confirmado | Registro inicial, confirmación, anulación | Sí | Puede confirmarse o anularse. |
| `PAGADO` | Pago confirmado y con impacto en caja | Confirmación de pago, totales cobrados | Sí | No se anula directamente. |
| `DEVOLUCION_PENDIENTE` | Pago cobrado pendiente de devolución por cancelación del evento | Cancelación definitiva de evento con pagos pagados | Sí | Está expuesto en `ResumenPagoEventoDTO` y en `PagoResponseDTO.estadoPago`. |

### Transiciones permitidas

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| N/A | `PENDIENTE` | `POST /api/pagos` | Evento válido, método activo, concepto válido, no sobrepago | Crea pago. |
| `PENDIENTE` | `PAGADO` | `PATCH /api/pagos/{id}/confirmar` | Pago pendiente, evento en estado permitido, no sobrepago | Asigna fecha, notifica, puede confirmar evento desde anticipo. |
| `PENDIENTE` | `CANCELADO` | `PATCH /api/pagos/{id}/anular` | Pago pendiente | Anula pago. |
| `PENDIENTE` | `CANCELADO` | Cancelación definitiva de evento | Evento en cancelación pendiente | Gestionado por `PagoCancelacionHandler`. |
| `PAGADO` | `DEVOLUCION_PENDIENTE` | Cancelación definitiva de evento | Evento en cancelación pendiente | Notifica monto pendiente de devolución. |

### Estados de evento relevantes para pagos

| Estado evento | Registrar pago | Confirmar pago | Notas |
|---|---|---|---|
| `COTIZACION` | Bloqueado | Bloqueado | No se aceptan pagos en cotización: es solo un presupuesto, no un compromiso. |
| `PROGRAMADO` | Permitido | Permitido | Puede disparar confirmación automática por anticipo. |
| `CONFIRMADO` | Permitido | Permitido | No dispara confirmación automática porque ya está confirmado. |
| `EN_MONTAJE` | Permitido | Permitido | Permite pagos durante montaje. |
| `FINALIZADO` | Bloqueado | Bloqueado | No se registran ni confirman pagos. |
| `CANCELACION_PENDIENTE` | Bloqueado | Bloqueado | La cancelación ya está en proceso. |
| `CANCELADO` | Bloqueado | Bloqueado | Estado terminal. |

## 7. Excepciones y respuestas de error

El sistema usa `ErrorResponse` con esta forma:

| Campo | Significado |
|---|---|
| `codigo` | Código HTTP numérico. |
| `mensaje` | Mensaje de error. |
| `errorCode` | Código interno opcional; puede ser `null`. |
| `timestamp` | Fecha/hora ISO local. |

`GlobalExceptionHandler` maneja `ArtepaxException` usando el `HttpStatus` definido por cada excepción. Validaciones de DTO (`MethodArgumentNotValidException`) devuelven `400`. `IllegalArgumentException` devuelve `400`. Errores de integridad devuelven `409`. Acceso denegado devuelve `403`. Errores no manejados devuelven `500` con mensaje genérico.

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `PagoException` | Regla de negocio de pago incumplida | `400` | Mensaje específico del service | Mostrar mensaje del backend. |
| `PagoNotFoundException` | Pago inexistente | `404` | `Pago no encontrado: {id}` | Mostrar error y refrescar listado. |
| `EventoNotFoundException` | Evento inexistente al registrar/resumir pago | `404` | `Evento no encontrado: {id}` | Evitar permitir pago sobre evento inexistente. |
| `MetodoPagoNotFoundException` | Método de pago inexistente | `404` | `Método de pago no encontrado: {id}` | Refrescar catálogos. |
| `ConceptoPagoNotFoundException` | Concepto de pago inexistente | `404` | `Concepto de pago no encontrado: {id}` | Refrescar catálogos. |
| `EstadoPagoNoConfiguradoException` | Falta un estado requerido en `estado_pago` | `500` | `Estado {estado} no encontrado en BD` | Mostrar error técnico; requiere corrección backend/BD. |
| `MetodoPagoDuplicadoException` | Ya existe método con el mismo nombre | `409` | `Ya existe un método de pago con el nombre: {nombre}` | Mostrar como conflicto de catálogo. |
| `MethodArgumentNotValidException` | Validación DTO fallida | `400` | Primer error de campo, formato `campo: mensaje` | Resaltar campo en formulario. |
| `IllegalArgumentException` | Validación manual de catálogo fallida | `400` | Ej. `El nombre debe contener texto válido` | Mostrar mensaje en formulario. |
| `AccessDeniedException` | Usuario sin rol requerido | `403` | Mensaje de Spring Security | Redirigir o mostrar sin permisos. |
| `Exception` | Error no manejado | `500` | `Error interno del servidor` | Mostrar mensaje genérico. |

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| `EventoRepository` | Repository de otro módulo | `PagoServiceImpl` | Buscar/bloquear evento al registrar o confirmar pagos; fallback del resumen. | Acoplamiento fuerte con Evento. |
| `EventoService` | Service de otro módulo | `PagoServiceImpl.verificarAnticipo(...)` | Confirmar evento automáticamente desde anticipo. | Acoplamiento operativo Pago -> Evento. |
| `EventoPagoValidator` | Handler/validator de Evento | `PagoServiceImpl.verificarAnticipo(...)` | Verificar si el anticipo mínimo fue alcanzado. | Comparte regla de anticipo entre Evento y Pago. |
| `NotificacionService` | Service de Notificación | `PagoServiceImpl`, `PagoCancelacionHandler` | Notificar pago recibido y devoluciones pendientes. | Si falla la notificación dentro de transacción, puede hacer rollback. |
| `VistaPagosEventoRepository` | Vista SQL | `PagoServiceImpl.getResumenPagos(...)` | Obtener resumen financiero del evento. | El DTO ya expone todos los campos de la vista. |
| `EstadoPagoRepository` | Repository local | `PagoServiceImpl`, `PagoCancelacionHandler` | Obtener estados por nombre. | Si el catálogo está incompleto, falla la operación. |
| `MetodoPagoRepository` | Repository local | `PagoServiceImpl`, `CatalogosPagoServiceImpl` | Validar método, crear y activar/desactivar. | Duplicados dependen de validación por nombre. |
| `ConceptoPagoRepository` | Repository local | `PagoServiceImpl`, `CatalogosPagoServiceImpl` | Validar concepto y listar catálogos. | Catálogo requerido para registrar pagos. |

```text
Dependencia circular:
- Detectada: No a nivel de beans directos PagoServiceImpl <-> EventoServiceImpl.
- Detalle: PagoServiceImpl depende de EventoService para confirmar desde anticipo. EventoServiceImpl no depende de PagoServiceImpl; usa PagoCancelacionHandler para evitar una dependencia circular directa.
- Riesgo: Existe acoplamiento bidireccional a nivel de módulos Pago/Eventos, pero se evitó el ciclo directo de servicios usando handler.
- Cómo se evita o debería evitarse: Mantener PagoCancelacionHandler independiente de EventoService y PagoService. Si crecen más reglas cruzadas, considerar eventos de dominio o handlers dedicados.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Registrar pago | Crea pago `PENDIENTE` | Pago | Pagos del evento, resumen de pagos, detalle del evento si muestra resumen. |
| Confirmar pago | Cambia pago a `PAGADO` y asigna fecha | Pago | Pagos del evento, resumen de pagos. |
| Confirmar pago | Notifica a administradores `PAGO_RECIBIDO` | Notificación | Notificaciones. |
| Confirmar pago | Puede confirmar evento automáticamente si anticipo mínimo se cumple | Evento | Detalle de evento, listado/calendario, resumen de pagos, notificaciones. |
| Anular pago | Cambia pago `PENDIENTE` a `CANCELADO` | Pago | Pagos del evento, resumen de pagos. |
| Confirmar cancelación de evento | `PENDIENTE -> CANCELADO`, `PAGADO -> DEVOLUCION_PENDIENTE` | Pago / Evento | Pagos del evento, resumen de pagos, estado del evento, notificaciones. |
| Confirmar cancelación de evento con pagos pagados | Notifica monto pendiente de devolución | Notificación | Notificaciones. |
| Crear método de pago | Crea método activo | Catálogos | Catálogo de métodos. |
| Toggle método de pago | Activa/desactiva método | Catálogos | Catálogo de métodos, formularios de pago. |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Registrar pago | Evento en estado permitido por backend; método activo; monto válido | Evento `CANCELADO`, `CANCELACION_PENDIENTE`, `FINALIZADO`, `COTIZACION` | “No se pueden registrar pagos en eventos con estado: {estado}”. |
| Confirmar pago | Pago en `PENDIENTE` y evento en estado permitido | Pago distinto de `PENDIENTE`; evento no permitido; sobrepago | Mostrar mensaje del backend. |
| Anular pago | Pago en `PENDIENTE` | Pago `PAGADO`, `CANCELADO`, `DEVOLUCION_PENDIENTE` | “Solo se pueden anular pagos en estado PENDIENTE”. |
| Crear método de pago | Nombre válido y no duplicado | Nombre vacío, numérico, duplicado o descripción inválida | Mostrar validación del backend. |
| Activar/desactivar método | Método existente | Método inexistente | “Método de pago no encontrado”. |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `monto` | Solo al registrar pago | Después de creado | No hay endpoint para editar monto. |
| `metodoPagoId` | Solo al registrar pago | Después de creado | No hay endpoint para editar método de un pago existente. |
| `conceptoPagoId` | Solo al registrar pago | Después de creado | No hay endpoint para editar concepto. |
| `estadoPago` | No editable directamente | Siempre | Cambia por confirmar, anular o cancelación de evento. |
| `fecha` | No editable | Siempre | Se asigna automáticamente al confirmar. |
| `metodoNombre` | Al crear método de pago | No hay endpoint de edición, solo toggle | Si se requiere edición, no está implementado. |
| `metodoActivo` | Mediante toggle | No se envía directamente | `PATCH /api/catalogos/metodos-pago/{id}/toggle`. |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Registrar pago | Lista de pagos del evento, resumen de pagos, detalle/resumen interno del evento. |
| Confirmar pago | Lista de pagos, resumen de pagos, evento, calendario/listado de eventos si el estado pudo cambiar, notificaciones. |
| Anular pago | Lista de pagos, resumen de pagos. |
| Confirmar cancelación de evento | Lista de pagos, resumen de pagos, evento, calendario/listado, notificaciones. |
| Crear método de pago | Catálogo de métodos. |
| Toggle método de pago | Catálogo de métodos y formularios que muestren métodos disponibles. |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `PENDIENTE` | Pago registrado sin confirmar | Badge pendiente; acciones: confirmar/anular. |
| `PAGADO` | Pago confirmado | Badge pagado; sin acción de anular. |
| `CANCELADO` | Pago pendiente anulado | Badge cancelado; solo lectura. |
| `DEVOLUCION_PENDIENTE` | Pago cobrado pendiente de devolución | Badge devolución pendiente; requiere gestión administrativa posterior. |

### 10.5 Reglas importantes

- No calcular sobrepago solo en frontend; el backend valida con bloqueo y suma de pagos activos.
- El frontend debe mostrar el mensaje de negocio devuelto por el backend cuando sea `400`.
- No permitir confirmar o anular pagos que no estén en `PENDIENTE`.
- No mostrar métodos de pago inactivos para registrar nuevos pagos.
- Después de confirmar pago, el evento puede cambiar de `PROGRAMADO` a `CONFIRMADO`; refrescar evento y calendario/listado.
- `fecha` puede venir `null` en pagos pendientes.
- `DEVOLUCION_PENDIENTE` puede aparecer como estado de pago después de cancelar un evento.
- La vista `vista_pagos_evento` es de resumen; no debe usarse para intentar modificar pagos.
- Los campos de devolución pendiente ya se exponen en `ResumenPagoEventoDTO`.
- El código bloquea correctamente el registro y confirmación de pagos en `COTIZACION`. Los pagos se habilitan a partir de `PROGRAMADO`.

## 10.6 Recomendaciones UX/UI concretas

| Escenario | Mejora recomendada |
|---|---|
| Lista de pagos por evento | Separar visualmente `PENDIENTE`, `PAGADO`, `CANCELADO` y `DEVOLUCION_PENDIENTE` con badges bien contrastados y acciones contextuales. |
| Confirmar pago | Deshabilitar el botón inmediatamente tras el click y mostrar estado “Confirmando…” para evitar doble confirmación. |
| Anular pago | Pedir confirmación modal solo si el pago está `PENDIENTE`; si no, ocultar la acción en lugar de dejar que falle. |
| Resumen financiero | Mostrar además de `totalCobrado` un bloque de “Devolución pendiente” cuando `tieneDevolucionPendiente = true`. |
| Catálogo de métodos | En formularios de pago, ocultar o deshabilitar métodos inactivos; en pantallas administrativas sí conviene mostrar todos con su estado. |
| Integración con eventos | Después de confirmar un pago, refrescar el bloque de estado del evento sin recargar toda la pantalla para que el cambio `PROGRAMADO -> CONFIRMADO` se vea inmediato. |

## Resumen para frontend

- El módulo expone pagos bajo `/api/pagos` y catálogos bajo `/api/catalogos`.
- Todo requiere rol `ADMIN`.
- Registrar pago crea un pago `PENDIENTE`.
- Confirmar pago cambia a `PAGADO`, asigna fecha, notifica y puede confirmar automáticamente el evento si cumple anticipo.
- Anular solo aplica a pagos `PENDIENTE`.
- Cancelar un evento puede cambiar pagos `PENDIENTE` a `CANCELADO` y pagos `PAGADO` a `DEVOLUCION_PENDIENTE`.
- `GET /api/pagos/evento/{eventoId}/resumen` devuelve siete campos, incluyendo `totalDevolucionPendiente` y `tieneDevolucionPendiente`.
- Los métodos de pago inactivos no deben ofrecerse para nuevos pagos.
- Después de confirmar pagos, refrescar también el evento porque puede cambiar de estado.
- `COTIZACION` no acepta pagos: es un presupuesto, no un compromiso. El flujo correcto es PROGRAMADO -> pagos -> CONFIRMADO.
