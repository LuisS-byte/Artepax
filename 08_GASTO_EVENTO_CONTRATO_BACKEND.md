# GastoEvento — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El submódulo GastoEvento administra los gastos variables asociados a un evento. Su responsabilidad es registrar gastos proyectados o desembolsados, consultar gastos por evento, calcular resumen financiero de gastos, marcar gastos como desembolsados y eliminar gastos que aún no han sido desembolsados.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Gestionar gastos variables atribuibles a eventos. |
| Controller principal | `GastoEventoController` |
| Service principal | `GastoEventoService` / `GastoEventoServiceImpl` |
| Entidades principales | `GastoEvento`, `Evento` |
| Tablas/vistas implicadas | `gasto_evento`, `evento` |
| Módulos relacionados | Evento, Rentabilidad |
| Tipo de módulo | Operativo / Financiero |

No administra pagos de clientes. No modifica el precio del evento. No genera movimientos de caja; solo clasifica gastos como `PROYECTADO` o `DESEMBOLSADO`.

## 2. Controllers y endpoints completos

Base path: `/api/gastos`

Requiere rol: `ADMIN`, por `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/gastos` | Crear gasto | `GastoEventoRequestDTO` | `GastoEventoResponseDTO` | `crear(dto)` | ADMIN | Solo permitido si el evento está en estado habilitado para gastos. |
| GET | `/api/gastos/evento/{eventoId}` | Listar gastos de un evento | No aplica | `List<GastoEventoResponseDTO>` | `obtenerPorEvento(eventoId)` | ADMIN | No pagina. |
| GET | `/api/gastos/evento/{eventoId}/resumen` | Obtener resumen de gastos por evento | No aplica | `ResumenGastoEventoDTO` | `getResumen(eventoId)` | ADMIN | Valida que el evento exista. |
| GET | `/api/gastos/{id}` | Obtener gasto por id | No aplica | `GastoEventoResponseDTO` | `obtenerPorId(id)` | ADMIN | Lanza 404 si no existe. |
| PATCH | `/api/gastos/{id}/desembolsar` | Marcar gasto como desembolsado | No aplica | `GastoEventoResponseDTO` | `desembolsar(id)` | ADMIN | Transición `PROYECTADO -> DESEMBOLSADO`. |
| DELETE | `/api/gastos/{id}` | Eliminar gasto | No aplica | `204 No Content` | `eliminar(id)` | ADMIN | Solo si no está desembolsado. |
| GET | `/api/gastos` | Listar todos los gastos paginados | `Pageable` | `Page<GastoEventoResponseDTO>` | `obtenerTodos(pageable)` | ADMIN | Default size 50, sort `gastoFecha`. |

### POST `/api/gastos`

- Propósito: crear un gasto variable para un evento.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: `GastoEventoRequestDTO`.
- Response: `GastoEventoResponseDTO`.
- HTTP esperado: `201 Created`.
- Método del service: `GastoEventoService.crear(dto)`.
- Validaciones principales:
  - `eventoId` obligatorio.
  - `gastoConcepto` obligatorio.
  - `gastoMonto` obligatorio y mayor a 0.
  - El evento debe existir.
  - El estado del evento debe estar en `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE`.
  - `gastoEstado`, si se envía, debe ser `PROYECTADO` o `DESEMBOLSADO`.
- Excepciones posibles: `EventoNotFoundException`, `GastoEventoException`, errores de validación DTO.
- Efectos secundarios: crea una fila en `gasto_evento`; afecta resumen de gastos y rentabilidad.
- Qué debe refrescar el frontend:
  - lista de gastos del evento.
  - resumen de gastos.
  - detalle interno del evento.
  - rentabilidad del evento, si está visible.

### GET `/api/gastos/evento/{eventoId}`

- Propósito: obtener gastos asociados a un evento.
- Path variables: `eventoId`.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<GastoEventoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `obtenerPorEvento(eventoId)`.
- Validaciones principales: no valida existencia del evento; consulta por FK.
- Excepciones posibles: error genérico si hay problema de persistencia.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend: no aplica; es lectura.

### GET `/api/gastos/evento/{eventoId}/resumen`

- Propósito: calcular totales de gastos proyectados y desembolsados por evento.
- Path variables: `eventoId`.
- Query params: no aplica.
- Response: `ResumenGastoEventoDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `getResumen(eventoId)`.
- Validaciones principales: el evento debe existir.
- Excepciones posibles: `EventoNotFoundException`.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend: no aplica; es lectura.

### GET `/api/gastos/{id}`

- Propósito: obtener un gasto específico.
- Path variables: `id`.
- Query params: no aplica.
- Response: `GastoEventoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `obtenerPorId(id)`.
- Validaciones principales: el gasto debe existir.
- Excepciones posibles: `GastoEventoNotFoundException`.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend: no aplica.

### PATCH `/api/gastos/{id}/desembolsar`

- Propósito: marcar un gasto como ya desembolsado.
- Path variables: `id`.
- Query params: no aplica.
- Response: `GastoEventoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `desembolsar(id)`.
- Validaciones principales:
  - el gasto debe existir.
  - no debe estar ya en `DESEMBOLSADO`.
- Excepciones posibles: `GastoEventoNotFoundException`, `GastoEventoException`.
- Efectos secundarios: cambia `gastoEstado` a `DESEMBOLSADO`; afecta resumen y rentabilidad.
- Qué debe refrescar el frontend:
  - lista de gastos.
  - resumen de gastos.
  - rentabilidad.

### DELETE `/api/gastos/{id}`

- Propósito: eliminar un gasto proyectado.
- Path variables: `id`.
- Query params: no aplica.
- Response: sin body.
- HTTP esperado: `204 No Content`.
- Método del service: `eliminar(id)`.
- Validaciones principales:
  - el gasto debe existir.
  - no puede estar en `DESEMBOLSADO`.
- Excepciones posibles: `GastoEventoNotFoundException`, `GastoEventoException`.
- Efectos secundarios: elimina fila de `gasto_evento`; afecta resumen y rentabilidad.
- Qué debe refrescar el frontend:
  - lista de gastos.
  - resumen de gastos.
  - rentabilidad.

### GET `/api/gastos`

- Propósito: listar gastos paginados.
- Query params: parámetros estándar de Spring Pageable: `page`, `size`, `sort`.
- Response: `Page<GastoEventoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `obtenerTodos(pageable)`.
- Validaciones principales: no aplica.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend: no aplica; es lectura.

## 3. DTOs request/response

### 3.1 DTOs Request

#### GastoEventoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `eventoId` | `Long` | `eventoId` | Sí | No | `@NotNull` | Debe corresponder a un evento existente. |
| `gastoConcepto` | `String` | `gastoConcepto` | Sí | No | `@NotBlank` | Concepto visible para administración. |
| `gastoMonto` | `BigDecimal` | `gastoMonto` | Sí | No | `@NotNull`, `@DecimalMin("0.01")` | Enviar número mayor a 0. |
| `gastoEstado` | `String` | `gastoEstado` | No | Sí | Validación manual: `PROYECTADO` o `DESEMBOLSADO` | Default: `PROYECTADO`. |
| `gastoNota` | `String` | `gastoNota` | No | Sí | Sin validación DTO | Nota administrativa opcional. |

### 3.2 DTOs Response

#### GastoEventoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `GastoEvento.id` | Identificador del gasto. |
| `eventoId` | `Long` | `eventoId` | No | `GastoEvento.evento.id` | Permite enlazar al evento. |
| `gastoConcepto` | `String` | `gastoConcepto` | No | entidad | Mostrar como descripción principal. |
| `gastoMonto` | `BigDecimal` | `gastoMonto` | No | entidad | Monto del gasto. |
| `gastoEstado` | `String` | `gastoEstado` | No | entidad | `PROYECTADO` o `DESEMBOLSADO`. |
| `gastoFecha` | `LocalDateTime` | `gastoFecha` | No | `@CreationTimestamp` | Fecha de creación. |
| `gastoNota` | `String` | `gastoNota` | Sí | entidad | Nota opcional. |
| `clienteNombre` | `String` | `clienteNombre` | Sí | evento asociado | Útil en listados globales. |
| `estadoEventoNombre` | `String` | `estadoEventoNombre` | Sí | evento asociado | Útil para bloquear acciones. |

#### ResumenGastoEventoDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `totalProyectado` | `BigDecimal` | `totalProyectado` | No | suma repository | Total en estado `PROYECTADO`. |
| `totalDesembolsado` | `BigDecimal` | `totalDesembolsado` | No | suma repository | Total en estado `DESEMBOLSADO`. |
| `totalPendiente` | `BigDecimal` | `totalPendiente` | No | service | Actualmente igual a `totalProyectado`. |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `GastoEvento` | `gasto_evento` | Tabla operativa | Registra gastos variables del evento. | Estados: `PROYECTADO`, `DESEMBOLSADO`. |
| `Evento` | `evento` | Tabla operativa | Evento al que pertenece el gasto. | Solo ciertos estados permiten crear gastos. |
| `VistaRentabilidadEvento` | `vista_rentabilidad_evento` | Vista | Usa gastos para calcular rentabilidad. | Se actualiza indirectamente al crear/desembolsar/eliminar gastos. |

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Crear gasto | `crear(dto)` | evento existente, estado de evento permitido, estado de gasto válido | Inserta `gasto_evento` | `GastoEventoResponseDTO` |
| Listar por evento | `obtenerPorEvento(eventoId)` | No valida evento | No modifica | Lista DTO |
| Obtener por id | `obtenerPorId(id)` | gasto existente | No modifica | DTO |
| Desembolsar | `desembolsar(id)` | gasto existente, no desembolsado | `gastoEstado = DESEMBOLSADO` | DTO |
| Eliminar | `eliminar(id)` | gasto existente, no desembolsado | elimina fila | Sin retorno |
| Listar todos | `obtenerTodos(pageable)` | No aplica | No modifica | página DTO |
| Resumen | `getResumen(eventoId)` | evento existente | No modifica | `ResumenGastoEventoDTO` |

### Flujo: crear gasto

1. Busca el evento por `eventoId`.
2. Valida que el evento esté en `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE`.
3. Determina estado del gasto: si no se envía, usa `PROYECTADO`.
4. Valida que el estado del gasto sea `PROYECTADO` o `DESEMBOLSADO`.
5. Guarda el gasto.
6. Retorna `GastoEventoResponseDTO`.

### Flujo: desembolsar gasto

1. Busca el gasto.
2. Si ya está en `DESEMBOLSADO`, rechaza la operación.
3. Cambia el estado a `DESEMBOLSADO`.
4. Guarda y retorna DTO.

### Flujo: eliminar gasto

1. Busca el gasto.
2. Si está en `DESEMBOLSADO`, rechaza la eliminación.
3. Elimina el gasto.
4. Retorna `204 No Content` desde controller.

## 6. Estados y transiciones

### Estados existentes

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `PROYECTADO` | Gasto estimado, aún no pagado | `gasto_evento.gasto_estado` | Sí | Puede eliminarse. |
| `DESEMBOLSADO` | Gasto ya ejecutado/pagado | `gasto_evento.gasto_estado` | Sí | No puede eliminarse ni desembolsarse de nuevo. |

### Transiciones permitidas

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| `PROYECTADO` | `DESEMBOLSADO` | `PATCH /api/gastos/{id}/desembolsar` | gasto existente y no desembolsado | Cambia resumen y rentabilidad. |

## 7. Excepciones y respuestas de error

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `MethodArgumentNotValidException` | DTO inválido | 400 | Primer campo inválido: `campo: mensaje` | Mostrar error junto al campo. |
| `EventoNotFoundException` | `eventoId` inexistente | 404 | `Evento no encontrado: {id}` | Refrescar datos o volver al listado. |
| `GastoEventoNotFoundException` | gasto inexistente | 404 | `Gasto no encontrado: {id}` | Refrescar listado. |
| `GastoEventoException` | estado inválido o regla incumplida | 400 | Mensaje de negocio | Mostrar mensaje del backend. |
| `AccessDeniedException` | usuario sin rol ADMIN | 403 | mensaje de acceso denegado | Redirigir o bloquear acción. |
| `Exception` | error no controlado | 500 | `Error interno del servidor` | Mostrar error genérico. |

El formato de error global es:

```json
{
  "codigo": 400,
  "mensaje": "mensaje de error",
  "errorCode": null,
  "timestamp": "2026-05-10T00:00:00"
}
```

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| Evento | Repository | `GastoEventoServiceImpl` | Validar existencia y estado del evento | Bajo |
| Rentabilidad | Vista indirecta | `vista_rentabilidad_evento` | Gastos impactan rentabilidad | Bajo |
| Seguridad | `@PreAuthorize` | Controller | Restringir a ADMIN | Bajo |

Dependencia circular:
- Detectada: No.
- Detalle: `GastoEventoServiceImpl` depende de `EventoRepository`, pero Evento no depende del service de gastos.
- Riesgo: bajo.
- Cómo se evita: dependencia a repository, no a service cruzado.

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Crear gasto | Inserta gasto | Evento/Rentabilidad | gastos, resumen, rentabilidad |
| Desembolsar gasto | Cambia estado del gasto | Rentabilidad | gastos, resumen, rentabilidad |
| Eliminar gasto | Borra gasto proyectado | Rentabilidad | gastos, resumen, rentabilidad |
| Consultar resumen | Ninguno | Ninguno | No aplica |

No genera notificaciones. No registra auditoría explícita en el código revisado.

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Crear gasto | Evento en `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE` | `COTIZACION`, `FINALIZADO`, `CANCELADO`, `CANCELACION_PENDIENTE` | “Solo se pueden registrar gastos en eventos programados, confirmados o en montaje.” |
| Desembolsar | Gasto en `PROYECTADO` | Gasto en `DESEMBOLSADO` | “El gasto ya fue desembolsado.” |
| Eliminar | Gasto en `PROYECTADO` | Gasto en `DESEMBOLSADO` | “No se puede eliminar un gasto ya desembolsado.” |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `gastoConcepto` | Solo creación | No hay endpoint de edición | Para modificar, no existe PUT/PATCH general. |
| `gastoMonto` | Solo creación | No hay endpoint de edición | No editar en frontend si ya fue creado. |
| `gastoEstado` | Creación o desembolso | No hay edición directa | Usar endpoint `/desembolsar`. |
| `gastoNota` | Solo creación | No hay endpoint de edición | No hay actualización general. |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Crear gasto | Lista de gastos, resumen de gastos, detalle interno de evento, rentabilidad |
| Desembolsar gasto | Lista de gastos, resumen de gastos, rentabilidad |
| Eliminar gasto | Lista de gastos, resumen de gastos, rentabilidad |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `PROYECTADO` | Gasto estimado | Badge pendiente/proyectado |
| `DESEMBOLSADO` | Gasto ya ejecutado | Badge desembolsado/cerrado |

### 10.5 Reglas importantes

- No permitir crear gastos para eventos fuera de `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE`.
- No permitir eliminar gastos `DESEMBOLSADO`.
- No permitir desembolsar dos veces.
- No asumir que hay endpoint de edición general de gastos.
- Refrescar rentabilidad si la pantalla la muestra.
- Mostrar mensajes de negocio devueltos por backend.

## Resumen para frontend

- El módulo usa `/api/gastos` y requiere rol `ADMIN`.
- Los gastos pertenecen a un evento.
- Solo se crean en eventos `PROGRAMADO`, `CONFIRMADO` o `EN_MONTAJE`.
- Estados: `PROYECTADO` y `DESEMBOLSADO`.
- Solo `PROYECTADO` puede eliminarse.
- `DESEMBOLSADO` es estado final operativo.
- No hay endpoint de edición general.
- Crear/desembolsar/eliminar impacta resumen y rentabilidad.
- `GET /api/gastos/evento/{eventoId}/resumen` da totales.
- El frontend debe refrescar gastos, resumen y rentabilidad después de acciones mutantes.
