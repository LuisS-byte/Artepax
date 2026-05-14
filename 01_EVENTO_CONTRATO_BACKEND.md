# Evento — Contrato Backend para Frontend

Documento generado a partir del código actual del módulo `evento` y archivos comunes de manejo de errores.

Alcance: este contrato documenta el módulo principal de eventos: ciclo de vida, disponibilidad, edición, extras, cancelación, stock, pagos, notificaciones, rentabilidad y detalle interno. `GastoEvento` y `TipoEvento` se mencionan como dependencias/submódulos, pero deberían documentarse aparte si se requiere contrato completo de sus controllers y services.

---

## 1. Responsabilidad del módulo

El módulo `evento` administra la entidad central del sistema: la planificación, transición de estados, reserva de stock, confirmación, montaje, finalización, cancelación, extras, disponibilidad, calendario, rentabilidad y resumen interno de eventos.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Gestionar el ciclo de vida operativo de eventos desde `COTIZACION` hasta `FINALIZADO` o `CANCELADO`. |
| Controller principal | `EventoController` |
| Service principal | `EventoService` / `EventoServiceImpl` |
| Entidades principales | `Evento`, `EstadoEvento`, `EventoProducto`, `EstadoReserva`, `VistaEventosPeriodo`, `VistaRentabilidadEvento` |
| Tablas/vistas implicadas | `evento`, `estado_evento`, `evento_producto`, `estado_reserva`, `vista_eventos_periodo`, `vista_rentabilidad_evento`, `vista_pagos_evento`, `gasto_evento` |
| Módulos relacionados | Clientes, Decoraciones, Inventario, Pagos, Cronograma, Notificaciones, Auditoría, Configuración, Gastos |
| Tipo de módulo | Operativo / Mixto |

### Qué administra

- Creación de eventos en estado `COTIZACION`.
- Edición completa/parcial según estado.
- Disponibilidad por fecha y horario.
- Transiciones de estado.
- Reserva, liberación y consumo de stock asociado al evento.
- Cancelación con estado intermedio `CANCELACION_PENDIENTE`.
- Gestión de pagos al confirmar cancelación mediante `PagoCancelacionHandler`.
- Notificaciones administrativas de confirmación, montaje, saldo pendiente y cancelación.
- Auditoría en acciones relevantes.
- Consulta de calendario, rentabilidad y resumen interno.

### Qué no administra directamente

- CRUD completo de clientes.
- CRUD completo de decoraciones.
- Registro y confirmación directa de pagos.
- CRUD completo de cronograma.
- CRUD completo de documentos.
- CRUD completo de gastos y tipos de evento, aunque existen clases relacionadas dentro del paquete `evento`.

---

## 2. Controllers y endpoints completos

Base path: `/api/eventos`

Seguridad declarada en controller: `@PreAuthorize("hasRole('ADMIN')")`. Todos los endpoints de este controller requieren rol `ADMIN`.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/eventos` | Crear evento | `EventoRequestDTO` | `EventoResponseDTO` | `crearEvento` | ADMIN | Crea en `COTIZACION`. |
| GET | `/api/eventos` | Listar eventos | No aplica | `List<EventoResponseDTO>` | `obtenerEventos` | ADMIN | Ordenado por fecha DESC y horaInicio DESC desde repository. |
| GET | `/api/eventos/{id}` | Obtener evento por ID | Path `id` | `EventoResponseDTO` | `obtenerEventoPorId` | ADMIN | Lanza `EventoNotFoundException` si no existe. |
| PUT | `/api/eventos/{id}` | Actualizar evento | `EventoUpdateDTO` | `EventoResponseDTO` | `actualizarEvento` | ADMIN | Edición completa en `COTIZACION`, parcial en `PROGRAMADO`. |
| PATCH | `/api/eventos/{id}/extras` | Agregar extras | `EventoExtraRequestDTO` | `EventoResponseDTO` | `agregarExtras` | ADMIN | Permitido en `PROGRAMADO` o `CONFIRMADO`. |
| PATCH | `/api/eventos/{id}/programar` | Programar evento | Path `id` | `EventoResponseDTO` | `programarEvento` | ADMIN | `COTIZACION -> PROGRAMADO`; reserva stock. |
| PATCH | `/api/eventos/{id}/confirmar` | Confirmar evento manualmente | Path `id` | `EventoResponseDTO` | `confirmarEvento` | ADMIN | `PROGRAMADO -> CONFIRMADO`; valida anticipo mínimo. |
| PATCH | `/api/eventos/{id}/iniciar-montaje` | Iniciar montaje | Path `id` | `EventoResponseDTO` | `iniciarMontaje` | ADMIN | `CONFIRMADO -> EN_MONTAJE`. |
| PATCH | `/api/eventos/{id}/finalizar` | Finalizar evento | Path `id` | `EventoResponseDTO` | `finalizarEvento` | ADMIN | `EN_MONTAJE -> FINALIZADO`; consume stock. |
| PATCH | `/api/eventos/{id}/cancelar` | Iniciar cancelación | `CancelacionEventoRequestDTO` | `EventoResponseDTO` | `iniciarCancelacion` | ADMIN | Directa si está en `COTIZACION`; con gracia si está en estados operativos. |
| PATCH | `/api/eventos/{id}/revertir-cancelacion` | Revertir cancelación pendiente | Path `id` | `EventoResponseDTO` | `revertirCancelacion` | ADMIN | Solo desde `CANCELACION_PENDIENTE`. |
| PATCH | `/api/eventos/{id}/confirmar-cancelacion` | Confirmar cancelación definitiva | Path `id` | `204 No Content` | `confirmarCancelacion` | ADMIN | Procesa pagos, libera stock, cancela cronograma. |
| GET | `/api/eventos/disponibilidad` | Verificar disponibilidad | Query params | `Boolean` | `verificarDisponibilidad` | ADMIN | Valida fecha/hora/ubicación para bloqueo. |
| GET | `/api/eventos/calendario` | Obtener calendario por rango | Query `inicio`, `fin` | `List<CalendarioEventoDTO>` | `obtenerCalendario` | ADMIN | Usa `vista_eventos_periodo`. |
| GET | `/api/eventos/{id}/rentabilidad` | Obtener rentabilidad | Path `id` | `RentabilidadEventoDTO` | `getRentabilidad` | ADMIN | Usa `vista_rentabilidad_evento`. |
| GET | `/api/eventos/{id}/detalle` | Obtener detalle interno | Path `id` | `EventoResumenInternoDTO` | `getResumenInterno` | ADMIN | Incluye resumen de pagos, gastos y rentabilidad opcional. |

### POST `/api/eventos`

- Propósito: crear una cotización de evento.
- Request body: `EventoRequestDTO`.
- Response: `EventoResponseDTO`.
- HTTP esperado: `201 Created`.
- Método del service: `crearEvento`.
- Validaciones principales:
  - Cliente obligatorio y activo.
  - Decoración obligatoria y activa.
  - Tipo de evento obligatorio y activo.
  - Fecha no pasada.
  - Si se envía horario, `horaInicio` y `horaFin` deben venir juntas.
  - `horaFin` debe ser posterior a `horaInicio`.
  - Si hay precio adicional mayor a cero, `notaPrecio` es obligatoria.
  - Valida disponibilidad si hay horario/ubicación.
- Excepciones posibles: `EventoException`, validaciones DTO, errores genéricos.
- Efectos secundarios:
  - Crea evento en `COTIZACION`.
  - Calcula `fechaExpiracion` usando `DIAS_EXPIRACION_COTIZACION`.
  - Copia precio base desde decoración.
  - `clienteNotificado = false`.
- Frontend debe refrescar:
  - Listado de eventos.
  - Calendario si la pantalla lo muestra.
  - Dashboard/KPIs si dependen de eventos.

### GET `/api/eventos`

- Propósito: listar eventos.
- Response: `List<EventoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `obtenerEventos`.
- Consulta usada: `eventoRepository.findAllWithRelations()`.
- Orden actual: `ORDER BY e.fecha DESC, e.horaInicio DESC`.
- Efectos secundarios: no.
- Frontend debe refrescar: no aplica; es endpoint de lectura.

### GET `/api/eventos/{id}`

- Propósito: obtener detalle básico de evento.
- Path variables: `id`.
- Response: `EventoResponseDTO`.
- HTTP esperado: `200 OK` o `404 Not Found`.
- Método del service: `obtenerEventoPorId`.
- Excepciones posibles: `EventoNotFoundException`.
- Efectos secundarios: no.

### PUT `/api/eventos/{id}`

- Propósito: actualizar evento.
- Request body: `EventoUpdateDTO`.
- Response: `EventoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `actualizarEvento`.
- Reglas:
  - En `COTIZACION`: edición completa.
  - En `PROGRAMADO`: edición parcial.
  - En otros estados: bloqueado.
- Edición parcial en `PROGRAMADO`:
  - Permitido: `ubicacion`, `nota`, `notaPrecio`.
  - Bloqueado: `clienteId`, `decoracionId`, `fecha`, `horaInicio`, `horaFin`.
- Efectos secundarios:
  - Puede cambiar precio base si cambia decoración en edición completa.
- Frontend debe refrescar:
  - Detalle del evento.
  - Listado de eventos.
  - Calendario si cambia fecha/hora/ubicación en edición completa.

### PATCH `/api/eventos/{id}/extras`

- Propósito: agregar productos extra a un evento operativo.
- Request body: `EventoExtraRequestDTO`.
- Response: `EventoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `agregarExtras`.
- Reglas:
  - Solo permitido en `PROGRAMADO` o `CONFIRMADO`.
  - Requiere al menos un producto.
  - Requiere `notaPrecio`.
  - Requiere `montoAdicional > 0`.
  - Consolida cantidades por producto.
  - Valida producto activo.
  - Reserva stock extra.
- Efectos secundarios:
  - Incrementa `precioAdicional`.
  - Combina `notaPrecio` existente con nueva nota.
  - Crea o incrementa reservas activas en `evento_producto`.
  - Registra auditoría `AGREGAR_EXTRAS`.
- Frontend debe refrescar:
  - Detalle del evento.
  - Inventario/stock si se visualiza disponibilidad.
  - Resumen de pagos porque cambia `precioTotal`.

### PATCH `/api/eventos/{id}/programar`

- Propósito: pasar un evento de cotización a programado.
- Response: `EventoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `programarEvento`.
- Transición: `COTIZACION -> PROGRAMADO`.
- Validaciones:
  - Estado actual debe ser `COTIZACION`.
  - Stock suficiente para productos de la decoración.
- Efectos secundarios:
  - Reserva stock en `evento_producto`.
  - Cambia estado a `PROGRAMADO`.
  - Registra auditoría `PROGRAMAR`.
- Frontend debe refrescar:
  - Detalle/listado de eventos.
  - Inventario.
  - Acciones disponibles del evento.

### PATCH `/api/eventos/{id}/confirmar`

- Propósito: confirmar manualmente un evento.
- Response: `EventoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `confirmarEvento`.
- Transición: `PROGRAMADO -> CONFIRMADO`.
- Validaciones:
  - Estado actual debe ser `PROGRAMADO`.
  - Anticipo mínimo cumplido según `PORCENTAJE_ANTICIPO_MINIMO`.
- Efectos secundarios:
  - Cambia estado a `CONFIRMADO`.
  - Setea `fechaConfirmacion`.
  - Registra auditoría `CONFIRMAR`.
  - Notifica confirmación manual con tipo `CONFIRMACION_RESERVA`.
- Frontend debe refrescar:
  - Detalle/listado de eventos.
  - Notificaciones.
  - Acciones disponibles.

### PATCH `/api/eventos/{id}/iniciar-montaje`

- Propósito: iniciar fase de montaje.
- Response: `EventoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `iniciarMontaje`.
- Transición: `CONFIRMADO -> EN_MONTAJE`.
- Efectos secundarios:
  - Cambia estado.
  - Notifica inicio de montaje con tipo `INICIO_MONTAJE`.
- Frontend debe refrescar:
  - Detalle/listado de eventos.
  - Notificaciones.
  - Acciones disponibles.

### PATCH `/api/eventos/{id}/finalizar`

- Propósito: finalizar evento.
- Response: `EventoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `finalizarEvento`.
- Transición: `EN_MONTAJE -> FINALIZADO`.
- Validaciones:
  - Estado actual debe ser `EN_MONTAJE`.
- Efectos secundarios:
  - Si hay saldo pendiente, genera notificación `SALDO_PENDIENTE`.
  - Consume stock reservado.
  - Registra movimientos de salida de inventario con origen `EVENTO`.
  - Cambia reservas a `CONSUMIDA`.
  - Cambia estado a `FINALIZADO`.
- Frontend debe refrescar:
  - Detalle/listado de eventos.
  - Inventario/movimientos.
  - Pagos/resumen si se muestra saldo.
  - Notificaciones.

### PATCH `/api/eventos/{id}/cancelar`

- Propósito: iniciar cancelación.
- Request body: `CancelacionEventoRequestDTO`.
- Response: `EventoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `iniciarCancelacion`.
- Reglas:
  - `motivo` obligatorio.
  - Si estado actual es `COTIZACION`: cancela directamente.
  - Si estado actual está en `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE`: pasa a `CANCELACION_PENDIENTE`.
  - Otros estados: bloqueado.
- Efectos secundarios:
  - Guarda `estadoAnterior` en cancelación con gracia.
  - Guarda `motivoCancelacion`.
  - Setea `fechaCancelacion`.
  - Notifica cancelación iniciada si entra a `CANCELACION_PENDIENTE`.
- Frontend debe refrescar:
  - Detalle/listado.
  - Acciones disponibles.
  - Notificaciones.

### PATCH `/api/eventos/{id}/revertir-cancelacion`

- Propósito: revertir cancelación pendiente.
- Response: `EventoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `revertirCancelacion`.
- Reglas:
  - Solo desde `CANCELACION_PENDIENTE`.
  - Requiere `estadoAnterior` no nulo.
- Efectos secundarios:
  - Restaura estado anterior.
  - Limpia `estadoAnterior`, `motivoCancelacion` y `fechaCancelacion`.
- Frontend debe refrescar:
  - Detalle/listado.
  - Acciones disponibles.

### PATCH `/api/eventos/{id}/confirmar-cancelacion`

- Propósito: confirmar cancelación definitiva.
- Response: `204 No Content`.
- Método del service: `confirmarCancelacion`.
- Transición: `CANCELACION_PENDIENTE -> CANCELADO`.
- Efectos secundarios:
  - Procesa pagos mediante `PagoCancelacionHandler`:
    - `PENDIENTE -> CANCELADO`.
    - `PAGADO -> DEVOLUCION_PENDIENTE`.
  - Si hay devoluciones pendientes, el handler notifica monto pendiente de devolución.
  - Libera reservas activas.
  - Cambia estado a `CANCELADO`.
  - Limpia `estadoAnterior`.
  - Cancela cronograma asociado.
  - Si no hubo devolución pendiente, notifica cancelación confirmada.
- Frontend debe refrescar:
  - Listado/detalle de eventos.
  - Pagos del evento.
  - Inventario/stock.
  - Cronograma.
  - Notificaciones.

### GET `/api/eventos/disponibilidad`

- Propósito: verificar si un horario está disponible.
- Query params:
  - `fecha: LocalDate`
  - `horaInicio: LocalTime`
  - `horaFin: LocalTime`
  - `ubicacion: String`
  - `eventoIdExcluir?: Long`
- Response: `Boolean`.
- Método del service: `verificarDisponibilidad`.
- Reglas:
  - `horaInicio`, `horaFin` y `ubicacion` son requeridos para validar.
  - `horaFin` debe ser posterior a `horaInicio`.
  - Aplica gap mínimo entre eventos. Intenta leer `MINUTOS_GAP_ENTRE_EVENTOS`; si falla, usa 0, luego aplica mínimo interno de 120 minutos.
- Nota importante:
  - El repository actual recibe `ubicacion`, pero las queries `existeSolapamiento` y `existeSolapamientoExcluyendoEvento` no filtran por ubicación. El comportamiento real bloquea por fecha/hora/gap en estados activos, no por ubicación.
- Frontend debe refrescar: no aplica.

### GET `/api/eventos/calendario`

- Propósito: consultar eventos en calendario.
- Query params:
  - `inicio: LocalDate`
  - `fin: LocalDate`
- Response: `List<CalendarioEventoDTO>`.
- Método del service: `obtenerCalendario`.
- Reglas:
  - `inicio` no puede ser mayor a `fin`.
- Fuente: `vista_eventos_periodo`.

### GET `/api/eventos/{id}/rentabilidad`

- Propósito: consultar rentabilidad calculada.
- Response: `RentabilidadEventoDTO`.
- Método del service: `getRentabilidad`.
- Fuente: `vista_rentabilidad_evento`.
- Error: si no existe rentabilidad para el evento, lanza `EventoException`.

### GET `/api/eventos/{id}/detalle`

- Propósito: obtener resumen administrativo ampliado.
- Response: `EventoResumenInternoDTO`.
- Método del service: `getResumenInterno`.
- Incluye:
  - Datos base del evento.
  - Resumen de pagos desde `vista_pagos_evento`.
  - Resumen de gastos desde `gasto_evento`.
  - Rentabilidad opcional desde `vista_rentabilidad_evento`.

---

## 3. DTOs request/response

### 3.1 DTOs Request

#### EventoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `clienteId` | `Long` | `clienteId` | Sí | No | `@NotNull` | Debe referir cliente activo. |
| `decoracionId` | `Long` | `decoracionId` | Sí | No | `@NotNull` | Debe referir decoración activa. |
| `tipoEventoId` | `Long` | `tipoEventoId` | Sí | No | `@NotNull` | Debe referir tipo activo. |
| `fecha` | `LocalDate` | `fecha` | Sí | No | `@NotNull`; service valida no pasada | Formato ISO `YYYY-MM-DD`. |
| `horaInicio` | `LocalTime` | `horaInicio` | No | Sí | Validación manual si se usa horario | Si se envía, debe enviarse también `horaFin`. |
| `horaFin` | `LocalTime` | `horaFin` | No | Sí | Validación manual si se usa horario | Debe ser posterior a `horaInicio`. |
| `ubicacion` | `String` | `ubicacion` | No | Sí | Validación manual para disponibilidad | Si se valida disponibilidad, debe existir. |
| `nota` | `String` | `nota` | No | Sí | No declarada | Observación interna. |
| `precioAdicional` | `BigDecimal` | `precioAdicional` | No | Sí | Manual si `> 0` requiere `notaPrecio` | Si no se envía, backend usa `0`. |
| `notaPrecio` | `String` | `notaPrecio` | Condicional | Sí | Manual si `precioAdicional > 0` | Justificación del precio adicional. |

#### EventoUpdateDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `clienteId` | `Long` | `clienteId` | No | Sí | Permitido solo en edición completa | Bloqueado en `PROGRAMADO`. |
| `decoracionId` | `Long` | `decoracionId` | No | Sí | Permitido solo en edición completa | Bloqueado en `PROGRAMADO`. |
| `fecha` | `LocalDate` | `fecha` | No | Sí | No pasada en edición completa | Bloqueado en `PROGRAMADO`. |
| `horaInicio` | `LocalTime` | `horaInicio` | No | Sí | Debe ir junto con `horaFin` | Bloqueado en `PROGRAMADO`. |
| `horaFin` | `LocalTime` | `horaFin` | No | Sí | Debe ir junto con `horaInicio` | Bloqueado en `PROGRAMADO`. |
| `ubicacion` | `String` | `ubicacion` | No | Sí | Permitido en edición completa y parcial | Editable en `PROGRAMADO`. |
| `nota` | `String` | `nota` | No | Sí | No declarada | Editable en `PROGRAMADO`. |
| `notaPrecio` | `String` | `notaPrecio` | Condicional | Sí | Obligatoria si precio adicional del evento es `> 0` | Editable en `PROGRAMADO`. |

#### CancelacionEventoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `motivo` | `String` | `motivo` | Sí | No | `@NotBlank` | Debe pedirse al usuario antes de cancelar. |

#### EventoExtraRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `items` | `List<EventoExtraItemRequestDTO>` | `items` | Sí | No | `@Valid`, `@NotEmpty` | Debe contener al menos un producto extra. |
| `notaPrecio` | `String` | `notaPrecio` | Sí | No | `@NotBlank` | Motivo de incremento de precio. |
| `montoAdicional` | `BigDecimal` | `montoAdicional` | Sí | No | `@NotNull`, `@DecimalMin(0.01)` | Incrementa `precioAdicional`. |

#### EventoExtraItemRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `productoId` | `Long` | `productoId` | Sí | No | `@NotNull` | Producto extra a reservar. |
| `cantidad` | `Integer` | `cantidad` | Sí | No | `@NotNull`, `@Min(1)` | Cantidad adicional requerida. |

### 3.2 DTOs Response

#### EventoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `Evento.id` | Identificador principal. |
| `fecha` | `LocalDate` | `fecha` | No | `Evento.fecha` | Fecha del evento. |
| `horaInicio` | `LocalTime` | `horaInicio` | Sí | `Evento.horaInicio` | Puede ser nulo. |
| `horaFin` | `LocalTime` | `horaFin` | Sí | `Evento.horaFin` | Puede ser nulo. |
| `ubicacion` | `String` | `ubicacion` | Sí | `Evento.ubicacion` | Puede ser nulo. |
| `nota` | `String` | `nota` | Sí | `Evento.nota` | Nota interna. |
| `motivoCancelacion` | `String` | `motivoCancelacion` | Sí | `Evento.motivoCancelacion` | Mostrar en estados de cancelación. |
| `fechaCreacion` | `LocalDateTime` | `fechaCreacion` | Sí | `Evento.fechaCreacion` | Auditoría visual. |
| `clienteId` | `Long` | `clienteId` | Sí | Relación cliente | Usar para navegación. |
| `clienteNombre` | `String` | `clienteNombre` | Sí | Relación cliente | Mostrar en tablas/cards. |
| `decoracionId` | `Long` | `decoracionId` | Sí | Relación decoración | Usar para navegación. |
| `decoracionNombre` | `String` | `decoracionNombre` | Sí | Relación decoración | Mostrar en UI. |
| `estadoEventoId` | `Long` | `estadoEventoId` | Sí | Relación estado | No basar lógica solo en ID. |
| `estadoEventoNombre` | `String` | `estadoEventoNombre` | Sí | Relación estado | Base para badges y acciones. |
| `tipoEventoId` | `Long` | `tipoEventoId` | Sí | Relación tipo evento | Usar para edición completa. |
| `tipoEventoNombre` | `String` | `tipoEventoNombre` | Sí | Relación tipo evento | Mostrar en UI. |
| `precioBase` | `BigDecimal` | `precioBase` | Sí | `Evento.precioBase` | Copiado desde decoración. |
| `precioAdicional` | `BigDecimal` | `precioAdicional` | Sí | `Evento.precioAdicional` | Incluye extras. |
| `precioTotal` | `BigDecimal` | `precioTotal` | Sí | Columna generada DB | No calcular manualmente en frontend como fuente final. |
| `notaPrecio` | `String` | `notaPrecio` | Sí | `Evento.notaPrecio` | Justificación de adicional/extras. |
| `clienteNotificado` | `Boolean` | `clienteNotificado` | Sí | `Evento.clienteNotificado` | Útil para documentos/notificación cliente. |
| `fechaConfirmacion` | `LocalDateTime` | `fechaConfirmacion` | Sí | `Evento.fechaConfirmacion` | Nulo si no confirmado. |
| `fechaCancelacion` | `LocalDateTime` | `fechaCancelacion` | Sí | `Evento.fechaCancelacion` | Nulo si no cancelado/en proceso. |
| `fechaExpiracion` | `LocalDate` | `fechaExpiracion` | Sí | `Evento.fechaExpiracion` | Usar en cotizaciones. |

#### CalendarioEventoDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `idEvento` | `Long` | `idEvento` | No | `vista_eventos_periodo` | ID de evento. |
| `fechaEvento` | `LocalDate` | `fechaEvento` | No | Vista | Fecha en calendario. |
| `horaInicio` | `LocalTime` | `horaInicio` | Sí | Vista | Puede ser nulo. |
| `horaFin` | `LocalTime` | `horaFin` | Sí | Vista | Puede ser nulo. |
| `ubicacion` | `String` | `ubicacion` | Sí | Vista | Lugar. |
| `estado` | `String` | `estado` | Sí | Vista | Badge. |
| `tipoEvento` | `String` | `tipoEvento` | Sí | Vista | Texto visible. |
| `clienteNombre` | `String` | `clienteNombre` | Sí | Vista | Texto visible. |
| `clienteTelefono` | `String` | `clienteTelefono` | Sí | Vista | Contacto. |
| `clienteCorreo` | `String` | `clienteCorreo` | Sí | Vista | Contacto. |
| `decoracion` | `String` | `decoracion` | Sí | Vista | Texto visible. |
| `decoracionImagen` | `String` | `decoracionImagen` | Sí | Vista | Imagen si existe. |
| `precioTotal` | `BigDecimal` | `precioTotal` | Sí | Vista | Monto total. |
| `clienteNotificado` | `Boolean` | `clienteNotificado` | Sí | Vista | Flag. |
| `fechaConfirmacion` | `LocalDateTime` | `fechaConfirmacion` | Sí | Vista | Fecha si aplica. |
| `fechaExpiracion` | `LocalDate` | `fechaExpiracion` | Sí | Vista | Cotización. |
| `fechaCreacion` | `LocalDateTime` | `fechaCreacion` | Sí | Vista | Registro. |
| `totalCobrado` | `BigDecimal` | `totalCobrado` | Sí | Vista | Resumen de pagos. |
| `saldoPendiente` | `BigDecimal` | `saldoPendiente` | Sí | Vista | Resumen de pagos. |
| `cobroCompleto` | `Boolean` | `cobroCompleto` | Sí | Vista | Badge financiero. |
| `tieneCronograma` | `Boolean` | `tieneCronograma` | Sí | Vista | Indicador operativo. |

#### EventoResumenInternoDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `idEvento` | `Long` | `idEvento` | No | Evento | ID. |
| `estadoEvento` | `String` | `estadoEvento` | Sí | Evento | Badge. |
| `fecha` | `LocalDate` | `fecha` | Sí | Evento | Fecha. |
| `horaInicio` | `LocalTime` | `horaInicio` | Sí | Evento | Hora. |
| `horaFin` | `LocalTime` | `horaFin` | Sí | Evento | Hora. |
| `ubicacion` | `String` | `ubicacion` | Sí | Evento | Lugar. |
| `clienteNombre` | `String` | `clienteNombre` | Sí | Relación cliente | Texto. |
| `decoracionNombre` | `String` | `decoracionNombre` | Sí | Relación decoración | Texto. |
| `tipoEventoNombre` | `String` | `tipoEventoNombre` | Sí | Relación tipo | Texto. |
| `nota` | `String` | `nota` | Sí | Evento | Interna. |
| `notaPrecio` | `String` | `notaPrecio` | Sí | Evento | Justificación precio. |
| `precioBase` | `BigDecimal` | `precioBase` | Sí | Evento | Monto. |
| `precioAdicional` | `BigDecimal` | `precioAdicional` | Sí | Evento | Monto. |
| `precioTotal` | `BigDecimal` | `precioTotal` | Sí | Evento | Monto. |
| `fechaConfirmacion` | `LocalDateTime` | `fechaConfirmacion` | Sí | Evento | Fecha. |
| `resumenPagos` | `ResumenPagoEventoDTO` | `resumenPagos` | No | `vista_pagos_evento` o fallback | Resumen financiero. Incluye `totalDevolucionPendiente` y `tieneDevolucionPendiente`. |
| `resumenGastos` | `ResumenGastoEventoDTO` | `resumenGastos` | No | Sumas `gasto_evento` | Resumen de gastos. |
| `rentabilidad` | `RentabilidadEventoDTO` | `rentabilidad` | Sí | `vista_rentabilidad_evento` | Puede venir `null`. |

#### RentabilidadEventoDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `idEvento` | `Long` | `idEvento` | No | `vista_rentabilidad_evento` | ID. |
| `fechaEvento` | `LocalDate` | `fechaEvento` | Sí | Vista | Fecha. |
| `estado` | `String` | `estado` | Sí | Vista | Estado. |
| `precioAcordado` | `BigDecimal` | `precioAcordado` | Sí | Vista | Ingreso esperado. |
| `ingresoCobrado` | `BigDecimal` | `ingresoCobrado` | Sí | Vista | Pagos cobrados. |
| `costoMateriales` | `BigDecimal` | `costoMateriales` | Sí | Vista | Costo de materiales. |
| `costoManoObra` | `BigDecimal` | `costoManoObra` | Sí | Vista | Costo de personal. |
| `gastosDesembolsados` | `BigDecimal` | `gastosDesembolsados` | Sí | Vista | Gastos reales. |
| `gastosProyectados` | `BigDecimal` | `gastosProyectados` | Sí | Vista | Gastos proyectados. |
| `gananciaNeta` | `BigDecimal` | `gananciaNeta` | Sí | Vista | Resultado. |

#### ResumenGastoEventoDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `totalProyectado` | `BigDecimal` | `totalProyectado` | No | Sumatoria service | Suma gastos `PROYECTADO`. |
| `totalDesembolsado` | `BigDecimal` | `totalDesembolsado` | No | Sumatoria service | Suma gastos `DESEMBOLSADO`. |
| `totalPendiente` | `BigDecimal` | `totalPendiente` | No | Actualmente igual a `totalProyectado` | Revisar semántica si frontend lo usa. |

---

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Evento` | `evento` | Tabla operativa | Entidad central del evento. | Contiene relaciones a cliente, decoración, tipo, estado actual y estado anterior. |
| `EstadoEvento` | `estado_evento` | Catálogo | Estados del ciclo de vida del evento. | Las transiciones se validan en backend. |
| `EventoProducto` | `evento_producto` | Tabla relación operativa | Reservas de producto por evento. | Usa PK compuesta `EventoProductoId`. |
| `EventoProductoId` | PK embebida | Relación | Identifica evento/producto. | Campos: `eventoId`, `productoId`. |
| `EstadoReserva` | `estado_reserva` | Catálogo | Estado de reserva de stock. | Estados usados: `ACTIVA`, `LIBERADA`, `CONSUMIDA`. |
| `VistaEventosPeriodo` | `vista_eventos_periodo` | Vista solo lectura | Resumen enriquecido para calendario/listados. | No sustituye detalle completo de `evento`. |
| `VistaRentabilidadEvento` | `vista_rentabilidad_evento` | Vista solo lectura | Rentabilidad calculada por evento. | Usada por endpoint `/rentabilidad` y detalle interno. |
| `GastoEvento` | `gasto_evento` | Tabla operativa relacionada | Gastos asociados a evento. | Contrato completo debería documentarse aparte. |
| `TipoEvento` | `tipo_evento` | Catálogo | Tipos de evento. | CRUD/documentación aparte. |

### Tabla `evento`

Campos principales mapeados por `Evento`:

- `id_evento`
- `id_cliente`
- `id_decoracion`
- `id_tipo_evento`
- `id_estadoevento`
- `id_estado_anterior`
- `event_fechaevento`
- `event_horainicio`
- `event_horafin`
- `event_ubicacion`
- `event_precio_base`
- `event_precio_adicional`
- `event_precio_total` como columna calculada/no editable desde JPA
- `event_nota_precio`
- `event_nota`
- `motivo_cancelacion`
- `event_cliente_notificado`
- `event_fecha_confirmacion`
- `event_fecha_cancelacion`
- `event_fecha_expiracion`
- `event_creacion`

### Vista `vista_eventos_periodo`

Uso principal: calendario y listados enriquecidos.

Columnas relevantes:

- Datos del cliente.
- Tipo, estado y decoración.
- Fecha, horario y ubicación.
- Precios.
- Estado financiero: `total_cobrado`, `saldo_pendiente`, `cobro_completo`.
- `tiene_cronograma`.

### Vista `vista_rentabilidad_evento`

Uso principal: rentabilidad.

Columnas:

- `precio_acordado`
- `ingreso_cobrado`
- `costo_materiales`
- `costo_mano_obra`
- `gastos_desembolsados`
- `gastos_proyectados`
- `ganancia_neta`

---

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Crear evento | `crearEvento` | Cliente/decoración/tipo activos, fecha, horario, disponibilidad, nota de precio | Crea `Evento` en `COTIZACION` | `EventoResponseDTO` |
| Listar eventos | `obtenerEventos` | No aplica | No modifica | `List<EventoResponseDTO>` |
| Obtener por ID | `obtenerEventoPorId` | Existencia | No modifica | `EventoResponseDTO` |
| Actualizar | `actualizarEvento` | Estado editable, campos permitidos, fecha/horario/disponibilidad | Actualiza campos del evento | `EventoResponseDTO` |
| Agregar extras | `agregarExtras` | Estado, nota, monto, productos activos, stock | Reserva extras, incrementa precio adicional | `EventoResponseDTO` |
| Ver disponibilidad | `verificarDisponibilidad` | Horario y parámetros | No modifica | `boolean` |
| Calendario | `obtenerCalendario` | Rango válido | No modifica | `List<CalendarioEventoDTO>` |
| Programar | `programarEvento` | `COTIZACION`, stock suficiente | Reserva stock, cambia a `PROGRAMADO` | `EventoResponseDTO` |
| Confirmar manual | `confirmarEvento` | `PROGRAMADO`, anticipo mínimo | Cambia a `CONFIRMADO`, setea fecha | `EventoResponseDTO` |
| Confirmar desde anticipo | `confirmarDesdeAnticipo` | `PROGRAMADO`, anticipo mínimo | Cambia a `CONFIRMADO`, setea fecha | `void` |
| Iniciar montaje | `iniciarMontaje` | `CONFIRMADO` | Cambia a `EN_MONTAJE` | `EventoResponseDTO` |
| Finalizar | `finalizarEvento` | `EN_MONTAJE` | Consume stock, cambia a `FINALIZADO` | `EventoResponseDTO` |
| Iniciar cancelación | `iniciarCancelacion` | Motivo, estado permitido | Cambia a `CANCELADO` o `CANCELACION_PENDIENTE` | `EventoResponseDTO` |
| Revertir cancelación | `revertirCancelacion` | `CANCELACION_PENDIENTE`, estado anterior | Restaura estado anterior | `EventoResponseDTO` |
| Confirmar cancelación | `confirmarCancelacion` | `CANCELACION_PENDIENTE` | Procesa pagos, libera stock, cancela cronograma, cambia a `CANCELADO` | `void` |
| Rentabilidad | `getRentabilidad` | Existencia en vista | No modifica | `RentabilidadEventoDTO` |
| Resumen interno | `getResumenInterno` | Existencia | No modifica | `EventoResumenInternoDTO` |

### Flujo: crear evento

1. Valida `precioAdicional` y `notaPrecio`.
2. Busca cliente, decoración y tipo de evento.
3. Valida que cliente, decoración y tipo estén activos.
4. Valida fecha no pasada.
5. Valida horario.
6. Valida disponibilidad.
7. Busca estado `COTIZACION`.
8. Mapea DTO a entidad.
9. Copia precio base desde decoración.
10. Calcula fecha de expiración con `DIAS_EXPIRACION_COTIZACION`.
11. Guarda y retorna `EventoResponseDTO`.

### Flujo: programar evento

1. Busca evento.
2. Valida transición `COTIZACION -> PROGRAMADO`.
3. Toma snapshot de auditoría.
4. Ejecuta `EventoStockHandler.validarYReservarStock`.
5. Busca estado `PROGRAMADO`.
6. Cambia estado.
7. Guarda.
8. Registra auditoría.
9. Retorna DTO.

### Flujo: confirmar evento manual

1. Busca evento.
2. Valida transición `PROGRAMADO -> CONFIRMADO`.
3. Valida anticipo mínimo con `EventoPagoValidator`.
4. Toma snapshot de auditoría.
5. Busca estado `CONFIRMADO`.
6. Cambia estado y setea `fechaConfirmacion`.
7. Guarda.
8. Registra auditoría.
9. Notifica confirmación manual.
10. Retorna DTO.

### Flujo: confirmar desde anticipo

1. Busca evento.
2. Si no está en `PROGRAMADO`, retorna sin error.
3. Valida anticipo mínimo.
4. Cambia a `CONFIRMADO`.
5. Setea `fechaConfirmacion`.
6. Registra auditoría.
7. Notifica confirmación automática.

### Flujo: iniciar cancelación

1. Valida motivo obligatorio.
2. Busca evento.
3. Si está en `COTIZACION`, cancela directamente.
4. Si está en `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE`, pasa a `CANCELACION_PENDIENTE`.
5. Guarda estado anterior.
6. Guarda motivo y fecha de cancelación.
7. Notifica cancelación iniciada.
8. Retorna DTO.

### Flujo: confirmar cancelación

1. Busca evento.
2. Valida transición `CANCELACION_PENDIENTE -> CANCELADO`.
3. Procesa pagos con `PagoCancelacionHandler`.
4. Libera reservas activas.
5. Cambia estado a `CANCELADO`.
6. Limpia `estadoAnterior`.
7. Cancela cronograma.
8. Guarda evento.
9. Si no hubo monto de devolución, notifica cancelación confirmada.
10. No retorna body.

### Flujo: finalizar evento

1. Busca evento.
2. Valida transición `EN_MONTAJE -> FINALIZADO`.
3. Consulta total pagado.
4. Si total pagado es menor al precio total, notifica saldo pendiente.
5. Consume stock reservado.
6. Cambia estado a `FINALIZADO`.
7. Guarda y retorna DTO.

### Flujo: agregar extras

1. Busca evento.
2. Valida que esté en `PROGRAMADO` o `CONFIRMADO`.
3. Valida nota de precio y monto adicional.
4. Consolida cantidades por producto.
5. Busca productos y valida activos.
6. Reserva stock extra.
7. Incrementa `precioAdicional`.
8. Combina notas de precio.
9. Guarda.
10. Registra auditoría.
11. Retorna DTO.

---

## 6. Estados y transiciones

### Estados existentes

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `COTIZACION` | Propuesta inicial sin reserva de stock | Creación, edición completa, programación | Sí | Estado inicial. |
| `PROGRAMADO` | Evento agendado con stock reservado | Confirmación, extras, edición parcial, cancelación | Sí | Mantiene reservas activas. |
| `CONFIRMADO` | Anticipo mínimo alcanzado | Montaje, extras, cancelación | Sí | Setea `fechaConfirmacion`. |
| `EN_MONTAJE` | Fase de montaje activa | Finalización, cancelación | Sí | Permite finalizar. |
| `FINALIZADO` | Evento completado | Estado terminal | Sí | Consume stock. |
| `CANCELACION_PENDIENTE` | Cancelación con tiempo de gracia | Revertir/confirmar cancelación | Sí | Mantiene reservas activas hasta confirmar. |
| `CANCELADO` | Evento cancelado definitivamente | Estado terminal | Sí | No debe permitir operación normal. |

### Transiciones permitidas

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| N/A | `COTIZACION` | `POST /api/eventos` | DTO válido, cliente/decoración/tipo activos | Crea evento. |
| `COTIZACION` | `PROGRAMADO` | `PATCH /{id}/programar` | Stock suficiente | Reserva stock, auditoría. |
| `PROGRAMADO` | `CONFIRMADO` | `PATCH /{id}/confirmar` | Anticipo mínimo | Notificación, auditoría. |
| `PROGRAMADO` | `CONFIRMADO` | Confirmación automática por pago | Anticipo mínimo | Notificación automática, auditoría. |
| `CONFIRMADO` | `EN_MONTAJE` | `PATCH /{id}/iniciar-montaje` | Estado exacto | Notificación. |
| `EN_MONTAJE` | `FINALIZADO` | `PATCH /{id}/finalizar` | Estado exacto | Consume stock, posible notificación de saldo. |
| `COTIZACION` | `CANCELADO` | `PATCH /{id}/cancelar` | Motivo obligatorio | Cancelación directa. |
| `PROGRAMADO` | `CANCELACION_PENDIENTE` | `PATCH /{id}/cancelar` | Motivo obligatorio | Notificación, guarda estado anterior. |
| `CONFIRMADO` | `CANCELACION_PENDIENTE` | `PATCH /{id}/cancelar` | Motivo obligatorio | Notificación, guarda estado anterior. |
| `EN_MONTAJE` | `CANCELACION_PENDIENTE` | `PATCH /{id}/cancelar` | Motivo obligatorio | Notificación, guarda estado anterior. |
| `CANCELACION_PENDIENTE` | Estado anterior | `PATCH /{id}/revertir-cancelacion` | Estado anterior no nulo | Limpia cancelación. |
| `CANCELACION_PENDIENTE` | `CANCELADO` | `PATCH /{id}/confirmar-cancelacion` | Estado exacto | Procesa pagos, libera stock, cancela cronograma. |

### Estados editables

| Estado | Modo de edición | Campos permitidos |
|---|---|---|
| `COTIZACION` | Completa | Cliente, decoración, fecha, horario, ubicación, nota, notaPrecio. |
| `PROGRAMADO` | Parcial | Ubicación, nota, notaPrecio. |
| `CONFIRMADO` | No editable por `PUT /{id}` | Bloquear edición general. |
| `EN_MONTAJE` | No editable por `PUT /{id}` | Bloquear edición general. |
| `FINALIZADO` | Terminal | Bloquear acciones mutantes generales. |
| `CANCELACION_PENDIENTE` | No editable por `PUT /{id}` | Permitir revertir o confirmar cancelación. |
| `CANCELADO` | Terminal | Bloquear acciones mutantes generales. |

### Estados de reserva

| Estado | Significado |
|---|---|
| `ACTIVA` | Reserva vigente y cuenta para disponibilidad. |
| `LIBERADA` | Reserva liberada por cancelación. |
| `CONSUMIDA` | Reserva consumida por finalización. |

---

## 7. Excepciones y respuestas de error

Formato de error común:

```json
{
  "codigo": 400,
  "mensaje": "Mensaje de error",
  "errorCode": null,
  "timestamp": "2026-05-09T..."
}
```

`ErrorResponse` contiene `codigo`, `mensaje`, `errorCode` y `timestamp`.

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `EventoException` | Regla de negocio incumplida | 400 | Mensaje específico del service | Mostrar mensaje del backend. |
| `EventoNotFoundException` | Evento no encontrado | 404 | `Evento no encontrado: {id}` o mensaje personalizado | Mostrar estado vacío/error. |
| `MethodArgumentNotValidException` | Validación DTO fallida | 400 | `{campo}: {mensaje}` | Resaltar campo en formulario. |
| `AccessDeniedException` | Usuario sin rol/permiso | 403 | Mensaje de Spring Security | Redirigir o mostrar sin permisos. |
| `DataIntegrityViolationException` | Violación de integridad | 409 | Genérico o específico de documento | Mostrar conflicto. |
| `Exception` | Error no manejado | 500 | `Error interno del servidor` | Mostrar mensaje genérico y registrar. |

### Errores de negocio relevantes para frontend

| Caso | Mensaje aproximado | HTTP |
|---|---|---|
| Fecha pasada | `No se puede crear o editar un evento en el pasado` | 400 |
| Horario incompleto | `Hora inicio y hora fin son obligatorias juntas` | 400 |
| Hora fin inválida | `La hora fin debe ser posterior a la hora inicio` | 400 |
| Disponibilidad inválida | `Para validar disponibilidad debe indicar horaInicio, horaFin y ubicación` | 400 |
| Evento solapado | `Ya existe un evento solapado en esa fecha y horario` | 400 |
| Nota de precio faltante | `La nota de precio es obligatoria cuando hay precio adicional` | 400 |
| Cliente inactivo/no encontrado | `Cliente no encontrado o inactivo` | 400 |
| Decoración inactiva | `La decoración seleccionada no está activa` | 400 |
| Tipo de evento inactivo/no encontrado | `Tipo de evento no encontrado o inactivo` | 400 |
| Transición inválida | `Transición inválida: el evento está en X, se requiere Y para pasar a Z` | 400 |
| Anticipo insuficiente | `No se puede confirmar el evento #... Anticipo mínimo requerido...` | 400 |
| Cancelación sin motivo | `El motivo de cancelación es obligatorio` | 400 |
| Revertir desde estado inválido | `Solo se puede revertir desde CANCELACION_PENDIENTE` | 400 |
| Sin estado anterior | `No hay estado anterior para restaurar` | 400 |
| Edición bloqueada | `Solo se pueden editar eventos en estado COTIZACION o PROGRAMADO` | 400 |
| Edición parcial inválida | `En estado PROGRAMADO solo se pueden editar ubicación, nota y motivo del precio adicional` | 400 |
| Extras en estado inválido | `Solo se pueden agregar extras a eventos en estado PROGRAMADO o CONFIRMADO` | 400 |
| Producto extra inactivo/no encontrado | `Producto no encontrado o inactivo` | 400 |
| Rentabilidad inexistente | `No se encontró rentabilidad para el evento {id}` | 400 |
| Fecha inicio mayor que fin en calendario | `La fecha inicio no puede ser mayor a la fecha fin` | 400 |

---

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| Clientes | `ClienteRepository` | `crearEvento`, `actualizarEvento` | Validar cliente existente y activo | Medio: evento no puede crearse sin cliente válido. |
| Decoración | `DecoracionRepository`, `DecoracionProductoRepository` | Crear, actualizar, programar | Validar decoración activa y obtener productos requeridos | Alto: impacta stock/precio base. |
| Producto/Inventario | `ProductoRepository`, `MovimientoInventarioRepository` | Extras, reserva, consumo | Validar productos, reservar y consumir stock | Alto: afecta disponibilidad. |
| Pagos | `PagoRepository`, `VistaPagosEventoRepository`, `PagoCancelacionHandler` | Confirmación, finalización, detalle, cancelación | Validar anticipo, resumen de pagos, devolución pendiente | Alto: reglas financieras. |
| Configuración | `ConfiguracionService` | Creación, disponibilidad, anticipo | Leer `DIAS_EXPIRACION_COTIZACION`, `PORCENTAJE_ANTICIPO_MINIMO`, `MINUTOS_GAP_ENTRE_EVENTOS` | Medio: configuración faltante puede afectar flujos. |
| Cronograma | `CronogramaService` | Confirmar cancelación | Cancelar actividades por evento | Medio: usa `@Lazy` para evitar ciclo. |
| Notificaciones | `NotificacionService` vía `EventoNotificacionHandler` | Confirmar, montaje, saldo, cancelación | Notificar admins | Medio: actualmente forma parte del flujo transaccional. |
| Auditoría | `AuditoriaService` | Programar, confirmar, extras | Registrar cambios relevantes | Bajo/medio: trazabilidad. |
| GastoEvento | `GastoEventoRepository` | Detalle interno | Construir resumen de gastos | Bajo: solo lectura en este contrato. |
| Vistas SQL | Repositories de vistas | Calendario, rentabilidad, resumen interno | Datos calculados | Medio: son solo lectura. |

Dependencia circular:

```text
Detectada: Parcialmente mitigada.
Detalle:
- EventoServiceImpl depende de CronogramaService con @Lazy.
- EventoServiceImpl no depende de PagoService; usa PagoCancelacionHandler para evitar ciclo con PagoServiceImpl.
Riesgo:
- Acoplamiento alto del módulo Evento porque orquesta muchos módulos.
Cómo se evita:
- Handlers dedicados: EventoStockHandler, EventoNotificacionHandler, EventoPagoValidator, PagoCancelacionHandler.
```

---

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Crear evento | Crea cotización y fecha de expiración | Evento / Configuración | Listado, calendario, dashboard |
| Actualizar evento | Cambia datos del evento; puede cambiar precio base si cambia decoración | Evento / Decoración | Detalle, listado, calendario |
| Agregar extras | Reserva stock adicional, incrementa precio adicional, audita | Evento / Inventario / Auditoría / Pagos | Detalle, inventario, resumen de pagos |
| Programar evento | Reserva stock y audita | Evento / Inventario / Auditoría | Detalle, listado, inventario |
| Confirmar evento | Valida anticipo, cambia estado, audita, notifica | Evento / Pagos / Auditoría / Notificaciones | Detalle, listado, notificaciones |
| Confirmar desde anticipo | Cambia estado automáticamente, audita, notifica | Evento / Pagos / Auditoría / Notificaciones | Detalle, listado, notificaciones |
| Iniciar montaje | Cambia estado y notifica | Evento / Notificaciones | Detalle, listado, notificaciones |
| Finalizar evento | Notifica saldo si aplica, consume stock, registra movimientos | Evento / Pagos / Inventario / Notificaciones | Detalle, inventario, pagos, notificaciones |
| Iniciar cancelación | Guarda motivo/fecha, estado anterior, notifica | Evento / Notificaciones | Detalle, listado, notificaciones |
| Revertir cancelación | Restaura estado anterior y limpia cancelación | Evento | Detalle, listado |
| Confirmar cancelación | Procesa pagos, libera stock, cancela cronograma, notifica | Evento / Pago / Inventario / Cronograma / Notificaciones | Detalle, listado, pagos, inventario, cronograma, notificaciones |
| Consultar detalle interno | Agrega pagos, gastos y rentabilidad | Evento / Pago / Gasto / Rentabilidad | No aplica |

---

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Crear evento | Usuario ADMIN | Sin permisos | `No tienes permisos para crear eventos.` |
| Editar completo | `COTIZACION` | Cualquier otro estado | `Solo se puede editar completamente una cotización.` |
| Editar parcial | `PROGRAMADO` | `CONFIRMADO`, `EN_MONTAJE`, `FINALIZADO`, `CANCELADO`, `CANCELACION_PENDIENTE` | `En programado solo puedes editar ubicación, nota y motivo del precio adicional.` |
| Programar | `COTIZACION` | Otros estados | `Solo se puede programar una cotización.` |
| Confirmar | `PROGRAMADO` | Otros estados | `Solo se puede confirmar un evento programado.` |
| Iniciar montaje | `CONFIRMADO` | Otros estados | `Solo se puede iniciar montaje de un evento confirmado.` |
| Finalizar | `EN_MONTAJE` | Otros estados | `Solo se puede finalizar un evento en montaje.` |
| Iniciar cancelación | `COTIZACION`, `PROGRAMADO`, `CONFIRMADO`, `EN_MONTAJE` | `FINALIZADO`, `CANCELADO`, `CANCELACION_PENDIENTE` | `Este evento no puede iniciar cancelación.` |
| Revertir cancelación | `CANCELACION_PENDIENTE` | Otros estados | `Solo se puede revertir una cancelación pendiente.` |
| Confirmar cancelación | `CANCELACION_PENDIENTE` | Otros estados | `Solo se puede confirmar una cancelación pendiente.` |
| Agregar extras | `PROGRAMADO`, `CONFIRMADO` | Otros estados | `Solo se pueden agregar extras en eventos programados o confirmados.` |
| Ver rentabilidad | Evento existente | Si no existe vista/rentabilidad | Mostrar error del backend. |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `clienteId` | `COTIZACION` | `PROGRAMADO` y demás estados | En `PROGRAMADO` el backend lo rechaza. |
| `decoracionId` | `COTIZACION` | `PROGRAMADO` y demás estados | Cambiar decoración actualiza precio base. |
| `fecha` | `COTIZACION` | `PROGRAMADO` y demás estados | Debe ser no pasada. |
| `horaInicio` | `COTIZACION` | `PROGRAMADO` y demás estados | Debe ir con `horaFin`. |
| `horaFin` | `COTIZACION` | `PROGRAMADO` y demás estados | Debe ir con `horaInicio`. |
| `ubicacion` | `COTIZACION`, `PROGRAMADO` | `CONFIRMADO` en adelante | En `PROGRAMADO` sí está permitido. |
| `nota` | `COTIZACION`, `PROGRAMADO` | `CONFIRMADO` en adelante | Nota interna. |
| `notaPrecio` | `COTIZACION`, `PROGRAMADO` | `CONFIRMADO` en adelante | Obligatoria si precio adicional > 0. |
| `precioAdicional` | Creación / extras | No se edita directamente en `EventoUpdateDTO` | Para incrementar, usar `/extras`. |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Crear evento | Listado, calendario, dashboard. |
| Actualizar evento | Detalle, listado, calendario si aplica. |
| Agregar extras | Detalle, pagos/resumen, inventario. |
| Programar | Detalle, listado, inventario, acciones disponibles. |
| Confirmar | Detalle, listado, notificaciones, acciones disponibles. |
| Iniciar montaje | Detalle, listado, notificaciones. |
| Finalizar | Detalle, listado, inventario, pagos, notificaciones. |
| Iniciar cancelación | Detalle, listado, notificaciones. |
| Revertir cancelación | Detalle, listado. |
| Confirmar cancelación | Detalle/listado, pagos, inventario, cronograma, notificaciones. |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `COTIZACION` | Propuesta inicial | Badge comercial; permitir edición completa/programar/cancelar. |
| `PROGRAMADO` | Evento agendado y stock reservado | Badge operativo; permitir edición parcial, confirmar, extras, cancelar. |
| `CONFIRMADO` | Anticipo mínimo cumplido | Badge confirmado; permitir montaje, extras, cancelar. |
| `EN_MONTAJE` | Montaje activo | Badge operativo; permitir finalizar/cancelar. |
| `FINALIZADO` | Evento completado | Badge terminal; bloquear acciones mutantes generales. |
| `CANCELACION_PENDIENTE` | Cancelación en gracia | Badge advertencia; permitir revertir o confirmar cancelación. |
| `CANCELADO` | Cancelado definitivo | Badge terminal; bloquear acciones mutantes generales. |

### 10.5 Reglas importantes

- No inventar acciones por estado. Usar `estadoEventoNombre` como base de habilitación.
- No editar directamente `precioTotal`; viene de BD como cálculo.
- No calcular anticipo mínimo en frontend como fuente final. El backend valida con configuración.
- No asumir que `horaInicio`, `horaFin` o `ubicacion` siempre existen.
- Si se envía una hora, enviar ambas: `horaInicio` y `horaFin`.
- Para cancelar, pedir siempre motivo.
- Después de confirmar cancelación, refrescar pagos porque pueden pasar a `CANCELADO` o `DEVOLUCION_PENDIENTE`.
- Para calendario, usar `/api/eventos/calendario`, no reconstruirlo desde listado si se necesitan datos enriquecidos.
- Para detalle administrativo con pagos/gastos/rentabilidad, usar `/api/eventos/{id}/detalle`.
- Para rentabilidad aislada, usar `/api/eventos/{id}/rentabilidad`.
- La disponibilidad actual valida ubicación como parámetro obligatorio, pero la query de solapamiento subida no filtra por ubicación. Tratarlo como comportamiento backend actual hasta que se corrija o confirme.
- Mostrar mensajes de error del backend cuando sean de negocio (`400`).
- Tratar vistas (`vista_eventos_periodo`, `vista_rentabilidad_evento`) como solo lectura.

### 10.6 Recomendaciones UX/UI concretas

| Escenario | Mejora recomendada |
|---|---|
| Card/detalle de evento | Mostrar estado del evento y estado de cobro juntos. Si el evento está en `CANCELACION_PENDIENTE`, destacar también si hay `tieneDevolucionPendiente`. |
| Acciones por estado | Renderizar solo las acciones válidas para el estado actual. Evitá botones visibles que siempre terminan en `400`. |
| Confirmación por anticipo | Después de confirmar un pago, refrescar automáticamente el badge del evento porque puede pasar a `CONFIRMADO` sin acción manual sobre el módulo evento. |
| Cancelación definitiva | En el modal final, advertir si existen pagos `PAGADO` porque pasarán a `DEVOLUCION_PENDIENTE`. |
| Detalle administrativo | En `/detalle`, mostrar `resumenPagos` y `resumenGastos` en tarjetas separadas para que el operador vea rápido cobrado, saldo y devolución pendiente. |
| Disponibilidad | Como la validación real hoy no distingue ubicación en query, evitar prometer en UI que dos eventos simultáneos en distinta ubicación serán aceptados automáticamente. |

---

## Resumen para frontend

1. Todos los endpoints de `EventoController` requieren rol `ADMIN`.
2. `EventoResponseDTO.estadoEventoNombre` es el campo principal para habilitar o bloquear acciones.
3. `COTIZACION` permite edición completa; `PROGRAMADO` permite edición parcial.
4. La programación reserva stock; la finalización consume stock; la cancelación confirmada libera stock.
5. La confirmación requiere anticipo mínimo y puede ocurrir manualmente o desde pagos.
6. La cancelación definitiva procesa pagos: pendientes se cancelan y pagados pasan a devolución pendiente.
7. Los endpoints mutantes suelen requerir refrescar módulos relacionados: pagos, inventario, cronograma y notificaciones.
8. `/api/eventos/{id}/detalle` es el endpoint más útil para detalle administrativo porque agrega pagos, gastos y rentabilidad.
9. `/api/eventos/calendario` usa una vista enriquecida para calendario/listados por rango.
10. `resumenPagos` en `/api/eventos/{id}/detalle` ya incluye `totalDevolucionPendiente` y `tieneDevolucionPendiente`.
11. No asumir campos no nulos en horarios, ubicación, notas o fechas de confirmación/cancelación.
