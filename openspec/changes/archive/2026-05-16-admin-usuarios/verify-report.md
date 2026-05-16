# Verify Report: admin-usuarios

## Resultado

**PASS_WITH_MANUAL_VERIFICATION_PENDING**

Razón: el backend está implementado y cubierto por tests (205 tests, 0 failures), pero falta la prueba real con PostgreSQL + frontend + filtro JWT.

---

## Validado

### Estructural

| Componente | Estado |
|------------|--------|
| DTOs: UsuarioCreateRequest, UsuarioEstadoRequest, UsuarioResponseDTO | ✅ Creados |
| UsuarioResponseDTO sin password | ✅ |
| RolRepository (JpaRepository<Rol, Integer>) | ✅ Creado |
| UsuarioRepository: existsByCorreoIgnoreCase, countActivosByRolNombre | ✅ Agregados |

### Reglas de negocio (Service)

| Regla | Estado |
|-------|--------|
| Creación de usuario LOCAL | ✅ |
| Password se guarda con BCrypt, no plano | ✅ |
| Correo único case-insensitive → 400 | ✅ |
| Rol inexistente → 400 | ✅ |
| Listar usuarios sin exponer password | ✅ |
| Autodesactivación bloqueada → 400 | ✅ |
| Desactivación del último ADMIN activo bloqueada → 400 | ✅ |
| Usuario nuevo forzado como LOCAL | ✅ |
| Usuario nuevo forzado como activo | ✅ |

### Endpoints (Controller)

| Endpoint | Estado |
|----------|--------|
| POST /api/usuarios → 201 | ✅ |
| GET /api/usuarios → 200 | ✅ |
| PATCH /api/usuarios/{id}/estado → 200 | ✅ |
| GET /api/usuarios/me preservado | ✅ |
| @PreAuthorize("hasRole('ADMIN')") en endpoints nuevos | ✅ |
| @Valid → 400 en body inválido | ✅ |

### No modificados (intactos)

| Componente | Estado |
|------------|--------|
| GoogleAuthService | ✅ Intacto |
| JwtService | ✅ Intacto |
| JwtAuthenticationFilter | ✅ Intacto |
| SecurityConfig | ✅ Intacto |
| GET /api/usuarios/me | ✅ Sin cambios |

### Tests

| Suite | Resultado |
|-------|-----------|
| Tests nuevos (PR 1-3) | 28/28 ✅ |
| Suite completa aplicable | 205/205 ✅ |
| ArtepaxApplicationTests | Pre-existing failure (requiere PostgreSQL) |

---

## Minor Deviation Accepted

**ResponseStatusException** usado en UsuarioService en lugar de ArtepaxException porque ArtepaxException es abstracta (no instanciable directamente). Comportamiento HTTP esperado preservado (400/404 según el caso).

---

## Pendiente

Verificación manual con PostgreSQL y frontend.

### Checklist manual para cerrar

1. **Login con admin existente:**
   `POST /auth/login` → 200 + JWT

2. **Crear usuario:**
   `POST /api/usuarios` con token ADMIN
   → 201 + UsuarioResponseDTO sin password
   → usu_tipo_auth = LOCAL
   → estado = true
   → password BCrypt en DB

3. **Login con nuevo usuario:**
   `POST /auth/login` → 200 + JWT

4. **Verificar token nuevo:**
   `GET /api/usuarios/me` → 200 + id, nombre, correo, telefono, rol

5. **Listar usuarios:**
   `GET /api/usuarios` → 200, sin password expuesto

6. **Cambiar estado:**
   `PATCH /api/usuarios/{id}/estado` → 200

7. **Negativos:**
   - correo duplicado → 400
   - rol inexistente → 400
   - autodesactivación → 400
   - último ADMIN activo → 400
   - sin ROLE_ADMIN → 403

8. **Smoke test:**
   `POST /auth/google` sigue funcionando igual

---

## Estado final

| Aspecto | Valor |
|---------|-------|
| Implementación completa | ✅ Sí |
| Tests verdes | ✅ 205/205 |
| Verify parcial | ✅ PASS_WITH_MANUAL_VERIFICATION_PENDING |
| Archive | ❌ No |
| Bloqueante | Verificación manual con DB real + frontend |
