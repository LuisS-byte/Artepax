# local-auth Specification

## Purpose

Autenticación local con email + contraseña para usuarios tipo `LOCAL`, devolviendo JWT compatible con el flujo existente. Los usuarios tipo `GOOGLE` no pueden usar este endpoint. El login Google existente (POST /auth/google) no se modifica.

## Requirements

### Requirement: Login Input Validation

The system MUST validate login requests and reject malformed input with HTTP 400 Bad Request.

#### Scenario: Missing email

- GIVEN a login request with blank or null email
- WHEN the request is submitted to POST /auth/login
- THEN the system returns 400 Bad Request with a field-level validation error message

#### Scenario: Invalid email format

- GIVEN a login request with a non-email string in the email field
- WHEN the request is submitted to POST /auth/login
- THEN the system returns 400 Bad Request with a field-level validation error message

#### Scenario: Missing password

- GIVEN a login request with blank or null password
- WHEN the request is submitted to POST /auth/login
- THEN the system returns 400 Bad Request with a field-level validation error message

---

### Requirement: Local User Authentication

The system MUST authenticate users with `tipoAuth = 'LOCAL'` using BCrypt password verification and issue a JWT on success. The system MUST return generic error messages to prevent user enumeration.

#### Scenario: Successful login

- GIVEN a user with `tipoAuth = 'LOCAL'`, `estado = true`, role `ADMIN`, and a BCrypt-hashed password
- WHEN a valid email and correct password are submitted to the login flow
- THEN the system returns 200 OK with an `AuthResponse` containing a valid JWT token

#### Scenario: Invalid credentials (wrong password)

- GIVEN a user with `tipoAuth = 'LOCAL'` and a BCrypt-hashed password
- WHEN an incorrect password is submitted
- THEN the system returns 401 Unauthorized with a generic error message (no indication of whether the email exists)

#### Scenario: Unknown email

- GIVEN no user exists with the submitted email and `tipoAuth = 'LOCAL'`
- WHEN the email is submitted
- THEN the system returns 401 Unauthorized with a generic error message (same as wrong password)

#### Scenario: GOOGLE user attempts local login

- GIVEN a user with `tipoAuth = 'GOOGLE'` and the submitted email
- WHEN login credentials are submitted to the local login flow
- THEN the system returns 401 Unauthorized — the `findByCorreoAndTipoAuth(correo, 'LOCAL')` query returns empty, producing the same generic error as invalid credentials

#### Scenario: Inactive user

- GIVEN a user with `tipoAuth = 'LOCAL'`, `estado = false`
- WHEN valid credentials are submitted
- THEN the system returns 403 Forbidden with a message indicating the user is inactive

#### Scenario: User without ADMIN role

- GIVEN a user with `tipoAuth = 'LOCAL'`, `estado = true`, but a non-ADMIN role (or null role)
- WHEN valid credentials are submitted
- THEN the system returns 403 Forbidden with a message indicating insufficient permissions

#### Scenario: Corrupted or non-BCrypt password hash

- GIVEN a user whose `usu_password` field contains a value that is not a valid BCrypt hash (e.g., `'GOOGLE_AUTH'`, plaintext, truncated hash, or corrupted data)
- WHEN login is attempted
- THEN `BCryptPasswordEncoder.matches()` detects the invalid hash format and returns `false` without throwing an exception; the system returns 401 Unauthorized with the same generic `"Credenciales inválidas"` message — no 500 error, no internal detail exposed

---

### Requirement: Local Login Endpoint

The system MUST expose `POST /auth/login` as a public endpoint (no authentication required), consistent with the existing `POST /auth/google` pattern.

#### Scenario: Endpoint accepts unauthenticated requests

- GIVEN the security configuration permits `/auth/**` without authentication
- WHEN an unauthenticated client sends a POST request to `/auth/login`
- THEN the request is processed by `LocalAuthService` without triggering a 401 from the security filter chain

#### Scenario: Valid request body triggers authentication

- GIVEN a `LoginRequest` with `@Valid` annotation on the controller method parameter
- WHEN a well-formed JSON body is posted to `/auth/login`
- THEN Spring's `MethodArgumentNotValidException` handler intercepts validation failures and returns structured error responses

---

### Requirement: JWT Compatibility With Existing Protected Endpoints

The JWT issued by local login MUST be functionally identical to the JWT issued by Google login — it MUST authorize access to all protected endpoints that already accept Google-issued tokens.

#### Scenario: Local JWT works with GET /api/usuarios/me

- GIVEN a successful local login that returns a JWT
- WHEN that JWT is used to call `GET /api/usuarios/me`
- THEN the response is 200 OK with the same user schema as a Google-issued token: `id`, `nombre`, `correo`, `telefono`, `rol`

#### Scenario: Local JWT rejected for unauthorized endpoints

- GIVEN a JWT from local login for a user with `ROLE_ADMIN`
- WHEN accessing an endpoint guarded by `@PreAuthorize("hasRole('ADMIN')")`
- THEN the request succeeds (JWT contains the same role information as Google-issued tokens)

---

### Requirement: Google Login Compatibility

The existing Google login flow (POST /auth/google) MUST continue to function without any behavioral changes. The GoogleAuthService class MUST NOT be modified.

#### Scenario: Google login unchanged after local login addition

- GIVEN the AuthController now has both `/auth/google` and `/auth/login` endpoints
- WHEN a valid Google ID token is submitted to `POST /auth/google`
- THEN the response is identical to before the change — 200 OK with JWT for authorized ADMIN users, same error responses for unauthorized/inactive users

---

### Requirement: Seed Data Integrity

The database seed/dump MUST have consistent `usu_tipo_auth` values.

#### Scenario: GOOGLE users have correct tipo_auth

- GIVEN a user authenticated via Google whose `usu_password` contains a non-BCrypt placeholder (e.g., `'GOOGLE_AUTH'`)
- WHEN the seed is reviewed and corrected
- THEN `usu_tipo_auth` is `'GOOGLE'` — the classification matches the authentication method (e.g., `ernestomendoza587@gmail.com` corrected from LOCAL to GOOGLE)

#### Scenario: LOCAL users have BCrypt-hashed password

- GIVEN a seed user intended for local authentication (`usu_tipo_auth = 'LOCAL'`)
- WHEN the seed is generated or corrected
- THEN the `usu_password` field contains a valid BCrypt hash (generated via `BCryptPasswordEncoder.encode()`) — not plaintext, not a placeholder

---

### Requirement: Unit Test Coverage

Unit tests for the local authentication service MUST cover all authentication flows using JUnit 5 and Mockito, mocking the repository, password encoder, and JWT service.

#### Scenario: Test coverage completeness

- GIVEN `LocalAuthServiceTest` in the test source tree
- WHEN the test suite runs
- THEN it covers: successful login, wrong password, unknown email, GOOGLE user attempt, inactive user, and non-ADMIN user (6 scenarios minimum)

---

### Requirement: Auth API Contract Documentation

The frontend-facing API contract MUST document `POST /auth/login` including the request body schema, success response, and all error codes with their meanings.

#### Scenario: Frontend can integrate with documented contract

- GIVEN the auth API contract is updated
- WHEN a frontend developer reads it
- THEN they find: endpoint path, HTTP method, request body fields (`email`, `password`), success response shape (`{ "token": "..." }`), and all error codes (400, 401, 403) with conditions
