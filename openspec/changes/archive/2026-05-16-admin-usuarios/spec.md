# Admin Usuarios Specification

## Purpose

Full administrative management of local users. Only users with `ROLE_ADMIN` can create, list, and activate/deactivate local users. Passwords are BCrypt-hashed server-side and never exposed in any response.

## Requirements

### Requirement: Crear usuario local

The system MUST allow ADMIN to create local users via `POST /api/usuarios`. The password MUST be BCrypt-hashed before persistence. New users MUST have `estado=true` and `tipoAuth=LOCAL`. Password MUST NOT appear in any response body.

#### Scenario: Creación exitosa

- GIVEN an authenticated ADMIN
- AND valid nombre, correo (unique case-insensitive), telefono, password (8–72 chars), and an existing rol id
- WHEN `POST /api/usuarios` receives the valid payload
- THEN a new Usuario is persisted with `estado=true`, `tipoAuth=LOCAL`, and BCrypt-hashed password
- AND the response returns 201 with `UsuarioResponseDTO` (no password field)

#### Scenario: Password fuera de rango

- GIVEN an authenticated ADMIN
- AND password length is less than 8 or greater than 72 characters
- WHEN `POST /api/usuarios` is called
- THEN the system returns 400 Bad Request with a field-level validation error on password

#### Scenario: Correo duplicado case-insensitive

- GIVEN an authenticated ADMIN
- AND a usuario already exists with a correo that differs only in letter case (e.g., `admin@artepax.com` vs `ADMIN@ARTEPAX.COM`)
- WHEN `POST /api/usuarios` is called with that correo
- THEN the system returns 400 Bad Request — el índice único `idx_usuario_correo_lower` en DB ya protege `lower(usu_correo)`, y `existsByCorreoIgnoreCase` lo replica en la capa de aplicación

#### Scenario: Rol inexistente (400, no 404)

- GIVEN an authenticated ADMIN
- AND the request includes an `idRol` that does not exist in the database
- WHEN `POST /api/usuarios` is called
- THEN the system returns 400 Bad Request "Rol no válido" — el recurso `/api/usuarios` existe; lo inválido es el `rolId` enviado, no la URL

#### Scenario: Campos requeridos ausentes

- GIVEN an authenticated ADMIN
- AND the request body is missing nombre, correo, password, or idRol
- WHEN `POST /api/usuarios` is called
- THEN the system returns 400 Bad Request with field-level validation errors

### Requirement: Listar usuarios

The system MUST allow ADMIN to list all users via `GET /api/usuarios`. The response body MUST use `UsuarioResponseDTO` and MUST NOT include the password hash.

#### Scenario: Listado exitoso

- GIVEN an authenticated ADMIN
- WHEN `GET /api/usuarios` is called
- THEN the system returns 200 OK with a list of `UsuarioResponseDTO` (id, nombre, correo, telefono, rol, estado, tipoAuth, fechaCreacion)
- AND no password field is present in any entry

### Requirement: Activar/desactivar usuario

The system MUST allow ADMIN to toggle user estado via `PATCH /api/usuarios/{id}/estado`. The system MUST NOT allow an ADMIN to deactivate themselves. The system MUST NOT allow deactivation of the last active ADMIN.

> **Alcance del guard**: la protección de "último ADMIN activo" solo aplica cuando el usuario objetivo es ADMIN (rol = "ADMIN") y tiene `estado = true`. No bloquea desactivar un EMPLEADO, ni desactivar un ADMIN cuando hay al menos otro ADMIN activo en el sistema.

#### Scenario: Desactivación exitosa

- GIVEN an authenticated ADMIN
- AND a target user with id={id} exists, is active, and is NOT the calling ADMIN
- AND the target is EMPLOYED or, if ADMIN, at least one other active ADMIN remains besides the target
- WHEN `PATCH /api/usuarios/{id}/estado` with `activo=false` is called
- THEN the target user's estado is set to false
- AND the response returns 200 OK with the updated `UsuarioResponseDTO`

#### Scenario: Activación exitosa

- GIVEN an authenticated ADMIN
- AND a target user with id={id} exists and is inactive
- WHEN `PATCH /api/usuarios/{id}/estado` with `activo=true` is called
- THEN the target user's estado is set to true
- AND the response returns 200 OK

#### Scenario: Autodesactivación

- GIVEN an authenticated ADMIN
- AND the target user id matches the calling ADMIN's own id
- WHEN `PATCH /api/usuarios/{id}/estado` with `activo=false` is called
- THEN the system returns 400 Bad Request ("no puede desactivarse a sí mismo")

#### Scenario: Último ADMIN activo

- GIVEN an authenticated ADMIN
- AND the target user is the only active ADMIN in the system
- WHEN `PATCH /api/usuarios/{id}/estado` with `activo=false` is called
- THEN the system returns 400 Bad Request ("no puede desactivar al último ADMIN activo")

#### Scenario: Usuario no encontrado

- GIVEN an authenticated ADMIN
- AND no user exists with the given id
- WHEN `PATCH /api/usuarios/{id}/estado` is called
- THEN the system returns 404 Not Found

### Requirement: Protección de endpoints

All new administrative endpoints (`POST`, `GET` list, `PATCH` estado) MUST require `ROLE_ADMIN`. Requests without this role MUST be rejected with 403 Forbidden. The existing `GET /api/usuarios/me` endpoint MUST remain unchanged.

#### Scenario: Acceso sin rol ADMIN

- GIVEN an authenticated user without `ROLE_ADMIN`
- WHEN any of `POST /api/usuarios`, `GET /api/usuarios`, or `PATCH /api/usuarios/{id}/estado` is called
- THEN the system returns 403 Forbidden

#### Scenario: GET /me preservado

- GIVEN any authenticated user (ADMIN or not)
- WHEN `GET /api/usuarios/me` is called
- THEN the endpoint returns current user data — no change in existing behavior

### Requirement: Contrato de API

The new endpoints MUST expose their contract via OpenAPI/Swagger annotations. Request and response DTOs MUST include field-level descriptions and validation constraints visible in the generated documentation so the frontend can integrate without ambiguity.
