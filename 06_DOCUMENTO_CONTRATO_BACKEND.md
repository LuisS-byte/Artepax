# Documento — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El módulo Documento administra la generación, consulta, reenvío y gestión de plantillas de documentos asociados a eventos. Su objetivo es producir documentos HTML/PDF basados en plantillas, enviarlos por correo al cliente cuando exista destinatario, registrar el estado de envío y mantener trazabilidad mediante auditoría.

El módulo no administra eventos, pagos ni clientes directamente. Los consulta para construir el documento, validar reglas y rellenar placeholders.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Generar documentos por evento, enviar/reintentar envío por correo, listar documentos y administrar plantillas. |
| Controller principal | `DocumentoController` |
| Service principal | `DocumentoService` / `DocumentoServiceImpl` |
| Entidades principales | `Documento`, `PlantillaDocumento` |
| Tablas/vistas implicadas | `documento`, `plantilla_documento`, `evento`, `vista_pagos_evento`, `pago` |
| Módulos relacionados | Evento, Pago, Configuración, Comunicación, Auditoría |
| Tipo de módulo | Operativo / Documental / Plantillas |

Puntos relevantes para frontend:

- Todos los endpoints requieren rol `ADMIN`.
- La generación de documentos puede terminar en estado `PENDIENTE`, `ENVIADO` o `FALLIDO`.
- Si el cliente no tiene correo, el documento se genera pero queda pendiente de envío.
- Ya existe control de duplicados por `evento + tipoDocumento` en service/policy y en entidad mediante constraint único `uk_documento_evento_tipo`.
- No existe endpoint de descarga directa del PDF; el DTO expone `archivoUrl`, pero el service actual genera PDF en memoria para correo y no asigna URL.

## 2. Controllers y endpoints completos

Base path:

```text
/api/documentos
```

Autorización:

```text
@PreAuthorize("hasRole('ADMIN')")
```

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/documentos/evento/{idEvento}/generar?tipo={tipo}` | Generar documento por evento y tipo | Path `idEvento`, query `tipo` | `DocumentoResponseDTO` | `generarDocumento(idEvento, tipo)` | ADMIN | Valida estado del evento, plantilla activa, duplicado y correo si intenta envío. |
| POST | `/api/documentos/evento/{idEvento}/aviso-saldo-pendiente` | Generar aviso formal de saldo pendiente | Path `idEvento` | `DocumentoResponseDTO` | `generarAvisoSaldoPendiente(idEvento)` | ADMIN | Usa `AVISO_SALDO_PENDIENTE` y valida saldo pendiente mayor a cero. |
| GET | `/api/documentos/evento/{idEvento}` | Listar documentos de un evento | Path `idEvento` | `List<DocumentoResponseDTO>` | `listarPorEvento(idEvento)` | ADMIN | Ordena por fecha de generación descendente desde repository. |
| GET | `/api/documentos/{id}` | Obtener documento por ID | Path `id` | `DocumentoResponseDTO` | `obtenerDocumento(id)` | ADMIN | Carga relaciones de evento, cliente, estado, tipo, decoración y plantilla. |
| POST | `/api/documentos/{id}/reenviar` | Reenviar documento al cliente | Path `id` | `DocumentoResponseDTO` | `reenviarDocumento(id)` | ADMIN | Valida intentos máximos y correo del cliente. |
| GET | `/api/documentos/plantillas` | Listar plantillas | No aplica | `List<PlantillaResponseDTO>` | `listarPlantillas()` | ADMIN | Devuelve activas e inactivas. |
| GET | `/api/documentos/plantillas/{id}` | Obtener plantilla por ID | Path `id` | `PlantillaResponseDTO` | `obtenerPlantilla(id)` | ADMIN | Lanza 404 si no existe. |
| PUT | `/api/documentos/plantillas/{id}` | Actualizar plantilla | `PlantillaUpdateDTO` | `PlantillaResponseDTO` | `actualizarPlantilla(id, dto)` | ADMIN | No permite cambiar `tipoDocumento`; solo contenido, campos editables y activo. |
| GET | `/api/documentos` | Listar todos los documentos paginados | `Pageable` | `Page<DocumentoResponseDTO>` | `obtenerTodos(pageable)` | ADMIN | Default `size=50`, `sort=docuFechaGeneracion`. |

### POST `/api/documentos/evento/{idEvento}/generar?tipo={tipo}`

- Propósito: generar un documento para un evento usando la plantilla activa del tipo solicitado.
- Path variables:
  - `idEvento`: ID del evento.
- Query params:
  - `tipo`: enum `DocumentoTipo`. Valores conocidos: `CONFIRMACION_RESERVA`, `COTIZACION_EVENTO`, `SALDO_PENDIENTE`, `AVISO_SALDO_PENDIENTE`.
- Request body: no aplica.
- Response: `DocumentoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `DocumentoServiceImpl.generarDocumento(idEvento, tipoDocumento)`.
- Validaciones principales:
  - El evento debe existir usando `EventoRepository.findDocumentoContextById`.
  - El tipo debe estar permitido según estado del evento.
  - No debe existir ya un documento del mismo tipo para el mismo evento.
  - Debe existir plantilla activa del tipo solicitado.
  - La plantilla debe estar activa.
- Excepciones posibles:
  - `EventoNotFoundException` si el evento no existe.
  - `DocumentoOperacionNoPermitidaException` si el estado no permite el documento, si ya existe duplicado o si no hay plantilla activa.
  - `DataIntegrityViolationException` si se viola `uk_documento_evento_tipo` por concurrencia.
  - `PdfGenerationException` o errores de email se capturan dentro del método y devuelven documento `FALLIDO`.
- Efectos secundarios:
  - Crea fila en `documento`.
  - Genera PDF en memoria.
  - Envía email si el cliente tiene correo.
  - Actualiza `docuEstadoEnvio`, `docuIntentosEnvio`, `docuFechaEnvio`.
  - Si se envía correctamente, marca `evento.clienteNotificado = true`.
  - Registra auditoría `GENERAR_DOCUMENTO`.
- Qué debe refrescar el frontend:
  - documentos del evento.
  - detalle del evento si se muestra `clienteNotificado`.
  - auditoría si existe pantalla visible.

### POST `/api/documentos/evento/{idEvento}/aviso-saldo-pendiente`

- Propósito: generar y enviar un aviso formal de saldo pendiente al cliente.
- Path variables:
  - `idEvento`: ID del evento.
- Query params: no aplica.
- Request body: no aplica.
- Response: `DocumentoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `DocumentoServiceImpl.generarAvisoSaldoPendiente(eventoId)`.
- Validaciones principales:
  - El evento debe existir.
  - El estado del evento debe permitir `AVISO_SALDO_PENDIENTE`.
  - No debe existir ya un documento `AVISO_SALDO_PENDIENTE` para ese evento.
  - El saldo pendiente calculado debe ser mayor a cero.
  - Debe existir plantilla activa `AVISO_SALDO_PENDIENTE`.
- Excepciones posibles:
  - `EventoNotFoundException`.
  - `DocumentoOperacionNoPermitidaException` por estado inválido, duplicado, saldo cero/negativo o plantilla ausente.
  - `DataIntegrityViolationException` por constraint único.
  - Errores de PDF/email son capturados y devuelven documento `FALLIDO`.
- Efectos secundarios:
  - Crea documento.
  - Rellena placeholders específicos de saldo pendiente.
  - Genera PDF.
  - Envía mediante `ComunicacionClienteService.enviarAvisoSaldoPendienteConDocumento`.
  - Actualiza estado de envío.
  - Marca cliente notificado si el envío fue exitoso.
  - Registra auditoría.
- Qué debe refrescar el frontend:
  - documentos del evento.
  - resumen financiero si se muestra junto al aviso.
  - detalle del evento por `clienteNotificado`.

### GET `/api/documentos/evento/{idEvento}`

- Propósito: listar documentos generados para un evento.
- Path variables:
  - `idEvento`: ID del evento.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<DocumentoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `DocumentoServiceImpl.listarPorEvento(idEvento)`.
- Validaciones principales:
  - El evento debe existir.
- Excepciones posibles:
  - `EventoNotFoundException`.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - tabla/listado de documentos del evento.

### GET `/api/documentos/{id}`

- Propósito: obtener detalle de un documento.
- Path variables:
  - `id`: ID del documento.
- Query params: no aplica.
- Request body: no aplica.
- Response: `DocumentoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `DocumentoServiceImpl.obtenerDocumento(id)`.
- Validaciones principales:
  - El documento debe existir.
- Excepciones posibles:
  - `DocumentoNotFoundException`.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - detalle del documento.

### POST `/api/documentos/{id}/reenviar`

- Propósito: reenviar un documento existente al correo del cliente.
- Path variables:
  - `id`: ID del documento.
- Query params: no aplica.
- Request body: no aplica.
- Response: `DocumentoResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `DocumentoServiceImpl.reenviarDocumento(id)`.
- Validaciones principales:
  - El documento debe existir.
  - `docuIntentosEnvio < 3`.
  - El cliente del evento debe tener correo.
  - Debe existir plantilla asociada o plantilla activa para el tipo del documento.
- Excepciones posibles:
  - `DocumentoNotFoundException`.
  - `DocumentoOperacionNoPermitidaException` por máximo de intentos, sin correo o sin plantilla activa.
  - Errores de PDF/email son capturados y devuelven documento `FALLIDO` con intento incrementado.
- Efectos secundarios:
  - Genera PDF de nuevo.
  - Envía email.
  - Actualiza `docuEstadoEnvio`, `docuIntentosEnvio`, `docuFechaEnvio`.
  - Marca cliente notificado si el envío fue exitoso.
  - Registra auditoría `REENVIAR_DOCUMENTO`.
- Qué debe refrescar el frontend:
  - fila del documento.
  - documentos del evento.
  - detalle del evento si se muestra `clienteNotificado`.

### GET `/api/documentos/plantillas`

- Propósito: listar plantillas disponibles.
- Path variables: no aplica.
- Query params: no aplica.
- Request body: no aplica.
- Response: `List<PlantillaResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `DocumentoServiceImpl.listarPlantillas()`.
- Validaciones principales: no aplica.
- Excepciones posibles: error genérico si falla consulta.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - listado de plantillas.

### GET `/api/documentos/plantillas/{id}`

- Propósito: obtener una plantilla por ID.
- Path variables:
  - `id`: ID de plantilla.
- Query params: no aplica.
- Request body: no aplica.
- Response: `PlantillaResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `DocumentoServiceImpl.obtenerPlantilla(id)`.
- Validaciones principales:
  - La plantilla debe existir.
- Excepciones posibles:
  - `PlantillaDocumentoNotFoundException`.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - detalle/editor de plantilla.

### PUT `/api/documentos/plantillas/{id}`

- Propósito: actualizar contenido HTML, campos editables y estado activo de una plantilla.
- Path variables:
  - `id`: ID de plantilla.
- Query params: no aplica.
- Request body: `PlantillaUpdateDTO`.
- Response: `PlantillaResponseDTO`.
- HTTP esperado: `200 OK`.
- Método del service: `DocumentoServiceImpl.actualizarPlantilla(id, dto)`.
- Validaciones principales:
  - `contenidoBase` no puede estar vacío.
  - La plantilla debe existir.
  - `tipoDocumento` no se edita desde este endpoint.
- Excepciones posibles:
  - `MethodArgumentNotValidException`.
  - `PlantillaDocumentoNotFoundException`.
- Efectos secundarios:
  - Actualiza `contenidoBase`.
  - Actualiza `camposEditables` si viene no null.
  - Actualiza `activo` si viene no null.
- Qué debe refrescar el frontend:
  - listado de plantillas.
  - detalle/editor de plantilla.

### GET `/api/documentos`

- Propósito: listar documentos paginados.
- Path variables: no aplica.
- Query params:
  - parámetros estándar de `Pageable`: `page`, `size`, `sort`.
- Request body: no aplica.
- Response: `Page<DocumentoResponseDTO>`.
- HTTP esperado: `200 OK`.
- Método del service: `DocumentoServiceImpl.obtenerTodos(pageable)`.
- Validaciones principales: no aplica.
- Excepciones posibles: error genérico si falla consulta.
- Efectos secundarios: no aplica.
- Qué debe refrescar el frontend:
  - listado global de documentos.

## 3. DTOs request/response

### 3.1 DTOs Request

#### `PlantillaUpdateDTO`

| Campo | Tipo Java | JSON esperado | Requerido | Nullable | Validaciones | Notas frontend |
|---|---|---|---|---|---|---|
| `contenidoBase` | `String` | `contenidoBase` | Sí | No | `@NotEmpty` | HTML base o texto con placeholders. No enviar vacío. |
| `camposEditables` | `String` | `camposEditables` | No | Sí | No declarada | Texto/JSON serializado de campos configurables. Si viene `null`, no se actualiza. |
| `activo` | `Boolean` | `activo` | No | Sí | No declarada | Si viene `null`, conserva el valor actual. |

No existen DTOs request para generación o reenvío de documentos. La generación usa path variable y query param.

### 3.2 DTOs Response

#### `DocumentoResponseDTO`

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `idDocumento` | `Long` | `idDocumento` | No | `documento.idDocumento` | ID principal. |
| `idEvento` | `Long` | `idEvento` | Sí | `documento.evento.id` | Puede ser null si la relación no está cargada, aunque normalmente debe venir. |
| `docuTipo` | `String` | `docuTipo` | Sí | `DocumentoTipo.name()` | Usar para badge/tipo documental. |
| `docuFechaGeneracion` | `LocalDateTime` | `docuFechaGeneracion` | Sí | `documento.docuFechaGeneracion` | Fecha de creación. |
| `archivoUrl` | `String` | `archivoUrl` | Sí | `documento.archivoUrl` | Actualmente puede venir null porque el service no asigna URL al PDF. |
| `docuEstadoEnvio` | `String` | `docuEstadoEnvio` | Sí | `DocumentoEstadoEnvio.name()` | Estados: `PENDIENTE`, `ENVIADO`, `FALLIDO`, `REENVIADO`. |
| `docuFechaEnvio` | `LocalDateTime` | `docuFechaEnvio` | Sí | `documento.docuFechaEnvio` | Null si no se ha enviado o falló sin fecha. |
| `docuIntentosEnvio` | `Integer` | `docuIntentosEnvio` | Sí | `documento.docuIntentosEnvio` | Usar para bloquear reenvío si llega a 3. |
| `clienteNombre` | `String` | `clienteNombre` | Sí | `evento.cliente.nombre` | Campo denormalizado para tabla. |
| `estadoEventoNombre` | `String` | `estadoEventoNombre` | Sí | `evento.estadoEvento.nombre` | Útil para contexto visual. |

#### `PlantillaResponseDTO`

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| `idPlantilla` | `Long` | `idPlantilla` | No | `plantilla_documento.idPlantilla` | ID principal. |
| `tipoDocumento` | `String` | `tipoDocumento` | Sí | `DocumentoTipo.name()` | No editable desde update. |
| `contenidoBase` | `String` | `contenidoBase` | Sí | `plantilla_documento.contenidoBase` | HTML/texto editable. |
| `camposEditables` | `String` | `camposEditables` | Sí | `plantilla_documento.camposEditables` | Puede ser JSON serializado. |
| `activo` | `Boolean` | `activo` | No | `plantilla_documento.activo` | Determina si puede usarse para generar documentos. |
| `fechaCreacion` | `LocalDateTime` | `fechaCreacion` | Sí | `plantilla_documento.fechaCreacion` | Solo lectura. |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Documento` | `documento` | Tabla operativa | Documento generado para un evento. | Tiene constraint único `uk_documento_evento_tipo` sobre `id_evento + docu_tipo`. |
| `PlantillaDocumento` | `plantilla_documento` | Tabla de plantilla | Define contenido base por tipo de documento. | `tipoDocumento` es único. |
| `DocumentoTipo` | Enum / `docu_tipo`, `tipo_documento` | Catálogo lógico | Tipos permitidos de documentos. | `CONFIRMACION_RESERVA`, `COTIZACION_EVENTO`, `SALDO_PENDIENTE`, `AVISO_SALDO_PENDIENTE`. |
| `DocumentoEstadoEnvio` | Enum / `docu_estado_envio` | Estado operativo | Estado del envío del documento. | `PENDIENTE`, `ENVIADO`, `FALLIDO`, `REENVIADO`. |
| `Evento` | `evento` | Entidad externa | Fuente de cliente, fecha, ubicación, decoración, tipo, precio y estado. | Se carga con relaciones mediante `findDocumentoContextById`. |
| `VistaPagosEvento` | `vista_pagos_evento` | Vista externa | Cálculo de saldo pendiente. | Se usa para aviso de saldo pendiente y placeholders financieros. |
| `Pago` | `pago` | Tabla externa | Cálculo de anticipo confirmado. | Se consulta por concepto `ANTICIPO`. |

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Generar documento | `generarDocumento(idEvento, tipoDocumento)` | Evento existente, estado permitido, no duplicado, plantilla activa | Inserta documento, actualiza estado de envío, puede marcar cliente notificado | `DocumentoResponseDTO` |
| Generar aviso de saldo | `generarAvisoSaldoPendiente(eventoId)` | Evento existente, estado permitido, no duplicado, saldo pendiente > 0, plantilla activa | Inserta documento, actualiza estado de envío, puede marcar cliente notificado | `DocumentoResponseDTO` |
| Reenviar documento | `reenviarDocumento(idDocumento)` | Documento existente, intentos < 3, cliente con correo, plantilla disponible | Actualiza estado, intentos y fecha de envío | `DocumentoResponseDTO` |
| Listar por evento | `listarPorEvento(idEvento)` | Evento existente | No modifica | `List<DocumentoResponseDTO>` |
| Obtener documento | `obtenerDocumento(idDocumento)` | Documento existente | No modifica | `DocumentoResponseDTO` |
| Listar todos | `obtenerTodos(pageable)` | No aplica | No modifica | `Page<DocumentoResponseDTO>` |
| Listar plantillas | `listarPlantillas()` | No aplica | No modifica | `List<PlantillaResponseDTO>` |
| Obtener plantilla | `obtenerPlantilla(idPlantilla)` | Plantilla existente | No modifica | `PlantillaResponseDTO` |
| Actualizar plantilla | `actualizarPlantilla(idPlantilla, dto)` | Plantilla existente, `contenidoBase` no vacío | Actualiza contenido, campos editables y activo | `PlantillaResponseDTO` |

### Flujo: generar documento

1. Busca el evento con contexto documental.
2. Valida que el tipo de documento pueda generarse según el estado del evento.
3. Valida que no exista un documento del mismo tipo para el evento.
4. Busca plantilla activa por tipo.
5. Crea documento en estado `PENDIENTE` con `docuIntentosEnvio = 0`.
6. Si el cliente no tiene correo, deja el documento pendiente y registra auditoría.
7. Si el cliente tiene correo:
   - rellena placeholders.
   - genera PDF en memoria.
   - envía email con adjunto.
   - si el envío sale bien, marca `ENVIADO`, incrementa intentos a 1, registra fecha de envío y marca cliente notificado.
   - si falla PDF/email, marca `FALLIDO`, intentos 1, sin fecha de envío.
8. Registra auditoría.
9. Retorna `DocumentoResponseDTO`.

### Flujo: generar aviso de saldo pendiente

1. Busca evento con contexto documental.
2. Valida generación permitida para `AVISO_SALDO_PENDIENTE`.
3. Valida que no exista duplicado.
4. Calcula resumen financiero:
   - `anticipoPagado` desde `PagoRepository.sumMontoConfirmadoByEventoIdAndConceptoNombre(..., "ANTICIPO")`.
   - `saldoPendiente` desde `VistaPagosEvento.getSaldoPendiente()` o fallback por pagos confirmados.
5. Valida que el saldo pendiente sea mayor a cero.
6. Busca plantilla activa.
7. Crea documento.
8. Si el cliente no tiene correo, deja pendiente y registra auditoría.
9. Si tiene correo, genera PDF y envía mediante `ComunicacionClienteService`.
10. Actualiza estado y auditoría.

### Flujo: reenviar documento

1. Busca documento con relaciones.
2. Valida intentos máximos y correo del cliente.
3. Resuelve plantilla:
   - usa la plantilla asociada si existe y está activa.
   - si no existe, busca una plantilla activa por tipo de documento.
4. Rellena placeholders.
5. Genera PDF.
6. Envía email.
7. Si sale bien, estado `REENVIADO`, incrementa intentos y fecha de envío.
8. Si falla, estado `FALLIDO`, incrementa intentos y sin fecha.
9. Registra auditoría.

### Flujo: actualizar plantilla

1. Busca plantilla por ID.
2. Actualiza `contenidoBase`.
3. Actualiza `camposEditables` solo si viene no null.
4. Actualiza `activo` solo si viene no null.
5. Guarda y retorna DTO.

## 6. Estados y transiciones

### Estados de envío de documento

| Estado | Significado | Dónde se usa | Visible en frontend | Notas |
|---|---|---|---|---|
| `PENDIENTE` | Documento creado, no enviado todavía. | Estado inicial de `Documento`. | Sí | Puede ocurrir si el cliente no tiene correo. |
| `ENVIADO` | Documento enviado correctamente por primera vez. | Generación exitosa. | Sí | `docuFechaEnvio` debe venir con valor. |
| `FALLIDO` | Error al generar PDF o enviar correo. | Catch de generación/reenvío. | Sí | El endpoint no lanza error; retorna documento fallido. |
| `REENVIADO` | Documento reenviado correctamente. | Reenvío exitoso. | Sí | Incrementa `docuIntentosEnvio`. |

### Tipos de documento

| Tipo | Significado | Estados de evento permitidos | Notas frontend |
|---|---|---|---|
| `CONFIRMACION_RESERVA` | Confirmación formal de reserva. | `CONFIRMADO`, `EN_MONTAJE`, `FINALIZADO` | Mostrar acción solo en esos estados. |
| `COTIZACION_EVENTO` | Documento de cotización. | `COTIZACION` | Mostrar acción solo en cotización. |
| `AVISO_SALDO_PENDIENTE` | Aviso formal de saldo pendiente. | `PROGRAMADO`, `CONFIRMADO`, `EN_MONTAJE`, `FINALIZADO` | Además requiere saldo pendiente > 0. |
| `SALDO_PENDIENTE` | Tipo declarado en enum. | No tiene regla explícita en `DocumentoPolicy`; cae en `default -> true`. | Revisar antes de exponerlo como acción UI. |

### Transiciones permitidas

| Desde | Hacia | Acción/Endpoint | Validación | Efectos secundarios |
|---|---|---|---|---|
| No existe documento | `PENDIENTE` | `POST /evento/{idEvento}/generar` | Evento, estado, plantilla, no duplicado | Crea documento. |
| `PENDIENTE` | `ENVIADO` | generación con correo y envío exitoso | PDF/email correctos | Marca cliente notificado y audita. |
| `PENDIENTE` | `FALLIDO` | generación con error de PDF/email | Error capturado | Incrementa intentos a 1 y audita. |
| `ENVIADO`/`FALLIDO`/`REENVIADO` | `REENVIADO` | `POST /{id}/reenviar` | Intentos < 3, correo, plantilla | Incrementa intentos, fecha envío, audita. |
| `ENVIADO`/`FALLIDO`/`REENVIADO` | `FALLIDO` | reenvío con error | Error capturado | Incrementa intentos y audita. |

## 7. Excepciones y respuestas de error

El módulo usa excepciones que extienden `ArtepaxException`. El formato general de error es:

```json
{
  "codigo": 422,
  "mensaje": "Mensaje del error",
  "errorCode": null,
  "timestamp": "2026-05-10T03:00:00"
}
```

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `EventoNotFoundException` | El evento no existe al generar/listar documentos. | 404 | `Evento no encontrado: {id}` | Mostrar error y volver al listado/detalle válido. |
| `DocumentoNotFoundException` | Documento no existe. | 404 | `Documento no encontrado: {id}` | Mostrar error de no encontrado. |
| `PlantillaDocumentoNotFoundException` | Plantilla no existe por ID. | 404 | `Plantilla no encontrada: {id}` | Mostrar error y refrescar listado. |
| `DocumentoOperacionNoPermitidaException` | Estado de evento inválido, duplicado, sin plantilla activa, sin correo para reenvío, límite de intentos o saldo pendiente cero. | 422 | Mensaje específico del service/policy. | Mostrar mensaje de negocio tal cual. |
| `PdfGenerationException` | Error generando PDF. | 500 si se propaga | `Error al generar el PDF: ...` | En generación/reenvío normalmente se captura y se retorna documento `FALLIDO`. |
| `EmailDeliveryException` | Error de envío de correo. | 500 si se propaga | Mensaje de delivery. | En generación/reenvío normalmente se captura y se retorna documento `FALLIDO`. |
| `MethodArgumentNotValidException` | `PlantillaUpdateDTO.contenidoBase` vacío. | 400 | `contenidoBase: El contenido base no puede estar vacío` | Marcar campo en formulario. |
| `DataIntegrityViolationException` | Duplicado concurrente `uk_documento_evento_tipo`. | 409 | `Ya existe un documento de este tipo para el evento especificado` | Refrescar documentos del evento. |
| `AccessDeniedException` | Usuario sin rol ADMIN. | 403 | Mensaje de acceso denegado. | Redirigir o mostrar sin permisos. |
| `Exception` | Error no manejado. | 500 | `Error interno del servidor` | Mostrar mensaje genérico y registrar reporte. |

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| Evento | `EventoRepository` | `generarDocumento`, `generarAvisoSaldoPendiente`, `listarPorEvento`, `marcarClienteNotificado` | Obtener contexto documental, validar existencia y actualizar `clienteNotificado`. | Medio: el módulo Documento modifica `evento.clienteNotificado`. |
| Pago | `PagoRepository`, `VistaPagosEventoRepository` | `obtenerResumenFinanciero` | Calcular anticipo pagado y saldo pendiente. | Medio: depende de semántica de pagos y vista financiera. |
| Configuración | `ConfiguracionService` | placeholders, asuntos y cuerpos de correo | Obtener nombre, correo, teléfono, política de cancelación e instrucciones de pago. | Medio: si falta una clave, puede fallar generación/envío. |
| Comunicación | `EmailService`, `ComunicacionClienteService` | generación y reenvío | Enviar PDF adjunto por correo. | Medio: errores se capturan y quedan como `FALLIDO`. |
| Auditoría | `AuditoriaService` | generación y reenvío | Registrar acciones `GENERAR_DOCUMENTO` y `REENVIAR_DOCUMENTO`. | Bajo/Medio: si auditoría falla dentro de transacción podría afectar el flujo. |
| PDF | `PdfGeneratorService` | generación/reenvío | Convertir HTML a PDF bytes. | Medio: HTML inválido o error de render marca documento fallido. |
| Plantillas | `PlantillaDocumentoRepository` | generación y reenvío | Obtener plantilla activa por tipo. | Bajo: si falta plantilla, se bloquea generación. |

Dependencia circular:

```text
Detectada: No en el módulo Documento.
Detalle: Documento depende de Evento, Pago, Configuración, Comunicación y Auditoría. No se observa que esos servicios dependan directamente de DocumentoService en los archivos revisados.
Riesgo: Bajo.
Cómo se evita: El módulo usa repositories/servicios auxiliares y no expone callbacks hacia EventoService.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Generar documento con envío exitoso | Crea documento, envía email, marca `clienteNotificado = true`, audita | Documento, Evento, Comunicación, Auditoría | Documentos del evento, detalle del evento. |
| Generar documento sin correo | Crea documento en `PENDIENTE`, audita | Documento, Auditoría | Documentos del evento. |
| Generar documento con error de PDF/email | Crea documento en `FALLIDO`, intentos `1`, audita | Documento, Auditoría | Documentos del evento. |
| Generar aviso saldo pendiente | Crea documento, valida saldo, envía aviso formal, audita | Documento, Pago, Evento, Comunicación, Auditoría | Documentos, detalle del evento, resumen financiero si se muestra. |
| Reenviar documento exitosamente | Estado `REENVIADO`, incrementa intentos, fecha de envío, audita | Documento, Comunicación, Auditoría, Evento | Fila/listado de documentos. |
| Reenviar documento fallido | Estado `FALLIDO`, incrementa intentos, audita | Documento, Auditoría | Fila/listado de documentos. |
| Actualizar plantilla | Modifica contenido/campos/activo | Documento | Listado y detalle de plantillas. |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Generar `COTIZACION_EVENTO` | Evento en `COTIZACION` y no existe documento de ese tipo. | Evento no está en `COTIZACION` o ya existe. | `Solo se puede generar cotización para eventos en cotización o ya existe un documento de este tipo.` |
| Generar `CONFIRMACION_RESERVA` | Evento en `CONFIRMADO`, `EN_MONTAJE` o `FINALIZADO` y no existe documento de ese tipo. | Evento en otro estado o duplicado. | `La confirmación de reserva requiere un evento confirmado o posterior.` |
| Generar `AVISO_SALDO_PENDIENTE` | Evento en `PROGRAMADO`, `CONFIRMADO`, `EN_MONTAJE` o `FINALIZADO`, saldo pendiente > 0 y no duplicado. | Saldo cero/negativo, estado no permitido o duplicado. | `No hay saldo pendiente o el estado del evento no permite generar este aviso.` |
| Reenviar documento | Documento existe, cliente tiene correo, intentos < 3. | `docuIntentosEnvio >= 3` o cliente sin correo. | `No se puede reenviar: límite de intentos alcanzado o cliente sin correo.` |
| Editar plantilla | Plantilla existe. | Plantilla no encontrada. | `Plantilla no encontrada.` |
| Cambiar tipo de plantilla | Nunca desde este endpoint. | Siempre. | `El tipo de documento no es editable.` |

### 10.2 Campos editables

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `contenidoBase` | En `PUT /plantillas/{id}`. | Si viene vacío. | Campo obligatorio en update. |
| `camposEditables` | En `PUT /plantillas/{id}` si se envía. | No aplica. | Si se omite o viene null, conserva valor actual. |
| `activo` | En `PUT /plantillas/{id}` si se envía. | No aplica. | Permite activar/desactivar plantilla. |
| `tipoDocumento` | No editable. | Siempre. | Es clave lógica para buscar plantilla. |
| `docuEstadoEnvio` | No editable desde frontend. | Siempre. | Lo controla backend. |
| `docuIntentosEnvio` | No editable desde frontend. | Siempre. | Lo controla backend. |
| `archivoUrl` | No editable desde este módulo. | Siempre. | Actualmente puede venir null. |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Generar documento | Documentos del evento, detalle del evento si muestra `clienteNotificado`. |
| Generar aviso saldo pendiente | Documentos del evento, resumen financiero si está visible, detalle del evento. |
| Reenviar documento | Documento específico o listado de documentos. |
| Actualizar plantilla | Listado de plantillas y detalle/editor de plantilla. |
| Error de duplicado | Listado de documentos del evento para mostrar el documento existente. |

### 10.4 Badges o estados visuales

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `PENDIENTE` | Documento generado pero no enviado. | Badge de advertencia/pendiente. |
| `ENVIADO` | Envío inicial exitoso. | Badge de éxito. |
| `REENVIADO` | Reenvío exitoso. | Badge de éxito con etiqueta de reenvío. |
| `FALLIDO` | Error de PDF o correo. | Badge de error y acción de reintento si intentos < 3. |
| `CONFIRMACION_RESERVA` | Documento de confirmación. | Tipo documental. |
| `COTIZACION_EVENTO` | Cotización. | Tipo documental. |
| `AVISO_SALDO_PENDIENTE` | Aviso formal de saldo. | Tipo documental. |
| `SALDO_PENDIENTE` | Tipo declarado sin regla específica en policy. | No exponer como acción hasta validar uso de negocio. |

### 10.5 Reglas importantes

- No permitir duplicar documentos del mismo tipo para el mismo evento.
- No asumir que `archivoUrl` siempre existe; actualmente puede venir `null`.
- No mostrar acción de reenvío si `docuIntentosEnvio >= 3`.
- Si el backend retorna documento `FALLIDO`, no necesariamente es error HTTP; es estado operativo del documento.
- Mostrar los mensajes de negocio del backend para estado no permitido, duplicado, saldo cero o cliente sin correo.
- Para plantillas, no intentar editar `tipoDocumento`.
- Tratar `PlantillaResponseDTO.contenidoBase` como HTML/texto sensible: debe editarse con cuidado y validarse visualmente.
- Refrescar documentos después de generación o reenvío.
- La disponibilidad de acciones depende del estado del evento y del tipo de documento.
- No usar `SALDO_PENDIENTE` como acción directa sin validar regla de negocio, porque la policy no define estados específicos para ese tipo.

## Resumen para frontend

- El módulo opera bajo `/api/documentos` y requiere rol `ADMIN`.
- La generación usa `idEvento` y `DocumentoTipo`; no usa body.
- Solo puede existir un documento por `evento + tipo`.
- La generación puede devolver `PENDIENTE`, `ENVIADO` o `FALLIDO` en el DTO.
- Si el cliente no tiene correo, el documento queda `PENDIENTE`.
- El reenvío incrementa `docuIntentosEnvio` y se bloquea al llegar a 3.
- `archivoUrl` puede venir `null`; no hay endpoint de descarga directa documentado en este módulo.
- `AVISO_SALDO_PENDIENTE` requiere saldo pendiente mayor a cero.
- Las plantillas permiten editar contenido HTML, campos editables y activo, pero no el tipo.
- Después de generar o reenviar, refrescar documentos del evento y, si aplica, el detalle del evento.
