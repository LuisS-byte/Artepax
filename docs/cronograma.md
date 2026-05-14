# cronograma — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Gestiona las actividades operativas (cronograma) asociadas a eventos: creación, edición, cambio de estado, cancelación y asignación de usuarios con pago asociado.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Actividades del cronograma y asignación de personal |
| Controller principal | `CronogramaController` (`/api/cronogramas`) |
| Service principal | `CronogramaServiceImpl` |
| Entidades principales | `Cronograma`, `CronogramaUsuario` (tablas `cronograma`, `cronograma_usuario`) |
| Tablas/vistas implicadas | `cronograma`, `cronograma_usuario`, `evento`, `usuario` |
| Módulos relacionados | `evento`, `authentication/user` |
| Tipo de módulo | Operativo |

## 2. Controllers y endpoints completos

Todos requieren `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Service | Auth | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/cronogramas` | Crear actividad | `CronogramaRequestDTO` | `CronogramaResponseDTO` (201) | `crear` | ADMIN | Valida evento y fechas |
| GET | `/api/cronogramas/evento/{idEvento}` | Actividades por evento | — | `List<CronogramaResponseDTO>` | `obtenerPorEvento` | ADMIN | |
| GET | `/api/cronogramas/{id}` | Obtener una | — | `CronogramaResponseDTO` | `obtenerPorId` | ADMIN | Incluye flags editable/cancelable |
| PUT | `/api/cronogramas/{id}` | Actualizar | `CronogramaUpdateDTO` | `CronogramaResponseDTO` | `actualizar` | ADMIN | Valida fechas contra evento |
| PATCH | `/api/cronogramas/{id}/estado` | Cambiar estado | `CronogramaEstadoRequestDTO` | `CronogramaResponseDTO` | `cambiarEstado` | ADMIN | Transiciones controladas |
| PATCH | `/api/cronogramas/{id}/cancelar` | Cancelar actividad | — | `204 No Content` | `cancelar` | ADMIN | |
| POST | `/api/cronogramas/{id}/usuarios` | Asignar usuario | `CronogramaUsuarioRequestDTO` | `CronogramaUsuarioResponseDTO` (201) | `asignarUsuario` | ADMIN | Con pago asignado |
| GET | `/api/cronogramas/{id}/usuarios` | Usuarios asignados | — | `List<CronogramaUsuarioResponseDTO>` | `obtenerUsuarios` | ADMIN | |
| DELETE | `/api/cronogramas/{id}/usuarios/{usuarioId}` | Remover usuario | — | `204 No Content` | `removerUsuario` | ADMIN | |
| GET | `/api/cronogramas` | Todos paginado | Pageable | `Page<CronogramaResponseDTO>` | `obtenerTodos` | ADMIN | Default: size=50, sort=fechaInicio |

### POST /api/cronogramas

- **Propósito:** Crear una actividad de cronograma para un evento.
- **Request body:** `CronogramaRequestDTO`
- **Response:** `CronogramaResponseDTO` con HTTP 201
- **Validaciones principales:**
  - `idEvento` obligatorio, evento debe existir
  - `actividad` obligatoria (`@NotBlank`)
  - `fechaInicio`, `fechaFin` obligatorias (`@NotNull`)
  - Evento debe estar en PROGRAMADO, CONFIRMADO o EN_MONTAJE
  - `fechaInicio < fechaFin`
  - Fechas deben estar dentro del día y horario del evento
- **Excepciones posibles:**
  - `EventoException` (400): "Evento no encontrado con ID: X"
  - `CronogramaException` (400): estado de evento no permitido
  - `CronogramaException` (400): fechas fuera del rango del evento
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** cronograma del evento

### GET /api/cronogramas/evento/{idEvento}

- **Propósito:** Listar todas las actividades de un evento.
- **Response:** `List<CronogramaResponseDTO>`
- **Validaciones principales:** ninguna
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/cronogramas/{id}

- **Propósito:** Obtener una actividad con flags de acciones permitidas.
- **Response:** `CronogramaResponseDTO` con `editable`, `cancelable`, `accionesPermitidas`, `estadosSiguientesPermitidos`
- **Excepciones posibles:** `CronogramaNotFoundException` (404)
- **Qué debe refrescar el frontend:** NO APLICA

### PUT /api/cronogramas/{id}

- **Propósito:** Actualizar datos de una actividad (nombre, fechas, observación).
- **Request body:** `CronogramaUpdateDTO` (todos los campos opcionales)
- **Validaciones principales:**
  - Actividad no puede estar en COMPLETADO o CANCELADO
  - Evento debe seguir en estado permitido (PROGRAMADO/CONFIRMADO/EN_MONTAJE)
  - Si cambian fechas, se revalidan contra el evento
- **Excepciones posibles:**
  - `CronogramaNotFoundException` (404)
  - `CronogramaException` (400): "No se puede editar una actividad en estado: X"
  - `CronogramaException` (400): "Solo se pueden crear o editar actividades en eventos PROGRAMADO, CONFIRMADO o EN_MONTAJE"
- **Qué debe refrescar el frontend:** detalle actividad, cronograma del evento

### PATCH /api/cronogramas/{id}/estado

- **Propósito:** Cambiar el estado de una actividad.
- **Request body:** `CronogramaEstadoRequestDTO` (`estado` obligatorio)
- **Validaciones principales:**
  - Estado debe ser válido: PENDIENTE, EN_PROGRESO, COMPLETADO, CANCELADO
  - Transiciones permitidas:
    - PENDIENTE → EN_PROGRESO o CANCELADO
    - EN_PROGRESO → COMPLETADO o CANCELADO
    - COMPLETADO y CANCELADO → ninguna
- **Excepciones posibles:**
  - `CronogramaException` (400): "Estado inválido: X"
  - `CronogramaException` (400): "Transición de cronograma inválida: X -> Y"
- **Qué debe refrescar el frontend:** detalle actividad, cronograma del evento

### PATCH /api/cronogramas/{id}/cancelar

- **Propósito:** Cancelar una actividad (estado → CANCELADO).
- **Response:** `204 No Content`
- **Validaciones principales:**
  - Si ya está CANCELADO, no hace nada (idempotente)
  - Transición válida desde PENDIENTE o EN_PROGRESO
- **Excepciones posibles:** `CronogramaException` (400): transición inválida desde COMPLETADO
- **Qué debe refrescar el frontend:** cronograma del evento

### POST /api/cronogramas/{id}/usuarios

- **Propósito:** Asignar un usuario a una actividad con pago asociado.
- **Request body:** `CronogramaUsuarioRequestDTO`
- **Response:** `CronogramaUsuarioResponseDTO` con HTTP 201
- **Validaciones principales:**
  - Actividad no puede estar en estado final (COMPLETADO/CANCELADO)
  - `usuarioId` debe existir y estar activo
  - `pagoTipo` debe ser FIJO o POR_HORA (`@Pattern`)
  - Si ya existe la asignación, se actualiza (upsert)
- **Excepciones posibles:**
  - `CronogramaException` (400): "No se puede asignar usuarios a una actividad X"
  - `CronogramaException` (400): "Usuario no encontrado: X"
  - `CronogramaException` (400): "El usuario está inactivo"
  - `CronogramaException` (400): "Tipo de pago inválido: X"
- **Qué debe refrescar el frontend:** usuarios asignados a la actividad

### GET /api/cronogramas/{id}/usuarios

- **Propósito:** Listar usuarios asignados a una actividad.
- **Response:** `List<CronogramaUsuarioResponseDTO>`
- **Excepciones posibles:** `CronogramaNotFoundException` (404)
- **Qué debe refrescar el frontend:** NO APLICA

### DELETE /api/cronogramas/{id}/usuarios/{usuarioId}

- **Propósito:** Remover un usuario de una actividad.
- **Response:** `204 No Content`
- **Excepciones posibles:** `CronogramaException` (400): "El usuario no está asignado a esta actividad"
- **Qué debe refrescar el frontend:** usuarios de la actividad

## 3. DTOs request/response

### 3.1 DTOs Request

#### CronogramaRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| idEvento | Long | number | Sí (`@NotNull`) | No | Evento debe existir y estar en PROGRAMADO/CONFIRMADO/EN_MONTAJE | |
| actividad | String | string | Sí (`@NotBlank`) | No | | |
| fechaInicio | LocalDateTime | ISO datetime | Sí (`@NotNull`) | No | Debe estar dentro del día/horario del evento | |
| fechaFin | LocalDateTime | ISO datetime | Sí (`@NotNull`) | No | Debe ser > fechaInicio | |

#### CronogramaUpdateDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Notas frontend |
|---|---|---|---|---|---|
| actividad | String | string | No | Sí | |
| fechaInicio | LocalDateTime | ISO datetime | No | Sí | |
| fechaFin | LocalDateTime | ISO datetime | No | Sí | |
| cronoObservacion | String | string | No | Sí | |

#### CronogramaEstadoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| estado | String | string | Sí (`@NotBlank`) | No | PENDIENTE, EN_PROGRESO, COMPLETADO o CANCELADO | |

#### CronogramaUsuarioRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| usuarioId | Long | number | Sí (`@NotNull`) | No | Usuario debe existir y estar activo | |
| pagoAsignado | BigDecimal | number | No | No (default: 0) | | Monto del pago al usuario |
| pagoTipo | String | string | No | No (default: "FIJO") | `@Pattern("FIJO|POR_HORA")` | |

### 3.2 DTOs Response

#### CronogramaResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| id | Long | number | No | entidad | |
| idEvento | Long | number | No | entidad.evento | |
| actividad | String | string | No | entidad | |
| fechaInicio | LocalDateTime | ISO datetime | No | entidad | |
| fechaFin | LocalDateTime | ISO datetime | No | entidad | |
| cronoEstado | String | string | No | entidad | PENDIENTE / EN_PROGRESO / COMPLETADO / CANCELADO |
| cronoObservacion | String | string | Sí | entidad | |
| clienteNombre | String | string | No | entidad.evento.cliente | Denormalizado |
| estadoEventoNombre | String | string | No | entidad.evento.estadoEvento | Denormalizado |
| editable | Boolean | boolean | No | policy | TRUE = se puede editar. FALSE si COMPLETADO/CANCELADO |
| cancelable | Boolean | boolean | No | policy | TRUE = se puede cancelar. FALSE si COMPLETADO/CANCELADO |
| accionesPermitidas | List\<String\> | array | No | policy | ["INICIAR","CANCELAR"] o ["COMPLETAR","CANCELAR"] según estado |
| estadosSiguientesPermitidos | List\<String\> | array | No | policy | Estados a los que puede transicionar |

#### CronogramaUsuarioResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| cronogramaId | Long | number | No | entidad | |
| usuarioId | Long | number | No | entidad | |
| usuarioNombre | String | string | No | entidad.usuario | Denormalizado |
| pagoAsignado | BigDecimal | number | No | entidad | |
| pagoTipo | String | string | No | entidad | FIJO / POR_HORA |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Cronograma` | `cronograma` | Tabla operativa | Actividades del evento | `crono_estado`: CHECK PENDIENTE/EN_PROGRESO/COMPLETADO/CANCELADO. `fecha_fin > fecha_inicio` (CHECK) |
| `CronogramaUsuario` | `cronograma_usuario` | Relación (puente) | Asignación de usuarios con pago | PK compuesta: `(id_cronograma, id_usuario)`. `pago_tipo`: FIJO/POR_HORA. `pago_asignado >= 0` |

**Constraint DB:**
- `chk_cronograma_estado`: solo acepta PENDIENTE, EN_PROGRESO, COMPLETADO, CANCELADO
- `chk_cronograma_fechas`: `fecha_fin > fecha_inicio`

## 5. Services y lógica de negocio

| Flujo | Método | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Crear | `crear` | Evento en estado permitido, fechas dentro del evento, fechaInicio < fechaFin | Crea Cronograma (PENDIENTE) | CronogramaResponseDTO |
| Listar por evento | `obtenerPorEvento` | — | — | List |
| Actualizar | `actualizar` | Actividad editable, evento permite cronograma, fechas válidas | Actualiza campos | CronogramaResponseDTO |
| Cambiar estado | `cambiarEstado` | Estado válido, transición permitida | Cambia cronoEstado | CronogramaResponseDTO |
| Cancelar | `cancelar` | Transición válida, idempotente | `cronoEstado = CANCELADO` | void |
| Cancelar por evento | `cancelarPorEvento` | — | Todas las actividades → CANCELADO | void (interno) |
| Asignar usuario | `asignarUsuario` | Actividad no final, usuario activo, pagoTipo válido | Upsert en cronograma_usuario | CronogramaUsuarioResponseDTO |
| Remover usuario | `removerUsuario` | Asignación existe | Delete de cronograma_usuario | void |

### Transiciones de estado (CronogramaPolicy)

| Desde | Puede ir a |
|---|---|
| PENDIENTE | EN_PROGRESO, CANCELADO |
| EN_PROGRESO | COMPLETADO, CANCELADO |
| COMPLETADO | (ninguna) |
| CANCELADO | (ninguna) |

### cancelarPorEvento (interno)

Llamado por `EventoServiceImpl.confirmarCancelacion()`. Marca todas las actividades del evento como CANCELADO mediante query nativa. No tiene endpoint.

## 6. Estados y transiciones

### Estados de actividad

| Estado | Significado | Visible en frontend | Notas |
|---|---|---|---|
| PENDIENTE | Creada, no iniciada | Sí (badge info) | Editable, cancelable |
| EN_PROGRESO | En ejecución | Sí (badge warning) | Editable, cancelable |
| COMPLETADO | Finalizada | Sí (badge success) | Terminal. No editable ni cancelable |
| CANCELADO | Cancelada | Sí (badge secondary) | Terminal |

### Eventos que permiten cronograma

PROGRAMADO, CONFIRMADO, EN_MONTAJE (`CronogramaPolicy.ESTADOS_EVENTO_PERMITIDOS`).

**Discrepancia DB:** El COMMENT del DDL SQL dice "Disponible desde CONFIRMADO" pero el código Java permite PROGRAMADO. El código es la fuente de verdad.

## 7. Excepciones y respuestas de error

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado |
|---|---|---|---|
| `CronogramaNotFoundException` | ID no existe | 404 | "Actividad de cronograma no encontrada: X" |
| `CronogramaException` | Regla de negocio | 400 | Variable según contexto |
| `EventoException` | Evento no encontrado | 400 | "Evento no encontrado con ID: X" |
| `MethodArgumentNotValidException` | `@Valid` falla | 400 | "campo: mensaje" |

**Mensajes frecuentes:**
- "Solo se pueden crear o editar actividades en eventos PROGRAMADO, CONFIRMADO o EN_MONTAJE. Estado actual: X"
- "No se puede editar una actividad en estado: X"
- "Transición de cronograma inválida: X -> Y"
- "Las fechas de la actividad deben estar dentro del día del evento: YYYY-MM-DD"
- "La actividad debe estar dentro del horario del evento: HH:mm - HH:mm"
- "No se puede asignar usuarios a una actividad X"
- "El usuario está inactivo"
- "Tipo de pago inválido: X"

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| `evento` (EventoRepository) | Repository | `crear` | Leer evento para validaciones | Bajo |
| `authentication/user` (UsuarioRepository) | Repository | `asignarUsuario` | Validar usuario | Bajo |

```text
Dependencia circular:
- Detectada: Sí (evento → cronograma, vía CronogramaService en EventoServiceImpl)
- Detalle: EventoServiceImpl inyecta CronogramaService con @Lazy para cancelarPorEvento().
  CronogramaServiceImpl inyecta EventoRepository para leer eventos.
- Riesgo: Bajo. @Lazy rompe el ciclo.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| POST crear | — | — | cronograma del evento |
| PUT actualizar | — | — | detalle actividad, cronograma evento |
| PATCH cambiar estado | — | — | detalle actividad, cronograma evento |
| PATCH cancelar | — | — | cronograma evento |
| POST asignar usuario | Upsert en cronograma_usuario | — | usuarios de la actividad |
| DELETE remover usuario | Delete de cronograma_usuario | — | usuarios de la actividad |
| Cancelar evento (interno) | Todas las actividades → CANCELADO | cronograma | cronograma evento |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando |
|---|---|---|
| Crear actividad | Evento en PROGRAMADO, CONFIRMADO, EN_MONTAJE | COTIZACION, FINALIZADO, CANCELADO, CANCELACION_PENDIENTE |
| Editar actividad | Actividad PENDIENTE o EN_PROGRESO | COMPLETADO, CANCELADO |
| Iniciar (PENDIENTE→EN_PROGRESO) | Actividad PENDIENTE | resto |
| Completar (EN_PROGRESO→COMPLETADO) | Actividad EN_PROGRESO | resto |
| Cancelar actividad | PENDIENTE o EN_PROGRESO | COMPLETADO, CANCELADO |
| Asignar/remover usuario | Actividad PENDIENTE o EN_PROGRESO | COMPLETADO, CANCELADO |

### 10.2 Usar flags del DTO

`CronogramaResponseDTO` incluye flags calculados por la policy:
- `editable`: usar para habilitar/deshabilitar botón de editar
- `cancelable`: usar para habilitar/deshabilitar botón de cancelar
- `accionesPermitidas`: lista de strings con las acciones disponibles (INICIAR, COMPLETAR, CANCELAR)
- `estadosSiguientesPermitidos`: estados a los que se puede transicionar

### 10.3 Reglas importantes

- Las fechas de la actividad deben estar dentro del día del evento y dentro del horario si el evento lo tiene definido.
- La asignación de usuarios es un upsert: si envías la misma combinación cronograma+usuario, se actualiza el pago.
- Al cancelar un evento, el sistema automáticamente cancela todas sus actividades. El frontend no necesita llamar a cancelar actividad individualmente.
- El DDL de la DB dice "Disponible desde CONFIRMADO" pero el código ya permite PROGRAMADO. Confiar en el código.

---

## Resumen para frontend

- CRUD de actividades en `/api/cronogramas` + asignación de usuarios con pago.
- Las actividades solo se pueden crear/editar en eventos PROGRAMADO, CONFIRMADO o EN_MONTAJE.
- Usar los flags `editable`, `cancelable`, `accionesPermitidas` del DTO para controlar la UI.
- Asignación de usuarios: upsert (crea o actualiza), con `pagoTipo` FIJO o POR_HORA.
- Transiciones de estado: PENDIENTE → EN_PROGRESO → COMPLETADO, o cualquiera → CANCELADO.
- Estados COMPLETADO y CANCELADO son terminales: no se editan, no se cancelan, no se asignan usuarios.
