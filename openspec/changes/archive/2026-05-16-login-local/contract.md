# Contrato Frontend — Autenticación Local

## Endpoints de Auth

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `POST` | `/auth/login` | ❌ Público | Login local (email + password) |
| `POST` | `/auth/google` | ❌ Público | Login con Google (existente, sin cambios) |
| `GET` | `/api/usuarios/me` | ✅ Bearer JWT | Datos del usuario autenticado |

---

## POST /auth/login

### Request

```
POST http://localhost:8080/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@artepax.com",
  "password": "miPassword123"
}
```

### Success Response — `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Error Responses

| Código | Condición | Cuerpo |
|--------|-----------|--------|
| `400` | Email vacío, formato inválido, o password vacío | `{ "email": "must be a well-formed email address" }` (o similar, errores de campo) |
| `401` | Credenciales inválidas, email no encontrado, usuario GOOGLE, o password incorrecta | `{ "error": "Credenciales inválidas" }` |
| `403` | Usuario inactivo | `{ "error": "Usuario inactivo" }` |
| `403` | Usuario sin rol ADMIN | `{ "error": "No tiene permisos" }` |

> **Importante**: `401` es genérico a propósito — no diferencia entre "email no existe", "password incorrecta" o "usuario GOOGLE". Esto evita user enumeration.

---

## Flujo de Login Local

```
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│  Frontend    │     │  Backend     │     │  PostgreSQL      │
│  (React/Vue) │     │  Spring Boot │     │                  │
└──────┬───────┘     └──────┬───────┘     └────────┬─────────┘
       │                    │                      │
       │  POST /auth/login  │                      │
       │  {email, password} │                      │
       │───────────────────>│                      │
       │                    │  SELECT usuario       │
       │                    │  WHERE correo=        │
       │                    │  AND tipo_auth=LOCAL  │
       │                    │──────────────────────>│
       │                    │                      │
       │                    │  BCrypt.matches()     │
       │                    │  ─ verificar password │
       │                    │                      │
       │  200 {token}       │                      │
       │<───────────────────│                      │
       │                    │                      │
       │  Guardar token     │                      │
       │  en localStorage  │                      │
       │  o sessionStorage  │                      │
       │                    │                      │
       │  GET /api/usuarios/me                      │
       │  Authorization:     │                      │
       │  Bearer <token>    │                      │
       │───────────────────>│                      │
       │                    │  Validar JWT          │
       │                    │  → extraer usuario    │
       │                    │                      │
       │  200 {id, nombre,  │                      │
       │  correo, telefono, │                      │
       │  rol}              │                      │
       │<───────────────────│                      │
```

---

## Uso del JWT en el Frontend

### 1. Guardar token después del login

```javascript
async function loginLocal(email, password) {
  const res = await fetch('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });

  if (!res.ok) {
    const error = await res.json();
    throw new Error(error.error); // "Credenciales inválidas" | "Usuario inactivo" | "No tiene permisos"
  }

  const data = await res.json();
  localStorage.setItem('jwt', data.token);
  return data.token;
}
```

### 2. Enviar token en requests autenticados

```javascript
async function fetchMe() {
  const token = localStorage.getItem('jwt');
  const res = await fetch('/api/usuarios/me', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  if (res.status === 401) {
    // Token expirado o inválido → redirigir a login
    localStorage.removeItem('jwt');
    window.location.href = '/login';
    return;
  }

  return res.json();
  // → { id: 1, nombre: "...", correo: "...", telefono: "...", rol: "ADMIN" }
}
```

### 3. Schema de `/api/usuarios/me`

```json
{
  "id": 1,
  "nombre": "Admin Artepax",
  "correo": "admin@artepax.com",
  "telefono": "099123456",
  "rol": "ADMIN"
}
```

---

## Integración con Login Google Existente

El login Google (`POST /auth/google`) **no cambia**. El frontend puede:

- Usar login **Google** para usuarios con cuenta Google
- Usar login **Local** para usuarios con email/contraseña
- Ambos devuelven el mismo formato `{ "token": "..." }`
- Ambos JWT funcionan con los mismos endpoints protegidos (`/api/**`)

### Decisión de flujo en el frontend

```javascript
async function handleLogin({ email, password, isGoogle }) {
  let token;

  if (isGoogle) {
    const googleToken = await getGoogleIdToken(); // Google Sign-In
    const res = await fetch('/auth/google', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken: googleToken })
    });
    const data = await res.json();
    token = data.token;
  } else {
    token = await loginLocal(email, password);
  }

  localStorage.setItem('jwt', token);
  const user = await fetchMe();
  return user;
}
```

---

## Códigos de Error (400)

Cuando la validación de campos falla, el backend devuelve `400` con errores por campo:

```json
{
  "email": "must be a well-formed email address",
  "password": "must not be blank"
}
```

Posibles errores de validación:

| Campo | Error | Condición |
|-------|-------|-----------|
| `email` | `must not be blank` | Email vacío o null |
| `email` | `must be a well-formed email address` | Formato inválido (falta `@`, etc.) |
| `password` | `must not be blank` | Password vacío o null |

---

## Tiempo de Expiración del Token

El JWT expira en **4 horas** (configurado vía `app.jwt.expiration` en `application.properties`). El frontend debe:

- Al recibir `401` en cualquier endpoint protegido, **eliminar el token guardado** y redirigir a login
- Opcionalmente, implementar renovación silenciosa vía refresh (no implementado actualmente — requiere cambio futuro)

---

## Frontend — Checklist de Implementación

- [ ] Formulario con campos `email` (input type=email) y `password` (input type=password)
- [ ] Validación client-side: email no vacío + formato, password no vacío
- [ ] Llamada a `POST /auth/login` con `Content-Type: application/json`
- [ ] Manejo de `400`: mostrar errores de validación por campo
- [ ] Manejo de `401`: mostrar "Credenciales inválidas"
- [ ] Manejo de `403`: mostrar "Usuario inactivo" o "No tiene permisos"
- [ ] Guardar `token` en `localStorage` o `sessionStorage`
- [ ] Redirigir al dashboard después de login exitoso
- [ ] Incluir `Authorization: Bearer <token>` en requests autenticados
- [ ] Detectar `401` en cualquier response y redirigir a login
- [ ] Botón de login Google existente sigue funcionando sin cambios
