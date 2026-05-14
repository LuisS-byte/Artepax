# common — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Código compartido transversal: configuración global, auditoría, jobs programados, manejo de excepciones y aspectos AOP.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Configuración del sistema, auditoría, jobs, cross-cutting concerns |
| Controllers | `ConfiguracionController` (`/api/configuracion`), `AuditoriaController` (`/api/auditoria`) |
| Services | `ConfiguracionService`, `AuditoriaService` |
| Jobs | `JobCancelacionesPendientes`, `JobCotizacionesVencidas`, `JobRecordatorioEvento`, `JobSaldoPendiente` |
| Tipo de módulo | Infraestructura + Configuración + Auditoría |

## 2. Endpoints

### ConfiguracionController (`/api/configuracion`)

Todos requieren `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Notas |
|---|---|---|---|---|---|
| GET | `/api/configuracion` | Listar todas | — | `List<ConfiguracionResponseDTO>` | |
| GET | `/api/configuracion/{clave}` | Obtener una | — | `ConfiguracionResponseDTO` | 404 si no existe |
| PUT | `/api/configuracion/{clave}` | Actualizar valor | `String` (raw body) | `ConfiguracionResponseDTO` | Limpia comillas del body |

### AuditoriaController (`/api/auditoria`)

Todos requieren `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Notas |
|---|---|---|---|---|---|
| GET | `/api/auditoria?pagina=0&tamanio=50` | Todos paginado | Query params | `List<AuditoriaResponseDTO>` | Máx 100 por página. Orden fecha DESC |
| GET | `/api/auditoria/tabla/{tabla}` | Por tabla | — | `List<AuditoriaResponseDTO>` | Ej: `tabla=evento` |
| GET | `/api/auditoria/tabla/{tabla}/registro/{id}` | Por registro | — | `List<AuditoriaResponseDTO>` | Ej: `tabla=evento/registro/5` |

## 3. DTOs

#### ConfiguracionResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Notas |
|---|---|---|---|---|
| configClave | String | string | No | Ej: `PORCENTAJE_ANTICIPO_MINIMO` |
| configValor | String | string | No | Valor como string (el frontend parsea según `configTipo`) |
| configTipo | String | string | No | INT, DECIMAL, BOOLEAN, STRING, JSON |
| configDescripcion | String | string | Sí | |

#### AuditoriaResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Notas |
|---|---|---|---|---|
| id | Long | number | No | |
| accion | String | string | No | CREAR, EDITAR, ELIMINAR, PROGRAMAR, CONFIRMAR, etc. |
| tablaAfectada | String | string | No | evento, pago, producto, etc. |
| idRegistro | Long | number | Sí | ID del registro afectado |
| descripcion | String | string | Sí | |
| fecha | LocalDateTime | ISO datetime | No | |

## 4. Claves de configuración existentes

| Clave | Tipo | Uso |
|---|---|---|
| `PORCENTAJE_ANTICIPO_MINIMO` | DECIMAL | Mínimo para confirmar evento |
| `TIEMPO_GRACIA_CANCELACION_MINUTOS` | INT | Minutos para revertir cancelación |
| `POLITICA_CANCELACION_MONTAJE` | STRING | Política comercial |
| `PORCENTAJE_STOCK_MINIMO_GLOBAL` | DECIMAL | Fallback de alerta |
| `DIAS_EXPIRACION_COTIZACION` | INT | Días hasta vencimiento |
| `NEGOCIO_NOMBRE` | STRING | Nombre en documentos |
| `NEGOCIO_CORREO` | STRING | Remitente de emails |
| `NEGOCIO_TELEFONO` | STRING | Teléfono en documentos |
| `DIAS_AVISO_SALDO_PENDIENTE` | INT | Días antes para aviso |
| `DIAS_RECORDATORIO_EVENTO` | INT | Días antes para recordatorio |

## 5. PUT /api/configuracion/{clave}

- **Request body:** String plano (no JSON). Ej: `"30"` o `"Mi Negocio"`.
- El controller limpia comillas: `.replace("\"", "")`.
- El service valida el tipo según `configTipo` y hace el parseo correspondiente.
- **Errores:** `ConfiguracionNotFoundException` (404), `IllegalArgumentException` (400) si el valor no es válido para el tipo.

## 6. Reglas para frontend

- Para actualizar configuración, enviar el valor como string plano en el body (no `{"valor": "..."}`).
- Los tipos de configuración (`configTipo`) indican cómo interpretar el valor: INT, DECIMAL, BOOLEAN, STRING, JSON.
- La auditoría es solo lectura. No se crea desde frontend.
- Los jobs son automáticos — no tienen endpoints.

---

## Resumen para frontend

- Configuración: GET listar, GET por clave, PUT actualizar (body string plano).
- Auditoría: GET paginado, GET por tabla, GET por tabla+registro. Solo lectura.
- Claves de configuración en mayúscula (CHECK en DB).
- Los 4 jobs son background — no requieren acción del frontend.
