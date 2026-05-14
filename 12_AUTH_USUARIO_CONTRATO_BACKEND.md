# Auth / Usuario — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El módulo Auth / Usuario administra la autenticación con Google, emisión y validación de JWT, carga del usuario autenticado y exposición del perfil básico del usuario actual para el frontend.

No administra CRUD completo de usuarios desde API. El login solo autoriza usuarios existentes en la tabla `usuario`, activos y con rol `ADMIN`.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Autenticación Google OAuth, emisión de JWT, validación de Bearer token y consulta del usuario autenticado |
| Controller principal | `AuthController`, `UsuarioController` |
| Service principal | `GoogleAuthService`, `UsuarioService`, `JwtService` |
| Entidades principales | `Usuario`, `Rol` |
| Tablas/vistas implicadas | `usuario`, `rol` |
| Módulos relacionados | Seguridad Spring, Google OAuth, CORS, módulos protegidos con JWT |
| Tipo de módulo | Seguridad / Auth / Perfil actual |

## 2. Controllers y endpoints completos

### Base paths

- Auth: `/auth`
- Usuario: `/api/usuarios`

### Seguridad general

`SecurityConfig` permite sin autenticación:

- `/auth/**`
- `/error`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/v3/api-docs/**`

Todo lo demás requiere autenticación JWT mediante `Authorization: Bearer <token>`.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/auth/google` | Login con Google | `GoogleAuthRequest` | `AuthResponse` | `GoogleAuthService.authenticateWithGoogle` | No | Retorna JWT si el usuario existe, está activo y es ADMIN |
| GET | `/api/usuarios/me` | Obtener usuario autenticado | No aplica | `Map<String,Object>` | No usa service directo | Sí | Devuelve datos básicos del principal autenticado |

### POST `/auth/google`

- Propósito: autenticar al usuario mediante `idToken` de Google y devolver un JWT interno del sistema.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: `GoogleAuthRequest`.
- Response: `AuthResponse`.
- HTTP esperado: `200 OK` si autentica correctamente.
- Método del service: `googleAuthService.authenticateWithGoogle(request)`.
- Validaciones principales:
  - Verifica el token de Google con `GoogleConfig.verifyToken`.
  - Busca usuario por correo en `usuario`.
  - Requiere usuario existente.
  - Requiere `estado = true`.
  - Requiere rol `ADMIN`.
- Excepciones posibles:
  - `401 Unauthorized`: token Google inválido.
  - `403 Forbidden`: usuario no autorizado, inactivo o sin permisos.
  - `500 Internal Server Error`: error no manejado.
- Efectos secundarios: no modifica base de datos.
- Qué debe refrescar el frontend:
  - guardar token JWT.
  - cargar `/api/usuarios/me` para obtener nombre/correo/rol.
  - redirigir a dashboard o pantalla inicial.

### GET `/api/usuarios/me`

- Propósito: obtener datos básicos del usuario autenticado.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: no aplica.
- Response: `Map<String,Object>` con `id`, `nombre`, `correo`, `telefono`, `rol`.
- HTTP esperado: `200 OK` si existe autenticación válida.
- Método del service: no usa service directo; toma `Authentication` del contexto de Spring Security.
- Validaciones principales:
  - Requiere `Authentication` no nulo.
  - Requiere `auth.isAuthenticated()`.
  - Hace cast de `auth.getPrincipal()` a `Usuario`.
- Excepciones posibles:
  - `401 Unauthorized`: si no hay autenticación o no está autenticado.
  - `500 Internal Server Error`: si el principal no es `Usuario` o ocurre error no manejado.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - datos de topbar/perfil.
  - rol/permisos visuales si se usan en UI.

## 3. DTOs request/response

### 3.1 DTOs Request

#### GoogleAuthRequest

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `idToken` | `String` | `idToken` | Sí funcionalmente | Sí a nivel DTO | No tiene `@NotBlank` ni `@NotNull` | El frontend debe enviar el credential/id token recibido de Google. Si va null o inválido, el backend responderá `401 Token inválido`. |

### 3.2 DTOs Response

#### AuthResponse

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `token` | `String` | `token` | No si login exitoso | `JwtService.generateToken(usuario)` | Guardar como Bearer token para consumir endpoints protegidos. |

#### Usuario actual (`GET /api/usuarios/me`)

No existe DTO dedicado. El controller retorna un `Map<String,Object>`.

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `Usuario.idUsuario` | Identificador del usuario autenticado |
| `nombre` | `String` | `nombre` | No | `Usuario.nombre` | Mostrar en topbar/perfil |
| `correo` | `String` | `correo` | No | `Usuario.correo` | Correo autenticado |
| `telefono` | `String` | `telefono` | No en respuesta | `Usuario.telefono` o `""` | Si es null en BD, retorna string vacío |
| `rol` | `String` | `rol` | No en respuesta | `Usuario.rol.nombre` o `""` | Para UI/visibilidad de acciones si aplica |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Usuario` | `usuario` | Tabla | Usuario administrativo autenticable | Implementa `UserDetails`; el username es `correo` |
| `Rol` | `rol` | Tabla catálogo | Rol del usuario | `Usuario.getAuthorities()` retorna `ROLE_` + `rol.nombre` |

### Usuario

Campos principales:

| Campo entidad | Columna | Uso frontend/backend |
|---|---|---|
| `idUsuario` | `id_usuario` | Identificador del usuario |
| `nombre` | `usu_nombre` | Nombre visible |
| `correo` | `usu_correo` | Username de Spring Security y subject del JWT |
| `password` | `usu_password` | Requerido por `UserDetails`, no expuesto al frontend |
| `telefono` | `usu_telefono` | Perfil básico en `/me` |
| `tipoAuth` | `usu_tipo_auth` | LOCAL/GOOGLE según modelo de datos; no se expone en `/me` |
| `estado` | `estado` | Habilita/deshabilita login y cuenta |
| `fechaCreacion` | `usu_fechacreacion` | No expuesto en `/me` |
| `rol` | `id_rol` | Autoridades `ROLE_ADMIN`, etc. |

### Rol

Campos principales:

| Campo entidad | Columna | Uso |
|---|---|---|
| `idRol` | `id_rol` | Identificador |
| `nombre` | `rol_nombre` | Nombre usado para autoridad Spring: `ROLE_` + nombre |
| `descripcion` | `rol_descripcion` | Texto descriptivo |

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Login Google | `GoogleAuthService.authenticateWithGoogle` | Token Google, usuario existente, usuario activo, rol ADMIN | No modifica BD | `AuthResponse` con JWT |
| Validar token Google | `GoogleConfig.verifyToken` | Audiencia contra `google.client.id` | No modifica BD | `GoogleIdToken.Payload` o null |
| Generar JWT | `JwtService.generateToken` | No aplica validación de negocio | No modifica BD | JWT firmado HS256 |
| Validar JWT por request | `JwtAuthenticationFilter.doFilterInternal` + `JwtService.isTokenValid` | Header Bearer, subject, expiración, usuario existente | Pobla `SecurityContextHolder` | Continúa filtro HTTP |
| Cargar usuario | `UsuarioService.loadUserByUsername` | Existencia por correo | No modifica BD | `UserDetails` |
| Perfil actual | `UsuarioController.obtenerUsuarioActual` | Authentication presente y autenticado | No modifica BD | `Map` con datos básicos |

### Flujo: login con Google

1. El frontend obtiene un `idToken` de Google.
2. Llama `POST /auth/google` con `{ "idToken": "..." }`.
3. `GoogleAuthService` valida el token usando `GoogleConfig.verifyToken`.
4. Si el payload es null, lanza `ResponseStatusException(401, "Token inválido")`.
5. Extrae el email del payload.
6. Busca el usuario en `UsuarioRepository.findByCorreo`.
7. Si no existe, responde `403 Usuario no autorizado`.
8. Si está inactivo, responde `403 Usuario inactivo`.
9. Si no tiene rol `ADMIN`, responde `403 No tiene permisos`.
10. Genera JWT usando `JwtService.generateToken(usuario)`.
11. Retorna `AuthResponse.token`.

### Flujo: consumo de endpoints protegidos

1. El frontend envía `Authorization: Bearer <token>`.
2. `JwtAuthenticationFilter` extrae el JWT.
3. `JwtService.extractUsername` obtiene el subject del token, que corresponde al correo.
4. `UsuarioService.loadUserByUsername` carga el usuario.
5. `JwtService.isTokenValid` valida subject y expiración.
6. Si el token es válido, se crea `UsernamePasswordAuthenticationToken` con authorities del usuario.
7. Spring Security permite continuar la request.

### Flujo: obtener usuario actual

1. El frontend llama `GET /api/usuarios/me` con Bearer token.
2. Spring Security resuelve el `Authentication`.
3. El controller valida que exista y esté autenticado.
4. Castea el principal a `Usuario`.
5. Retorna `id`, `nombre`, `correo`, `telefono` y `rol`.

## 6. Estados y transiciones

Este módulo no maneja una máquina de estados de dominio compleja, pero sí usa flags/catálogos relevantes.

### Estados existentes

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `Usuario.estado = true` | Usuario activo | Login y `UserDetails.isEnabled()` | Indirecto | Puede autenticarse si además cumple rol |
| `Usuario.estado = false` | Usuario inactivo | Login | No directamente | Login responde `403 Usuario inactivo` |
| `Rol.nombre = ADMIN` | Usuario administrador | Login Google y authorities | Sí, en `/api/usuarios/me` | Login Google exige ADMIN |

### Transiciones permitidas

No hay endpoint en este módulo para activar/desactivar usuarios ni cambiar roles.

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| No autenticado | Autenticado | `POST /auth/google` | Google token válido, usuario existe, activo, ADMIN | Emite JWT |
| Token válido | Perfil cargado | `GET /api/usuarios/me` | JWT válido | Retorna perfil actual |
| Token expirado/ inválido | No autenticado | Filtro JWT | Validación de JWT falla | Request no obtiene authentication |

## 7. Excepciones y respuestas de error

Formato general de error del sistema:

```json
{
  "codigo": 400,
  "mensaje": "Mensaje de error",
  "errorCode": null,
  "timestamp": "2026-05-10T03:00:00"
}
```

| Excepción / condición | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `ResponseStatusException` | Token Google inválido | `401` | `Token inválido` | Mostrar error de login y pedir reintento |
| `ResponseStatusException` | Usuario no existe en BD | `403` | `Usuario no autorizado` | Mostrar acceso no autorizado |
| `ResponseStatusException` | Usuario inactivo | `403` | `Usuario inactivo` | Indicar contactar administrador |
| `ResponseStatusException` | Usuario sin rol ADMIN | `403` | `No tiene permisos` | Bloquear acceso al sistema |
| Sin `Authentication` en `/api/usuarios/me` | Token ausente/inválido o request no autenticada | `401` | Sin cuerpo según controller | Redirigir a login |
| `AccessDeniedException` | Acceso a endpoint sin permisos por method security | `403` | Mensaje de Spring Security | Mostrar acceso denegado |
| `Exception` genérica | Error no controlado | `500` | `Error interno del servidor` | Mostrar error genérico y registrar detalle si aplica |

Notas:

- `GoogleAuthRequest` no tiene `@Valid`, `@NotBlank` ni `@NotNull`; si `idToken` llega null, el flujo puede terminar como token inválido o error no manejado según comportamiento de la librería Google.
- El filtro JWT no captura explícitamente excepciones de token expirado o token malformado; esas excepciones pueden propagarse como errores de seguridad/servidor según configuración global.
- `GlobalExceptionHandler` transforma `ResponseStatusException` usando `ex.getStatusCode().value()` y `ex.getReason()`.

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| Google OAuth | Servicio externo / librería Google | `GoogleConfig.verifyToken` | Validar `idToken` | Si falla Google o config, no hay login |
| `google.client.id` | Configuración | `GoogleConfig` | Validar audiencia del token | Config incorrecta invalida login |
| `app.jwt.secret` | Configuración | `JwtService` | Firmar y validar JWT | Debe ser Base64 válido y suficientemente fuerte |
| `app.jwt.exp-hours` | Configuración | `JwtService` | Definir expiración | Si es muy bajo, expira rápido; si es alto, más riesgo |
| `UsuarioRepository` | Repository | `UsuarioService` | Buscar usuarios por correo y admins activos | Login depende de datos correctos |
| `Rol` | Entidad catálogo | `Usuario.getAuthorities()` y login | Determinar permisos | Si rol no es ADMIN, login se bloquea |
| CORS | Configuración web | `CorsConfig` / `SecurityConfig` | Permitir frontend web | Origen mal configurado bloquea navegador |
| Spring Security | Framework | `SecurityConfig`, `JwtAuthenticationFilter` | Proteger endpoints | Error de configuración expone o bloquea APIs |

```text
Dependencia circular:
- Detectada: No
- Detalle: Auth depende de UsuarioRepository/UsuarioService y seguridad; otros módulos dependen indirectamente del usuario autenticado por Spring Security.
- Riesgo: Bajo.
- Cómo se evita o cómo debería evitarse: mantener Auth como capa transversal; no inyectar services de negocio dentro de seguridad.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| `POST /auth/google` exitoso | Emite JWT | Seguridad / frontend session | Guardar token, cargar `/api/usuarios/me`, redirigir a dashboard |
| `POST /auth/google` fallido | No crea sesión | Frontend auth | Limpiar sesión local y mostrar error |
| Request con Bearer token válido | Pobla `SecurityContextHolder` | Todos los módulos protegidos | No aplica visualmente |
| `GET /api/usuarios/me` | Retorna perfil actual | Layout/topbar/permisos UI | Topbar, menú, controles por rol |
| Token expirado/inválido | Request no queda autenticada | Todos los módulos protegidos | Redirigir a login o renovar sesión si existiera refresh |

No hay efectos secundarios de escritura en base de datos durante login. El módulo no crea usuarios automáticamente desde Google.

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Entrar al sistema | Google login devuelve `token` | Error `401` o `403` | `No se pudo iniciar sesión` / mostrar mensaje backend |
| Consumir endpoints protegidos | Existe JWT guardado | No hay JWT | Redirigir a login |
| Mostrar dashboard | `/api/usuarios/me` responde `200` | `/api/usuarios/me` responde `401/403` | `Sesión no válida` |
| Mostrar acciones admin | `rol = ADMIN` | rol vacío o distinto | `No tiene permisos` |
| Cerrar sesión | Siempre que exista token local | No aplica | Limpiar token y datos de usuario |

### 10.2 Campos editables

No hay formularios de edición de usuario en este módulo.

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `idToken` | Solo durante login | Después de autenticarse | Lo genera Google, no debe ser editado manualmente |
| Datos de `/me` | No editable desde este módulo | Siempre | Son datos de solo lectura para frontend |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Login exitoso | `/api/usuarios/me`, menú lateral/topbar, estado global de sesión |
| Error de autenticación | Limpiar token local y estado de usuario |
| Inicio de app con token existente | Validar llamando `/api/usuarios/me` |
| Cierre de sesión frontend | Limpiar token, usuario, caches protegidas |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `rol = ADMIN` | Usuario administrador | Mostrar menú administrativo completo |
| `rol = ""` | Rol no disponible | Tratar como sin permisos |
| `401` | No autenticado/token inválido | Redirigir a login |
| `403` | Autenticado pero sin autorización o bloqueado | Mostrar acceso denegado |

### 10.5 Reglas importantes

- Enviar siempre `Authorization: Bearer <token>` para rutas distintas a `/auth/**` y documentación Swagger.
- Guardar el token devuelto por `/auth/google` de forma consistente y enviarlo en cada request protegida.
- Al cargar la aplicación, consultar `/api/usuarios/me` para validar sesión y obtener datos de usuario.
- No asumir que el token contiene nombre o rol; el backend usa subject=email, pero el frontend debe usar `/api/usuarios/me` para datos de perfil.
- Manejar `401` como sesión inválida o expirada.
- Manejar `403` como usuario no autorizado, inactivo o sin permisos.
- No crear usuarios automáticamente desde frontend; el backend solo acepta usuarios ya registrados en BD.
- El login Google solo permite usuarios `ADMIN`; si más adelante se permiten otros roles, debe cambiarse `GoogleAuthService`.
- `telefono` en `/api/usuarios/me` nunca viene null: si no existe, viene como string vacío.
- Actualmente no hay refresh token; si el JWT expira, el frontend debe pedir login nuevamente.

## Resumen para frontend

- El login se hace con `POST /auth/google` enviando `idToken`.
- El backend devuelve solo `{ "token": "..." }`.
- Todas las rutas protegidas requieren `Authorization: Bearer <token>`.
- Después del login, consumir `GET /api/usuarios/me` para obtener `id`, `nombre`, `correo`, `telefono` y `rol`.
- Solo usuarios existentes, activos y con rol `ADMIN` pueden iniciar sesión.
- `/api/usuarios/me` no usa DTO dedicado; devuelve un objeto tipo map.
- No hay CRUD de usuarios en este módulo.
- El frontend debe redirigir a login ante `401`.
- El frontend debe mostrar acceso denegado ante `403`.
- No existe refresh token; la sesión depende de la expiración del JWT configurada en backend.
