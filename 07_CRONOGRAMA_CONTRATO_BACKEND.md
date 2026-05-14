# Cronograma — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El módulo Cronograma administra las actividades operativas asociadas a un evento y la asignación de usuarios a esas actividades. Su objetivo es permitir que el administrador cree, edite, consulte, cancele y cambie el estado de actividades del cronograma, además de asignar personal con pago definido por actividad.

El módulo no crea eventos, no modifica pagos del cliente y no finaliza eventos. Depende del estado del evento para permitir o bloquear actividades.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Gestionar actividades operativas por evento y asignación de usuarios a actividades. |
| Controller principal | `CronogramaController` |
| Service principal | `CronogramaService` / `CronogramaServiceImpl` |
| Entidades principales | `Cronograma`, `CronogramaUsuario`, `CronogramaUsuarioId` |
| Tablas/vistas implicadas | `cronograma`, `cronograma_usuario`, `evento`, `usuario` |
| Módulos relacionados | Evento, Usuario/Auth |
| Tipo de módulo | Operativo / Asignación de personal |

Puntos relevantes para frontend:

- Todos los endpoints requieren rol `ADMIN`.
- Las actividades solo pueden crearse o editarse si el evento está en `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE`.
- Las actividades tienen estado propio: `PENDIENTE`, `EN_PROGRESO`, `COMPLETADO`, `CANCELADO`.
- El response ya incluye flags y listas útiles para UI: `editable`, `cancelable`, `accionesPermitidas`, `estadosSiguientesPermitidos`.
- La cancelación definitiva de un evento usa `cancelarPorEvento(eventoId)` desde el módulo Evento, por lo que el cronograma puede cambiar indirectamente cuando se confirma una cancelación de evento.

## 2. Controllers y endpoints completos

Base path:

```text
/api/cronogramas
```

Autorización:

```text
@PreAuthorize("hasRole('ADMIN')")
```

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/cronogramas` | Crear actividad de cronograma | `CronogramaRequestDTO` | `CronogramaResponseDTO` | `crear(dto)` | ADMIN | Valida estado del evento y rango de fechas. |
| GET | `/api/cronogramas/evento/{idEvento}` | Listar actividades de un evento | Path `idEvento` | `List<CronogramaResponseDTO>` | `obtenerPorEvento(idEvento)` | ADMIN | No valida existencia del evento directamente; consulta por FK. |
| GET | `/api/cronogramas/{id}` | Obtener actividad por ID | Path `id` | `CronogramaResponseDTO` | `obtenerPorId(id)` | ADMIN | 404 si no existe. |
| PUT | `/api/cronogramas/{id}` | Actualizar actividad | `CronogramaUpdateDTO` | `CronogramaResponseDTO` | `actualizar(id, dto)` | ADMIN | Actualización parcial; no tiene `@Valid` en controller. |
| PATCH | `/api/cronogramas/{id}/estado` | Cambiar estado de actividad | `CronogramaEstadoRequestDTO` | `CronogramaResponseDTO` | `cambiarEstado(id, estado)` | ADMIN | Valida estado válido y transición permitida. |
| PATCH | `/api/cronogramas/{id}/cancelar` | Cancelar actividad individual | Path `id` | `204 No Content` | `cancelar(id)` | ADMIN | Idempotente si ya está `CANCELADO`. |
| POST | `/api/cronogramas/{id}/usuarios` | Asignar usuario a actividad | `CronogramaUsuarioRequestDTO` | `CronogramaUsuarioResponseDTO` | `asignarUsuario(id, dto)` | ADMIN | Crea o actualiza asignación por clave compuesta. |
| GET | `/api/cronogramas/{id}/usuarios` | Listar usuarios asignados | Path `id` | `List<CronogramaUsuarioResponseDTO>` | `obtenerUsuarios(id)` | ADMIN | Valida que la actividad exista. |
| DELETE | `/api/cronogramas/{id}/usuarios/{usuarioId}` | Remover usuario asignado | Path `id`, `usuarioId` | `204 No Content` | `removerUsuario(id, usuarioId)` | ADMIN | Falla si el usuario no estaba asignado. |
| GET | `/api/cronogramas` | Listar cronogramas paginados | `Pageable` | `Page<CronogramaResponseDTO>` | `obtenerTodos(pageable)` | ADMIN | Default `size=50`, `sort=fechaInicio`. |

### POST `/api/cronogramas`

- Propósito: crear una actividad operativa asociada a un evento.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: `CronogramaRequestDTO`.
- Response: `CronogramaResponseDTO`.
- HTTP esperado: `201 Created`.
- Método del service: `CronogramaServiceImpl.crear(dto)`.
- Validaciones principales:
  - `idEvento`, `actividad`, `fechaInicio` y `fechaFin` son obligatorios.
  - El evento debe existir.
  - El evento debe estar en `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE`.
  - `fechaInicio` debe ser anterior a `fechaFin`.
  - Las fechas deben estar dentro del día del evento.
  - Si el evento tiene horario, la actividad debe quedar dentro de ese rango.
- Excepciones posibles:
  - `EventoException` si no existe el evento.
  - `CronogramaException` si el estado del evento no permite cronograma o si las fechas son inválidas.
  - `MethodArgumentNotValidException` si faltan campos obligatorios del DTO.
- Efectos secundarios:
  - Crea fila en `cronograma` con estado inicial `PENDIENTE`.
- Qué debe refrescar el frontend:
  - Cronograma del evento.
  - Detalle interno del evento si muestra `tieneCronograma` o actividades.

### GET `/api/cronogramas/evento/{idEvento}`

- Propósito: listar actividades asociadas a un evento.
- Path variables:
  - `idEvento`: ID del evento.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<CronogramaResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `CronogramaServiceImpl.obtenerPorEvento(idEvento)`.
- Validaciones principales: no valida explícitamente existencia del evento.
- Excepciones posibles: no específicas en el service para esta consulta.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - Solo la lista de actividades del evento.

### GET `/api/cronogramas/{id}`

- Propósito: obtener una actividad por ID.
- Path variables:
  - `id`: ID de la actividad.
- Query params: no aplica.
- Request body: no aplica.
- Response: `CronogramaResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `CronogramaServiceImpl.obtenerPorId(id)`.
- Validaciones principales:
  - La actividad debe existir.
- Excepciones posibles:
  - `CronogramaNotFoundException` si no existe.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - Detalle o drawer de actividad, si aplica.

### PUT `/api/cronogramas/{id}`

- Propósito: actualizar parcialmente una actividad.
- Path variables:
  - `id`: ID de la actividad.
- Query params: no aplica.
- Request body: `CronogramaUpdateDTO`.
- Response: `CronogramaResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `CronogramaServiceImpl.actualizar(id, dto)`.
- Validaciones principales:
  - La actividad debe existir.
  - La actividad no puede estar `COMPLETADO` ni `CANCELADO`.
  - El evento debe seguir en `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE`.
  - Si se modifican fechas, deben quedar dentro del día y horario del evento.
- Excepciones posibles:
  - `CronogramaNotFoundException`.
  - `CronogramaException` con códigos como `CRONOGRAMA_NO_EDITABLE`, `CRONOGRAMA_EVENTO_NO_PERMITE_EDICION`, `CRONOGRAMA_FECHA_INVALIDA` o `CRONOGRAMA_FUERA_RANGO_EVENTO`.
- Efectos secundarios:
  - Modifica datos de `cronograma`.
- Qué debe refrescar el frontend:
  - Actividad actual.
  - Lista de cronograma del evento.

Nota técnica: aunque `CronogramaUpdateDTO` tiene `@Size`, el endpoint no usa `@Valid` en `@RequestBody`, por lo que esas validaciones pueden no ejecutarse en controller.

### PATCH `/api/cronogramas/{id}/estado`

- Propósito: cambiar el estado de una actividad.
- Path variables:
  - `id`: ID de la actividad.
- Query params: no aplica.
- Request body: `CronogramaEstadoRequestDTO`.
- Response: `CronogramaResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `CronogramaServiceImpl.cambiarEstado(id, nuevoEstado)`.
- Validaciones principales:
  - `estado` es obligatorio.
  - El estado debe ser uno de `PENDIENTE`, `EN_PROGRESO`, `COMPLETADO`, `CANCELADO`.
  - La transición debe estar permitida.
- Excepciones posibles:
  - `CronogramaNotFoundException`.
  - `CronogramaException` por estado inválido o transición inválida.
  - `MethodArgumentNotValidException` si falta `estado`.
- Efectos secundarios:
  - Cambia `crono_estado`.
- Qué debe refrescar el frontend:
  - Actividad actual.
  - Lista de cronograma del evento.
  - Badges, acciones permitidas y estados siguientes.

### PATCH `/api/cronogramas/{id}/cancelar`

- Propósito: cancelar una actividad individual.
- Path variables:
  - `id`: ID de la actividad.
- Query params: no aplica.
- Request body: no aplica.
- Response: sin body.
- HTTP esperado: `204 No Content`.
- Método del service: `CronogramaServiceImpl.cancelar(id)`.
- Validaciones principales:
  - La actividad debe existir.
  - Si ya está `CANCELADO`, no hace nada.
  - Solo se permite cancelar desde `PENDIENTE` o `EN_PROGRESO`.
- Excepciones posibles:
  - `CronogramaNotFoundException`.
  - `CronogramaException` por transición inválida si está `COMPLETADO`.
- Efectos secundarios:
  - Cambia `crono_estado` a `CANCELADO`.
- Qué debe refrescar el frontend:
  - Cronograma del evento.
  - Actividad actual.

### POST `/api/cronogramas/{id}/usuarios`

- Propósito: asignar un usuario a una actividad del cronograma con un pago asociado.
- Path variables:
  - `id`: ID de la actividad.
- Query params: no aplica.
- Request body: `CronogramaUsuarioRequestDTO`.
- Response: `CronogramaUsuarioResponseDTO`.
- HTTP esperado: `201 Created`.
- Método del service: `CronogramaServiceImpl.asignarUsuario(id, dto)`.
- Validaciones principales:
  - La actividad debe existir.
  - La actividad no debe estar en estado final (`COMPLETADO`, `CANCELADO`).
  - El usuario debe existir.
  - El usuario debe estar activo.
  - `pagoTipo` debe ser `FIJO` o `POR_HORA`.
- Excepciones posibles:
  - `CronogramaNotFoundException`.
  - `CronogramaException` si el usuario no existe, está inactivo, el tipo de pago es inválido o la actividad está finalizada/cancelada.
  - `MethodArgumentNotValidException` si falta `usuarioId` o `pagoTipo` no cumple el patrón.
- Efectos secundarios:
  - Crea o actualiza fila en `cronograma_usuario`.
- Qué debe refrescar el frontend:
  - Usuarios asignados de la actividad.
  - Detalle del cronograma si muestra personal o costo asignado.
  - Rentabilidad del evento si se muestra costo de mano de obra calculado desde asignaciones.

### GET `/api/cronogramas/{id}/usuarios`

- Propósito: listar usuarios asignados a una actividad.
- Path variables:
  - `id`: ID de la actividad.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<CronogramaUsuarioResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `CronogramaServiceImpl.obtenerUsuarios(id)`.
- Validaciones principales:
  - La actividad debe existir.
- Excepciones posibles:
  - `CronogramaNotFoundException`.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - Lista de usuarios asignados.

### DELETE `/api/cronogramas/{id}/usuarios/{usuarioId}`

- Propósito: remover un usuario asignado a una actividad.
- Path variables:
  - `id`: ID de la actividad.
  - `usuarioId`: ID del usuario.
- Query params: no aplica.
- Request body: no aplica.
- Response: sin body.
- HTTP esperado: `204 No Content`.
- Método del service: `CronogramaServiceImpl.removerUsuario(id, usuarioId)`.
- Validaciones principales:
  - La asignación debe existir.
- Excepciones posibles:
  - `CronogramaException` si el usuario no está asignado a esta actividad.
- Efectos secundarios:
  - Elimina fila en `cronograma_usuario`.
- Qué debe refrescar el frontend:
  - Usuarios asignados de la actividad.
  - Rentabilidad del evento si se muestra costo de mano de obra.

### GET `/api/cronogramas`

- Propósito: listar actividades de cronograma paginadas.
- Path variables: no aplica.
- Query params:
  - `page`, `size`, `sort` vía `Pageable`.
- Request body: no aplica.
- Response: `Page<CronogramaResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `CronogramaServiceImpl.obtenerTodos(pageable)`.
- Validaciones principales: no aplica.
- Excepciones posibles: errores genéricos de paginación si parámetros inválidos.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - Listado general de cronograma.

## 3. DTOs request/response

### 3.1 DTOs Request

#### CronogramaRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `idEvento` | `Long` | `idEvento` | Sí | No | `@NotNull` | Evento al que pertenece la actividad. |
| `actividad` | `String` | `actividad` | Sí | No | `@NotBlank` | Nombre/descripción corta de la actividad. |
| `fechaInicio` | `LocalDateTime` | `fechaInicio` | Sí | No | `@NotNull`; validación manual de rango | Debe estar dentro del día y horario del evento. |
| `fechaFin` | `LocalDateTime` | `fechaFin` | Sí | No | `@NotNull`; validación manual de rango | Debe ser posterior a `fechaInicio`. |

#### CronogramaUpdateDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `actividad` | `String` | `actividad` | No | Sí | `@Size(max=255)` | Solo campos no nulos se actualizan. |
| `fechaInicio` | `LocalDateTime` | `fechaInicio` | No | Sí | Validación manual de rango | Si cambia fecha/hora, debe seguir dentro del evento. |
| `fechaFin` | `LocalDateTime` | `fechaFin` | No | Sí | Validación manual de rango | Debe ser posterior a `fechaInicio` final. |
| `cronoObservacion` | `String` | `cronoObservacion` | No | Sí | `@Size(max=255)` | Observación administrativa. |

Nota: las anotaciones `@Size` pueden no ejecutarse porque el controller no usa `@Valid` en el endpoint `PUT /api/cronogramas/{id}`.

#### CronogramaEstadoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `estado` | `String` | `estado` | Sí | No | `@NotBlank`; validación manual `CronogramaEstado.isValid` | Debe ser `PENDIENTE`, `EN_PROGRESO`, `COMPLETADO` o `CANCELADO`. |

#### CronogramaUsuarioRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `usuarioId` | `Long` | `usuarioId` | Sí | No | `@NotNull` | Usuario a asignar a la actividad. |
| `pagoAsignado` | `BigDecimal` | `pagoAsignado` | No | Sí | No tiene validación positiva en DTO | Default `0`. El frontend debería evitar negativos aunque el DTO no lo valida. |
| `pagoTipo` | `String` | `pagoTipo` | No | Sí | `@Pattern("^(FIJO|POR_HORA)$")`; validación manual | Default `FIJO`. |

### 3.2 DTOs Response

#### CronogramaResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `Cronograma.id` | Identificador de actividad. |
| `idEvento` | `Long` | `idEvento` | Sí | `Cronograma.evento.id` | Puede quedar null si la relación no está cargada/no existe. |
| `actividad` | `String` | `actividad` | No | `cronograma.actividad` | Texto de actividad. |
| `fechaInicio` | `LocalDateTime` | `fechaInicio` | No | `cronograma.fechaInicio` | Inicio de actividad. |
| `fechaFin` | `LocalDateTime` | `fechaFin` | No | `cronograma.fechaFin` | Fin de actividad. |
| `cronoEstado` | `String` | `cronoEstado` | No | `cronograma.cronoEstado` | Badge principal. |
| `cronoObservacion` | `String` | `cronoObservacion` | Sí | `cronograma.cronoObservacion` | Nota administrativa. |
| `clienteNombre` | `String` | `clienteNombre` | Sí | `evento.cliente.nombre` | Dato denormalizado para listas. |
| `estadoEventoNombre` | `String` | `estadoEventoNombre` | Sí | `evento.estadoEvento.nombre` | Útil para bloquear acciones. |
| `editable` | `Boolean` | `editable` | No | `CronogramaPolicy.isEditable` | Usar para habilitar edición. |
| `cancelable` | `Boolean` | `cancelable` | No | `CronogramaPolicy.isCancelable` | Usar para habilitar cancelar. |
| `accionesPermitidas` | `List<String>` | `accionesPermitidas` | No | `CronogramaPolicy.getAccionesPermitidas` | Usar para botones de acción. |
| `estadosSiguientesPermitidos` | `List<String>` | `estadosSiguientesPermitidos` | No | `CronogramaPolicy.getEstadosSiguientesPermitidos` | Usar para selects/transiciones. |

#### CronogramaUsuarioResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `cronogramaId` | `Long` | `cronogramaId` | No | `CronogramaUsuarioId.idCronograma` | ID de actividad. |
| `usuarioId` | `Long` | `usuarioId` | No | `CronogramaUsuarioId.idUsuario` | ID de usuario. |
| `usuarioNombre` | `String` | `usuarioNombre` | No | `Usuario.nombre` | Nombre para UI. |
| `pagoAsignado` | `BigDecimal` | `pagoAsignado` | No | `cronograma_usuario.pago_asignado` | Costo mano de obra. |
| `pagoTipo` | `String` | `pagoTipo` | No | `cronograma_usuario.pago_tipo` | `FIJO` o `POR_HORA`. |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Cronograma` | `cronograma` | Tabla operativa | Actividades de un evento. | Estado inicial `PENDIENTE`. |
| `CronogramaUsuario` | `cronograma_usuario` | Relación | Asignación usuario-actividad con pago. | Clave compuesta `id_cronograma + id_usuario`. |
| `CronogramaUsuarioId` | `cronograma_usuario` | Embeddable | Clave compuesta. | Usado para asignar/remover usuarios. |
| `Evento` | `evento` | Tabla externa | Evento al que pertenece el cronograma. | Se consulta para validar estado y rango temporal. |
| `Usuario` | `usuario` | Tabla externa | Usuario asignable a actividades. | Debe existir y estar activo. |

No hay vistas SQL propias del módulo Cronograma.

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Crear actividad | `crear(dto)` | Evento existe, estado del evento, fechas dentro del evento | Crea `cronograma` | `CronogramaResponseDTO` |
| Listar por evento | `obtenerPorEvento(idEvento)` | No valida evento explícitamente | No modifica | `List<CronogramaResponseDTO>` |
| Listar paginado | `obtenerTodos(pageable)` | No aplica | No modifica | `Page<CronogramaResponseDTO>` |
| Obtener por ID | `obtenerPorId(id)` | Actividad existe | No modifica | `CronogramaResponseDTO` |
| Actualizar actividad | `actualizar(id, dto)` | Actividad editable, evento permitido, fechas válidas | Actualiza campos no nulos | `CronogramaResponseDTO` |
| Cambiar estado | `cambiarEstado(id, estado)` | Estado válido y transición permitida | Cambia `cronoEstado` | `CronogramaResponseDTO` |
| Cancelar actividad | `cancelar(id)` | Transición a `CANCELADO` permitida | Cambia `cronoEstado` | `void` |
| Cancelar por evento | `cancelarPorEvento(eventoId)` | No valida evento en service | Cancela actividades no finales | `void` |
| Asignar usuario | `asignarUsuario(cronogramaId, dto)` | Actividad no final, usuario existe/activo, pagoTipo válido | Crea/actualiza `cronograma_usuario` | `CronogramaUsuarioResponseDTO` |
| Listar usuarios | `obtenerUsuarios(cronogramaId)` | Actividad existe | No modifica | `List<CronogramaUsuarioResponseDTO>` |
| Remover usuario | `removerUsuario(cronogramaId, usuarioId)` | Asignación existe | Elimina relación | `void` |

### Flujo: crear actividad

1. Busca el evento por `idEvento`.
2. Valida que el evento permita cronograma.
3. Valida que `fechaInicio` sea anterior a `fechaFin`.
4. Valida que la actividad ocurra el mismo día del evento.
5. Si el evento tiene horario, valida que la actividad quede dentro de ese horario.
6. Mapea el DTO a `Cronograma`.
7. Guarda con estado inicial `PENDIENTE`.
8. Retorna `CronogramaResponseDTO` con flags de UI calculados por `CronogramaPolicy`.

### Flujo: actualizar actividad

1. Busca la actividad.
2. Valida que no esté `COMPLETADO` ni `CANCELADO`.
3. Valida que el evento siga permitiendo cronograma.
4. Actualiza solo los campos no nulos del DTO.
5. Valida fechas resultantes.
6. Guarda y retorna DTO actualizado.

### Flujo: cambiar estado

1. Valida que el estado enviado exista en `CronogramaEstado`.
2. Busca la actividad.
3. Valida transición mediante `CronogramaPolicy`.
4. Si el estado es igual al actual, retorna sin modificar.
5. Si es distinto, actualiza `cronoEstado` y guarda.

### Flujo: asignar usuario

1. Busca la actividad.
2. Bloquea asignación si la actividad está `COMPLETADO` o `CANCELADO`.
3. Busca el usuario.
4. Valida que el usuario esté activo.
5. Valida que `pagoTipo` sea `FIJO` o `POR_HORA`.
6. Si la asignación ya existe, la actualiza; si no existe, la crea.
7. Retorna datos de usuario asignado.

## 6. Estados y transiciones

### Estados existentes

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `PENDIENTE` | Actividad creada, no iniciada | Estado inicial de `Cronograma` | Sí | Permite iniciar o cancelar. |
| `EN_PROGRESO` | Actividad iniciada | Transición desde `PENDIENTE` | Sí | Permite completar o cancelar. |
| `COMPLETADO` | Actividad terminada | Estado final | Sí | No editable, no cancelable. |
| `CANCELADO` | Actividad cancelada | Estado final | Sí | No editable, no cancelable. |

### Transiciones permitidas

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| `PENDIENTE` | `EN_PROGRESO` | `PATCH /{id}/estado` | Transición permitida | Actualiza estado. |
| `PENDIENTE` | `CANCELADO` | `PATCH /{id}/estado` o `PATCH /{id}/cancelar` | Transición permitida | Actualiza estado. |
| `EN_PROGRESO` | `COMPLETADO` | `PATCH /{id}/estado` | Transición permitida | Actualiza estado. |
| `EN_PROGRESO` | `CANCELADO` | `PATCH /{id}/estado` o `PATCH /{id}/cancelar` | Transición permitida | Actualiza estado. |
| `COMPLETADO` | Cualquiera | No permitido | Estado final | No modifica. |
| `CANCELADO` | Cualquiera | No permitido | Estado final | No modifica. |

Estados de evento permitidos para crear/editar actividades:

```text
PROGRAMADO
CONFIRMADO
EN_MONTAJE
```

Nota de consistencia: `CronogramaEstado.ESTADOS_EVENTO_PERMITIDOS` comenta `CONFIRMADO` y `EN_MONTAJE`, pero `CronogramaPolicy` es la regla usada realmente por el service e incluye `PROGRAMADO`, `CONFIRMADO` y `EN_MONTAJE`.

## 7. Excepciones y respuestas de error

Formato general de error del backend:

```json
{
  "codigo": 400,
  "mensaje": "...",
  "errorCode": "...",
  "timestamp": "..."
}
```

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `CronogramaException` | Regla de negocio incumplida | 400 | Mensaje específico del service/policy | Mostrar mensaje del backend. |
| `CronogramaNotFoundException` | Actividad no existe | 404 | `Cronograma no encontrado con id: {id}` | Mostrar no encontrado y volver al listado. |
| `EventoException` | Evento asociado no existe en creación | 400 | `Evento no encontrado con ID: {id}` | Mostrar error y solicitar recargar eventos. |
| `MethodArgumentNotValidException` | Faltan campos obligatorios o patrón inválido | 400 | Primer error de validación por campo | Marcar campo en formulario. |
| `AccessDeniedException` | Usuario sin rol permitido | 403 | Mensaje de acceso denegado | Redirigir o bloquear acción. |
| `Exception` | Error inesperado | 500 | `Error interno del servidor` | Mostrar mensaje genérico y registrar incidencia. |

Códigos de negocio propios de cronograma:

| Código | Cuándo se usa |
|---|---|
| `CRONOGRAMA_TRANSICION_INVALIDA` | Transición de estado no permitida. |
| `CRONOGRAMA_NO_EDITABLE` | Intento de editar actividad completada/cancelada. |
| `CRONOGRAMA_FECHA_INVALIDA` | `fechaInicio` no es anterior a `fechaFin`. |
| `CRONOGRAMA_FUERA_RANGO_EVENTO` | Actividad fuera del día u horario del evento. |
| `CRONOGRAMA_EVENTO_NO_PERMITE_EDICION` | Evento en estado que no permite cronograma. |

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| Evento | `EventoRepository` | `crear`, `validarEventoPermiteCronograma`, `validarFechasActividad` | Asociar actividad a evento y validar estado/fecha/horario | Medio: las reglas dependen del estado del evento. |
| Usuario/Auth | `UsuarioRepository` | `asignarUsuario` | Buscar usuario y validar que esté activo | Bajo/Medio: depende del modelo de usuario. |
| CronogramaPolicy | Policy interna | Crear, actualizar, cambiar estado, cancelar, mapper | Centralizar reglas de estado, edición y flags UI | Bajo. |
| CronogramaMapper | Mapper interno | Respuestas y creación | Aplanar entidad a DTO y calcular flags UI | Bajo. |
| EventoServiceImpl | Llamada externa inversa | `EventoServiceImpl.confirmarCancelacion()` llama `cancelarPorEvento` | Cancelación en cascada por evento | Medio: dependencia funcional evento → cronograma. |

Dependencia circular:

```text
- Detectada: No directa en este módulo.
- Detalle: CronogramaServiceImpl depende de EventoRepository, no de EventoService. EventoServiceImpl sí depende de CronogramaService para cancelar cronograma por evento.
- Riesgo: Bajo/Medio. Hay acoplamiento funcional por cancelación, pero no ciclo service-service directo desde Cronograma hacia EventoService.
- Cómo se evita: Cronograma usa EventoRepository para leer estado/fechas; Evento usa CronogramaService solo para cancelar actividades en cascada.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Crear actividad | Inserta fila en `cronograma` | Cronograma / Evento | Cronograma del evento; detalle del evento si muestra `tieneCronograma`. |
| Actualizar actividad | Modifica datos y fechas | Cronograma | Actividad y listado del evento. |
| Cambiar estado | Actualiza `crono_estado` | Cronograma | Actividad, badges y acciones disponibles. |
| Cancelar actividad | Cambia estado a `CANCELADO` | Cronograma | Actividad y listado del evento. |
| Cancelar evento | `EventoServiceImpl` llama `cancelarPorEvento` | Evento / Cronograma | Cronograma del evento, detalle de evento, estado del evento. |
| Asignar usuario | Crea o actualiza `cronograma_usuario` | Cronograma / Usuario / Rentabilidad | Usuarios asignados; rentabilidad si se muestra mano de obra. |
| Remover usuario | Elimina asignación | Cronograma / Rentabilidad | Usuarios asignados; rentabilidad si se muestra mano de obra. |

No se detectan notificaciones, auditoría ni documentos generados directamente desde este módulo.

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Crear actividad | Evento en `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE` | Evento en `COTIZACION`, `FINALIZADO`, `CANCELADO`, `CANCELACION_PENDIENTE` | `El estado del evento no permite crear cronograma.` |
| Editar actividad | `editable = true` | `editable = false`, normalmente `COMPLETADO` o `CANCELADO` | `No se puede editar una actividad finalizada o cancelada.` |
| Iniciar actividad | `accionesPermitidas` contiene `INICIAR` | No está en acciones permitidas | `La actividad no puede iniciarse desde su estado actual.` |
| Completar actividad | `accionesPermitidas` contiene `COMPLETAR` | No está en acciones permitidas | `La actividad no puede completarse desde su estado actual.` |
| Cancelar actividad | `cancelable = true` o `accionesPermitidas` contiene `CANCELAR` | `COMPLETADO` o `CANCELADO` | `La actividad no se puede cancelar.` |
| Asignar usuario | Actividad no está en estado final | Actividad `COMPLETADO` o `CANCELADO` | `No se puede asignar personal a una actividad finalizada o cancelada.` |
| Remover usuario | La asignación existe | La asignación no existe | `El usuario no está asignado a esta actividad.` |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `actividad` | Actividad editable | `COMPLETADO`, `CANCELADO` | Máximo 255 según DTO, pero endpoint no usa `@Valid`. |
| `fechaInicio` | Actividad editable | `COMPLETADO`, `CANCELADO` | Debe estar dentro del día/horario del evento. |
| `fechaFin` | Actividad editable | `COMPLETADO`, `CANCELADO` | Debe ser posterior a `fechaInicio`. |
| `cronoObservacion` | Actividad editable | `COMPLETADO`, `CANCELADO` | Máximo 255 según DTO. |
| `pagoAsignado` | Usuario asignable | Actividad final | No tiene validación positiva en DTO; el frontend debería impedir negativos. |
| `pagoTipo` | Usuario asignable | Actividad final | Solo `FIJO` o `POR_HORA`. |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Crear actividad | Cronograma del evento; detalle/resumen del evento si muestra cronograma. |
| Actualizar actividad | Actividad actual y cronograma del evento. |
| Cambiar estado | Actividad actual, cronograma del evento, botones/acciones. |
| Cancelar actividad | Cronograma del evento. |
| Asignar usuario | Usuarios asignados y rentabilidad si está visible. |
| Remover usuario | Usuarios asignados y rentabilidad si está visible. |
| Cancelar evento | Cronograma del evento y detalle del evento. |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `PENDIENTE` | Actividad programada pero no iniciada | Badge inicial; mostrar acciones iniciar/cancelar. |
| `EN_PROGRESO` | Actividad activa | Badge operativo; mostrar completar/cancelar. |
| `COMPLETADO` | Actividad terminada | Estado final; bloquear edición/cancelación. |
| `CANCELADO` | Actividad cancelada | Estado final; bloquear edición/cancelación. |

### 10.5 Reglas importantes

- No crear actividades si el evento no está en `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE`.
- No permitir actividades fuera del día del evento.
- Si el evento tiene hora inicio/fin, no permitir actividades fuera de ese rango.
- Usar `editable`, `cancelable`, `accionesPermitidas` y `estadosSiguientesPermitidos` del response para construir acciones UI.
- No asumir que un estado puede saltar libremente. Las transiciones válidas son estrictas.
- No permitir asignar usuarios a actividades `COMPLETADO` o `CANCELADO`.
- No asumir que `pagoAsignado` siempre es positivo; el backend no lo valida explícitamente.
- Para `PUT /api/cronogramas/{id}`, validar en frontend longitud de `actividad` y `cronoObservacion`, porque el controller no usa `@Valid`.
- Después de asignar/remover usuarios, refrescar rentabilidad si la pantalla la muestra.

## Resumen para frontend

- Base path: `/api/cronogramas`.
- Todos los endpoints requieren rol `ADMIN`.
- El cronograma se asocia a eventos, pero no crea ni cambia estados de evento directamente.
- Solo se permiten actividades para eventos en `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE`.
- Estados de actividad: `PENDIENTE`, `EN_PROGRESO`, `COMPLETADO`, `CANCELADO`.
- Usar `editable`, `cancelable`, `accionesPermitidas` y `estadosSiguientesPermitidos` para mostrar botones.
- Fechas de actividad deben estar dentro del día del evento y dentro del horario si el evento lo tiene.
- La asignación de usuarios usa `cronograma_usuario` y puede afectar rentabilidad por mano de obra.
- La cancelación de evento cancela actividades no finales por cascada funcional desde `EventoServiceImpl`.
- Hay un gap menor: el endpoint de actualización no usa `@Valid`, por lo que conviene validar longitudes en frontend.
