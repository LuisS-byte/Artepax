# TipoEvento — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El submódulo TipoEvento administra el catálogo editable de tipos de evento. Permite listar tipos existentes, crear nuevos tipos activos y activar/desactivar tipos sin borrar historial.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Gestionar catálogo de tipos de evento. |
| Controller principal | `TipoEventoController` |
| Service principal | `TipoEventoService` / `TipoEventoServiceImpl` |
| Entidades principales | `TipoEvento` |
| Tablas/vistas implicadas | `tipo_evento` |
| Módulos relacionados | Evento |
| Tipo de módulo | Catálogo |

No crea eventos. No elimina tipos físicamente. No valida si un tipo está siendo usado antes de desactivarlo; solo alterna `tipoActivo`.

## 2. Controllers y endpoints completos

Base path: `/api/tipos-evento`

Requiere rol: `ADMIN`, por `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| GET | `/api/tipos-evento` | Listar tipos | No aplica | `List<TipoEventoResponseDTO>` | `listar()` | ADMIN | Retorna activos e inactivos. |
| POST | `/api/tipos-evento` | Crear tipo | `TipoEventoRequestDTO` | `TipoEventoResponseDTO` | `crear(nombre, descripcion)` | ADMIN | Crea activo por defecto. |
| PATCH | `/api/tipos-evento/{id}/toggle` | Activar/desactivar | No aplica | `TipoEventoResponseDTO` | `toggle(id)` | ADMIN | Alterna `tipoActivo`. |

### GET `/api/tipos-evento`

- Propósito: listar el catálogo completo de tipos de evento.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<TipoEventoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `TipoEventoService.listar()`.
- Validaciones principales: no aplica.
- Excepciones posibles: errores genéricos.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend: no aplica; es lectura.

### POST `/api/tipos-evento`

- Propósito: crear un nuevo tipo de evento activo.
- Request body: `TipoEventoRequestDTO`.
- Response: `TipoEventoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `TipoEventoService.crear(nombre, descripcion)`.
- Validaciones principales:
  - `nombre` obligatorio.
  - `nombre` entre 2 y 100 caracteres.
  - `nombre` solo letras y espacios por DTO.
  - `descripcion` máximo 500 caracteres.
  - service valida que nombre contenga al menos una letra.
  - service valida que descripción, si existe, contenga al menos una letra.
- Excepciones posibles: validación DTO, `TipoEventoException`, `DataIntegrityViolationException` si nombre duplicado viola unicidad de BD.
- Efectos secundarios: inserta fila en `tipo_evento` con `tipoActivo = true`.
- Qué debe refrescar el frontend:
  - lista de tipos de evento.
  - selects de creación/edición de evento.

### PATCH `/api/tipos-evento/{id}/toggle`

- Propósito: alternar el estado activo/inactivo del tipo de evento.
- Path variables: `id`.
- Request body: no aplica.
- Response: `TipoEventoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `TipoEventoService.toggle(id)`.
- Validaciones principales: el tipo debe existir.
- Excepciones posibles: `TipoEventoNotFoundException`.
- Efectos secundarios: cambia `tipoActivo`.
- Qué debe refrescar el frontend:
  - lista de tipos de evento.
  - selects de creación/edición de evento.

## 3. DTOs request/response

### 3.1 DTOs Request

#### TipoEventoRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `nombre` | `String` | `nombre` | Sí | No | `@NotBlank`, `@Size(min=2,max=100)`, `@Pattern` letras y espacios | No enviar números solos ni símbolos. |
| `descripcion` | `String` | `descripcion` | No | Sí | `@Size(max=500)`; validación manual si no está vacía | Si se envía, debe contener texto válido. |

### 3.2 DTOs Response

#### TipoEventoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `tipo_evento.id_tipo_evento` | Identificador para selects. |
| `tipoNombre` | `String` | `tipoNombre` | No | `tipo_nombre` | Texto visible. |
| `tipoDescripcion` | `String` | `tipoDescripcion` | Sí | `tipo_descripcion` | Descripción opcional. |
| `tipoActivo` | `Boolean` | `tipoActivo` | No | `tipo_activo` | Determina si debe estar disponible para nuevos eventos. |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `TipoEvento` | `tipo_evento` | Catálogo | Clasifica eventos por tipo. | Permite desactivar sin borrar historial. |
| `Evento` | `evento` | Tabla operativa | Consume `tipo_evento` por FK. | Crear evento valida que el tipo esté activo. |

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Listar | `listar()` | No aplica | No modifica | lista DTO |
| Crear | `crear(nombre, descripcion)` | texto válido | inserta tipo activo | DTO |
| Activar/desactivar | `toggle(id)` | existencia del tipo | invierte `tipoActivo` | DTO |

### Flujo: listar tipos

1. Consulta todos los registros de `tipo_evento`.
2. Convierte cada entidad a `TipoEventoResponseDTO`.
3. Retorna activos e inactivos.

### Flujo: crear tipo

1. Valida nombre con reglas DTO y validación manual.
2. Valida descripción si existe.
3. Crea `TipoEvento` con `tipoActivo = true`.
4. Guarda en base.
5. Retorna DTO.

### Flujo: toggle

1. Busca tipo por id.
2. Si no existe, lanza `TipoEventoNotFoundException`.
3. Invierte `tipoActivo`.
4. Guarda y retorna DTO.

## 6. Estados y transiciones

Este módulo no maneja estados de ciclo de vida complejos. Maneja un flag booleano.

### Estados existentes

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `tipoActivo = true` | Tipo disponible para nuevas operaciones | `tipo_evento.tipo_activo` | Sí | Debe aparecer como opción seleccionable. |
| `tipoActivo = false` | Tipo desactivado | `tipo_evento.tipo_activo` | Sí | Debe ocultarse o marcarse inactivo para nuevos eventos. |

### Transiciones permitidas

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| Activo | Inactivo | `PATCH /api/tipos-evento/{id}/toggle` | tipo existe | Deja de estar disponible para nuevos eventos si frontend filtra activos. |
| Inactivo | Activo | `PATCH /api/tipos-evento/{id}/toggle` | tipo existe | Vuelve a estar disponible. |

## 7. Excepciones y respuestas de error

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `MethodArgumentNotValidException` | DTO inválido | 400 | Primer campo inválido | Mostrar error junto al campo. |
| `TipoEventoException` | texto inválido por validación manual | 400 | `El nombre debe contener texto válido` o similar | Mostrar mensaje del backend. |
| `TipoEventoNotFoundException` | id inexistente en toggle | 404 | `Tipo de evento no encontrado: {id}` | Refrescar listado. |
| `DataIntegrityViolationException` | nombre duplicado por restricción única | 409 | `Error de integridad de datos` | Mostrar mensaje de duplicado genérico o pedir otro nombre. |
| `AccessDeniedException` | usuario sin rol ADMIN | 403 | mensaje de acceso denegado | Bloquear acceso. |
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
| Evento | Relación de base | `evento.id_tipo_evento` | Clasificar eventos | Bajo |
| Seguridad | `@PreAuthorize` | Controller | Restringir a ADMIN | Bajo |

Dependencia circular:
- Detectada: No.
- Detalle: `TipoEventoServiceImpl` solo depende de `TipoEventoRepository`.
- Riesgo: bajo.
- Cómo se evita: no depende de `EventoService`.

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Crear tipo | Inserta nuevo catálogo activo | Evento | lista de tipos, selects de eventos |
| Toggle tipo | Cambia disponibilidad para nuevas operaciones | Evento | lista de tipos, selects de eventos |
| Listar | Ninguno | Ninguno | No aplica |

No genera auditoría explícita en el código revisado. No genera notificaciones.

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Crear tipo | Nombre válido | Nombre vacío, solo números o símbolos | “El nombre solo puede contener letras y espacios.” |
| Toggle tipo | Tipo existe | Mientras se procesa la petición | “No se pudo actualizar el estado del tipo.” |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `nombre` | Solo creación | No hay endpoint de edición | No existe PUT/PATCH para renombrar. |
| `descripcion` | Solo creación | No hay endpoint de edición | No existe edición posterior. |
| `tipoActivo` | Vía toggle | No editar directamente | Usar endpoint `/toggle`. |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Crear tipo | Lista de tipos, selects de creación/edición de evento |
| Toggle tipo | Lista de tipos, selects de creación/edición de evento |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `tipoActivo = true` | Tipo disponible | Badge Activo |
| `tipoActivo = false` | Tipo desactivado | Badge Inactivo |

### 10.5 Reglas importantes

- No mostrar tipos inactivos como opción normal para crear eventos.
- Si se muestran tipos inactivos en administración, diferenciarlos visualmente.
- No asumir que existe endpoint para editar nombre o descripción.
- El backend lista activos e inactivos; el frontend debe filtrar según contexto.
- En formularios, replicar validaciones básicas para mejor UX, pero confiar en backend como fuente final.

## Resumen para frontend

- El módulo usa `/api/tipos-evento` y requiere rol `ADMIN`.
- Es un catálogo simple.
- Lista activos e inactivos.
- Crear tipo requiere `nombre` válido.
- Los nombres aceptan letras y espacios.
- La descripción es opcional.
- Crear devuelve `200 OK`, no `201`.
- Toggle activa/desactiva el tipo.
- No hay endpoint de edición ni eliminación.
- Para crear eventos, el frontend debe usar solo tipos activos.
