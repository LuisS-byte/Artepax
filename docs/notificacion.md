# notificacion — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Gestiona las notificaciones internas del sistema: generación automática desde jobs y servicios, consulta por usuario autenticado y marcado como leída.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Notificaciones internas para admins |
| Controller principal | `NotificacionController` (`/api/notificaciones`) |
| Service principal | `NotificacionService` + impl |
| Entidades principales | `Notificacion`, `TipoNotificacion`, `EstadoNotificacion` |
| Tablas/vistas implicadas | `notificacion`, `tipo_notificacion`, `estado_notificacion` |
| Módulos relacionados | `authentication/user`, `evento` |
| Tipo de módulo | Lectura + marcado |

## 2. Controllers y endpoints completos

| Método | Ruta | Acción | Request | Response | Auth | Notas |
|---|---|---|---|---|---|---|
| GET | `/api/notificaciones/mias` | Mis notificaciones | `@AuthenticationPrincipal` | `List<NotificacionResponseDTO>` | ADMIN | Del usuario autenticado |
| PATCH | `/api/notificaciones/{id}/leer` | Marcar leída | `@AuthenticationPrincipal` | `204 No Content` | ADMIN | |

### GET /api/notificaciones/mias

- **Propósito:** Obtener notificaciones del usuario autenticado.
- **Response:** `List<NotificacionResponseDTO>` con `tipoNotificacion`, `estadoNotificacion`, `eventoId`, `mensaje`, `fechaEnvio`
- **Validaciones principales:** ninguna
- **Qué debe refrescar el frontend:** NO APLICA

### PATCH /api/notificaciones/{id}/leer

- **Propósito:** Marcar una notificación como LEIDA.
- **Response:** `204 No Content`
- **Validaciones principales:** Usuario autenticado
- **Qué debe refrescar el frontend:** lista de notificaciones

## 3. DTOs

#### NotificacionResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| id | Long | number | No | entidad | |
| mensaje | String | string | No | entidad | |
| tipoNotificacion | String | string | No | entidad.tipoNotificacion | RECORDATORIO_EVENTO, ALERTA_STOCK, CONFIRMACION_RESERVA, INICIO_MONTAJE, CANCELACION_EVENTO, PAGO_RECIBIDO, COTIZACION_VENCIDA, SALDO_PENDIENTE |
| estadoNotificacion | String | string | No | entidad.estadoNotificacion | PENDIENTE, ENVIADA, FALLIDA, LEIDA |
| eventoId | Long | number | Sí | entidad | null si no asociada a evento. Usar para deep link |
| fechaEnvio | LocalDateTime | ISO datetime | No | entidad | |

## 4. Generación de notificaciones (interna)

Las notificaciones no se crean desde el frontend. Son generadas automáticamente por:

| Origen | Tipo | Cuándo |
|---|---|---|
| `PagoServiceImpl.confirmarPago` | PAGO_RECIBIDO | Al confirmar un pago |
| `EventoServiceImpl` vía `EventoNotificacionHandler` | CONFIRMACION_RESERVA, INICIO_MONTAJE, CANCELACION_EVENTO | Transiciones de estado |
| `PagoCancelacionHandler` | CANCELACION_EVENTO | Cancelación con devolución pendiente |
| `MovimientoServiceImpl` | ALERTA_STOCK | Stock bajo después de movimiento |
| `JobRecordatorioEvento` | RECORDATORIO_EVENTO | Días antes del evento |
| `JobCotizacionesVencidas` | COTIZACION_VENCIDA | Cotizaciones expiradas |
| `JobSaldoPendiente` | SALDO_PENDIENTE | Eventos con saldo pendiente |

## 5. Reglas que debe respetar el frontend

- Las notificaciones con `eventoId` no nulo permiten deep link al evento: `GET /api/eventos/{eventoId}`.
- `estadoNotificacion = LEIDA` se obtiene llamando a `PATCH /{id}/leer`.
- El frontend no crea notificaciones. Solo las consulta y las marca como leídas.
- Tipos de notificación: RECORDATORIO_EVENTO, ALERTA_STOCK, CONFIRMACION_RESERVA, INICIO_MONTAJE, CANCELACION_EVENTO, PAGO_RECIBIDO, COTIZACION_VENCIDA, SALDO_PENDIENTE.

---

## Resumen para frontend

- Solo 2 endpoints: GET `/mias` y PATCH `/{id}/leer`.
- Las notificaciones son generadas por el backend automáticamente. El frontend solo consulta y marca como leídas.
- `eventoId` (nullable) permite navegar al evento relacionado.
- Usar `@AuthenticationPrincipal` para identificar al usuario.
