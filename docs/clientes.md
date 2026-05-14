# clientes — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

CRUD de clientes con soft delete, búsqueda por nombre, listado de inactivos, reactivación e historial de eventos asociados.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Gestión de clientes que contratan eventos |
| Controller principal | `ClienteController` (`/api/clientes`) |
| Service principal | `ClienteServiceImpl` |
| Entidades principales | `Cliente` (tabla `cliente`) |
| Tablas/vistas implicadas | `cliente` |
| Módulos relacionados | `evento` (EventoRepository para historial) |
| Tipo de módulo | CRUD |

## 2. Controllers y endpoints completos

Todos requieren `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Service | Auth | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/clientes` | Crear cliente | `ClienteRequestDTO` | `ClienteResponseDTO` (201) | `crearCliente` | ADMIN | Valida correo único |
| GET | `/api/clientes` | Listar activos | — | `List<ClienteResponseDTO>` | `obtenerTodos` | ADMIN | Solo `activo=true` |
| GET | `/api/clientes/{id}` | Obtener uno | — | `ClienteResponseDTO` | `obtenerPorId` | ADMIN | |
| PUT | `/api/clientes/{id}` | Actualizar | `ClienteUpdateDTO` | `ClienteResponseDTO` | `actualizar` | ADMIN | Valida correo único si cambia |
| DELETE | `/api/clientes/{id}` | Desactivar | — | `204 No Content` | `eliminar` | ADMIN | Soft delete: `activo=false` |
| GET | `/api/clientes/{id}/eventos` | Historial eventos | — | `List<EventoResponseDTO>` | `obtenerEventosPorCliente` | ADMIN | Eventos del cliente (todos) |
| GET | `/api/clientes/buscar?nombre=` | Buscar por nombre | `nombre` (query) | `List<ClienteResponseDTO>` | `buscarPorNombre` | ADMIN | Case-insensitive, contiene. Solo activos |
| GET | `/api/clientes/inactivos` | Listar inactivos | — | `List<ClienteResponseDTO>` | `obtenerInactivos` | ADMIN | Solo `activo=false` |
| PATCH | `/api/clientes/{id}/reactivar` | Reactivar | — | `ClienteResponseDTO` | `reactivar` | ADMIN | `activo=true` |

### POST /api/clientes

- **Propósito:** Crear un nuevo cliente.
- **Request body:** `ClienteRequestDTO`
- **Response:** `ClienteResponseDTO` con HTTP 201
- **Validaciones principales:**
  - `nombre` obligatorio (`@NotBlank`)
  - `telefono` obligatorio (`@NotBlank`)
  - `correo` obligatorio (`@NotBlank`, `@Email`)
  - Correo único: si ya existe → `IllegalArgumentException` (400)
- **Excepciones posibles:**
  - `IllegalArgumentException` (400): "El correo ya está en uso"
  - `MethodArgumentNotValidException` (400): errores de validación DTO
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** listado de clientes

### GET /api/clientes

- **Propósito:** Listar todos los clientes activos.
- **Response:** `List<ClienteResponseDTO>` — solo `activo=true`
- **Validaciones principales:** ninguna
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/clientes/{id}

- **Propósito:** Obtener un cliente por ID (activo o inactivo).
- **Response:** `ClienteResponseDTO`
- **Excepciones posibles:** `ClienteNotFoundException` (404) — "Cliente no encontrado: X"
- **Qué debe refrescar el frontend:** NO APLICA

### PUT /api/clientes/{id}

- **Propósito:** Actualizar datos del cliente.
- **Request body:** `ClienteUpdateDTO` (`nombre`, `telefono` obligatorios; `direccion`, `correo` opcionales)
- **Validaciones principales:**
  - Si cambia `correo` y el nuevo ya existe → error
- **Excepciones posibles:**
  - `ClienteNotFoundException` (404)
  - `IllegalArgumentException` (400): "El correo ya está en uso"
- **Efectos secundarios:** ninguno
- **Qué debe refrescar el frontend:** listado de clientes, detalle

### DELETE /api/clientes/{id}

- **Propósito:** Desactivar cliente (soft delete).
- **Response:** `204 No Content`
- **Excepciones posibles:** `ClienteNotFoundException` (404)
- **Efectos secundarios:** `activo = false`
- **Qué debe refrescar el frontend:** listado de clientes

### GET /api/clientes/{id}/eventos

- **Propósito:** Obtener todos los eventos (historial) de un cliente.
- **Response:** `List<EventoResponseDTO>` — incluye eventos de cualquier estado
- **Excepciones posibles:** `ClienteNotFoundException` (404)
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/clientes/buscar?nombre=X

- **Propósito:** Buscar clientes activos cuyo nombre contenga el texto (case-insensitive).
- **Query params:** `nombre` (String)
- **Response:** `List<ClienteResponseDTO>` — solo activos
- **Qué debe refrescar el frontend:** NO APLICA

### GET /api/clientes/inactivos

- **Propósito:** Listar clientes desactivados.
- **Response:** `List<ClienteResponseDTO>` — solo `activo=false`
- **Qué debe refrescar el frontend:** NO APLICA

### PATCH /api/clientes/{id}/reactivar

- **Propósito:** Reactivar un cliente desactivado.
- **Response:** `ClienteResponseDTO`
- **Excepciones posibles:** `ClienteNotFoundException` (404)
- **Efectos secundarios:** `activo = true`
- **Qué debe refrescar el frontend:** listado de clientes

## 3. DTOs request/response

### 3.1 DTOs Request

#### ClienteRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| nombre | String | string | Sí (`@NotBlank`) | No | | |
| telefono | String | string | Sí (`@NotBlank`) | No | | No único en BD |
| correo | String | string | Sí (`@NotBlank`, `@Email`) | No | Único en BD | |
| direccion | String | string | No | Sí | | |

#### ClienteUpdateDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| nombre | String | string | Sí (`@NotBlank`) | No | | |
| telefono | String | string | Sí (`@NotBlank`) | No | | |
| direccion | String | string | No | Sí | | |
| correo | String | string | No | Sí | `@Email` si se envía. Si cambia, valida unicidad | |

### 3.2 DTOs Response

#### ClienteResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| id | Long | number | No | entidad | |
| nombre | String | string | No | entidad | |
| telefono | String | string | No | entidad | |
| correo | String | string | No | entidad | |
| direccion | String | string | Sí | entidad | |
| activo | Boolean | boolean | No | entidad | TRUE = activo, FALSE = inactivo |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Cliente` | `cliente` | Tabla operativa | Datos del cliente | `cli_activo` (soft delete). `cli_correo` único cuando no NULL (índice parcial) |

**Constraint DB:** `cliente` tiene un índice único parcial `idx_cliente_correo_unique` sobre `LOWER(cli_correo)` cuando no es NULL.

## 5. Services y lógica de negocio

| Flujo | Método | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Crear | `crearCliente` | Correo único | Crea Cliente (`activo=true`) | ClienteResponseDTO |
| Listar activos | `obtenerTodos` | — | — | List (solo activos) |
| Listar inactivos | `obtenerInactivos` | — | — | List (solo inactivos) |
| Obtener | `obtenerPorId` | Existencia | — | ClienteResponseDTO |
| Actualizar | `actualizar` | Existencia, correo único si cambia | Actualiza nombre, telefono, direccion, correo | ClienteResponseDTO |
| Eliminar | `eliminar` | Existencia | `activo = false` | void |
| Reactivar | `reactivar` | Existencia | `activo = true` | ClienteResponseDTO |
| Buscar | `buscarPorNombre` | — | — | List (solo activos) |
| Historial eventos | `obtenerEventosPorCliente` | Existencia | — | List\<EventoResponseDTO\> |

## 6. Estados y transiciones

Estados de Cliente: `activo = true` / `activo = false`.

| Estado | Significado | Visible en frontend | Notas |
|---|---|---|---|
| Activo (true) | Disponible para nuevos eventos | Sí | |
| Inactivo (false) | Desactivado (soft delete) | Sí | No debería aparecer en selectores de nuevo evento |

Transiciones: DELETE → `activo=false`. PATCH `/reactivar` → `activo=true`.

## 7. Excepciones y respuestas de error

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `ClienteNotFoundException` | ID no existe | 404 | "Cliente no encontrado: X" | Redirigir |
| `IllegalArgumentException` | Correo duplicado | 400 | "El correo ya está en uso" | Mostrar error en campo correo |
| `MethodArgumentNotValidException` | `@Valid` falla | 400 | "campo: mensaje" | Mostrar error en el campo |

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| `evento` (EventoRepository) | Repository | `obtenerEventosPorCliente` | Listar eventos del cliente | Bajo |
| `evento` (EventoMapper) | Mapper | `obtenerEventosPorCliente` | Mapear eventos a DTO | Bajo |

```text
Dependencia circular:
- Detectada: No
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| POST crear | — | — | listado clientes |
| PUT actualizar | — | — | listado clientes, detalle |
| DELETE eliminar | `activo = false` | — | listado clientes |
| PATCH reactivar | `activo = true` | — | listado clientes |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Seleccionar en nuevo evento | Cliente activo | Cliente inactivo | No mostrar en dropdown |
| Editar | Siempre | — | |
| Eliminar (desactivar) | Siempre | — | Confirmar: "¿Desactivar este cliente?" |
| Reactivar | Cliente inactivo | Cliente activo | Ocultar botón si ya está activo |

### 10.2 Reglas importantes

- `GET /api/clientes` solo devuelve activos. Para ver inactivos usar `GET /api/clientes/inactivos`.
- `GET /api/clientes/{id}` devuelve el cliente aunque esté inactivo.
- Correo es único en BD (case-insensitive). Si se intenta crear/actualizar con uno existente → error 400.
- Clientes inactivos no deben aparecer en selectores de creación de evento. El backend lo valida (`validarClienteActivo` en EventoServiceImpl).
- El historial de eventos (`GET /api/clientes/{id}/eventos`) devuelve eventos de todos los estados, no solo activos.

---

## Resumen para frontend

- CRUD estándar en `/api/clientes` con soft delete (activo/inactivo) + reactivación.
- `GET /api/clientes` solo activos. Usar `/inactivos` y `/reactivar` para gestionar bajas.
- Búsqueda: `GET /api/clientes/buscar?nombre=X` — case-insensitive, solo activos.
- Historial de eventos del cliente: `GET /api/clientes/{id}/eventos`.
- Correo único — validar en frontend antes de enviar.
- Clientes inactivos no deben seleccionarse para nuevos eventos.
