# evento/tipoevento — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Catálogo de tipos de evento. CRUD simple con toggle activar/desactivar.

| Elemento | Detalle |
|---|---|
| Controller | `TipoEventoController` (`/api/tipos-evento`) |
| Service | `TipoEventoServiceImpl` |
| Entidad | `TipoEvento` (tabla `tipo_evento`) |
| Tipo de módulo | Catálogo |

## 2. Endpoints

Todos requieren `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Notas |
|---|---|---|---|---|---|
| GET | `/api/tipos-evento` | Listar todos | — | `List<TipoEventoResponseDTO>` | |
| POST | `/api/tipos-evento` | Crear | `TipoEventoRequestDTO` | `TipoEventoResponseDTO` | Valida nombre con letras |
| PATCH | `/api/tipos-evento/{id}/toggle` | Activar/desactivar | — | `TipoEventoResponseDTO` | Invierte `tipoActivo` |

### POST /api/tipos-evento

- **Request:** `{ "nombre": "Boda", "descripcion": "Evento de boda" }`
- **Validaciones:** `nombre` obligatorio (`@NotBlank`, `@Size(2,100)`, `@Pattern` solo letras). Service valida regex con al menos una letra. `descripcion` opcional, máx 500.
- **Errores:** `TipoEventoException` (400): "El nombre debe contener texto válido"

### PATCH /api/tipos-evento/{id}/toggle

- Invierte `tipoActivo`. Si estaba `true` → `false`, y viceversa.
- **Errores:** `TipoEventoNotFoundException` (404)

## 3. DTOs

#### TipoEventoRequestDTO

| Campo | Tipo | Requerido | Validaciones |
|---|---|---|---|
| nombre | String | Sí | `@NotBlank`, `@Size(2,100)`, `@Pattern` solo letras |
| descripcion | String | No | `@Size(max=500)` |

#### TipoEventoResponseDTO

| Campo | Tipo | Nullable | Notas |
|---|---|---|---|
| id | Long | No | |
| tipoNombre | String | No | |
| tipoDescripcion | String | Sí | |
| tipoActivo | Boolean | No | FALSE = no mostrar en selectores |

## 4. Reglas para frontend

- Tipos con `tipoActivo = false` no deben aparecer en selectores de creación de evento.
- El backend valida `tipoActivo` al crear evento (`validarTipoEventoActivo`).
- El nombre debe contener al menos una letra real (service valida regex `.*[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ].*`).

---

## Resumen para frontend

- 3 endpoints: GET listar, POST crear, PATCH toggle.
- Toggle invierte `tipoActivo` (soft delete).
- Solo mostrar tipos activos en selectores.
