# Auditoría — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El módulo de Auditoría conserva trazabilidad de acciones relevantes del sistema: creación, edición, cancelaciones, confirmaciones, finalizaciones, pagos y cambios en clientes. Expone endpoints de consulta para que el frontend administrativo pueda revisar historial general, por tabla o por registro.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Registrar y consultar acciones relevantes del sistema. |
| Controller principal | `AuditoriaController` |
| Service principal | `AuditoriaService` |
| Entidades principales | `Auditoria` |
| Tablas/vistas implicadas | `auditoria` |
| Módulos relacionados | Evento, Pago, Cliente, Documento, Inventario, Usuario/Auth |
| Tipo de módulo | Auditoría / Reporte / Trazabilidad |

El frontend consume este módulo como solo lectura. La creación de auditoría ocurre desde servicios o aspectos, no desde un endpoint público de frontend.

## 2. Controllers y endpoints completos

Base path: `/api/auditoria`

Autorización declarada: `@PreAuthorize("hasRole('ADMIN')")` en el controller.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| GET | `/api/auditoria` | Obtener auditoría general paginada | Query params `pagina`, `tamanio` | `List<AuditoriaResponseDTO>` | Repository + mapper directo | ADMIN | Ordena por fecha DESC; limita tamaño máximo a 100. |
| GET | `/api/auditoria/tabla/{tabla}` | Obtener auditoría por tabla | Path `tabla` | `List<AuditoriaResponseDTO>` | Repository + mapper directo | ADMIN | Orden fecha DESC. |
| GET | `/api/auditoria/tabla/{tabla}/registro/{id}` | Obtener auditoría por tabla y registro | Path `tabla`, `id` | `List<AuditoriaResponseDTO>` | Repository + mapper directo | ADMIN | Útil para historial de detalle. |

### GET `/api/auditoria`

- Propósito: consultar registros generales de auditoría.
- Path variables: no aplica.
- Query params:
  - `pagina`: default `0`.
  - `tamanio`: default `50`, máximo efectivo `100`.
- Request body: no aplica.
- Response: lista de `AuditoriaResponseDTO`.
- HTTP esperado: `200 OK`.
- Método ejecutado: `auditoriaRepository.findAll(pageable)`.
- Validaciones principales: límite seguro de tamaño.
- Excepciones posibles: error genérico si falla consulta.
- Efectos secundarios: ninguno.
- Qué debe refrescar el frontend: tabla/listado de auditoría.

### GET `/api/auditoria/tabla/{tabla}`

- Propósito: consultar historial de una tabla concreta.
- Path variables: `tabla`.
- Query params: no aplica.
- Request body: no aplica.
- Response: lista de `AuditoriaResponseDTO`.
- HTTP esperado: `200 OK`.
- Método ejecutado: `findByTablaAfectadaOrderByFechaDesc(tabla)`.
- Validaciones principales: ninguna validación de catálogo de tabla en controller.
- Excepciones posibles: error genérico si falla consulta.
- Efectos secundarios: ninguno.
- Qué debe refrescar el frontend: listado filtrado por tabla.

### GET `/api/auditoria/tabla/{tabla}/registro/{id}`

- Propósito: consultar historial de un registro concreto.
- Path variables: `tabla`, `id`.
- Query params: no aplica.
- Request body: no aplica.
- Response: lista de `AuditoriaResponseDTO`.
- HTTP esperado: `200 OK`.
- Método ejecutado: `findByIdRegistroAndTablaAfectadaOrderByFechaDesc(id, tabla)`.
- Validaciones principales: ninguna validación de existencia del registro origen.
- Excepciones posibles: error genérico si falla consulta.
- Efectos secundarios: ninguno.
- Qué debe refrescar el frontend: historial del registro.

## 3. DTOs request/response

### 3.1 DTOs Request

No existen DTOs de request. El módulo solo expone consultas por query params y path variables.

### 3.2 DTOs Response

#### `AuditoriaResponseDTO`

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | number | No | `Auditoria.id` | ID del registro de auditoría. |
| `usuarioId` | `Long` | number | Sí | `Auditoria.usuario.idUsuario` | Puede ser null si no se asoció usuario. |
| `accion` | `String` | string | No | `Auditoria.accion` | Acción ejecutada. |
| `tablaAfectada` | `String` | string | No | `Auditoria.tablaAfectada` | Tabla o dominio afectado. |
| `idRegistro` | `Long` | number | Sí | `Auditoria.idRegistro` | ID del registro afectado. |
| `valorAnterior` | `String` | string JSON | Sí | `Auditoria.valorAnterior` | Snapshot JSON serializado. |
| `valorNuevo` | `String` | string JSON | Sí | `Auditoria.valorNuevo` | Snapshot JSON serializado. |
| `descripcion` | `String` | string | Sí | `Auditoria.descripcion` | Mensaje humano. |
| `fecha` | `LocalDateTime` | string datetime | No | `Auditoria.fecha` | Orden principal DESC. |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Auditoria` | `auditoria` | Tabla | Registro histórico de acciones. | Guarda snapshots JSON. |
| `Usuario` | `usuario` | Relación | Usuario que ejecutó acción. | Puede ser null. |

Campos importantes de `Auditoria`:

- `valorAnterior` y `valorNuevo` se guardan como JSON mediante `@JdbcTypeCode(SqlTypes.JSON)`.
- `fecha` se asigna con `@CreationTimestamp`.
- `usuario` se intenta resolver desde `SecurityContextHolder` por correo.

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Registrar auditoría con strings | `registrar(accion, tabla, id, valorAnterior, valorNuevo, descripcion)` | Nada crítico; captura errores internamente | Inserta en `auditoria` | `void` |
| Registrar auditoría con objetos | `registrar(accion, tabla, id, Object anterior, Object nuevo, descripcion)` | Serialización JSON | Inserta en `auditoria` | `void` |
| Consultar general | Controller usa repository | Paginación | Nada | Lista DTO |
| Consultar por tabla | Controller usa repository | No valida catálogo | Nada | Lista DTO |
| Consultar por registro | Controller usa repository | No valida existencia origen | Nada | Lista DTO |

### Flujo: registrar auditoría

1. Se crea una entidad `Auditoria`.
2. Se asignan `accion`, `tablaAfectada`, `idRegistro`, snapshots y descripción.
3. Se obtiene el correo del usuario autenticado desde `SecurityContextHolder`.
4. Se busca el usuario por correo.
5. Si existe, se asocia a la auditoría.
6. Se guarda el registro.
7. Si ocurre error, se captura y se registra en log; no se relanza.

### Flujo: auditoría automática por aspecto

`AuditoriaAspect` registra acciones de forma automática sobre métodos concretos:

| Módulo | Método interceptado | Acción registrada | Tabla |
|---|---|---|---|
| Evento | `crearEvento` | `CREAR` | `evento` |
| Evento | `actualizarEvento` | `EDITAR` | `evento` |
| Evento | `iniciarCancelacion` | `CANCELAR` | `evento` |
| Evento | `revertirCancelacion` | `EDITAR` | `evento` |
| Evento | `finalizarEvento` | `FINALIZAR` | `evento` |
| Pago | `registrarPago` | `CREAR` | `pago` |
| Pago | `confirmarPago` | `CONFIRMAR` | `pago` |
| Pago | `anularPago` | `ANULAR` | `pago` |
| Cliente | `crearCliente` | `CREAR` | `cliente` |
| Cliente | `actualizar` | `EDITAR` | `cliente` |

Adicionalmente, algunos services registran auditoría manualmente usando `AuditoriaService`, por ejemplo eventos en programación, confirmación o extras según se observó en el módulo Evento.

## 6. Estados y transiciones

No aplica como estados propios del módulo. La auditoría registra cambios de otros módulos.

### Acciones conocidas

| Acción | Significado | Visible en frontend | Notas |
|---|---|---|---|
| `CREAR` | Alta de registro | Sí | Usada en evento, pago, cliente. |
| `EDITAR` | Modificación | Sí | Usada en evento/cliente. |
| `PROGRAMAR` | Programar evento | Sí | Constante disponible. |
| `CONFIRMAR` | Confirmar pago/evento | Sí | Usada en pago y evento. |
| `CANCELAR` | Cancelación | Sí | Usada en evento. |
| `REVERTIR_CANCELACION` | Reversión | Sí | Constante disponible. |
| `FINALIZAR` | Finalización | Sí | Evento finalizado. |
| `AGREGAR_EXTRAS` | Extras de evento | Sí | Constante disponible. |
| `ANULAR` | Anulación de pago | Sí | Pago pendiente anulado. |
| `AJUSTAR_STOCK` | Ajuste de stock | Sí | Constante disponible. |
| `GENERAR_DOCUMENTO` | Documento generado | Sí | Constante disponible. |
| `REENVIAR_DOCUMENTO` | Documento reenviado | Sí | Constante disponible. |

## 7. Excepciones y respuestas de error

El registro de auditoría desde `AuditoriaService` captura excepciones internas y no las relanza. Por tanto, un fallo de auditoría no debería romper la operación principal.

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `AccessDeniedException` | Usuario sin rol ADMIN consulta auditoría | 403 | Mensaje de seguridad | Mostrar sin permisos. |
| `Exception` | Error inesperado consultando auditoría | 500 | `Error interno del servidor` | Mostrar error genérico. |

Formato estándar de error:

```json
{
  "codigo": 500,
  "mensaje": "Error interno del servidor",
  "errorCode": null,
  "timestamp": "..."
}
```

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| `AuditoriaRepository` | Repository | Controller/Service | Consultar y guardar auditoría | Bajo |
| `AuditoriaMapper` | Mapper | Controller | Convertir entidad a DTO | Bajo |
| `UsuarioRepository` | Repository de Auth | `AuditoriaService` | Asociar usuario actual por correo | Medio si el principal no es correo |
| `SecurityContextHolder` | Seguridad | `AuditoriaService` | Obtener usuario autenticado | Medio |
| `ObjectMapper` | Serialización | Service/Aspect | Crear snapshots JSON | Bajo |
| `EventoRepository` | Repository externo | `AuditoriaAspect` | Capturar estado anterior de evento | Medio |
| `ClienteRepository` | Repository externo | `AuditoriaAspect` | Capturar estado anterior de cliente | Medio |
| `PagoRepository` | Repository externo | `AuditoriaAspect` | Capturar estado anterior de pago | Medio |

```text
Dependencia circular:
- Detectada: No como ciclo de service directo.
- Detalle: Auditoría depende de repositories de otros módulos para snapshots en el aspecto, pero otros módulos solo consumen AuditoriaService.
- Riesgo: Medio. El aspecto agrega acoplamiento transversal con Evento, Cliente y Pago.
- Cómo se evita o cómo debería evitarse: Mantener AuditoriaService como destino de escritura y evitar que módulos dependan del controller de auditoría.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Crear evento | Registro de auditoría | Auditoría | Historial si está visible |
| Actualizar evento | Registro con valor anterior y nuevo | Auditoría | Historial de evento |
| Iniciar cancelación | Registro de cancelación | Auditoría | Historial de evento |
| Revertir cancelación | Registro de edición | Auditoría | Historial de evento |
| Finalizar evento | Registro de finalización | Auditoría | Historial de evento |
| Registrar pago | Registro de creación de pago | Auditoría | Historial de pago/evento |
| Confirmar pago | Registro con estado anterior | Auditoría | Historial de pago/evento |
| Anular pago | Registro de anulación | Auditoría | Historial de pago/evento |
| Crear/actualizar cliente | Registro de cliente | Auditoría | Historial de cliente |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Ver auditoría general | Usuario ADMIN | Sin permiso | No tienes permisos para ver auditoría. |
| Ver auditoría por tabla | Usuario ADMIN | Sin permiso | No tienes permisos para consultar esta auditoría. |
| Ver auditoría por registro | Usuario ADMIN | Sin permiso | No tienes permisos para consultar este historial. |
| Crear/editar auditoría manual | Nunca | Siempre | La auditoría se genera automáticamente por backend. |

### 10.2 Campos editables

No hay campos editables desde frontend. El módulo es solo lectura.

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Acción mutante en evento/pago/cliente/documento/inventario | Historial si el panel de auditoría está abierto |
| Abrir detalle de registro | Consultar `/api/auditoria/tabla/{tabla}/registro/{id}` |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `CREAR` | Registro creado | Badge de creación |
| `EDITAR` | Registro editado | Badge de edición |
| `CONFIRMAR` | Confirmación | Badge de confirmación |
| `CANCELAR` | Cancelación | Badge de cancelación |
| `FINALIZAR` | Finalización | Badge de cierre |
| `ANULAR` | Anulación | Badge de anulación |
| `AJUSTAR_STOCK` | Ajuste inventario | Badge inventario |

### 10.5 Reglas importantes

- No enviar requests para crear auditoría desde frontend.
- No asumir que `usuarioId` siempre existe.
- `valorAnterior` y `valorNuevo` son strings JSON; el frontend puede mostrarlos formateados si puede parsearlos.
- Las consultas por tabla no validan catálogo; usar nombres de tabla reales (`evento`, `pago`, `cliente`, etc.).
- El listado general usa paginación básica, pero retorna lista, no objeto Page completo.
- El tamaño máximo efectivo es 100 aunque se mande más.

## Resumen para frontend

- Auditoría es solo lectura y solo ADMIN.
- No existe endpoint para crear auditoría manual.
- El listado general acepta `pagina` y `tamanio`.
- `tamanio` se limita internamente a 100.
- Hay consulta por tabla y por tabla + registro.
- Los snapshots llegan como strings JSON.
- `usuarioId` puede ser null.
- Las acciones deben mostrarse como badges funcionales.
- Un fallo interno al registrar auditoría no debería romper la operación principal.
- Para historial de detalle, usar `/api/auditoria/tabla/{tabla}/registro/{id}`.
