# Proposal: Admin User Management (admin-usuarios)

## Intent

ADMIN autenticado necesita crear, listar y activar/desactivar usuarios LOCALES sin depender de la DB directamente. Hoy solo existe `GET /api/usuarios/me` — no hay CRUD de usuarios. Este cambio expone endpoints administrativos seguros protegidos con `@PreAuthorize("hasRole('ADMIN')")`.

## Scope

### In Scope

1. **POST /api/usuarios** — crear usuario LOCAL con password hasheado via BCrypt
2. **GET /api/usuarios** — listar usuarios sin exponer `password`
3. **PATCH /api/usuarios/{id}/estado** — activar/desactivar usuario
4. **RolRepository** — consultar existencia de rol por nombre
5. **existsByCorreoIgnoreCase** en `UsuarioRepository` — validación unique case-insensitive
6. **UsuarioResponseDTO** — DTO de salida sin password
7. **CreateUsuarioRequestDTO** — DTO de entrada con validación (`@Size(min=8, max=72)` en password)
8. Tests unitarios para UsuarioService + UsuarioController

### Out of Scope

- Edición completa de usuario (nombre, email, rol, password)
- Creación de usuarios GOOGLE
- Permisos por módulo
- Recuperación / cambio de password

## Capabilities

### New Capabilities

- `admin-usuarios`: Gestión administrativa de usuarios locales (CRUD parcial solo para ADMIN autenticado).

### Modified Capabilities

- None

## Approach

Reutilizar `PasswordEncoder` (BCrypt) ya existente de `SecurityConfig`. Agregar métodos a `UsuarioService`, crear DTOs, y un `RolRepository`. Controller existente `UsuarioController` se expande con 3 endpoints protegidos por `@PreAuthorize("hasRole('ADMIN')")`. Usar `@Valid` en request bodies.

**Política de roles**: El endpoint permite seleccionar cualquier rol existente en DB. Sin embargo, el sistema actual protege casi todo con `ROLE_ADMIN`. Un usuario `EMPLEADO` existiría pero no podría acceder a la mayoría de los módulos — eso se resuelve en un cambio futuro de permisos.

**Reglas de negocio:**
- Solo ADMIN puede consumir endpoints administrativos.
- La creación solo genera usuarios `tipoAuth = 'LOCAL'`.
- El password se recibe plano (`@Size(min=8, max=72)`) y se guarda con BCrypt.
- Nunca devolver `usu_password` en responses.
- Correo único case-insensitive.
- Rol debe existir en DB.
- Usuario nuevo inicia con `estado = true`.
- No crear usuarios GOOGLE desde este flujo.
- No permitir que un ADMIN se desactive a sí mismo.
- No permitir desactivar el último ADMIN activo — validar conteo con `findAdminsActivos()`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `auth/user/controller/UsuarioController.java` | Modified | +POST, +GET list, +PATCH estado |
| `auth/user/service/UsuarioService.java` | Modified | +crear, +listar, +cambiarEstado |
| `auth/user/repository/UsuarioRepository.java` | Modified | +existsByCorreoIgnoreCase |
| `auth/user/repository/RolRepository.java` | **New** | JpaRepository<Rol, Integer>, findFirstByNombre |
| `auth/user/dto/CreateUsuarioRequest.java` | **New** | DTO entrada: nombre, correo, password, telefono, idRol |
| `auth/user/dto/UsuarioResponse.java` | **New** | DTO salida: sin password |
| `auth/user/dto/EstadoRequest.java` | **New** | DTO para PATCH: activo |

## Risks

**Risk Level: Bajo-Medio** — toca administración de cuentas, roles, estado de usuarios y acceso al sistema. No hay complejidad técnica alta, pero el impacto de un error es significativo.

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| ADMIN se desactiva a sí mismo siendo el único ADMIN activo | Low | High | Validar en servicio: no permitir autodesactivación + no permitir desactivar último ADMIN activo |
| Correo duplicado con diferencias de case | Low | Medium | existsByCorreoIgnoreCase en repository |
| Password expuesta en logs/responses | Low | High | DTO separado sin campo password; `@JsonIgnore` en entity nunca es suficiente solo |
| ADMIN accidentalmente crea usuario con rol incorrecto | Low | Low | Rol se valida contra DB; si el rol existe pero no da acceso, es decisión consciente |
| Usuario EMPLEADO existe pero no puede operar | Medium | Low | Documentado como política actual; es esperado hasta que se implementen permisos por módulo |

## Rollback Plan

Revertir commit que agregue los endpoints. Los endpoints existentes (`GET /me`) no se ven afectados. Quitar `RolRepository` y DTOs nuevos no rompe nada — son no utilizados hasta que se referencia.

## Dependencies

- `PasswordEncoder` bean ya existe (login-local change). Ninguna externa.

## Success Criteria

- [ ] POST /api/usuarios con datos válidos crea usuario LOCAL con password hasheado BCrypt, estado=true
- [ ] POST /api/usuarios con password < 8 caracteres devuelve 400
- [ ] POST /api/usuarios con password > 72 caracteres devuelve 400
- [ ] POST /api/usuarios con correo existente (case-insensitive) devuelve 409 Conflict
- [ ] POST /api/usuarios con rol inexistente devuelve 404
- [ ] GET /api/usuarios devuelve lista + nunca incluye password
- [ ] PATCH /api/usuarios/{id}/estado desactiva/activa correctamente
- [ ] PATCH donde ADMIN se desactiva a sí mismo devuelve 400
- [ ] PATCH que desactivaría al único ADMIN activo devuelve 400
- [ ] Endpoints rechazan peticiones sin rol ADMIN (403)
- [ ] GET /api/usuarios/me existente sigue funcionando sin cambios
- [ ] Tests unitarios cubren crear, listar, cambiarEstado, + todos los edge cases
