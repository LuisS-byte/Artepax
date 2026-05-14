# Configuración — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El módulo de Configuración administra parámetros globales editables por administración. Estos parámetros son consumidos por otros servicios y jobs para definir reglas operativas como porcentaje mínimo de anticipo, días de expiración de cotizaciones, tiempo de gracia de cancelación, días de aviso de saldo pendiente y recordatorios.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Exponer y actualizar parámetros globales del sistema. |
| Controller principal | `ConfiguracionController` |
| Service principal | `ConfiguracionService`, `ConfiguracionServiceImpl` |
| Entidades principales | `Configuracion` |
| Tablas/vistas implicadas | `configuracion` |
| Módulos relacionados | Evento, Pago, Documento, Jobs, Notificación |
| Tipo de módulo | Configuración / Catálogo operativo |

El frontend debe tratar este módulo como configuración administrativa. No debe recalcular reglas críticas con valores hardcodeados si el backend ya usa una clave configurable.

## 2. Controllers y endpoints completos

Base path: `/api/configuracion`

Autorización declarada: `@PreAuthorize("hasRole('ADMIN')")` en el controller.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| GET | `/api/configuracion` | Listar configuraciones | No aplica | `List<ConfiguracionResponseDTO>` | `configuracionService.listar()` | ADMIN | Retorna todas las claves. |
| GET | `/api/configuracion/{clave}` | Obtener configuración por clave | Path variable `clave` | `ConfiguracionResponseDTO` | `configuracionService.obtener(clave)` | ADMIN | Lanza 404 si no existe. |
| PUT | `/api/configuracion/{clave}` | Actualizar valor de configuración | Body string plano | `ConfiguracionResponseDTO` | `configuracionService.actualizar(clave, valor)` | ADMIN | El controller hace `trim()` y remueve comillas dobles. |

### GET `/api/configuracion`

- Propósito: listar todas las configuraciones globales.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: no aplica.
- Response: lista de `ConfiguracionResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `listar()`.
- Validaciones principales: ninguna a nivel controller.
- Excepciones posibles: error genérico si falla la consulta.
- Efectos secundarios: ninguno.
- Qué debe refrescar el frontend: vista/listado de configuración.

### GET `/api/configuracion/{clave}`

- Propósito: obtener una clave específica.
- Path variables: `clave`.
- Query params: no aplica.
- Request body: no aplica.
- Response: `ConfiguracionResponseDTO`.
- HTTP esperado: `200 OK`; `404 NOT_FOUND` si la clave no existe.
- Método del service: `obtener(clave)`.
- Validaciones principales: existencia de la clave.
- Excepciones posibles: `ConfiguracionNotFoundException`.
- Efectos secundarios: ninguno.
- Qué debe refrescar el frontend: detalle de configuración si aplica.

### PUT `/api/configuracion/{clave}`

- Propósito: actualizar el valor de una clave.
- Path variables: `clave`.
- Query params: no aplica.
- Request body: string plano; ejemplo: `"30"`, `30`, `true`.
- Response: `ConfiguracionResponseDTO` actualizado.
- HTTP esperado: `200 OK`; `400 BAD_REQUEST` si el valor no coincide con el tipo; `404 NOT_FOUND` si la clave no existe.
- Método del service: `actualizar(clave, valor)`.
- Validaciones principales: existencia de la clave y casteo según `configTipo`.
- Excepciones posibles: `ConfiguracionNotFoundException`, `IllegalArgumentException`.
- Efectos secundarios: invalida caché local de esa clave.
- Qué debe refrescar el frontend: listado de configuración y cualquier pantalla que dependa del parámetro actualizado.

## 3. DTOs request/response

### 3.1 DTOs Request

No hay DTO formal de request para actualización. `PUT /api/configuracion/{clave}` recibe el valor como `String` en el body.

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| body | `String` | String plano | Sí | No | Validado contra `configTipo` en service | Enviar valor compatible con el tipo de la configuración. |

### 3.2 DTOs Response

#### `ConfiguracionResponseDTO`

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `configClave` | `String` | string | No | `Configuracion.configClave` | Identificador de la configuración. |
| `configValor` | `String` | string | No | `Configuracion.configValor` | Siempre llega como texto, aunque represente INT, DECIMAL o BOOLEAN. |
| `configTipo` | `String` | string | No | `Configuracion.configTipo` | Puede ser `INT`, `DECIMAL`, `BOOLEAN`, `STRING`, `JSON`. |
| `configDescripcion` | `String` | string | Sí | `Configuracion.configDescripcion` | Texto de ayuda para UI. |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Configuracion` | `configuracion` | Tabla | Parámetros globales editables. | La clave es la PK. El valor se guarda como texto. |

La tabla permite valores heterogéneos. El backend castea según `configTipo` en `ConfiguracionServiceImpl`.

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Leer string | `getString(clave)` | Existencia de clave | Nada | `String` |
| Leer entero | `getInt(clave)` | Existencia y formato entero | Nada | `Integer` |
| Leer decimal | `getDecimal(clave)` | Existencia y formato decimal | Nada | `BigDecimal` |
| Leer boolean | `getBoolean(clave)` | Existencia | Nada | `Boolean` |
| Listar | `listar()` | Nada | Nada | Lista DTO |
| Obtener | `obtener(clave)` | Existencia | Nada | DTO |
| Actualizar | `actualizar(clave, valor)` | Existencia y tipo | `config_valor` | DTO |
| Invalidar caché | `invalidarCache(clave)` | Nada | Caché local | `void` |

### Flujo: lectura de configuración tipada

1. El service llama a `getRaw(clave)`.
2. `getRaw()` usa caché local `ConcurrentHashMap`.
3. Si la clave no está en caché, consulta `ConfiguracionRepository.findById`.
4. Si no existe, lanza `ConfiguracionNotFoundException`.
5. Para `getInt()` y `getDecimal()`, intenta castear el valor; si falla, lanza `IllegalStateException`.

### Flujo: actualizar configuración

1. Busca la clave.
2. Valida que el nuevo valor sea compatible con `configTipo`.
3. Para `INT`, valida `Integer.parseInt`.
4. Para `DECIMAL`, valida `BigDecimal`.
5. Para `BOOLEAN`, solo acepta `true` o `false` sin distinguir mayúsculas.
6. Actualiza `configValor`.
7. Guarda la entidad.
8. Invalida la caché de esa clave.
9. Retorna DTO actualizado.

## 6. Estados y transiciones

No aplica. Este módulo no maneja estados propios.

Los valores de `configTipo` sí condicionan validación:

| Tipo | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `INT` | Entero | Service y jobs | Sí | Enviar números enteros. |
| `DECIMAL` | Decimal | Reglas financieras | Sí | Enviar decimal válido. |
| `BOOLEAN` | Booleano | Configuración binaria | Sí | Enviar `true` o `false`. |
| `STRING` | Texto | Parámetros textuales | Sí | Sin validación especial. |
| `JSON` | JSON como texto | Configuración compleja | Sí | No hay validación estructural visible en service actual. |

## 7. Excepciones y respuestas de error

Formato general de error:

```json
{
  "codigo": 400,
  "mensaje": "...",
  "errorCode": null,
  "timestamp": "..."
}
```

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `ConfiguracionNotFoundException` | Clave inexistente | 404 | `Configuración no encontrada: {clave}` | Mostrar error y no permitir edición. |
| `IllegalArgumentException` | Valor inválido al actualizar según tipo | 400 | Mensaje específico del tipo inválido | Mantener modal/form abierto y corregir valor. |
| `IllegalStateException` | Valor almacenado no puede castearse al tipo solicitado por backend | 422 | `Configuración '{clave}' no es un ... válido` | Reportar como inconsistencia interna. |
| `AccessDeniedException` | Usuario sin rol ADMIN | 403 | Mensaje de seguridad | Redirigir o mostrar sin permisos. |
| `Exception` | Error no manejado | 500 | `Error interno del servidor` | Mostrar mensaje genérico. |

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| `ConfiguracionRepository` | Repository | `ConfiguracionServiceImpl` | Leer y actualizar claves | Bajo |
| Evento | Consumidor | `EventoServiceImpl`, jobs | Expiración, gap, cancelación, recordatorios | Cambios de claves afectan reglas de evento |
| Pago | Consumidor indirecto | `EventoPagoValidator` | Anticipo mínimo | Cambios de porcentaje afectan confirmación |
| Documento/Comunicación | Consumidor indirecto | Render/mensajes | Datos del negocio | Medio si faltan claves |
| Jobs | Consumidor | Jobs de common | Días de aviso y tiempo de gracia | Config inválida puede romper jobs |

```text
Dependencia circular:
- Detectada: No.
- Detalle: Configuración es módulo proveedor; otros servicios la consumen.
- Riesgo: Bajo. El riesgo principal es dato inválido o clave faltante.
- Cómo se evita o cómo debería evitarse: Mantener seed/migraciones de configuración sincronizadas y validar tipo al editar.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Actualizar clave | Invalida caché local de configuración | Configuración / módulos consumidores | Listado de configuración; pantallas afectadas por esa regla |
| Cambiar `PORCENTAJE_ANTICIPO_MINIMO` | Afecta confirmación de eventos | Evento/Pago | Detalle de evento y pagos si se recalcula en UI |
| Cambiar `TIEMPO_GRACIA_CANCELACION_MINUTOS` | Afecta job de cancelación pendiente | Evento/Jobs | No hay refresco inmediato obligatorio |
| Cambiar días de aviso | Afecta jobs de notificación | Notificación/Jobs | No hay refresco inmediato obligatorio |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Ver configuración | Usuario ADMIN | Sin permiso | No tienes permisos para ver configuración. |
| Editar configuración | Usuario ADMIN y clave existente | Sin permiso o clave inexistente | No se puede editar esta configuración. |
| Guardar valor | El valor coincide con `configTipo` | Valor inválido | El valor no coincide con el tipo esperado. |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `configValor` | Siempre que el usuario tenga permiso | Sin permiso | Validar según `configTipo`. |
| `configClave` | No editable | Siempre | Es la PK. |
| `configTipo` | No editable desde este endpoint | Siempre | Cambiar tipo requeriría migración o endpoint adicional. |
| `configDescripcion` | No editable desde este endpoint | Siempre | Solo se actualiza valor. |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Actualizar configuración | Listado de configuración y detalle de la clave |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `INT` | Valor entero | Mostrar input numérico entero. |
| `DECIMAL` | Valor decimal | Mostrar input numérico decimal. |
| `BOOLEAN` | Booleano | Mostrar switch/select true/false. |
| `STRING` | Texto | Mostrar input texto. |
| `JSON` | JSON como texto | Mostrar textarea/campo avanzado. |

### 10.5 Reglas importantes

- No enviar objetos JSON complejos al `PUT` salvo que la configuración sea de tipo `JSON` y el backend lo espere como texto.
- No permitir editar clave ni tipo desde este contrato.
- Mostrar el mensaje del backend cuando el valor no coincida con el tipo.
- Después de guardar, refrescar la clave para confirmar el valor normalizado.

## Resumen para frontend

- El módulo es solo ADMIN.
- `GET /api/configuracion` lista todas las claves.
- `PUT /api/configuracion/{clave}` recibe un string plano, no un DTO JSON estructurado.
- `configValor` siempre llega como texto.
- `configTipo` define cómo debe validarse el input.
- El backend valida `INT`, `DECIMAL` y `BOOLEAN`.
- Actualizar una clave invalida caché del backend.
- Cambiar valores puede afectar reglas de Evento, Pago y Jobs.
- El error estándar usa `codigo`, `mensaje`, `errorCode`, `timestamp`.
- No inventar claves en frontend; usar las devueltas por el backend.
