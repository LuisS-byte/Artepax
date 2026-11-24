# Proposal: Login Local (email + password) — v2 (ajustada)

## Intent

Sistema actualmente solo soporta autenticación Google OAuth2. Se necesita login local con email y contraseña para que usuarios tipo `LOCAL` puedan autenticarse sin depender de Google. DB ya tiene `usu_password` y `usu_tipo_auth` listos — falta implementación del lado backend. **No existe infraestructura de password local**: sin PasswordEncoder, sin BCrypt, sin AuthenticationManager. Todo debe construirse.

## Scope

### In Scope (8 deliverables)

1. **LoginRequestDTO** — DTO con `@Email` + `@NotBlank`, validación Bean Validation.
2. **PasswordEncoder BCrypt bean** — `@Bean` en configuración de seguridad.
3. **UsuarioRepository.findByCorreoAndTipoAuth(correo, tipoAuth)** — query filtrada crítica; impide que usuarios GOOGLE se autentiquen vía login local.
4. **LocalAuthService** — valida credenciales, filtra por `tipoAuth = 'LOCAL'`, verifica estado activo y rol ADMIN, valida password con BCrypt, genera JWT. Mismo patrón que `GoogleAuthService`, sin tocarlo.
5. **POST /auth/login en AuthController** — endpoint público (ya cubierto por `SecurityConfig` que permite `/auth/**`).
6. **Tests unitarios de LocalAuthService** — JUnit 5 + Mockito, cobertura de flujos feliz, credenciales inválidas, usuario GOOGLE, usuario inactivo, sin rol ADMIN.
7. **Corrección de seed/dump** — `usuario2@example.com` tiene `usu_password = 'GOOGLE_AUTH'` con `usu_tipo_auth = 'LOCAL'` (inconsistente). Debe corregirse a `usu_tipo_auth = 'GOOGLE'`. Además, seed de usuarios LOCALES con password BCrypt hasheado.
8. **Actualización de contrato Auth y matriz frontend** — documentar el nuevo endpoint, request/response, códigos de error, y actualizar la matriz de endpoints disponibles en el frontend.

### Out of Scope

- Endpoint de creación de usuarios locales — se pospone para otro cambio.
- Refactor de `GoogleAuthService` — no se toca (solo se agrega wiring en el controller).
- Enriquecer `AuthResponse` con datos del usuario — se puede hacer después.

## Capabilities

### New Capabilities

- `local-auth`: Autenticación local con email y contraseña vía BCrypt, devolviendo JWT compatible con el mismo flujo existente.

### Modified Capabilities

- None

## Approach

**Approach: Servicios separados.** Crear `LocalAuthService` sin tocar `GoogleAuthService`. El patrón de `GoogleAuthService` (validar → buscar usuario → validar estado → validar rol → generar JWT) se replica en `LocalAuthService`.

### Implementación mínima

**LoginRequest DTO:**
```java
@Data
public class LoginRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;
}
```

**PasswordEncoder bean:**
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**Repository query:**
```java
Optional<Usuario> findByCorreoAndTipoAuth(String correo, String tipoAuth);
```

**LocalAuthService:**
```java
@Service
@RequiredArgsConstructor
public class LocalAuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository
            .findByCorreoAndTipoAuth(request.getEmail(), "LOCAL")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario inactivo");
        }

        if (usuario.getRol() == null || !"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        return AuthResponse.builder()
            .token(jwtService.generateToken(usuario))
            .build();
    }
}
```

**AuthController (agregar endpoint):**
```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final GoogleAuthService googleAuthService;
    private final LocalAuthService localAuthService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.authenticateWithGoogle(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginLocal(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(localAuthService.login(request));
    }
}
```

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `auth/controller/AuthController.java` | Modified | Agregar `@PostMapping("/login")` e inyectar `LocalAuthService` |
| `auth/dto/LoginRequest.java` | **New** | DTO con email + password, Bean Validation |
| `auth/service/LocalAuthService.java` | **New** | Servicio de autenticación local |
| `security/SecurityConfig.java` | Modified | Agregar `@Bean PasswordEncoder` |
| `user/repository/UsuarioRepository.java` | Modified | Agregar `findByCorreoAndTipoAuth()` |
| `auth/service/LocalAuthServiceTest.java` | **New** | Tests unitarios (JUnit 5 + Mockito) |
| DB seed/ArtepaxMARCELO.sql | Modified | Corregir `usu_tipo_auth` de ernestomendoza + seed LOCAL con BCrypt |
| Frontend auth contract | **New/modified** | Documentar `POST /auth/login` y matriz de endpoints |

## Risks

**Risk Level: Bajo-Medio**

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Usuarios GOOGLE intentan login local | Low | Low | `LocalAuthService` usa `findByCorreoAndTipoAuth("LOCAL")` — usuarios GOOGLE no matchean |
| Contraseñas legacy sin hashear (ej: `GOOGLE_AUTH` en campo password) | Medium | Low | BCrypt.matches falla gracefulmente, error genérico "Credenciales inválidas". No crashea. |
| Data inconsistency: `usuario2@example.com` con `usu_tipo_auth = 'LOCAL'` pero `usu_password = 'GOOGLE_AUTH'` | Medium | Low | Corregido en seed/dump: `usu_tipo_auth = 'GOOGLE'`. Incluido en deliverable 7. |
| Código no tiene infraestructura de password local (sin BCrypt, sin PasswordEncoder, sin AuthenticationManager) | — | — | No es riesgo, es estado actual. Los deliverables 2, 3, 4 construyen esta infraestructura desde cero. |
| Duplicación mínima con GoogleAuthService | Low | Low | ~8 líneas compartidas; no justifica abstracción. Servicios separados es más claro. |

## Rollback Plan

Revertir commit. O en caliente: quitar `@PostMapping("/login")` del controller y remover el bean `PasswordEncoder` — el login Google sigue funcionando sin cambios.

## Dependencies

- Ninguna externa. `spring-security-crypto` ya viene con Spring Boot Starter Security (`BCryptPasswordEncoder` incluido).

## Success Criteria

- [ ] `POST /auth/login` con credenciales LOCALES válidas devuelve JWT 200 OK.
- [ ] `POST /auth/login` con usuario GOOGLE devuelve 401 Unauthorized (mensaje genérico).
- [ ] `POST /auth/login` con contraseña incorrecta devuelve 401 Unauthorized.
- [ ] `POST /auth/login` con usuario inactivo devuelve 403 Forbidden.
- [ ] `POST /auth/login` sin rol ADMIN devuelve 403 Forbidden.
- [ ] Login Google existente sigue funcionando (no se tocó `GoogleAuthService`).
- [ ] Tests unitarios de `LocalAuthService` cubren todos los flujos (happy path + 4 errores).
- [ ] `usuario2@example.com` tiene `usu_tipo_auth = 'GOOGLE'` en el dump/seed.
