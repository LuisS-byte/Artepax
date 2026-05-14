# pago — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Gestiona el registro, confirmación y anulación de pagos de clientes por evento, más los catálogos asociados (estados, conceptos, métodos de pago). Incluye lógica de blindaje contra sobrecobros, validación de anticipo mínimo, auto-transición de evento, y procesamiento de devoluciones al cancelar.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Gestión de pagos de clientes y sus catálogos |
| Controller principal | `PagoController` (`/api/pagos`) + `CatalogosPagoController` (`/api/catalogos`) |
| Service principal | `PagoServiceImpl` + `CatalogosPagoServiceImpl` |
| Entidades principales | `Pago`, `EstadoPago`, `ConceptoPago`, `MetodoPago`, `VistaPagosEvento` |
| Tablas/vistas implicadas | `pago`, `estado_pago`, `concepto_pago`, `metodo_pago`, `vista_pagos_evento` |
| Módulos relacionados | `evento`, `notificacion`, `common` |
| Tipo de módulo | Mixto (Operativo + Catálogo + Reporte) |

## 2. Controllers y endpoints completos

### 2.1 PagoController (`/api/pagos`)

Todos los endpoints requieren `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/pagos` | Registrar pago | `PagoRequestDTO` | `PagoResponseDTO` (201) | `registrarPago` | ADMIN | Lock pesimista en evento |
| PATCH | `/api/pagos/{id}/confirmar` | Confirmar pago | — | `PagoResponseDTO` | `confirmarPago` | ADMIN | Lock pesimista. Dispara auto-transición si anticipo |
| PATCH | `/api/pagos/{id}/anular` | Anular pago | — | `PagoResponseDTO` | `anularPago` | ADMIN | Solo PENDIENTE → CANCELADO |
| GET | `/api/pagos/evento/{eventoId}` | Pagos por evento | — | `List<PagoResponseDTO>` | `obtenerPorEvento` | ADMIN | Con JOIN FETCH |
| GET | `/api/pagos/evento/{eventoId}/resumen` | Resumen financiero | — | `ResumenPagoEventoDTO` | `getResumenPagos` | ADMIN | Vista SQL |
| GET | `/api/pagos` | Listado paginado | — | `Page<PagoResponseDTO>` | `obtenerTodos` | ADMIN | Default: size=50, sort=id |

### 2.2 CatalogosPagoController (`/api/catalogos`)

| Método | Ruta | Acción | Request | Response | Service llamado | Auth | Notas |
|---|---|---|---|---|---|---|---|
| GET | `/api/catalogos/estados-pago` | Listar estados | — | `List<EstadoPagoResponseDTO>` | `listarEstados` | ADMIN | Solo lectura |
| GET | `/api/catalogos/conceptos-pago` | Listar conceptos | — | `List<ConceptoPagoResponseDTO>` | `listarConceptos` | ADMIN | Solo lectura |
| GET | `/api/catalogos/metodos-pago` | Listar métodos | — | `List<MetodoPagoResponseDTO>` | `listarMetodos` | ADMIN | Solo lectura |
| POST | `/api/catalogos/metodos-pago` | Crear método | `MetodoPagoCreateDTO` | `MetodoPagoResponseDTO` | `crearMetodo` | ADMIN | Valida nombre con letras |
| PATCH | `/api/catalogos/metodos-pago/{id}/toggle` | Activar/desactivar | — | `MetodoPagoResponseDTO` | `toggleMetodo` | ADMIN | Toggle booleano |

### POST /api/pagos

- **Propósito:** Registrar un nuevo pago para un evento. El pago queda en estado PENDIENTE hasta confirmación.
- **Request body:** `PagoRequestDTO`
- **Response:** `PagoResponseDTO` con HTTP 201
- **Método del service:** `registrarPago(PagoRequestDTO)`
- **Validaciones principales:**
  - `eventoId`, `monto`, `metodoPagoId`, `conceptoPagoId` son `@NotNull`
  - `monto > 0` (`@DecimalMin("0.01")`)
  - Lock pesimista en evento (`findByIdForUpdate`)
  - Evento NO puede estar en CANCELADO, CANCELACION_PENDIENTE, FINALIZADO, COTIZACION
  - Suma de pagos activos (PENDIENTE + PAGADO) + nuevo monto no puede exceder `precioTotal`
  - Método de pago debe estar activo (`metodoActivo = true`)
- **Excepciones posibles:**
  - `EventoNotFoundException` (404)
  - `PagoException` (400): "No se pueden registrar pagos en eventos con estado: X"
  - `PagoException` (400): "El pago excede el precio total del evento. Saldo pendiente: X"
  - `PagoException` (400): "El método de pago está inactivo"
  - `MetodoPagoNotFoundException` (404)
  - `ConceptoPagoNotFoundException` (404)
  - `EstadoPagoNoConfiguradoException` (500): "Estado PENDIENTE no encontrado en BD"
- **Efectos secundarios:** ninguno inmediato (el pago queda PENDIENTE)
- **Qué debe refrescar el frontend:** listado de pagos del evento, resumen de pagos

### PATCH /api/pagos/{id}/confirmar

- **Propósito:** Confirmar un pago pendiente. Cambia estado a PAGADO, registra fecha, notifica, y verifica si el anticipo alcanza para auto-confirmar el evento.
- **Path variables:** `id` (Long) — ID del pago
- **Response:** `PagoResponseDTO`
- **Método del service:** `confirmarPago(Long)`
- **Validaciones principales:**
  - Pago debe existir
  - Pago debe estar en estado PENDIENTE
  - Lock pesimista en evento (`findByIdForUpdate`)
  - Evento debe estar en COTIZACION, PROGRAMADO, CONFIRMADO o EN_MONTAJE (no CANCELADO, CANCELACION_PENDIENTE, FINALIZADO, COTIZACION)
  - Suma de pagos confirmados (excluyendo este) + monto no puede exceder `precioTotal`
- **Excepciones posibles:**
  - `PagoNotFoundException` (404)
  - `PagoException` (400): "Solo se pueden confirmar pagos en estado PENDIENTE"
  - `PagoException` (400): "No se pueden confirmar pagos en eventos con estado: X"
  - `PagoException` (400): "El pago excede el precio total del evento. Saldo pendiente: X"
  - `EstadoPagoNoConfiguradoException` (500): "Estado PAGADO no encontrado en BD"
- **Efectos secundarios:**
  - `pago.fecha = LocalDateTime.now()`
  - Notificación `PAGO_RECIBIDO` a admins
  - Si evento está en PROGRAMADO y anticipo mínimo alcanzado → auto-transición a CONFIRMADO (`eventoService.confirmarDesdeAnticipo`)
- **Qué debe refrescar el frontend:** listado de pagos, resumen de pagos, detalle del evento, notificaciones

### PATCH /api/pagos/{id}/anular

- **Propósito:** Anular un pago pendiente (sin impacto financiero real).
- **Path variables:** `id` (Long)
- **Response:** `PagoResponseDTO`
- **Método del service:** `anularPago(Long)`
- **Validaciones principales:**
  - Pago debe existir
  - Pago debe estar en estado PENDIENTE
- **Excepciones posibles:**
  - `PagoNotFoundException` (404)
  - `PagoException` (400): "Solo se pueden anular pagos en estado PENDIENTE"
  - `EstadoPagoNoConfiguradoException` (500)
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** listado de pagos del evento

### GET /api/pagos/evento/{eventoId}

- **Propósito:** Obtener todos los pagos de un evento con relaciones cargadas.
- **Path variables:** `eventoId` (Long)
- **Response:** `List<PagoResponseDTO>` con JOIN FETCH de evento, cliente, estadoEvento, estadoPago, conceptoPago, metodoPago
- **Validaciones principales:** ninguna
- **Excepciones posibles:** ninguna (devuelve lista vacía si no hay)
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/pagos/evento/{eventoId}/resumen

- **Propósito:** Obtener resumen financiero del evento (vista SQL).
- **Path variables:** `eventoId` (Long)
- **Response:** `ResumenPagoEventoDTO`
- **Validaciones principales:** ninguna
- **Excepciones posibles:** `EventoNotFoundException` (404) — si no hay vista ni evento
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/pagos

- **Propósito:** Listado paginado de todos los pagos.
- **Query params:** `Pageable` (default: size=50, sort=id)
- **Response:** `Page<PagoResponseDTO>`
- **Validaciones principales:** ninguna
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** NO APLICA

### Endpoints de catálogos (CatalogosPagoController)

Los endpoints GET de catálogos (`estados-pago`, `conceptos-pago`, `metodos-pago`) son listados simples sin paginación ni filtros. Devuelven todos los registros.

**POST /api/catalogos/metodos-pago:**
- Request: `MetodoPagoCreateDTO` con `nombre` (`@NotBlank`, `@Size(min=2, max=100)`, `@Pattern` solo letras y espacios) y `descripcion` (`@Size(max=500)`)
- Service valida además que el nombre contenga al menos una letra (regex `.*[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ].*`)
- Si el nombre ya existe → `MetodoPagoDuplicadoException` (409)
- Crea con `metodoActivo = true`

**PATCH /api/catalogos/metodos-pago/{id}/toggle:**
- Invierte `metodoActivo` (true ↔ false)
- Si no existe → `MetodoPagoNotFoundException` (404)

## 3. DTOs request/response

### 3.1 DTOs Request

#### PagoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| eventoId | Long | number | Sí (`@NotNull`) | No | | ID del evento al que se asocia el pago |
| monto | BigDecimal | number | Sí (`@NotNull`, `@DecimalMin("0.01")`) | No | > 0 | |
| metodoPagoId | Long | number | Sí (`@NotNull`) | No | Método debe existir y estar activo | |
| conceptoPagoId | Long | number | Sí (`@NotNull`) | No | Concepto debe existir | |

#### MetodoPagoCreateDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| nombre | String | string | Sí (`@NotBlank`) | No | `@Size(2,100)`, `@Pattern` solo letras y espacios. Service valida regex de letra | |
| descripcion | String | string | No | Sí | `@Size(max=500)` | |

### 3.2 DTOs Response

#### PagoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| id | Long | number | No | entidad | |
| monto | BigDecimal | number | No | entidad | |
| fecha | LocalDateTime | ISO datetime | Sí | entidad | null si PENDIENTE; se llena al confirmar |
| eventoId | Long | number | No | entidad.evento | |
| estadoPago | String | string | No | entidad.estadoPago | PENDIENTE / PAGADO / CANCELADO / DEVOLUCION_PENDIENTE |
| conceptoPago | String | string | No | entidad.conceptoPago | ANTICIPO / ABONO / SALDO_TOTAL / PENALIZACION |
| metodoPago | String | string | No | entidad.metodoPago | EFECTIVO / TRANSFERENCIA / TARJETA |
| clienteNombre | String | string | No | entidad.evento.cliente | Denormalizado del evento |
| estadoEventoNombre | String | string | No | entidad.evento.estadoEvento | Denormalizado del evento |

#### ResumenPagoEventoDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| idEvento | Long | number | No | vista_pagos_evento / evento | |
| precioTotal | BigDecimal | number | No | vista / evento | |
| totalCobrado | BigDecimal | number | No | vista / 0 | Suma de pagos PAGADO |
| saldoPendiente | BigDecimal | number | No | vista / precioTotal | `precioTotal - totalCobrado` |
| cobroCompleto | Boolean | boolean | No | vista / false | TRUE cuando saldoPendiente <= 0 |

#### EstadoPagoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| id | Long | number | No | entidad | |
| estadoNombre | String | string | No | entidad | PENDIENTE, PAGADO, CANCELADO (DB también tiene DEVOLUCION_PENDIENTE) |
| estadoDescripcion | String | string | Sí | entidad | |

#### ConceptoPagoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| id | Long | number | No | entidad | |
| conceptoNombre | String | string | No | entidad | ANTICIPO, ABONO, SALDO_TOTAL, PENALIZACION |
| conceptoDescripcion | String | string | Sí | entidad | |

#### MetodoPagoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| id | Long | number | No | entidad | |
| metodoNombre | String | string | No | entidad | |
| metodoDescripcion | String | string | Sí | entidad | |
| metodoActivo | Boolean | boolean | No | entidad | FALSE = no mostrar en selectores de nuevo pago |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Pago` | `pago` | Tabla operativa | Pagos de clientes por evento | `pago_fecha` nullable mientras PENDIENTE. `pago_monto` > 0 (CHECK) |
| `EstadoPago` | `estado_pago` | Catálogo | Estados del ciclo de vida | PENDIENTE, PAGADO, CANCELADO. En runtime también DEVOLUCION_PENDIENTE (usado por PagoCancelacionHandler) |
| `ConceptoPago` | `concepto_pago` | Catálogo | Propósito del pago | ANTICIPO, ABONO, SALDO_TOTAL, PENALIZACION |
| `MetodoPago` | `metodo_pago` | Catálogo | Medio de pago | `metodo_activo` permite ocultar sin borrar. EFECTIVO, TRANSFERENCIA, TARJETA |
| — | `vista_pagos_evento` | Vista | Resumen financiero por evento | Solo lectura. `total_cobrado`, `saldo_pendiente`, `cobro_completo`. Suma solo PAGADO |

**Estado DEVOLUCION_PENDIENTE:** No está en el catálogo `estado_pago` sembrado pero es usado por `PagoCancelacionHandler` (línea 67-68) al procesar cancelaciones. Si el estado no existe en BD, lanza `EstadoPagoNoConfiguradoException` (500).

**Vista `vista_pagos_evento`**: LEFT JOIN entre `evento` y `pago` filtrando solo `PAGADO`. Columnas: `id_evento`, `precio_total`, `total_cobrado`, `saldo_pendiente`, `cobro_completo`.

## 5. Services y lógica de negocio

### PagoServiceImpl

| Flujo | Método | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Registrar | `registrarPago` | Evento existe, estado evento permitido, no sobrepago, método activo, catálogos existen | Crea Pago en PENDIENTE, `fecha = null` | PagoResponseDTO |
| Confirmar | `confirmarPago` | Pago PENDIENTE, estado evento permitido, no sobrepago con confirmados | Estado → PAGADO, fecha = now, notifica, auto-transición evento si anticipo | PagoResponseDTO |
| Anular | `anularPago` | Pago PENDIENTE | Estado → CANCELADO | PagoResponseDTO |
| Listar por evento | `obtenerPorEvento` | — | — | List\<PagoResponseDTO\> |
| Listar todos | `obtenerTodos` | — | — | Page\<PagoResponseDTO\> |
| Resumen | `getResumenPagos` | — | — | ResumenPagoEventoDTO |

### Flujo: registrarPago

1. Lock pesimista en evento (`findByIdForUpdate`) — bloquea la fila para operaciones concurrentes
2. Valida estado del evento: bloquea CANCELADO, CANCELACION_PENDIENTE, FINALIZADO, COTIZACION
3. Suma `sumMontoActivoByEventoId` (PENDIENTE + PAGADO) — asegura que total activo + nuevo monto no exceda `precioTotal`
4. Busca estado PENDIENTE del catálogo
5. Busca y valida método de pago (debe existir y estar activo)
6. Busca concepto de pago
7. Crea Pago con `fecha = null`
8. Guarda y retorna DTO

### Flujo: confirmarPago

1. Busca pago por ID, valida que esté PENDIENTE
2. Lock pesimista en evento (`findByIdForUpdate`)
3. Valida estado del evento: permite COTIZACION, PROGRAMADO, CONFIRMADO, EN_MONTAJE
4. Suma pagos confirmados excluyendo este pago — valida no sobrepago
5. Cambia estado a PAGADO, `fecha = now`
6. Notifica a admins (`PAGO_RECIBIDO`)
7. `verificarAnticipo(evento)`: si evento en PROGRAMADO y anticipo suficiente → `eventoService.confirmarDesdeAnticipo(id)` (auto-transición a CONFIRMADO)
8. Retorna DTO

### Flujo: verificarAnticipo (método privado)

- Solo aplica si evento está en PROGRAMADO
- Delega a `eventoPagoValidator.tieneAnticipoSuficiente(evento)` que compara contra `PORCENTAJE_ANTICIPO_MINIMO`
- Si suficiente → `eventoService.confirmarDesdeAnticipo(evento.getId())`
- No lanza excepción si no alcanza

### CatalogosPagoServiceImpl

| Flujo | Método | Qué valida | Qué retorna |
|---|---|---|---|
| Listar estados | `listarEstados` | — | List\<EstadoPagoResponseDTO\> |
| Listar conceptos | `listarConceptos` | — | List\<ConceptoPagoResponseDTO\> |
| Listar métodos | `listarMetodos` | — | List\<MetodoPagoResponseDTO\> |
| Crear método | `crearMetodo` | Nombre con letras, no duplicado | MetodoPagoResponseDTO |
| Toggle método | `toggleMetodo` | Método existe | MetodoPagoResponseDTO |

### PagoCancelacionHandler (usado por EventoServiceImpl al cancelar)

| Flujo | Método | Qué hace |
|---|---|---|
| Procesar cancelación | `procesarPagosPorCancelacion` | PENDIENTE → CANCELADO; PAGADO → DEVOLUCION_PENDIENTE; notifica si hay devolución |

Flujo detallado:
1. Busca pagos del evento en estados PENDIENTE o PAGADO
2. Si no hay pagos → retorna BigDecimal.ZERO
3. PENDIENTE → CANCELADO (sin impacto financiero)
4. PAGADO → DEVOLUCION_PENDIENTE (requiere gestión administrativa)
5. Acumula `totalDevolucion`
6. Si `totalDevolucion > 0` → notifica a admins con `CANCELACION_EVENTO`
7. Retorna `totalDevolucion`

## 6. Estados y transiciones

### Estados de pago

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| PENDIENTE | Registrado, no confirmado | post-registrar | Sí (badge warning) | Anulable. No cuenta para anticipo |
| PAGADO | Confirmado, impacto real | post-confirmar | Sí (badge success) | No anulable directamente (pasa por cancelación de evento). Cuenta para anticipo |
| CANCELADO | Anulado sin impacto | post-anular | Sí (badge secondary) | Terminal |
| DEVOLUCION_PENDIENTE | Requiere devolución | post-cancelar evento | Sí (badge danger) | Creado por PagoCancelacionHandler. NO IMPLEMENTADO en catálogo sembrado (requiere insert manual) |

### Transiciones de pago

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| (nuevo) | PENDIENTE | `POST /api/pagos` | No sobrepago, método activo, evento en estado válido | — |
| PENDIENTE | PAGADO | `PATCH /api/pagos/{id}/confirmar` | No sobrepago, estado evento válido | Notificación, auto-transición evento |
| PENDIENTE | CANCELADO | `PATCH /api/pagos/{id}/anular` | — | — |
| PENDIENTE | CANCELADO | Cancelación de evento | — | PagoCancelacionHandler |
| PAGADO | DEVOLUCION_PENDIENTE | Cancelación de evento | — | PagoCancelacionHandler, notificación |

**Nota sobre DEVOLUCION_PENDIENTE:** Este estado es referenciado en `PagoCancelacionHandler` pero NO existe en el seed de `estado_pago`. Si se cancela un evento con pagos PAGADO, el sistema intentará buscar "DEVOLUCION_PENDIENTE" y lanzará `EstadoPagoNoConfiguradoException` (500). Requiere que el DBA inserte este estado en `estado_pago`.

## 7. Excepciones y respuestas de error

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `PagoException` | Regla de negocio incumplida | 400 | Variable | Mostrar mensaje al usuario |
| `PagoNotFoundException` | ID de pago no existe | 404 | "Pago no encontrado: X" | Redirigir o mostrar error |
| `MetodoPagoNotFoundException` | ID de método no existe | 404 | "Método de pago no encontrado: X" | Refrescar catálogo |
| `ConceptoPagoNotFoundException` | ID de concepto no existe | 404 | "Concepto de pago no encontrado: X" | Refrescar catálogo |
| `MetodoPagoDuplicadoException` | Nombre de método ya existe | 409 | "Ya existe un método de pago con el nombre: X" | Mostrar error, sugerir otro nombre |
| `EstadoPagoNoConfiguradoException` | Estado no existe en BD | 500 | "Estado X no encontrado en BD" | Error interno — notificar admin |
| `EventoNotFoundException` | ID de evento no existe | 404 | "Evento no encontrado: X" | Redirigir |
| `IllegalArgumentException` | Validación de texto en catálogos | 400 | "El campo debe contener texto válido" | Mostrar error en el campo |
| `MethodArgumentNotValidException` | `@Valid` falla en DTO | 400 | "campo: mensaje" | Mostrar error de validación |

**Mensajes de error frecuentes:**
- "No se pueden registrar pagos en eventos con estado: X"
- "El pago excede el precio total del evento. Saldo pendiente: X"
- "El método de pago está inactivo"
- "Solo se pueden confirmar pagos en estado PENDIENTE"
- "No se pueden confirmar pagos en eventos con estado: X"
- "Solo se pueden anular pagos en estado PENDIENTE"
- "El nombre del método de pago es obligatorio"
- "El nombre solo puede contener letras y espacios"

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| `evento` (EventoRepository) | Repository | `registrarPago`, `confirmarPago`, `getResumenPagos` | Leer y lockear evento | Alto: usa `findByIdForUpdate` |
| `evento` (EventoService) | Service | `verificarAnticipo` | Auto-transición a CONFIRMADO | Medio: usa `confirmarDesdeAnticipo` |
| `evento` (EventoPagoValidator) | Handler | `verificarAnticipo` | Validar anticipo mínimo | Bajo |
| `notificacion` (NotificacionService) | Service | `confirmarPago`, `PagoCancelacionHandler` | Enviar notificaciones | Bajo |
| `vista_pagos_evento` | Vista SQL | `getResumenPagos` | Datos consolidados | Solo lectura |

```text
Dependencia circular:
- Detectada: No
- Detalle: pago → evento (service + repository). Evento → pago (handler PagoCancelacionHandler, validator EventoPagoValidator).
  No es circular porque PagoCancelacionHandler y EventoPagoValidator están en el módulo pago/evento respectivamente
  pero la dirección de dependencia es evento → pago (handler) y pago → evento (service).
- Riesgo: Bajo. Son dependencias cruzadas pero no circulares en el mismo package.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| POST registrar | Lock en evento | evento | pagos del evento, resumen |
| PATCH confirmar | Notificación PAGO_RECIBIDO, auto-transición evento si anticipo | notificacion, evento | pagos, resumen, evento, notificaciones |
| PATCH anular | — | — | pagos del evento |
| Cancelación de evento (vía PagoCancelacionHandler) | PENDIENTE → CANCELADO, PAGADO → DEVOLUCION_PENDIENTE, notificación si devolución | notificacion | pagos del evento |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Registrar pago | Evento en PROGRAMADO, CONFIRMADO, EN_MONTAJE | COTIZACION, CANCELADO, CANCELACION_PENDIENTE, FINALIZADO | "No se pueden registrar pagos en este estado" |
| Confirmar pago | Pago PENDIENTE + evento en estado válido | Pago ya PAGADO, CANCELADO o DEVOLUCION_PENDIENTE | "Solo pagos pendientes" |
| Anular pago | Pago PENDIENTE | Pago en cualquier otro estado | "Solo se pueden anular pagos pendientes" |
| Seleccionar método inactivo | — | `metodoActivo = false` | No mostrar en dropdown |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| Todos los campos de PagoRequestDTO | Al crear | Después de creado | El pago no se edita, solo cambia de estado |
| metodoActivo (catálogo) | Toggle libre | — | Solo oculta del selector, no borra historial |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| POST registrar | `GET /api/pagos/evento/{id}`, `GET /api/pagos/evento/{id}/resumen` |
| PATCH confirmar | pagos del evento, resumen, detalle evento (cambió estado si auto-transición), notificaciones |
| PATCH anular | pagos del evento |
| Cancelar evento | pagos del evento (pueden aparecer en DEVOLUCION_PENDIENTE) |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| PENDIENTE | Registrado, no impacta caja | Badge warning |
| PAGADO | Confirmado, impacto real | Badge success |
| CANCELADO | Anulado | Badge secondary |
| DEVOLUCION_PENDIENTE | Requiere acción admin | Badge danger |

### 10.5 Reglas importantes

- El monto del pago se valida contra `precioTotal` del evento (no contra saldo). Si el total de pagos activos + nuevo pago excede `precioTotal`, se rechaza.
- `sumMontoActivoByEventoId` incluye tanto PENDIENTE como PAGADO. Un pago pendiente "reserva" parte del saldo aunque no esté confirmado.
- La auto-transición de evento (PROGRAMADO → CONFIRMADO) ocurre automáticamente al confirmar un pago si el anticipo alcanza `PORCENTAJE_ANTICIPO_MINIMO`. El frontend debe esperar que el evento cambie de estado sin acción explícita del usuario.
- `ResumenPagoEventoDTO.cobroCompleto` es TRUE cuando `saldoPendiente <= 0`. Usar para mostrar badge "Pagado" en el evento.
- No mostrar métodos de pago con `metodoActivo = false` en selectores de nuevo pago, pero sí mostrarlos en pagos históricos.
- El estado DEVOLUCION_PENDIENTE requiere que exista en `estado_pago`. Si no existe, la cancelación de eventos con pagos PAGADO fallará con error 500.

---

## Resumen para frontend

- Dos controladores: `/api/pagos` (operaciones) y `/api/catalogos` (catálogos de pago). Todos requieren ADMIN.
- El flujo de pago es: registrar (PENDIENTE) → confirmar (PAGADO). La anulación solo aplica a PENDIENTE.
- El blindaje contra sobrecobro usa lock pesimista y suma pagos PENDIENTE + PAGADO. No se puede registrar si el total excede `precioTotal`.
- Al confirmar un pago, el evento puede auto-transicionar a CONFIRMADO si el anticipo alcanza el mínimo. Refrescar el detalle del evento después de confirmar.
- `ResumenPagoEventoDTO` se obtiene de `/api/pagos/evento/{id}/resumen`. Usar `cobroCompleto` para indicadores visuales.
- Métodos de pago inactivos (`metodoActivo=false`) no deben aparecer en dropdowns de creación.
- El estado DEVOLUCION_PENDIENTE es usado internamente por PagoCancelacionHandler al cancelar eventos con pagos ya confirmados.
- Los catálogos (estados, conceptos, métodos) son endpoints GET simples sin paginación.
- La creación de métodos de pago valida que el nombre contenga letras reales (no solo números).
