# evento — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Gestiona el ciclo de vida completo de eventos: creación, transiciones de estado, reservas de stock, validación de solapamientos, cancelaciones con tiempo de gracia, extras, gastos, rentabilidad y calendario.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Ciclo de vida del evento (COTIZACION → PROGRAMADO → CONFIRMADO → EN_MONTAJE → FINALIZADO / CANCELADO) |
| Controller principal | `EventoController` (`/api/eventos`) |
| Service principal | `EventoServiceImpl` |
| Entidades principales | `Evento`, `EstadoEvento`, `TipoEvento`, `EventoProducto`, `EstadoReserva`, `GastoEvento` |
| Tablas/vistas implicadas | `evento`, `estado_evento`, `tipo_evento`, `evento_producto`, `estado_reserva`, `gasto_evento`, `vista_eventos_periodo`, `vista_rentabilidad_evento`, `vista_pagos_evento` |
| Módulos relacionados | `clientes`, `inventario/decoracion`, `inventario/producto`, `pago`, `cronograma`, `documento`, `notificacion`, `common` |
| Tipo de módulo | Mixto (CRUD + Operativo + Reporte) |

## 2. Controllers y endpoints completos

Todos los endpoints requieren `@PreAuthorize("hasRole('ADMIN')")` a nivel de clase.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/eventos` | Crear cotización | `EventoRequestDTO` | `EventoResponseDTO` (201) | `crearEvento` | ADMIN | |
| GET | `/api/eventos` | Listar todos | — | `List<EventoResponseDTO>` | `obtenerEventos` | ADMIN | JOIN FETCH con cliente, decoración, tipo, estado. Orden DESC por fecha |
| GET | `/api/eventos/{id}` | Obtener uno | — | `EventoResponseDTO` | `obtenerEventoPorId` | ADMIN | |
| PUT | `/api/eventos/{id}` | Editar evento | `EventoUpdateDTO` | `EventoResponseDTO` | `actualizarEvento` | ADMIN | Completa en COTIZACION, parcial en PROGRAMADO |
| PATCH | `/api/eventos/{id}/extras` | Agregar extras | `EventoExtraRequestDTO` | `EventoResponseDTO` | `agregarExtras` | ADMIN | Solo PROGRAMADO o CONFIRMADO |
| PATCH | `/api/eventos/{id}/programar` | Programar | — | `EventoResponseDTO` | `programarEvento` | ADMIN | COTIZACION → PROGRAMADO. Reserva stock |
| PATCH | `/api/eventos/{id}/confirmar` | Confirmar | — | `EventoResponseDTO` | `confirmarEvento` | ADMIN | PROGRAMADO → CONFIRMADO. Valida anticipo |
| PATCH | `/api/eventos/{id}/iniciar-montaje` | Iniciar montaje | — | `EventoResponseDTO` | `iniciarMontaje` | ADMIN | CONFIRMADO → EN_MONTAJE |
| PATCH | `/api/eventos/{id}/finalizar` | Finalizar | — | `EventoResponseDTO` | `finalizarEvento` | ADMIN | EN_MONTAJE → FINALIZADO. Consume stock |
| PATCH | `/api/eventos/{id}/cancelar` | Iniciar cancelación | `CancelacionEventoRequestDTO` | `EventoResponseDTO` | `iniciarCancelacion` | ADMIN | COTIZACION cancela directo; otros → CANCELACION_PENDIENTE |
| PATCH | `/api/eventos/{id}/revertir-cancelacion` | Revertir cancelación | — | `EventoResponseDTO` | `revertirCancelacion` | ADMIN | CANCELACION_PENDIENTE → estado anterior |
| PATCH | `/api/eventos/{id}/confirmar-cancelacion` | Confirmar cancelación | — | `204 No Content` | `confirmarCancelacion` | ADMIN | CANCELACION_PENDIENTE → CANCELADO. Procesa pagos, libera stock, cancela cronograma |
| GET | `/api/eventos/disponibilidad` | Verificar disponibilidad | Query params | `Boolean` | `verificarDisponibilidad` | ADMIN | `fecha`, `horaInicio`, `horaFin`, `ubicacion`, `eventoIdExcluir`(opt) |
| GET | `/api/eventos/calendario` | Calendario | `inicio`, `fin` (LocalDate) | `List<CalendarioEventoDTO>` | `obtenerCalendario` | ADMIN | Vista `vista_eventos_periodo`. Orden ASC por fecha/hora |
| GET | `/api/eventos/{id}/rentabilidad` | Rentabilidad | — | `RentabilidadEventoDTO` | `getRentabilidad` | ADMIN | Vista `vista_rentabilidad_evento` |
| GET | `/api/eventos/{id}/detalle` | Resumen interno admin | — | `EventoResumenInternoDTO` | `getResumenInterno` | ADMIN | Incluye evento + pagos + gastos + rentabilidad |

### POST /api/eventos

- **Propósito:** Crear un evento en estado COTIZACION.
- **Path variables:** —
- **Query params:** —
- **Request body:** `EventoRequestDTO`
- **Response:** `EventoResponseDTO` con HTTP 201
- **Método del service:** `crearEvento(EventoRequestDTO)`
- **Validaciones principales:**
  - `clienteId`, `decoracionId`, `tipoEventoId`, `fecha` son `@NotNull`
  - Cliente debe estar activo (`cli_activo = true`)
  - Decoración debe estar ACTIVO
  - TipoEvento debe tener `tipoActivo = true`
  - Fecha no puede ser pasada
  - Horario: si se provee `horaInicio`/`horaFin`, ambos obligatorios y `horaFin > horaInicio`
  - Solapamiento: se verifica contra eventos en estados PROGRAMADO/CONFIRMADO/EN_MONTAJE/CANCELACION_PENDIENTE con gap mínimo 120 min. La ubicación NO se usa para filtrar solapamiento (solo tiempo)
  - Si `precioAdicional > 0`, `notaPrecio` es obligatoria
- **Excepciones posibles:** EventoException (400), EventoNotFoundException (404)
- **Efectos secundarios:**
  - `precioBase` se copia de `decoracion.decor_precio`
  - `fechaExpiracion` = hoy + `DIAS_EXPIRACION_COTIZACION`
  - `clienteNotificado = false`
- **Qué debe refrescar el frontend:** listado de eventos, calendario

### GET /api/eventos

- **Propósito:** Listar todos los eventos con relaciones cargadas.
- **Path variables:** —
- **Query params:** —
- **Request body:** —
- **Response:** `List<EventoResponseDTO>`
- **Método del service:** `obtenerEventos()`
- **Validaciones principales:** ninguna
- **Excepciones posibles:** ninguna
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** NO APLICA (es consulta)

### GET /api/eventos/{id}

- **Propósito:** Obtener un evento por ID.
- **Path variables:** `id` (Long)
- **Query params:** —
- **Request body:** —
- **Response:** `EventoResponseDTO`
- **Método del service:** `obtenerEventoPorId(Long)`
- **Validaciones principales:** ninguna
- **Excepciones posibles:** `EventoNotFoundException` (404) — "Evento no encontrado con ID: X"
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** NO APLICA

### PUT /api/eventos/{id}

- **Propósito:** Editar datos del evento. Comportamiento depende del estado actual.
- **Path variables:** `id` (Long)
- **Query params:** —
- **Request body:** `EventoUpdateDTO` (todos los campos opcionales)
- **Response:** `EventoResponseDTO`
- **Método del service:** `actualizarEvento(Long, EventoUpdateDTO)`
- **Validaciones principales:**
  - Solo editable en COTIZACION (modo COMPLETA) o PROGRAMADO (modo PARCIAL)
  - **Modo COMPLETA (COTIZACION):** permite cambiar cliente, decoración, fecha, horaInicio, horaFin, ubicacion, nota, notaPrecio. Valida fecha no pasada, horario, disponibilidad.
  - **Modo PARCIAL (PROGRAMADO):** SOLO permite cambiar ubicacion, nota, notaPrecio. Bloquea clienteId, decoracionId, fecha, horaInicio, horaFin. Si se envían campos bloqueados → error.
  - Si cambia decoración, `precioBase` se actualiza al precio de la nueva decoración
- **Excepciones posibles:**
  - `EventoException` (400): "Solo se pueden editar eventos en estado COTIZACION o PROGRAMADO"
  - `EventoException` (400): "En estado PROGRAMADO solo se pueden editar ubicación, nota y motivo del precio adicional"
  - `EventoException` (400): "La nota de precio es obligatoria cuando hay precio adicional"
  - `EventoException` (400): "No se puede crear o editar un evento en el pasado"
  - `EventoException` (400): "Ya existe un evento solapado en esa fecha y horario"
  - `EventoNotFoundException` (404)
- **Efectos secundarios:** Ninguno visible para frontend (no notifica, no audita visiblemente en este endpoint)
- **Qué debe refrescar el frontend:** detalle del evento, listado de eventos, calendario

### PATCH /api/eventos/{id}/extras

- **Propósito:** Agregar productos extra a un evento ya programado/confirmado. Incrementa precio adicional.
- **Path variables:** `id` (Long)
- **Query params:** —
- **Request body:** `EventoExtraRequestDTO`
- **Response:** `EventoResponseDTO`
- **Método del service:** `agregarExtras(Long, EventoExtraRequestDTO)`
- **Validaciones principales:**
  - Solo PROGRAMADO o CONFIRMADO
  - `items` no vacío (`@NotEmpty`)
  - `notaPrecio` obligatoria (`@NotBlank`)
  - `montoAdicional > 0` (`@DecimalMin("0.01")`)
  - Cada item: `productoId` obligatorio, `cantidad >= 1`
  - Producto debe existir y estar ACTIVO
  - Stock suficiente para los extras (validado por `EventoStockHandler`)
- **Excepciones posibles:**
  - `EventoException` (400): "Solo se pueden agregar extras a eventos en estado PROGRAMADO o CONFIRMADO"
  - `EventoException` (400): "La nota de precio es obligatoria"
  - `EventoException` (400): "El monto adicional debe ser mayor a 0"
  - `EventoException` (400): "Producto no encontrado o inactivo"
  - `EventoNotFoundException` (404)
- **Efectos secundarios:**
  - Reserva stock de los productos extra (`EventoStockHandler.validarYReservarExtras`)
  - Incrementa `precioAdicional` (acumulativo)
  - Concatena `notaPrecio` con " | " como separador
  - Registra auditoría (`AGREGAR_EXTRAS`)
- **Qué debe refrescar el frontend:** detalle del evento, pagos (precio total cambió), inventario

### PATCH /api/eventos/{id}/programar

- **Propósito:** Transición COTIZACION → PROGRAMADO. Reserva stock.
- **Path variables:** `id` (Long)
- **Query params:** —
- **Request body:** —
- **Response:** `EventoResponseDTO`
- **Método del service:** `programarEvento(Long)`
- **Validaciones principales:**
  - Estado actual debe ser COTIZACION
  - Stock suficiente para todos los productos de la decoración (`EventoStockHandler.validarYReservarStock`)
- **Excepciones posibles:**
  - `EventoException` (400): "Transición inválida: el evento está en X, se requiere COTIZACION para pasar a PROGRAMADO"
  - `EventoException` (400): error de stock desde `EventoStockHandler`
  - `EventoNotFoundException` (404)
- **Efectos secundarios:**
  - Crea reservas en `evento_producto` con estado ACTIVA
  - Registra auditoría (`PROGRAMAR`)
- **Qué debe refrescar el frontend:** detalle del evento, listado, inventario, calendario

### PATCH /api/eventos/{id}/confirmar

- **Propósito:** Transición PROGRAMADO → CONFIRMADO. Valida anticipo mínimo.
- **Path variables:** `id` (Long)
- **Query params:** —
- **Request body:** —
- **Response:** `EventoResponseDTO`
- **Método del service:** `confirmarEvento(Long)`
- **Validaciones principales:**
  - Estado actual debe ser PROGRAMADO
  - Anticipo mínimo alcanzado (`EventoPagoValidator.validarAnticipoMinimo`)
- **Excepciones posibles:**
  - `EventoException` (400): "Transición inválida..."
  - `EventoException` (400): error de anticipo desde `EventoPagoValidator`
  - `EventoNotFoundException` (404)
- **Efectos secundarios:**
  - `fechaConfirmacion = LocalDateTime.now()`
  - Registra auditoría (`CONFIRMAR`)
  - Dispara notificación de confirmación manual (`notificacionHandler.notificarConfirmacionManual`)
- **Qué debe refrescar el frontend:** detalle del evento, listado, calendario, notificaciones

### PATCH /api/eventos/{id}/iniciar-montaje

- **Propósito:** Transición CONFIRMADO → EN_MONTAJE.
- **Path variables:** `id` (Long)
- **Query params:** —
- **Request body:** —
- **Response:** `EventoResponseDTO`
- **Método del service:** `iniciarMontaje(Long)`
- **Validaciones principales:** Estado actual debe ser CONFIRMADO
- **Excepciones posibles:** `EventoException` (400), `EventoNotFoundException` (404)
- **Efectos secundarios:** Notificación de inicio de montaje (`notificacionHandler.notificarInicioMontaje`)
- **Qué debe refrescar el frontend:** detalle del evento, listado, notificaciones

### PATCH /api/eventos/{id}/finalizar

- **Propósito:** Transición EN_MONTAJE → FINALIZADO. Consume stock.
- **Path variables:** `id` (Long)
- **Query params:** —
- **Request body:** —
- **Response:** `EventoResponseDTO`
- **Método del service:** `finalizarEvento(Long)`
- **Validaciones principales:** Estado actual debe ser EN_MONTAJE
- **Excepciones posibles:** `EventoException` (400), `EventoNotFoundException` (404)
- **Efectos secundarios:**
  - `stockHandler.consumirStock(evento)` — cambia reservas a CONSUMIDA y registra movimientos de inventario
  - Si el total pagado < precio total, envía notificación de saldo pendiente
- **Qué debe refrescar el frontend:** detalle del evento, listado, inventario, pagos

### PATCH /api/eventos/{id}/cancelar

- **Propósito:** Iniciar proceso de cancelación.
- **Path variables:** `id` (Long)
- **Request body:** `CancelacionEventoRequestDTO` (`motivo` obligatorio, `@NotBlank`)
- **Response:** `EventoResponseDTO`
- **Método del service:** `iniciarCancelacion(Long, String)`
- **Validaciones principales:**
  - `motivo` no puede ser null o blank
  - Si estado es COTIZACION → cancela directamente a CANCELADO
  - Si estado es PROGRAMADO/CONFIRMADO/EN_MONTAJE → va a CANCELACION_PENDIENTE
  - Estados CANCELADO, CANCELACION_PENDIENTE, FINALIZADO no permiten cancelación
- **Excepciones posibles:**
  - `EventoException` (400): "El motivo de cancelación es obligatorio"
  - `EventoException` (400): "No se puede cancelar un evento en estado: X"
  - `EventoNotFoundException` (404)
- **Efectos secundarios:**
  - Guarda `estadoAnterior` (para posible reversión)
  - `fechaCancelacion = LocalDateTime.now()`
  - `motivoCancelacion` = el enviado
  - Si viene de PROGRAMADO/CONFIRMADO/EN_MONTAJE: notificación (`notificacionHandler.notificarCancelacionIniciada`)
  - Si es COTIZACION: cancelación directa sin notificación
- **Qué debe refrescar el frontend:** detalle del evento, listado, calendario, notificaciones

### PATCH /api/eventos/{id}/revertir-cancelacion

- **Propósito:** Revertir cancelación pendiente al estado anterior.
- **Path variables:** `id` (Long)
- **Query params:** —
- **Request body:** —
- **Response:** `EventoResponseDTO`
- **Método del service:** `revertirCancelacion(Long)`
- **Validaciones principales:**
  - Estado actual debe ser CANCELACION_PENDIENTE
  - Debe existir `estadoAnterior`
- **Excepciones posibles:**
  - `EventoException` (400): "Solo se puede revertir desde CANCELACION_PENDIENTE"
  - `EventoException` (400): "No hay estado anterior para restaurar"
  - `EventoNotFoundException` (404)
- **Efectos secundarios:**
  - Restaura `estadoEvento = estadoAnterior`
  - Limpia `estadoAnterior`, `motivoCancelacion`, `fechaCancelacion`
- **Qué debe refrescar el frontend:** detalle del evento, listado, calendario

### PATCH /api/eventos/{id}/confirmar-cancelacion

- **Propósito:** Confirmar cancelación definitiva. CANCELACION_PENDIENTE → CANCELADO.
- **Path variables:** `id` (Long)
- **Query params:** —
- **Request body:** —
- **Response:** `204 No Content`
- **Método del service:** `confirmarCancelacion(Long)` (void)
- **Validaciones principales:** Estado actual debe ser CANCELACION_PENDIENTE
- **Excepciones posibles:** `EventoException` (400), `EventoNotFoundException` (404)
- **Efectos secundarios:**
  - Procesa pagos de cancelación (`PagoCancelacionHandler.procesarPagosPorCancelacion`)
  - Libera reservas de stock (`stockHandler.liberarReservas`) — cambia a LIBERADA
  - Cancela cronograma asociado (`cronogramaService.cancelarPorEvento`)
  - Si no hubo devolución (totalDevolucion = 0), envía notificación (`notificacionHandler.notificarCancelacionConfirmada`)
- **Qué debe refrescar el frontend:** detalle del evento, listado, calendario, inventario, cronograma, pagos

### GET /api/eventos/disponibilidad

- **Propósito:** Verificar si un horario está disponible para un nuevo evento.
- **Query params:**
  - `fecha` (LocalDate, requerido)
  - `horaInicio` (LocalTime, requerido)
  - `horaFin` (LocalTime, requerido)
  - `ubicacion` (String, requerido)
  - `eventoIdExcluir` (Long, opcional — para ediciones)
- **Response:** `Boolean` (true = disponible)
- **Método del service:** `verificarDisponibilidad(fecha, horaInicio, horaFin, ubicacion, eventoIdExcluir)`
- **Validaciones principales:**
  - `horaInicio` y `horaFin` deben proveerse juntos o ambos nulos
  - `horaFin > horaInicio`
  - Solapamiento validado con gap mínimo de 120 minutos. NO se filtra por ubicación
- **Excepciones posibles:** `EventoException` (400) — "Para validar disponibilidad debe indicar horaInicio, horaFin y ubicación"
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/eventos/calendario

- **Propósito:** Obtener eventos en un rango de fechas para vista de calendario.
- **Query params:** `inicio` (LocalDate), `fin` (LocalDate)
- **Response:** `List<CalendarioEventoDTO>` ordenado ASC por fecha y hora
- **Método del service:** `obtenerCalendario(LocalDate, LocalDate)`
- **Validaciones principales:** `inicio` no puede ser mayor a `fin`
- **Excepciones posibles:** `EventoException` (400) — "La fecha inicio no puede ser mayor a la fecha fin"
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/eventos/{id}/rentabilidad

- **Propósito:** Obtener rentabilidad calculada del evento (vista SQL).
- **Path variables:** `id` (Long)
- **Response:** `RentabilidadEventoDTO`
- **Método del service:** `getRentabilidad(Long)`
- **Validaciones principales:** ninguna
- **Excepciones posibles:** `EventoException` (400) — "No se encontró rentabilidad para el evento X"
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/eventos/{id}/detalle

- **Propósito:** Resumen interno administrativo con datos de evento + pagos + gastos + rentabilidad.
- **Path variables:** `id` (Long)
- **Response:** `EventoResumenInternoDTO`
- **Método del service:** `getResumenInterno(Long)`
- **Validaciones principales:** ninguna
- **Excepciones posibles:** `EventoNotFoundException` (404)
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** NO APLICA

## 3. DTOs request/response

### 3.1 DTOs Request

#### EventoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| clienteId | Long | number | Sí (`@NotNull`) | No | — | |
| decoracionId | Long | number | Sí (`@NotNull`) | No | — | |
| tipoEventoId | Long | number | Sí (`@NotNull`) | No | — | |
| fecha | LocalDate | "YYYY-MM-DD" | Sí (`@NotNull`) | No | No pasada (service) | |
| horaInicio | LocalTime | "HH:mm" | No | Sí | Si se provee, horaFin obligatorio | |
| horaFin | LocalTime | "HH:mm" | No | Sí | Si se provee, horaInicio obligatorio. Debe ser > horaInicio | |
| ubicacion | String | string | No | Sí | | Si está, se usa en disponibilidad (pero no filtra solapamiento) |
| nota | String | string | No | Sí | | Nota libre del evento |
| precioAdicional | BigDecimal | number | No | Sí | Si > 0, notaPrecio obligatorio (service) | |
| notaPrecio | String | string | No | Sí | Obligatorio si precioAdicional > 0 | Justificación del cargo extra |

#### EventoUpdateDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| clienteId | Long | number | No | Sí | Bloqueado en modo PARCIAL | Solo cambiable en COTIZACION |
| decoracionId | Long | number | No | Sí | Bloqueado en modo PARCIAL | Solo cambiable en COTIZACION. Al cambiar, actualiza precioBase |
| fecha | LocalDate | "YYYY-MM-DD" | No | Sí | Bloqueado en modo PARCIAL | |
| horaInicio | LocalTime | "HH:mm" | No | Sí | Bloqueado en modo PARCIAL | |
| horaFin | LocalTime | "HH:mm" | No | Sí | Bloqueado en modo PARCIAL | |
| ubicacion | String | string | No | Sí | Editable en PROGRAMADO | |
| nota | String | string | No | Sí | Editable en PROGRAMADO | |
| notaPrecio | String | string | No | Sí | Editable en PROGRAMADO | |

#### EventoExtraRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| items | List\<EventoExtraItemRequestDTO\> | array | Sí (`@NotEmpty`) | No | Al menos 1 item | |
| notaPrecio | String | string | Sí (`@NotBlank`) | No | | Justificación del extra |
| montoAdicional | BigDecimal | number | Sí (`@NotNull`, `@DecimalMin("0.01")`) | No | > 0 | Incremento sobre precio actual |

#### EventoExtraItemRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| productoId | Long | number | Sí (`@NotNull`) | No | Producto debe existir y estar ACTIVO | |
| cantidad | Integer | number | Sí (`@NotNull`, `@Min(1)`) | No | >= 1 | |

#### CancelacionEventoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| motivo | String | string | Sí (`@NotBlank`) | No | Service valida no blank también | |

### 3.2 DTOs Response

#### EventoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| id | Long | number | No | entidad | |
| fecha | LocalDate | "YYYY-MM-DD" | No | entidad | |
| horaInicio | LocalTime | "HH:mm" | Sí | entidad | |
| horaFin | LocalTime | "HH:mm" | Sí | entidad | |
| ubicacion | String | string | Sí | entidad | |
| nota | String | string | Sí | entidad | Nota libre del evento |
| motivoCancelacion | String | string | Sí | entidad | Solo visible si está en cancelación |
| fechaCreacion | LocalDateTime | ISO datetime | No | entidad | |
| clienteId | Long | number | No | entidad.cliente | |
| clienteNombre | String | string | No | entidad.cliente | |
| decoracionId | Long | number | No | entidad.decoracion | |
| decoracionNombre | String | string | No | entidad.decoracion | |
| estadoEventoId | Long | number | No | entidad.estadoEvento | |
| estadoEventoNombre | String | string | No | entidad.estadoEvento | Usar para badges de estado |
| tipoEventoId | Long | number | No | entidad.tipoEvento | |
| tipoEventoNombre | String | string | No | entidad.tipoEvento | |
| precioBase | BigDecimal | number | No | entidad | Copiado de decoración al crear |
| precioAdicional | BigDecimal | number | No | entidad | Acumula extras |
| precioTotal | BigDecimal | number | No | entidad (columna generada) | `precioBase + precioAdicional`. NO editable |
| notaPrecio | String | string | Sí | entidad | Justificación del adicional |
| clienteNotificado | Boolean | boolean | No | entidad | TRUE cuando se envió documento al cliente |
| fechaConfirmacion | LocalDateTime | ISO datetime | Sí | entidad | Solo en CONFIRMADO+ |
| fechaCancelacion | LocalDateTime | ISO datetime | Sí | entidad | Solo cuando inicia cancelación |
| fechaExpiracion | LocalDate | "YYYY-MM-DD" | Sí | entidad | Solo en COTIZACION |

#### CalendarioEventoDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| idEvento | Long | number | No | vista_eventos_periodo | |
| fechaEvento | LocalDate | "YYYY-MM-DD" | No | vista | |
| horaInicio | LocalTime | "HH:mm" | Sí | vista | |
| horaFin | LocalTime | "HH:mm" | Sí | vista | |
| ubicacion | String | string | Sí | vista | |
| estado | String | string | No | vista | Badge de estado |
| tipoEvento | String | string | No | vista | |
| clienteNombre | String | string | No | vista | |
| clienteTelefono | String | string | Sí | vista | |
| clienteCorreo | String | string | Sí | vista | |
| decoracion | String | string | No | vista | Nombre de la decoración |
| decoracionImagen | String | string (URL) | Sí | vista | |
| precioTotal | BigDecimal | number | No | vista | |
| clienteNotificado | Boolean | boolean | No | vista | |
| fechaConfirmacion | LocalDateTime | ISO datetime | Sí | vista | |
| fechaExpiracion | LocalDate | "YYYY-MM-DD" | Sí | vista | |
| fechaCreacion | LocalDateTime | ISO datetime | No | vista | |
| totalCobrado | BigDecimal | number | No | vista (LEFT JOIN vista_pagos_evento) | |
| saldoPendiente | BigDecimal | number | No | vista | |
| cobroCompleto | Boolean | boolean | No | vista | TRUE = pagado completamente |
| tieneCronograma | Boolean | boolean | No | vista (EXISTS) | |

#### RentabilidadEventoDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| idEvento | Long | number | No | vista_rentabilidad_evento | |
| fechaEvento | LocalDate | "YYYY-MM-DD" | No | vista | |
| estado | String | string | No | vista | |
| precioAcordado | BigDecimal | number | No | vista | = precioBase + precioAdicional |
| ingresoCobrado | BigDecimal | number | No | vista | Suma pagos PAGADO |
| costoMateriales | BigDecimal | number | No | vista | Suma `cantidad_reservada * produ_precio_unitario` |
| costoManoObra | BigDecimal | number | No | vista | Suma `pago_asignado` de cronograma |
| gastosDesembolsados | BigDecimal | number | No | vista | Suma gastos DESEMBOLSADO |
| gastosProyectados | BigDecimal | number | No | vista | Suma gastos PROYECTADO |
| gananciaNeta | BigDecimal | number | No | vista | ingreso - materiales - manoObra - desembolsados |

#### EventoResumenInternoDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| idEvento | Long | number | No | entidad | |
| estadoEvento | String | string | No | entidad | |
| fecha | LocalDate | "YYYY-MM-DD" | No | entidad | |
| horaInicio | LocalTime | "HH:mm" | Sí | entidad | |
| horaFin | LocalTime | "HH:mm" | Sí | entidad | |
| ubicacion | String | string | Sí | entidad | |
| clienteNombre | String | string | Sí | entidad | null si no tiene cliente |
| decoracionNombre | String | string | Sí | entidad | |
| tipoEventoNombre | String | string | Sí | entidad | |
| nota | String | string | Sí | entidad | |
| notaPrecio | String | string | Sí | entidad | |
| precioBase | BigDecimal | number | No | entidad | |
| precioAdicional | BigDecimal | number | No | entidad | |
| precioTotal | BigDecimal | number | No | entidad (generado) | |
| fechaConfirmacion | LocalDateTime | ISO datetime | Sí | entidad | |
| resumenPagos | ResumenPagoEventoDTO | object | No | vista_pagos_evento | Ver módulo `pago` |
| resumenGastos | ResumenGastoEventoDTO | object | No | service (agregación) | `totalProyectado`, `totalDesembolsado`, `totalPendiente` |
| rentabilidad | RentabilidadEventoDTO | object | Sí | vista_rentabilidad_evento | null si no hay datos |

#### ResumenGastoEventoDTO (record)

| Campo | Tipo Java | JSON esperado | Notas |
|---|---|---|---|
| totalProyectado | BigDecimal | number | Suma gastos PROYECTADO |
| totalDesembolsado | BigDecimal | number | Suma gastos DESEMBOLSADO |
| totalPendiente | BigDecimal | number | Igual a totalProyectado (misma query) |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Evento` | `evento` | Tabla operativa | Entidad central del negocio | `precio_total` es columna generada, no escribible |
| `EstadoEvento` | `estado_evento` | Catálogo | Estados del ciclo de vida | 7 estados: COTIZACION, PROGRAMADO, CONFIRMADO, EN_MONTAJE, FINALIZADO, CANCELADO, CANCELACION_PENDIENTE |
| `TipoEvento` | `tipo_evento` | Catálogo | Tipos de evento | `tipo_activo` permite soft-delete |
| `EventoProducto` | `evento_producto` | Relación | Reserva de productos por evento | PK compuesta: `(id_evento, id_producto)`. Incluye `id_estado_reserva`, `fecha_reserva`, `fecha_liberacion`, `motivo_liberacion` |
| `EstadoReserva` | `estado_reserva` | Catálogo | Estados de reserva | ACTIVA (bloquea stock), LIBERADA (cancelación), CONSUMIDA (finalizado) |
| `GastoEvento` | `gasto_evento` | Tabla operativa | Gastos variables del evento | Estados: PROYECTADO, DESEMBOLSADO |
| — | `vista_eventos_periodo` | Vista | Calendario/dashboard enriquecido | Solo lectura. No expone `id_estado_anterior`, `event_nota`, `motivo_cancelacion`, `event_fecha_cancelacion` |
| — | `vista_rentabilidad_evento` | Vista | Cálculo de rentabilidad | Solo lectura. Datos financieros internos |
| — | `vista_pagos_evento` | Vista | Resumen de cobros | Solo lectura. `total_cobrado`, `saldo_pendiente`, `cobro_completo` |

**Constraint DB relevante para frontend:**
- `chk_evento_nota_precio`: `event_nota_precio` es obligatorio cuando `event_precio_adicional > 0`
- `chk_evento_horario`: `event_horafin > event_horainicio` cuando ambos existen

**Tabla `evento_producto` en DB vs NOTAS_DB_ACTUALIZADO.md:**
El dump SQL (2026-05-09) contiene las columnas `id_estado_reserva`, `fecha_reserva`, `fecha_liberacion`, `motivo_liberacion`. La entidad Java `EventoProducto` las mapea correctamente. `NOTAS_DB_ACTUALIZADO.md` sección 3.13 está desactualizado (omite estas 4 columnas). El SQL dump y el código Java son la fuente de verdad.

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Crear cotización | `crearEvento` | Cliente activo, decoración activa, tipo activo, fecha no pasada, horario, solapamiento, notaPrecio si adicional | Crea Evento en COTIZACION, copia precioBase, calcula fechaExpiracion | EventoResponseDTO |
| Programar | `programarEvento` | Transición COTIZACION→PROGRAMADO, stock suficiente | Crea reservas en evento_producto (ACTIVA), cambia estado | EventoResponseDTO |
| Confirmar | `confirmarEvento` | Transición PROGRAMADO→CONFIRMADO, anticipo mínimo | Cambia estado, fechaConfirmacion, notifica | EventoResponseDTO |
| Iniciar montaje | `iniciarMontaje` | Transición CONFIRMADO→EN_MONTAJE | Cambia estado, notifica | EventoResponseDTO |
| Finalizar | `finalizarEvento` | Transición EN_MONTAJE→FINALIZADO | Consume stock (CONSUMIDA), genera movimientos inventario, verifica saldo | EventoResponseDTO |
| Cancelar | `iniciarCancelacion` | Estado permitido, motivo obligatorio | COTIZACION→CANCELADO directo; otros→CANCELACION_PENDIENTE con estadoAnterior | EventoResponseDTO |
| Revertir cancelación | `revertirCancelacion` | Estado=CANCELACION_PENDIENTE, tiene estadoAnterior | Restaura estado anterior, limpia fechas/motivo | EventoResponseDTO |
| Confirmar cancelación | `confirmarCancelacion` | Transición CANCELACION_PENDIENTE→CANCELADO | Procesa pagos devolución, libera stock, cancela cronograma, notifica si aplica | void (204) |
| Editar | `actualizarEvento` | Estado=COTIZACION o PROGRAMADO, modo parcial/completo | Actualiza campos según modo | EventoResponseDTO |
| Extras | `agregarExtras` | Estado=PROGRAMADO/CONFIRMADO, stock extras, monto>0 | Incrementa precioAdicional, reserva stock extra, concatena notaPrecio | EventoResponseDTO |
| Calendario | `obtenerCalendario` | inicio <= fin | — | List\<CalendarioEventoDTO\> |
| Disponibilidad | `verificarDisponibilidad` | horaInicio/horaFin/ubicacion, gap 120 min, sin filtro por ubicación | — | Boolean |
| Rentabilidad | `getRentabilidad` | — | — | RentabilidadEventoDTO |
| Resumen interno | `getResumenInterno` | — | — | EventoResumenInternoDTO |

### Flujo: crearEvento

1. Valida `notaPrecio` obligatorio si `precioAdicional > 0`
2. Busca y valida cliente activo
3. Busca y valida decoración ACTIVA
4. Busca y valida tipoEvento activo
5. Valida fecha no pasada
6. Valida horario (ambos o ninguno, horaFin > horaInicio)
7. Valida disponibilidad (solapamiento con gap 120 min, sin filtrar ubicación)
8. Busca estado COTIZACION del catálogo
9. Crea Evento: precioBase = decoracion.precio, fechaExpiracion = hoy + DIAS_EXPIRACION_COTIZACION, clienteNotificado = false
10. Guarda en BD
11. Retorna DTO

### Flujo: programarEvento

1. Obtiene evento, valida transición COTIZACION → PROGRAMADO
2. `stockHandler.validarYReservarStock(evento)` — copia `decoracionxproducto` a `evento_producto` con estado ACTIVA
3. Cambia estado a PROGRAMADO
4. Guarda, registra auditoría (PROGRAMAR)
5. Retorna DTO

### Flujo: finalizarEvento (transiciones con efectos en stock)

1. Valida transición EN_MONTAJE → FINALIZADO
2. Verifica si total pagado < precio total → notifica saldo pendiente
3. `stockHandler.consumirStock(evento)` — cambia reservas a CONSUMIDA, registra movimientos SALIDA
4. Cambia estado a FINALIZADO
5. Guarda, retorna DTO

### Flujo: confirmarCancelacion (transición definitiva)

1. Valida transición CANCELACION_PENDIENTE → CANCELADO
2. `pagoCancelacionHandler.procesarPagosPorCancelacion(evento, motivo)` — calcula devolución, crea pagos PENALIZACION si aplica
3. Busca estado CANCELADO
4. `stockHandler.liberarReservas(evento, motivo)` — cambia reservas a LIBERADA con fecha y motivo
5. Cambia estado a CANCELADO, limpia estadoAnterior
6. `cronogramaService.cancelarPorEvento(id)` — marca actividades como CANCELADO
7. Guarda
8. Si totalDevolucion == 0 → notifica cancelación confirmada

## 6. Estados y transiciones

### Estados existentes

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| COTIZACION | Propuesta sin compromiso | Estado inicial | Sí (badge) | Editable completamente. Expira según DIAS_EXPIRACION_COTIZACION |
| PROGRAMADO | Stock reservado, evento agendado | post-programar | Sí (badge) | Editable parcialmente (ubicacion, nota, notaPrecio). Permite cronograma |
| CONFIRMADO | Anticipo mínimo alcanzado | post-confirmar | Sí (badge) | Permite cronograma, extras, montaje |
| EN_MONTAJE | Fase activa de preparación | post-iniciar-montaje | Sí (badge) | Stock sigue reservado |
| FINALIZADO | Evento completado | post-finalizar | Sí (badge) | Terminal. Stock consumido |
| CANCELACION_PENDIENTE | Cancelación iniciada con tiempo de gracia | post-cancelar | Sí (badge, warning) | Reversible. El job la consume tras TIEMPO_GRACIA_CANCELACION_MINUTOS |
| CANCELADO | Cancelación definitiva | post-confirmar-cancelacion o cancelar desde COTIZACION | Sí (badge, danger) | Terminal. Stock liberado |

### Transiciones permitidas

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| COTIZACION | PROGRAMADO | `PATCH /{id}/programar` | Stock suficiente | Reserva stock (ACTIVA), auditoría |
| PROGRAMADO | CONFIRMADO | `PATCH /{id}/confirmar` | Anticipo mínimo | fechaConfirmacion, notificación, auditoría |
| CONFIRMADO | EN_MONTAJE | `PATCH /{id}/iniciar-montaje` | — | Notificación |
| EN_MONTAJE | FINALIZADO | `PATCH /{id}/finalizar` | — | Consume stock, movimientos inventario |
| COTIZACION | CANCELADO | `PATCH /{id}/cancelar` | Motivo obligatorio | Directo, sin tiempo de gracia |
| PROGRAMADO | CANCELACION_PENDIENTE | `PATCH /{id}/cancelar` | Motivo obligatorio | Notificación, guarda estadoAnterior |
| CONFIRMADO | CANCELACION_PENDIENTE | `PATCH /{id}/cancelar` | Motivo obligatorio | Notificación, guarda estadoAnterior |
| EN_MONTAJE | CANCELACION_PENDIENTE | `PATCH /{id}/cancelar` | Motivo obligatorio | Notificación, guarda estadoAnterior |
| CANCELACION_PENDIENTE | estado anterior | `PATCH /{id}/revertir-cancelacion` | Tiene estadoAnterior | Limpia fechas/motivo cancelación |
| CANCELACION_PENDIENTE | CANCELADO | `PATCH /{id}/confirmar-cancelacion` | — | Procesa pagos, libera stock, cancela cronograma |

**Estados terminales:** FINALIZADO, CANCELADO
**Estados editables:** COTIZACION (completa), PROGRAMADO (parcial)
**Estados que bloquean acciones:** CANCELADO, FINALIZADO (no permiten edición, pagos ni transiciones)

### Confirmación automática desde pagos

El método `confirmarDesdeAnticipo(Long id)` es llamado por `PagoServiceImpl.verificarAnticipo()` cuando un pago confirmado hace que el anticipo alcance el mínimo configurado. No tiene endpoint propio — es interno.

## 7. Excepciones y respuestas de error

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `EventoException` | Regla de negocio incumplida | 400 | Variable según validación | Mostrar mensaje del backend al usuario |
| `EventoNotFoundException` | ID no existe | 404 | "Evento no encontrado: X" | Redirigir o mostrar "no encontrado" |
| `MethodArgumentNotValidException` | `@Valid` falla en DTO | 400 | "campo: mensaje" | Mostrar error de validación en el campo |
| `DataIntegrityViolationException` | Violación de constraint DB | 409 | "Error de integridad de datos" | Notificar error genérico |
| `IllegalArgumentException` | Validación manual en catálogos/ servicios | 400 | Variable | Mostrar mensaje |
| `Exception` (genérica) | Error no manejado | 500 | "Error interno del servidor" | Mostrar error genérico |

**Mensajes de error frecuentes que verá el frontend:**

- "Cliente no encontrado o inactivo"
- "La decoración seleccionada no está activa"
- "Tipo de evento no encontrado o inactivo"
- "No se puede crear o editar un evento en el pasado"
- "Hora inicio y hora fin son obligatorias juntas"
- "La hora fin debe ser posterior a la hora inicio"
- "Ya existe un evento solapado en esa fecha y horario"
- "La nota de precio es obligatoria cuando hay precio adicional"
- "Transición inválida: el evento está en X, se requiere Y para pasar a Z"
- "Solo se pueden editar eventos en estado COTIZACION o PROGRAMADO"
- "En estado PROGRAMADO solo se pueden editar ubicación, nota y motivo del precio adicional"
- "Solo se pueden agregar extras a eventos en estado PROGRAMADO o CONFIRMADO"
- "El motivo de cancelación es obligatorio"
- "No se puede cancelar un evento en estado: X"
- "Solo se puede revertir desde CANCELACION_PENDIENTE"
- "No hay estado anterior para restaurar"
- "No se encontró rentabilidad para el evento X"
- "Para validar disponibilidad debe indicar horaInicio, horaFin y ubicación"
- "No se permite actualizar solo una parte del horario"

**GlobalExceptionHandler:**
- `ArtepaxException` (y subclases como `EventoException`) → `ErrorResponse` con `status` y `message`
- `ErrorResponse` tiene estructura: `{ "status": 400, "message": "...", "errorCode": "..." }`

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| `clientes` | Repository | `crearEvento`, `actualizarEvento` | Validar y asignar cliente | Bajo |
| `inventario/decoracion` | Repository | `crearEvento`, `actualizarEvento` | Validar decoración activa, copiar precioBase | Medio: si cambia schema de Decoracion |
| `inventario/producto` | Repository | `agregarExtras` | Validar existencia de productos | Bajo |
| `inventario/producto/StockProducto` | Handler (`EventoStockHandler`) | `programarEvento`, `agregarExtras`, `finalizarEvento`, `confirmarCancelacion` | Reservar, validar, consumir, liberar stock | Alto: dependencia central |
| `pago` | Handler (`EventoPagoValidator`) | `confirmarEvento`, `confirmarDesdeAnticipo` | Validar anticipo mínimo | Medio |
| `pago` | Handler (`PagoCancelacionHandler`) | `confirmarCancelacion` | Procesar devoluciones | Medio |
| `cronograma` | Service (`CronogramaService`) | `confirmarCancelacion` | Cancelar actividades en cascada | Bajo (via `@Lazy`) |
| `notificacion` | Handler (`EventoNotificacionHandler`) | `confirmarEvento`, `iniciarMontaje`, `finalizarEvento`, `iniciarCancelacion`, `confirmarCancelacion` | Enviar notificaciones | Bajo |
| `common` (auditoría) | Service (`AuditoriaService`) | Múltiples flujos | Registrar acciones | Bajo |
| `common` (configuración) | Service (`ConfiguracionService`) | `crearEvento`, `existeSolapamiento` | DIAS_EXPIRACION_COTIZACION, MINUTOS_GAP_ENTRE_EVENTOS | Bajo |
| `vista_eventos_periodo` | Vista SQL | `obtenerCalendario` | Datos consolidados | Solo lectura |
| `vista_rentabilidad_evento` | Vista SQL | `getRentabilidad`, `getResumenInterno` | Cálculos financieros | Solo lectura |
| `vista_pagos_evento` | Vista SQL | `getResumenInterno` | Resumen de cobros | Solo lectura |

```text
Dependencia circular:
- Detectada: Sí (evento → cronograma, vía CronogramaService)
- Detalle: EventoServiceImpl inyecta CronogramaService para cancelarPorEvento().
  CronogramaServiceImpl inyecta EventoRepository para leer eventos.
- Riesgo: Bajo. El acoplamiento es solo de lectura desde cronograma hacia evento.
- Cómo se evita: CronogramaService se inyecta con @Lazy en EventoServiceImpl.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| POST crear | — | — | listado eventos, calendario |
| PUT editar (COMPLETA) | Cambia precioBase si cambia decoración | evento | detalle evento, listado |
| PATCH programar | Crea reservas en evento_producto (ACTIVA), auditoría | inventario, common | evento, inventario |
| PATCH confirmar | Notificación confirmación manual, auditoría | notificacion, common | evento, notificaciones |
| PATCH iniciar-montaje | Notificación inicio montaje | notificacion | evento, notificaciones |
| PATCH finalizar | Consume stock (CONSUMIDA), movimientos inventario, notificación saldo si aplica | inventario, notificacion | evento, inventario, pagos |
| PATCH cancelar | Guarda estadoAnterior, fechaCancelacion, motivoCancelacion | evento | evento, calendario |
| PATCH cancelar (COTIZACION) | Cancelación directa, sin tiempo de gracia | evento | evento |
| PATCH revertir-cancelacion | Restaura estado, limpia fechas/motivo | evento | evento, calendario |
| PATCH confirmar-cancelacion | Procesa devolución pagos, libera stock (LIBERADA), cancela cronograma, notifica si aplica | pago, inventario, cronograma, notificacion | evento, pagos, inventario, cronograma |
| PATCH extras | Reserva stock extra, incrementa precioAdicional, concatena notaPrecio, auditoría | inventario, common | evento, pagos, inventario |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Editar evento (completo) | COTIZACION | PROGRAMADO, CONFIRMADO, EN_MONTAJE, FINALIZADO, CANCELADO, CANCELACION_PENDIENTE | "Solo se pueden editar cotizaciones" |
| Editar ubicación/nota/notaPrecio | PROGRAMADO | resto de estados | "Edición limitada en este estado" |
| Programar | COTIZACION | resto de estados | "Solo se puede programar una cotización" |
| Confirmar | PROGRAMADO | resto de estados | "Se requiere anticipo mínimo" |
| Iniciar montaje | CONFIRMADO | resto de estados | |
| Finalizar | EN_MONTAJE | resto de estados | |
| Cancelar | COTIZACION, PROGRAMADO, CONFIRMADO, EN_MONTAJE | CANCELADO, CANCELACION_PENDIENTE, FINALIZADO | "No se puede cancelar en este estado" |
| Revertir cancelación | CANCELACION_PENDIENTE | resto de estados | "Solo se puede revertir una cancelación pendiente" |
| Confirmar cancelación | CANCELACION_PENDIENTE | resto de estados | |
| Agregar extras | PROGRAMADO, CONFIRMADO | resto de estados | "Solo disponible en eventos programados o confirmados" |
| Ver rentabilidad | cualquier estado con datos | — | |
| Ver detalle interno | cualquier estado | — | |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| clienteId | COTIZACION | PROGRAMADO+ | |
| decoracionId | COTIZACION | PROGRAMADO+ | Al cambiar, se actualiza precioBase |
| fecha | COTIZACION | PROGRAMADO+ | |
| horaInicio | COTIZACION | PROGRAMADO+ | |
| horaFin | COTIZACION | PROGRAMADO+ | |
| ubicacion | COTIZACION, PROGRAMADO | CONFIRMADO+ | |
| nota | COTIZACION, PROGRAMADO | CONFIRMADO+ | |
| notaPrecio | COTIZACION, PROGRAMADO | CONFIRMADO+ | Se puede modificar también vía extras (concatena) |
| precioAdicional | Solo vía PATCH extras | — | No se edita directamente, se acumula |
| precioBase | Solo vía cambio de decoración | — | |
| precioTotal | NUNCA | — | Columna generada: `precioBase + precioAdicional` |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| crear, editar, programar, confirmar, iniciar-montaje, finalizar | listado eventos, calendario, detalle evento |
| programar, finalizar, cancelar (confirmar), extras | inventario (stock) |
| confirmar, cancelar | pagos |
| cancelar (confirmar) | cronograma |
| confirmar, iniciar-montaje, cancelar | notificaciones |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| COTIZACION | Propuesta pendiente | Badge neutral/info |
| PROGRAMADO | Agendado con stock reservado | Badge primary |
| CONFIRMADO | Anticipo pagado | Badge success |
| EN_MONTAJE | En preparación activa | Badge warning |
| FINALIZADO | Completado | Badge secondary |
| CANCELACION_PENDIENTE | Cancelación en tiempo de gracia | Badge warning + contador regresivo |
| CANCELADO | Cancelado definitivamente | Badge danger |

### 10.5 Reglas importantes

- No permitir editar `precioTotal` — es columna generada automáticamente.
- En PROGRAMADO, solo enviar `ubicacion`, `nota`, `notaPrecio` en el PUT. Cualquier otro campo será rechazado.
- El solapamiento de eventos NO depende de la ubicación. Dos eventos en la misma franja horaria son conflictivos aunque tengan distinta ubicación.
- El gap mínimo entre eventos es de 120 minutos (2 horas). Esto significa que no puede haber un evento que empiece menos de 2 horas después de que termine otro, ni que termine menos de 2 horas antes de que empiece otro.
- `notaPrecio` es obligatorio cuando `precioAdicional > 0`. Validar en frontend antes de enviar.
- Al cancelar, siempre exigir `motivo` al usuario antes de llamar al endpoint.
- La cancelación desde COTIZACION es directa (sin CANCELACION_PENDIENTE). No mostrar opción de revertir en ese caso.
- `CalendarioEventoDTO` viene de una vista SQL — no asumir que tiene todos los campos de `EventoResponseDTO`. En particular, no expone `id_estado_anterior`, `event_nota`, `motivo_cancelacion` ni `event_fecha_cancelacion`.
- `EventoResumenInternoDTO` (`/detalle`) es solo lectura para uso administrativo interno.
- El job de cancelaciones pendientes (`JobCancelacionesPendientes`) procesa automáticamente eventos cuyo tiempo de gracia expiró. El frontend debe esperar que un evento en CANCELACION_PENDIENTE pueda pasar a CANCELADO sin intervención del usuario.

---

## Resumen para frontend

- El endpoint base es `/api/eventos`. Todos requieren rol ADMIN.
- Los estados del evento son 7, con transiciones estrictas orquestadas por el backend.
- `precioTotal` es generado por la DB — nunca se envía en request, solo se lee.
- En PROGRAMADO solo se pueden editar `ubicacion`, `nota`, `notaPrecio`. Cualquier otro campo en el PUT será rechazado.
- El solapamiento es puramente por tiempo (gap mínimo 2 horas), sin importar la ubicación.
- La cancelación desde COTIZACION es directa e irreversible. Desde otros estados, pasa por CANCELACION_PENDIENTE con tiempo de gracia y posibilidad de reversión.
- `CalendarioEventoDTO` (vista) no reemplaza a `EventoResponseDTO` — no tiene todos los campos.
- Para detalle administrativo completo usar `GET /{id}/detalle` que devuelve `EventoResumenInternoDTO` con evento + pagos + gastos + rentabilidad.
- Las excepciones de negocio (`EventoException`) vienen con HTTP 400 y mensaje descriptivo — mostrarlo al usuario.
- El frontend debe bloquear acciones según el estado del evento (ver tabla 10.1).
- `notaPrecio` debe ser obligatorio en el form cuando `precioAdicional > 0`.
