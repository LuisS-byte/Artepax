# Common — Jobs y Manejo Global de Errores

## 1. Responsabilidad del módulo

Este documento cubre la parte transversal de `common` que no corresponde estrictamente a Configuración o Auditoría: jobs programados y manejo global de excepciones. Su impacto para frontend es indirecto: los jobs generan notificaciones o cambios de estado, y el `GlobalExceptionHandler` define el formato estándar de error que consumen todas las pantallas.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Ejecutar procesos programados y normalizar errores API. |
| Controller principal | No aplica |
| Service principal | No aplica como módulo propio; usa servicios de Evento, Configuración y Notificación |
| Entidades principales | No aplica |
| Tablas/vistas implicadas | `evento`, `vista_eventos_periodo`, `notificacion`, `configuracion` |
| Módulos relacionados | Evento, Notificación, Configuración, Pago |
| Tipo de módulo | Soporte transversal / Jobs / Errores |

## 2. Controllers y endpoints completos

No hay endpoints propios para jobs ni para el manejo global de errores.

El formato de errores aplica a todos los controllers del sistema mediante `@RestControllerAdvice`.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| NO APLICA | NO APLICA | Los jobs se ejecutan por scheduler | NO APLICA | NO APLICA | Services internos | NO APLICA | No son invocados por frontend. |

## 3. DTOs request/response

### 3.1 DTOs Request

No aplica. Los jobs no reciben request de frontend.

### 3.2 DTOs Response

#### `ErrorResponse`

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `codigo` | `int` | number | No | HTTP status | Código HTTP numérico. |
| `mensaje` | `String` | string | No | Excepción | Mensaje a mostrar o interpretar. |
| `errorCode` | `String` | string | Sí | `ArtepaxException.errorCode` | Muchas excepciones pueden enviarlo null. |
| `timestamp` | `String` | string ISO local datetime | No | `LocalDateTime.now()` | Fecha/hora del error. |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Evento` | `evento` | Tabla | Procesar cancelaciones y cotizaciones vencidas. | Jobs consultan eventos. |
| `VistaEventosPeriodo` | `vista_eventos_periodo` | Vista | Buscar eventos por rango y saldos. | Usada por recordatorio y saldo pendiente. |
| `Configuracion` | `configuracion` | Tabla | Parámetros de jobs. | Días de aviso y tiempo de gracia. |
| `Notificacion` | `notificacion` | Tabla | Notificaciones generadas por jobs. | Creación delegada a `NotificacionService`. |

## 5. Services y lógica de negocio

| Flujo | Método del job/service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Cancelaciones pendientes | `JobCancelacionesPendientes.procesarCancelacionesPendientes()` | Tiempo de gracia vencido | Evento pasa a cancelado vía `EventoService` | `void` |
| Cotizaciones vencidas | `JobCotizacionesVencidas.procesarCotizacionesVencidas()` | `fechaExpiracion < hoy` | Crea notificación | `void` |
| Recordatorio evento | `JobRecordatorioEvento.procesarRecordatorios()` | Fecha objetivo y estado activo | Crea notificación | `void` |
| Saldo pendiente | `JobSaldoPendiente.procesarSaldosPendientes()` | Saldo > 0 y estado activo | Crea notificación | `void` |
| Manejo de errores | `GlobalExceptionHandler` | Tipo de excepción | Normaliza response | `ErrorResponse` |

### Flujo: JobCancelacionesPendientes

1. Lee `TIEMPO_GRACIA_CANCELACION_MINUTOS` desde configuración.
2. Calcula límite con `LocalDateTime.now().minusMinutes(minutosGracia)`.
3. Consulta eventos en `CANCELACION_PENDIENTE`.
4. Si `fechaCancelacion` es anterior al límite, llama `eventoService.confirmarCancelacion(evento.getId())`.
5. Si un evento falla, registra error en log y continúa con el siguiente.
6. Frecuencia: `fixedDelayString = ${artepax.job.cancelacion.intervalo-ms:60000}`.

### Flujo: JobCotizacionesVencidas

1. Corre diariamente a las 8:00 AM.
2. Consulta cotizaciones vencidas con `fechaExpiracion < hoy`.
3. No cambia estado del evento.
4. Crea notificación `COTIZACION_VENCIDA` para administradores.

### Flujo: JobRecordatorioEvento

1. Corre diariamente a las 8:00 AM.
2. Lee `DIAS_RECORDATORIO_EVENTO`.
3. Calcula una fecha objetivo.
4. Consulta `vista_eventos_periodo` para esa fecha.
5. Ignora estados `CANCELADO`, `FINALIZADO`, `CANCELACION_PENDIENTE`.
6. Crea notificación `RECORDATORIO_EVENTO`.

### Flujo: JobSaldoPendiente

1. Corre diariamente a las 8:00 AM.
2. Lee `DIAS_AVISO_SALDO_PENDIENTE`.
3. Consulta eventos desde hoy hasta la fecha límite.
4. Si `saldoPendiente > 0` y el estado está activo, crea notificación `SALDO_PENDIENTE`.
5. Ignora estados `CANCELADO`, `FINALIZADO`, `CANCELACION_PENDIENTE`.

### Flujo: manejo global de errores

1. Si se lanza `ArtepaxException`, usa su `HttpStatus` y `errorCode`.
2. Si hay validación DTO (`MethodArgumentNotValidException`), devuelve el primer error de campo con 400.
3. Si hay `AccessDeniedException`, devuelve 403.
4. Si hay `DataIntegrityViolationException`, devuelve 409. Si detecta `uk_documento_evento_tipo`, devuelve mensaje específico.
5. Si hay excepción no manejada, registra stack trace y devuelve 500 con mensaje genérico.

## 6. Estados y transiciones

Este documento no define estados propios, pero los jobs respetan estados de Evento.

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `CANCELACION_PENDIENTE` | Evento esperando tiempo de gracia | Job cancelaciones | Sí | Puede pasar a `CANCELADO`. |
| `CANCELADO` | Evento cancelado | Jobs lo excluyen | Sí | Estado terminal. |
| `FINALIZADO` | Evento terminado | Jobs lo excluyen | Sí | Estado terminal operativo. |
| Estados activos | Eventos próximos | Recordatorio/saldo | Sí | Excluye cancelados/finalizados/cancelación pendiente. |

### Transiciones relevantes

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| `CANCELACION_PENDIENTE` | `CANCELADO` | Job llama `EventoService.confirmarCancelacion()` | Tiempo de gracia expirado | Pagos, stock, cronograma, notificación según módulo Evento |

## 7. Excepciones y respuestas de error

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `ArtepaxException` | Errores de dominio | Status de la excepción | `ex.getMessage()` | Mostrar mensaje de negocio. |
| `IllegalArgumentException` | Argumento inválido | 400 | `ex.getMessage()` | Corregir datos. |
| `IllegalStateException` | Estado interno inconsistente | 422 | `ex.getMessage()` | Reportar inconsistencia. |
| `MethodArgumentNotValidException` | Validación `@Valid` | 400 | Primer campo inválido | Mostrar error de campo. |
| `ResponseStatusException` | Error con status explícito | Status de excepción | `ex.getReason()` | Mostrar mensaje. |
| `AccessDeniedException` | Permiso insuficiente | 403 | `ex.getMessage()` | Redirigir o mostrar sin permisos. |
| `DataIntegrityViolationException` | Violación de constraint | 409 | Mensaje genérico o específico de documento duplicado | Mostrar conflicto. |
| `Exception` | Error no manejado | 500 | `Error interno del servidor` | Mostrar mensaje genérico. |

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| `EventoService` | Service | `JobCancelacionesPendientes` | Confirmar cancelación | Medio; efectos transaccionales amplios |
| `EventoRepository` | Repository | Jobs | Consultar eventos | Bajo |
| `VistaEventosPeriodoRepository` | Vista repository | Recordatorio y saldo pendiente | Consulta por rango | Medio si la vista cambia |
| `ConfiguracionService` | Service | Jobs | Leer parámetros | Medio si faltan claves |
| `NotificacionService` | Service | Jobs | Crear notificaciones admin | Medio si notificación falla |
| `GlobalExceptionHandler` | Advice global | Todos los controllers | Normalizar errores | Bajo |

```text
Dependencia circular:
- Detectada: No en jobs.
- Detalle: Jobs consumen services/repositories de otros módulos.
- Riesgo: Medio por acoplamiento operativo con Evento y Notificación.
- Cómo se evita o cómo debería evitarse: Mantener jobs como orquestadores simples y no poner reglas de negocio complejas fuera de services.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Job confirma cancelación | Evento pasa a `CANCELADO`; pagos/stock/cronograma pueden cambiar | Evento, Pago, Stock, Cronograma, Notificación | Listado/detalle de evento, pagos, cronograma, notificaciones |
| Job cotización vencida | Crea notificación | Notificación | Notificaciones |
| Job recordatorio | Crea notificación | Notificación | Notificaciones |
| Job saldo pendiente | Crea notificación | Notificación | Notificaciones |
| Error global | Normaliza respuesta | Todos los módulos | Mostrar mensaje apropiado |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Ejecutar jobs manualmente | No aplica | Siempre | Los jobs no tienen endpoint público. |
| Mostrar notificaciones generadas por jobs | Usuario autenticado con acceso | Sin permiso | No tienes permisos para ver notificaciones. |
| Mostrar error de validación | Cuando `codigo=400` | No aplica | Usar `mensaje` del backend. |
| Mostrar conflicto | Cuando `codigo=409` | No aplica | Usar `mensaje` del backend. |

### 10.2 Campos editables

No aplica. Los jobs no tienen formularios. Para errores, el frontend solo consume `ErrorResponse`.

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Jobs programados ejecutan en background | Notificaciones, dashboard, listados si el usuario está en la pantalla afectada |
| Error en request | No refrescar automáticamente salvo que la acción haya sido parcialmente visible; mostrar error |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `codigo=400` | Error de validación o negocio | Mensaje de formulario |
| `codigo=403` | Sin permisos | Pantalla de acceso denegado |
| `codigo=404` | Recurso no encontrado | Mensaje de no encontrado |
| `codigo=409` | Conflicto de datos | Conflicto/duplicado |
| `codigo=422` | Estado interno inválido | Error operativo |
| `codigo=500` | Error no manejado | Mensaje genérico |

### 10.5 Reglas importantes

- El frontend no invoca jobs directamente.
- Las pantallas deben tolerar cambios generados en background, sobre todo cancelaciones automáticas y notificaciones.
- Usar siempre `ErrorResponse.mensaje` para errores de negocio.
- No exponer stack traces al usuario; el backend solo devuelve mensaje genérico en 500.
- Si una acción falla con 409 de documento duplicado, mostrar el mensaje específico del backend.

## Resumen para frontend

- Los jobs no tienen endpoints públicos.
- Los jobs generan notificaciones y pueden cambiar eventos por cancelación automática.
- Cancelaciones automáticas ocurren según `TIEMPO_GRACIA_CANCELACION_MINUTOS`.
- Recordatorios y saldos pendientes corren diariamente a las 8:00 AM.
- El frontend debe refrescar notificaciones periódicamente o bajo acción del usuario.
- Todos los errores siguen `ErrorResponse`.
- El primer error de validación DTO se devuelve como `campo: mensaje`.
- 409 puede indicar duplicado o integridad de datos.
- 500 siempre devuelve `Error interno del servidor`.
- No asumir que el estado mostrado está congelado; puede cambiar por job.
