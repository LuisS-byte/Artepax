# Cliente — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El módulo Cliente administra el catálogo operativo de clientes de Artepax. Permite crear, consultar, actualizar, desactivar, reactivar, buscar clientes activos y consultar el historial de eventos asociados a un cliente.

No realiza borrado físico. La eliminación es lógica mediante `cli_activo = false`.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Gestión CRUD administrativa de clientes y consulta de eventos por cliente |
| Controller principal | `ClienteController` |
| Service principal | `ClienteService` / `ClienteServiceImpl` |
| Entidades principales | `Cliente` |
| Tablas/vistas implicadas | `cliente`; consulta indirecta a `evento` para historial |
| Módulos relacionados | Evento |
| Tipo de módulo | CRUD operativo |

## 2. Controllers y endpoints completos

Base path: `/api/clientes`

Seguridad: el controller completo está protegido con `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/clientes` | Crear cliente | `ClienteRequestDTO` | `ClienteResponseDTO` | `crearCliente` | ADMIN | Retorna 201 |
| GET | `/api/clientes` | Listar clientes activos | No aplica | `List<ClienteResponseDTO>` | `obtenerTodos` | ADMIN | No retorna inactivos |
| GET | `/api/clientes/{id}` | Obtener cliente por ID | No aplica | `ClienteResponseDTO` | `obtenerPorId` | ADMIN | Puede retornar 404 |
| PUT | `/api/clientes/{id}` | Actualizar cliente | `ClienteUpdateDTO` | `ClienteResponseDTO` | `actualizar` | ADMIN | Requiere nombre y teléfono |
| DELETE | `/api/clientes/{id}` | Desactivar cliente | No aplica | `Void` | `eliminar` | ADMIN | Soft delete; retorna 204 |
| GET | `/api/clientes/{id}/eventos` | Historial de eventos por cliente | No aplica | `List<EventoResponseDTO>` | `obtenerEventosPorCliente` | ADMIN | Depende de módulo Evento |
| GET | `/api/clientes/buscar?nombre=...` | Buscar clientes activos por nombre | Query param `nombre` | `List<ClienteResponseDTO>` | `buscarPorNombre` | ADMIN | Búsqueda case-insensitive parcial |
| GET | `/api/clientes/inactivos` | Listar clientes inactivos | No aplica | `List<ClienteResponseDTO>` | `obtenerInactivos` | ADMIN | Para administración |
| PATCH | `/api/clientes/{id}/reactivar` | Reactivar cliente | No aplica | `ClienteResponseDTO` | `reactivar` | ADMIN | Cambia `activo` a true |

### POST `/api/clientes`

- Propósito: crear un cliente activo.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: `ClienteRequestDTO`.
- Response: `ClienteResponseDTO`.
- HTTP esperado: `201 Created`.
- Método del service: `clienteService.crearCliente(requestDTO)`.
- Validaciones principales: `@Valid` sobre el DTO; correo único en service.
- Excepciones posibles: validación DTO `400`; correo duplicado `400`; error genérico `500`.
- Efectos secundarios: crea fila en `cliente` con `activo = true`.
- Qué debe refrescar el frontend: listado de clientes, selector de clientes en creación de eventos.

### GET `/api/clientes`

- Propósito: listar clientes activos.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<ClienteResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `clienteService.obtenerTodos()`.
- Validaciones principales: no aplica.
- Excepciones posibles: error genérico `500`.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend: no aplica; se consume para tabla/listado y selectores.

### GET `/api/clientes/{id}`

- Propósito: obtener detalle de un cliente.
- Path variables: `id`.
- Query params: no aplica.
- Request body: no aplica.
- Response: `ClienteResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `clienteService.obtenerPorId(id)`.
- Validaciones principales: existencia del cliente.
- Excepciones posibles: `ClienteNotFoundException` `404`.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend: detalle del cliente si se modifica desde otra pantalla.

### PUT `/api/clientes/{id}`

- Propósito: actualizar datos del cliente.
- Path variables: `id`.
- Query params: no aplica.
- Request body: `ClienteUpdateDTO`.
- Response: `ClienteResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `clienteService.actualizar(id, updateDTO)`.
- Validaciones principales: `@Valid`; existencia del cliente; correo único si cambia.
- Excepciones posibles: validación DTO `400`; correo duplicado `400`; cliente no encontrado `404`.
- Efectos secundarios: actualiza fila en `cliente`.
- Qué debe refrescar el frontend: detalle del cliente, listado de clientes, selectores si muestran nombre/correo actualizado.

### DELETE `/api/clientes/{id}`

- Propósito: desactivar cliente.
- Path variables: `id`.
- Query params: no aplica.
- Request body: no aplica.
- Response: sin cuerpo.
- HTTP esperado: `204 No Content`.
- Método del service: `clienteService.eliminar(id)`.
- Validaciones principales: existencia del cliente.
- Excepciones posibles: cliente no encontrado `404`.
- Efectos secundarios: `cliente.activo = false`; no borra físicamente.
- Qué debe refrescar el frontend: listado de clientes activos, listado de inactivos, selectores de clientes.

### GET `/api/clientes/{id}/eventos`

- Propósito: obtener eventos asociados a un cliente.
- Path variables: `id`.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<EventoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `clienteService.obtenerEventosPorCliente(clienteId)`.
- Validaciones principales: existencia del cliente.
- Excepciones posibles: cliente no encontrado `404`.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend: historial de eventos del cliente.

### GET `/api/clientes/buscar?nombre=...`

- Propósito: buscar clientes activos por coincidencia parcial de nombre.
- Path variables: no aplica.
- Query params: `nombre` requerido.
- Request body: no aplica.
- Response: `List<ClienteResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `clienteService.buscarPorNombre(nombre)`.
- Validaciones principales: no hay validación explícita de nombre vacío.
- Excepciones posibles: error genérico `500`.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend: resultados de búsqueda.

### GET `/api/clientes/inactivos`

- Propósito: listar clientes desactivados.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<ClienteResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `clienteService.obtenerInactivos()`.
- Validaciones principales: no aplica.
- Excepciones posibles: error genérico `500`.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend: listado de clientes inactivos.

### PATCH `/api/clientes/{id}/reactivar`

- Propósito: reactivar cliente desactivado.
- Path variables: `id`.
- Query params: no aplica.
- Request body: no aplica.
- Response: `ClienteResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `clienteService.reactivar(id)`.
- Validaciones principales: existencia del cliente.
- Excepciones posibles: cliente no encontrado `404`.
- Efectos secundarios: `cliente.activo = true`.
- Qué debe refrescar el frontend: listados de activos e inactivos, selectores de clientes.

## 3. DTOs request/response

### 3.1 DTOs Request

#### ClienteRequestDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `nombre` | `String` | `nombre` | Sí | No | `@NotBlank` | Campo obligatorio para crear |
| `telefono` | `String` | `telefono` | Sí | No | `@NotBlank` | Campo obligatorio para crear |
| `correo` | `String` | `correo` | Sí | No | `@NotBlank`, `@Email` | Debe ser único según service |
| `direccion` | `String` | `direccion` | No | Sí | No tiene validación explícita | Opcional |

#### ClienteUpdateDTO

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `nombre` | `String` | `nombre` | Sí | No | `@NotBlank` | El update exige nombre |
| `telefono` | `String` | `telefono` | Sí | No | `@NotBlank` | El update exige teléfono |
| `correo` | `String` | `correo` | No | Sí | `@Email` | Si cambia, debe ser único |
| `direccion` | `String` | `direccion` | No | Sí | No tiene validación explícita | Opcional |

Nota: aunque `correo` no tiene `@NotBlank` en update, el service asigna directamente `cliente.setCorreo(dto.getCorreo())`. El frontend debe decidir si permite limpiar correo; el backend actual no lo bloquea explícitamente.

### 3.2 DTOs Response

#### ClienteResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `Cliente.id` | Identificador principal |
| `nombre` | `String` | `nombre` | No | `Cliente.nombre` | Mostrar en tablas/selectores |
| `telefono` | `String` | `telefono` | Sí | `Cliente.telefono` | Puede venir vacío/null según datos |
| `correo` | `String` | `correo` | Sí | `Cliente.correo` | Usar para contacto/documentos |
| `direccion` | `String` | `direccion` | Sí | `Cliente.direccion` | Opcional |
| `activo` | `Boolean` | `activo` | No | `Cliente.activo` | Controla badges y acciones |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Cliente` | `cliente` | Tabla | Datos de clientes | Usa soft delete con `cli_activo` |
| `Evento` | `evento` | Tabla externa | Historial de eventos por cliente | Se accede mediante `EventoRepository.findByClienteId` |

### Cliente

Campos principales:

| Campo Java | Columna SQL | Notas |
|---|---|---|
| `id` | `id_cliente` | PK autogenerada |
| `nombre` | `cli_nombre` | Obligatorio |
| `telefono` | `cli_telefono` | Longitud 20 |
| `correo` | `cli_correo` | Longitud 100 |
| `direccion` | `cli_direccion` | Longitud 255 |
| `activo` | `cli_activo` | Soft delete |
| `fechaCreacion` | `cli_fechacreacion` | `@CreationTimestamp`, no se expone en DTO actual |

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Crear cliente | `crearCliente` | Correo no duplicado | Inserta cliente | `ClienteResponseDTO` |
| Listar activos | `obtenerTodos` | No aplica | No modifica | Lista de `ClienteResponseDTO` |
| Listar inactivos | `obtenerInactivos` | No aplica | No modifica | Lista de `ClienteResponseDTO` |
| Obtener por ID | `obtenerPorId` | Existencia | No modifica | `ClienteResponseDTO` |
| Actualizar | `actualizar` | Existencia y correo único si cambia | Actualiza datos | `ClienteResponseDTO` |
| Desactivar | `eliminar` | Existencia | `activo=false` | `void` |
| Reactivar | `reactivar` | Existencia | `activo=true` | `ClienteResponseDTO` |
| Buscar por nombre | `buscarPorNombre` | No aplica | No modifica | Lista de `ClienteResponseDTO` |
| Eventos por cliente | `obtenerEventosPorCliente` | Existencia del cliente | No modifica | Lista de `EventoResponseDTO` |

### Flujo: crear cliente

1. Valida DTO con `@Valid` desde controller.
2. Verifica `clienteRepository.existsByCorreo(dto.getCorreo())`.
3. Si el correo existe, lanza `IllegalArgumentException("El correo ya está en uso")`.
4. Mapea DTO a entidad.
5. Guarda en `cliente`.
6. Retorna `ClienteResponseDTO`.

### Flujo: actualizar cliente

1. Busca cliente por ID.
2. Si no existe, lanza `ClienteNotFoundException`.
3. Si el correo cambia y ya existe, lanza `IllegalArgumentException`.
4. Actualiza nombre, teléfono, dirección y correo.
5. Guarda entidad.
6. Retorna `ClienteResponseDTO`.

### Flujo: eliminar cliente

1. Busca cliente por ID.
2. Si no existe, lanza `ClienteNotFoundException`.
3. Cambia `activo` a `false`.
4. Guarda entidad.
5. No elimina físicamente.

### Flujo: reactivar cliente

1. Busca cliente por ID.
2. Si no existe, lanza `ClienteNotFoundException`.
3. Cambia `activo` a `true`.
4. Guarda entidad.
5. Retorna `ClienteResponseDTO`.

### Flujo: obtener eventos por cliente

1. Verifica que el cliente exista con `existsById`.
2. Si no existe, lanza `ClienteNotFoundException`.
3. Consulta `eventoRepository.findByClienteId(clienteId)`.
4. Mapea cada evento con `EventoMapper.toResponseDTO`.
5. Retorna lista de `EventoResponseDTO`.

## 6. Estados y transiciones

El módulo Cliente no maneja una máquina de estados compleja. Maneja un estado booleano operativo: `activo`.

### Estados existentes

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `activo = true` | Cliente disponible para operación | Listado principal, creación de eventos | Sí | `GET /api/clientes` solo devuelve activos |
| `activo = false` | Cliente desactivado | Listado de inactivos | Sí | No se elimina de BD |

### Transiciones permitidas

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| `true` | `false` | `DELETE /api/clientes/{id}` | Cliente existe | Sale del listado activo |
| `false` | `true` | `PATCH /api/clientes/{id}/reactivar` | Cliente existe | Regresa al listado activo |

Estados terminales: no aplica.

## 7. Excepciones y respuestas de error

El sistema usa `ErrorResponse` con esta estructura:

| Campo | Tipo | Descripción |
|---|---|---|
| `codigo` | `int` | Código HTTP |
| `mensaje` | `String` | Mensaje visible |
| `errorCode` | `String` | Puede ser `null` |
| `timestamp` | `String` | Fecha/hora ISO local |

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `ClienteNotFoundException` | Cliente no existe | `404` | `Cliente no encontrado con id: {id}` | Mostrar error y volver al listado |
| `IllegalArgumentException` | Correo duplicado | `400` | `El correo ya está en uso` | Mostrar error en campo correo |
| `MethodArgumentNotValidException` | Fallan anotaciones DTO | `400` | `{campo}: {mensaje}` | Mostrar validación de formulario |
| `AccessDeniedException` | Usuario sin rol ADMIN | `403` | Mensaje de acceso denegado | Redirigir o mostrar prohibido |
| `Exception` | Error no manejado | `500` | `Error interno del servidor` | Mostrar error genérico |

Notas:

- `ClienteNotFoundException` extiende `ArtepaxException`, por lo que `GlobalExceptionHandler` respeta el `HttpStatus` definido por la excepción.
- Los errores de validación del DTO retornan el primer error de campo encontrado.
- El correo duplicado usa `IllegalArgumentException`, no una excepción de dominio custom.

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| Evento | Repository | `ClienteServiceImpl.obtenerEventosPorCliente` | Consultar eventos del cliente | Acoplamiento funcional con historial |
| Evento | Mapper | `ClienteServiceImpl.obtenerEventosPorCliente` | Convertir eventos a `EventoResponseDTO` | Si cambia DTO de evento, impacta este endpoint |
| Common | Exception handling | Global | Formato de errores | Bajo |
| Security | `@PreAuthorize` | Controller | Restringir a ADMIN | Bajo |

Dependencia circular:

```text
Dependencia circular:
- Detectada: No en este módulo.
- Detalle: ClienteServiceImpl depende de EventoRepository y EventoMapper para consultar historial, pero no se observa dependencia directa de ClienteService desde EventoService en este paquete.
- Riesgo: Bajo.
- Cómo se evita o cómo debería evitarse: mantener el acceso a eventos solo para lectura histórica; no mover lógica de eventos al módulo cliente.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Crear cliente | Inserta cliente activo | Cliente | Listado de clientes, selectores |
| Actualizar cliente | Modifica datos del cliente | Cliente / Evento visualmente | Detalle, listado, historial si muestra nombre |
| Desactivar cliente | Cambia `activo=false` | Cliente | Listado activos, inactivos, selectores |
| Reactivar cliente | Cambia `activo=true` | Cliente | Listado activos, inactivos, selectores |
| Obtener eventos por cliente | No modifica datos | Evento | No aplica |

No se detectan notificaciones, auditoría explícita ni jobs en este módulo según el ZIP recibido.

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Crear cliente | Formulario válido | Falta nombre/teléfono/correo o correo inválido | Complete los campos obligatorios |
| Editar cliente | Cliente existe | Cliente no encontrado | Cliente no disponible |
| Desactivar cliente | Cliente activo | Cliente ya inactivo o no encontrado | Cliente no disponible |
| Reactivar cliente | Cliente inactivo | Cliente ya activo o no encontrado | Cliente no disponible |
| Ver historial de eventos | Cliente existe | Cliente no encontrado | No se pudo cargar el historial |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `nombre` | Crear/editar | No aplica | Obligatorio |
| `telefono` | Crear/editar | No aplica | Obligatorio |
| `correo` | Crear/editar | Si duplica otro correo | Crear exige correo; update permite null a nivel DTO |
| `direccion` | Crear/editar | No aplica | Opcional |
| `activo` | Solo mediante desactivar/reactivar | No editar directo desde formulario general | Usar endpoints dedicados |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Crear cliente | Listado activo, selector de clientes |
| Actualizar cliente | Listado, detalle, historial si muestra nombre/correo |
| Desactivar cliente | Listado activo, listado inactivo, selector de clientes |
| Reactivar cliente | Listado activo, listado inactivo, selector de clientes |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `activo=true` | Cliente activo | Badge “Activo”; disponible en selectores |
| `activo=false` | Cliente inactivo | Badge “Inactivo”; mostrar acción “Reactivar” |

### 10.5 Reglas importantes

- El frontend no debe ejecutar borrado físico; `DELETE` desactiva el cliente.
- `GET /api/clientes` devuelve solo clientes activos.
- Para administrar clientes desactivados se debe usar `GET /api/clientes/inactivos`.
- Para reactivar se debe usar `PATCH /api/clientes/{id}/reactivar`.
- El correo debe tratarse como único; si el backend devuelve `El correo ya está en uso`, mostrarlo junto al campo correo.
- El endpoint de historial retorna `EventoResponseDTO`; si cambia el contrato de Evento, puede cambiar la UI del historial.
- No asumir que `fechaCreacion` está disponible en `ClienteResponseDTO`; la entidad la tiene, pero el DTO no la expone.
