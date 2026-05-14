# Notificación — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El módulo Notificación administra notificaciones internas para usuarios administrativos. Permite consultar las notificaciones del usuario autenticado y marcar una notificación propia como leída. También expone un service usado por otros módulos para generar notificaciones a todos los administradores activos.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Crear, consultar y marcar como leídas notificaciones internas. |
| Controller principal | `NotificacionController` |
| Service principal | `NotificacionService` / `NotificacionServiceImpl` |
| Entidades principales | `Notificacion`, `TipoNotificacion`, `EstadoNotificacion` |
| Tablas/vistas implicadas | `notificacion`, `tipo_notificacion`, `estado_notificacion` |
| Módulos relacionados | Auth/Usuario, Evento, Pago, Documento, Inventario, Cronograma, Jobs del sistema |
| Tipo de módulo | Operativo / Comunicación interna |

El módulo no envía correos. No crea notificaciones directamente desde el frontend; las notificaciones son generadas por servicios internos mediante `notificarAdmins(...)`.

## 2. Controllers y endpoints completos

Base path: `/api/notificaciones`

Requiere rol: `ADMIN`, por `@PreAuthorize("hasRole('ADMIN')")` en el controller.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| GET | `/api/notificaciones/mias` | Obtener mis notificaciones | No aplica | `List<NotificacionResponseDTO>` | `obtenerMias(usuario.getIdUsuario())` | ADMIN | Usa `@AuthenticationPrincipal Usuario`. |
| PATCH | `/api/notificaciones/{id}/leer` | Marcar notificación como leída | No aplica | `204 No Content` | `marcarLeida(id, usuario.getIdUsuario())` | ADMIN | Solo permite marcar notificaciones propias. |

### GET `/api/notificaciones/mias`

- Propósito: obtener las notificaciones del usuario autenticado.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<NotificacionResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `NotificacionService.obtenerMias(Long usuarioId)`.
- Validaciones principales: requiere usuario autenticado como principal.
- Excepciones posibles: `AccessDeniedException` si no tiene rol, error genérico si el principal no es compatible.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - panel/lista de notificaciones.
  - contador de notificaciones pendientes, si existe.

### PATCH `/api/notificaciones/{id}/leer`

- Propósito: marcar una notificación del usuario autenticado como leída.
- Path variables: `id` de la notificación.
- Query params: no aplica.
- Request body: no aplica.
- Response: vacío.
- HTTP esperado: `204 No Content`.
- Método del service: `NotificacionService.marcarLeida(Long notificacionId, Long idUsuarioAutenticado)`.
- Validaciones principales:
  - la notificación debe existir.
  - la notificación debe pertenecer al usuario autenticado.
  - si ya está `LEIDA`, el método retorna sin modificar.
- Excepciones posibles: `NotificacionNotFoundException`, `AccessDeniedException`, `IllegalStateException` si no existe el estado `LEIDA` en BD.
- Efectos secundarios: cambia estado de la notificación a `LEIDA`.
- Qué debe refrescar el frontend:
  - lista de notificaciones.
  - contador de pendientes/no leídas.

## 3. DTOs request/response

### 3.1 DTOs Request

No existen DTOs request para endpoints públicos del módulo.

| DTO | Uso | Notas |
|---|---|---|
| NO APLICA | Los endpoints no reciben body | `PATCH /{id}/leer` usa solo path variable. |

### 3.2 DTOs Response

#### NotificacionResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `id` | `Long` | `id` | No | `notificacion.id_notificacion` | Identificador para marcar como leída. |
| `mensaje` | `String` | `mensaje` | No | `notificacion.mensaje` | Texto principal visible. |
| `tipoNotificacion` | `String` | `tipoNotificacion` | No | `tipo_notificacion.nombre_tipo` | Usar para badge/categoría. |
| `estadoNotificacion` | `String` | `estadoNotificacion` | No | `estado_notificacion.nombre_estado` | Determina si está pendiente/leída. |
| `eventoId` | `Long` | `eventoId` | Sí | `notificacion.id_evento` | Puede ser `null`; no siempre hay evento asociado. |
| `fechaEnvio` | `LocalDateTime` | `fechaEnvio` | No | `notificacion.fecha_envio` | Orden y fecha visible. |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Notificacion` | `notificacion` | Tabla operativa | Registro de notificaciones internas por usuario. | Tiene relación con usuario, tipo, estado y evento opcional. |
| `TipoNotificacion` | `tipo_notificacion` | Catálogo | Define categorías de notificación. | Se busca por `nombreTipo`. |
| `EstadoNotificacion` | `estado_notificacion` | Catálogo | Define estados técnicos de notificación. | El service usa `PENDIENTE` y `LEIDA`. |
| `Usuario` | `usuario` | Tabla operativa | Destinatario de la notificación. | `notificarAdmins()` crea una notificación por admin activo. |
| `Evento` | `evento` | Tabla operativa | Asociación opcional para deep link o contexto. | `eventoId` puede ser `null` en response. |

No hay vistas SQL en este módulo.

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Notificar admins | `notificarAdmins(tipoNombre, mensaje, evento)` | tipo y estado `PENDIENTE` deben existir | crea notificaciones para admins activos | void |
| Obtener mías | `obtenerMias(usuarioId)` | no valida ownership porque filtra por usuario | no modifica | lista DTO |
| Marcar leída | `marcarLeida(notificacionId, idUsuarioAutenticado)` | existencia, ownership, estado `LEIDA` | cambia estado a `LEIDA` | void |

### Flujo: generar notificaciones a administradores

1. Busca `TipoNotificacion` por `tipoNombre`.
2. Busca `EstadoNotificacion` con nombre `PENDIENTE`.
3. Consulta usuarios administradores activos mediante `UsuarioRepository.findAdminsActivos()`.
4. Crea una fila en `notificacion` por cada admin activo.
5. Asocia el evento recibido, si existe.
6. Guarda cada notificación.

Este flujo es interno. No tiene endpoint público directo.

### Flujo: obtener mis notificaciones

1. Recibe el `idUsuario` desde el usuario autenticado.
2. Consulta `findByUsuarioIdUsuarioOrderByFechaEnvioDesc(usuarioId)`.
3. Convierte cada entidad a `NotificacionResponseDTO`.
4. Ordena por fecha de envío descendente desde el repository.

### Flujo: marcar notificación como leída

1. Busca la notificación por ID.
2. Si no existe, lanza `NotificacionNotFoundException`.
3. Compara `notificacion.usuario.idUsuario` contra el usuario autenticado.
4. Si no coincide, lanza `AccessDeniedException`.
5. Si ya está en estado `LEIDA`, retorna sin cambios.
6. Busca el estado `LEIDA`.
7. Cambia el estado y guarda.

## 6. Estados y transiciones

### Estados existentes

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `PENDIENTE` | Notificación creada/no leída inicialmente | `notificarAdmins()` | Sí | Estado inicial al crear. |
| `LEIDA` | Notificación marcada como leída | `marcarLeida()` | Sí | Estado final para lectura. |
| `ENVIADA` | Estado de catálogo disponible | NO VERIFICADO en service actual | Sí, si aparece en datos | El service actual no lo asigna. |
| `FALLIDA` | Estado de catálogo disponible | NO VERIFICADO en service actual | Sí, si aparece en datos | El service actual no lo asigna. |

### Transiciones permitidas

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| NO EXISTE | `PENDIENTE` | llamada interna `notificarAdmins(...)` | tipo y estado existen; hay admins activos | crea una notificación por admin. |
| `PENDIENTE` | `LEIDA` | `PATCH /api/notificaciones/{id}/leer` | ownership del usuario autenticado | actualiza estado. |
| `LEIDA` | `LEIDA` | `PATCH /api/notificaciones/{id}/leer` | ownership | operación idempotente; no cambia. |

Estados terminales: `LEIDA` en el flujo actual.

Estados que bloquean acciones: el backend no bloquea marcar una notificación ya leída; simplemente retorna sin cambios.

## 7. Excepciones y respuestas de error

Formato general de error:

```json
{
  "codigo": 400,
  "mensaje": "mensaje del error",
  "errorCode": null,
  "timestamp": "2026-05-10T03:00:00"
}
```

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `NotificacionNotFoundException` | Se intenta marcar una notificación inexistente | `404` | `Notificación no encontrada: {id}` | Mostrar error y refrescar lista. |
| `AccessDeniedException` | Usuario intenta modificar notificación de otro usuario | `403` | `No puede modificar esta notificación` | Bloquear acción y mostrar acceso denegado. |
| `IllegalStateException` | Falta catálogo `PENDIENTE` o `LEIDA` en BD | `422` | Mensaje del estado faltante | Error operativo; reportar a admin técnico. |
| `AccessDeniedException` por rol | Usuario sin rol `ADMIN` accede al controller | `403` | Mensaje de seguridad | Redirigir o mostrar sin permisos. |
| `Exception` | Error no manejado | `500` | `Error interno del servidor` | Mostrar mensaje genérico. |

Validaciones DTO: no aplica; no hay request body.

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| Auth/Usuario | Entity + Repository | `Notificacion`, `NotificacionServiceImpl` | Destinatario y búsqueda de admins activos | Medio: si cambia `Usuario` o `findAdminsActivos()`, afecta creación de notificaciones. |
| Evento | Entity | `Notificacion`, `notificarAdmins(...)` | Asociar notificación con evento opcional | Bajo: la relación puede ser `null`. |
| Seguridad Spring | `@AuthenticationPrincipal`, `@PreAuthorize` | Controller | Obtener usuario autenticado y restringir a ADMIN | Medio: depende de que el principal sea `Usuario`. |
| Common Exception | `ArtepaxException`, `GlobalExceptionHandler` | Excepciones | Formato común de errores | Bajo. |
| Módulos operativos | Service interno | Evento, Pago, Documento, Inventario, Jobs | Generan notificaciones mediante `notificarAdmins()` | Medio: si falla notificación dentro de transacciones de otros módulos puede provocar rollback. |

```text
Dependencia circular:
- Detectada: No en este módulo.
- Detalle: Notificación depende de Usuario y Evento como referencias; otros módulos dependen de NotificacionService para generar mensajes.
- Riesgo: Puede haber acoplamiento operativo si notificarAdmins() falla dentro de transacciones de módulos como Evento o Pago.
- Cómo se evita o cómo debería evitarse: Mantener NotificacionService sin dependencia de services operativos. Si se requiere best-effort, desacoplar con eventos de dominio/cola o manejo transaccional separado.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| `notificarAdmins(...)` | Crea notificaciones para todos los admins activos | Notificación / Usuario | panel de notificaciones, contador. |
| `PATCH /api/notificaciones/{id}/leer` | Cambia estado a `LEIDA` | Notificación | lista de notificaciones, contador. |
| Eventos operativos de otros módulos | Pueden crear notificaciones internas | Evento, Pago, Documento, Inventario, Cronograma, Jobs | notificaciones después de acciones mutantes. |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Ver mis notificaciones | Usuario autenticado con rol ADMIN | Sin sesión o sin rol | `No tienes permisos para ver notificaciones.` |
| Marcar como leída | Notificación pertenece al usuario autenticado | Notificación de otro usuario o inexistente | `No puedes modificar esta notificación.` |
| Abrir detalle de evento | `eventoId != null` | `eventoId == null` | `Esta notificación no está asociada a un evento.` |

### 10.2 Campos editables

No hay campos editables desde frontend. La única acción de modificación es marcar como leída.

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `estadoNotificacion` | Solo vía endpoint `PATCH /{id}/leer` | No editar directamente | El frontend no debe enviar el nuevo estado. |
| `mensaje` | NO APLICA | Siempre | Generado por backend. |
| `tipoNotificacion` | NO APLICA | Siempre | Generado por backend. |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Entrar al sistema/admin layout | `GET /api/notificaciones/mias` |
| Marcar una notificación como leída | lista de notificaciones y contador de pendientes |
| Confirmar/cancelar/finalizar evento | notificaciones |
| Confirmar pago | notificaciones |
| Generar/enviar documento | notificaciones, si el flujo genera |
| Alertas de inventario | notificaciones |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `PENDIENTE` | Notificación no leída o pendiente de atención | Mostrar como no leída/pendiente. |
| `LEIDA` | Ya fue revisada | Mostrar como leída. |
| `CONFIRMACION_RESERVA` | Confirmación de evento/reserva | Badge informativo de confirmación. |
| `INICIO_MONTAJE` | Inicio de montaje | Badge operativo. |
| `CANCELACION_EVENTO` | Cancelación iniciada o confirmada | Badge de advertencia operativa. |
| `PAGO_RECIBIDO` | Pago confirmado | Badge financiero. |
| `SALDO_PENDIENTE` | Evento con saldo pendiente | Badge financiero/alerta. |
| `ALERTA_STOCK` | Stock bajo o alerta de inventario | Badge de inventario. |
| `RECORDATORIO_EVENTO` | Recordatorio de evento próximo | Badge calendario. |
| `COTIZACION_VENCIDA` | Cotización vencida | Badge comercial. |

### 10.5 Reglas importantes

- No asumir que toda notificación tiene `eventoId`.
- No permitir navegar a detalle de evento si `eventoId` es `null`.
- No enviar estado nuevo desde frontend; el endpoint solo marca como leída.
- Mostrar `mensaje` tal como llega del backend.
- Usar `tipoNotificacion` para categorizar visualmente.
- Usar `estadoNotificacion` para diferenciar leídas/no leídas.
- Refrescar contador después de marcar como leída.
- Manejar `403` cuando el usuario intente operar una notificación que no le pertenece.
- El frontend no crea notificaciones; las genera backend por eventos de dominio.
