# authentication — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Autenticación de usuarios vía Google OAuth2, emisión de JWT, y endpoint para obtener datos del usuario autenticado.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Login Google + identidad del usuario actual |
| Controllers | `AuthController` (`/auth`), `UsuarioController` (`/api/usuarios`) |
| Services | `GoogleAuthService`, `UsuarioService` (UserDetailsService), `JwtService` |
| Entidades | `Usuario`, `Rol` (tablas `usuario`, `rol`) |
| Tipo de módulo | Seguridad |

## 2. Endpoints

### AuthController (`/auth`) — público, sin `@PreAuthorize`

| Método | Ruta | Acción | Request | Response | Notas |
|---|---|---|---|---|---|
| POST | `/auth/google` | Login con Google | `GoogleAuthRequest` (`credential`: string, token de Google) | `AuthResponse` (`token`: JWT) | Público |

### UsuarioController (`/api/usuarios`) — requiere autenticación

| Método | Ruta | Acción | Request | Response | Notas |
|---|---|---|---|---|---|
| GET | `/api/usuarios/me` | Datos del usuario autenticado | `Authentication` | `Map<String, Object>` | `id`, `nombre`, `correo`, `telefono`, `rol` |

### POST /auth/google

- **Propósito:** Autenticar con token de Google, crear usuario si no existe, devolver JWT.
- **Request:** `{ "credential": "google-id-token" }`
- **Response:** `{ "token": "jwt-string" }`
- **Validaciones:** Token de Google válido, email verificado
- **Errores:** 401 si token inválido
- **Efectos secundarios:** Crea `Usuario` en BD si es primer login (con `usu_tipo_auth=GOOGLE`)

### GET /api/usuarios/me

- **Propósito:** Obtener datos del usuario autenticado para mostrar en UI (nombre, avatar, rol).
- **Response:** `{ "id": 1, "nombre": "Admin", "correo": "admin@test.com", "telefono": "", "rol": "ADMIN" }`
- **Nota:** No requiere `@PreAuthorize` explícito — usa el `Authentication` del contexto de seguridad.

## 3. Reglas que debe respetar el frontend

- `/auth/google` es el único endpoint público. Todos los demás requieren JWT en header `Authorization: Bearer <token>`.
- El JWT se obtiene del response de `/auth/google` → `token`.
- `GET /api/usuarios/me` devuelve el rol del usuario. Usar para control de UI (mostrar/ocultar secciones según ADMIN/EMPLEADO).
- El sistema usa `@PreAuthorize("hasRole('ADMIN')")` en todos los controllers de negocio. Solo ADMIN puede operar.

---

## Resumen para frontend

- Login: `POST /auth/google` con token de Google → JWT.
- Usuario actual: `GET /api/usuarios/me` → nombre, correo, rol.
- JWT requerido en todas las demás llamadas.
