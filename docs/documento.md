# documento — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Generación de documentos PDF (confirmaciones, cotizaciones, avisos de saldo) a partir de plantillas HTML con placeholders, envío por email al cliente, reenvío con control de intentos y gestión de plantillas.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Generación, envío y gestión de documentos y plantillas |
| Controller principal | `DocumentoController` (`/api/documentos`) |
| Service principal | `DocumentoServiceImpl` |
| Entidades principales | `Documento`, `PlantillaDocumento` (tablas `documento`, `plantilla_documento`) |
| Tablas/vistas implicadas | `documento`, `plantilla_documento`, `evento` |
| Módulos relacionados | `evento`, `comunicacion` (EmailService), `pago` (VistaPagosEvento), `common` (auditoría, configuración) |
| Tipo de módulo | Operativo + Catálogo |

## 2. Controllers y endpoints completos

Todos requieren `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Ruta | Acción | Request | Response | Service | Auth | Notas |
|---|---|---|---|---|---|---|---|
| POST | `/api/documentos/evento/{idEvento}/generar?tipo=` | Generar documento | `tipo` (query enum) | `DocumentoResponseDTO` | `generarDocumento` | ADMIN | Bloquea duplicados |
| POST | `/api/documentos/evento/{idEvento}/aviso-saldo-pendiente` | Generar aviso saldo | — | `DocumentoResponseDTO` | `generarAvisoSaldoPendiente` | ADMIN | Bloquea duplicados |
| GET | `/api/documentos/evento/{idEvento}` | Listar por evento | — | `List<DocumentoResponseDTO>` | `listarPorEvento` | ADMIN | |
| GET | `/api/documentos/{id}` | Obtener uno | — | `DocumentoResponseDTO` | `obtenerDocumento` | ADMIN | |
| POST | `/api/documentos/{id}/reenviar` | Reenviar documento | — | `DocumentoResponseDTO` | `reenviarDocumento` | ADMIN | Control de reintentos (máx 3) |
| GET | `/api/documentos` | Todos paginado | Pageable | `Page<DocumentoResponseDTO>` | `obtenerTodos` | ADMIN | Default: size=50, sort=docuFechaGeneracion |
| GET | `/api/documentos/plantillas` | Listar plantillas | — | `List<PlantillaResponseDTO>` | `listarPlantillas` | ADMIN | |
| GET | `/api/documentos/plantillas/{id}` | Obtener plantilla | — | `PlantillaResponseDTO` | `obtenerPlantilla` | ADMIN | |
| PUT | `/api/documentos/plantillas/{id}` | Actualizar plantilla | `PlantillaUpdateDTO` | `PlantillaResponseDTO` | `actualizarPlantilla` | ADMIN | Actualiza HTML |

### POST /api/documentos/evento/{idEvento}/generar

- **Propósito:** Generar un documento para un evento usando la plantilla del tipo indicado.
- **Query params:** `tipo` (enum `DocumentoTipo`): `CONFIRMACION_RESERVA`, `COTIZACION_EVENTO`, `SALDO_PENDIENTE`, `AVISO_SALDO_PENDIENTE`
- **Validaciones principales:**
  - Evento debe existir y estar en estado permitido (`DocumentoPolicy`)
  - No permite duplicados: si ya existe un documento del mismo tipo para el evento → error
  - Debe existir plantilla activa para ese tipo
  - Si el cliente no tiene correo → crea documento PENDIENTE sin enviar
- **Excepciones posibles:**
  - `EventoNotFoundException` (404)
  - `DocumentoOperacionNoPermitidaException`: "Ya existe un documento de tipo X para este evento"
  - `DocumentoOperacionNoPermitidaException`: "No existe plantilla activa para el tipo: X"
  - `DataIntegrityViolationException` (409): "Ya existe un documento de este tipo para el evento especificado"
- **Efectos secundarios:**
  - Genera PDF, envía email con adjunto
  - Si envío exitoso: `docuEstadoEnvio = ENVIADO`, `event_cliente_notificado = true`
  - Si envío falla: `docuEstadoEnvio = FALLIDO` (no rollback)
  - Registra auditoría (`GENERAR_DOCUMENTO`)
- **Qué debe refrescar el frontend:** listado de documentos del evento, detalle evento

### POST /api/documentos/evento/{idEvento}/aviso-saldo-pendiente

- **Propósito:** Generar y enviar un aviso formal de saldo pendiente al cliente.
- **Validaciones principales:**
  - Igual que `generarDocumento` pero específico para `AVISO_SALDO_PENDIENTE`
  - Bloquea duplicados
  - Valida que exista saldo pendiente > 0
- **Efectos secundarios:** mismos que generar documento
- **Qué debe refrescar el frontend:** listado de documentos

### POST /api/documentos/{id}/reenviar

- **Propósito:** Reenviar un documento previamente generado al cliente.
- **Validaciones principales:**
  - Documento debe existir
  - `DocumentoPolicy.validarReenvioPermitido` — controla límite de intentos (máx 3)
- **Excepciones posibles:**
  - `DocumentoNotFoundException` (404)
  - `DocumentoOperacionNoPermitidaException`: límite de reintentos excedido
- **Efectos secundarios:**
  - Incrementa `docuIntentosEnvio`
  - Si exitoso: `docuEstadoEnvio = REENVIADO`
  - Si falla: `docuEstadoEnvio = FALLIDO`
  - Actualiza `event_cliente_notificado`
  - Registra auditoría (`REENVIAR_DOCUMENTO`)
- **Qué debe refrescar el frontend:** documento, listado

## 3. DTOs request/response

### 3.1 DTOs Request

#### PlantillaUpdateDTO

| Campo | Tipo Java | JSON esperado | Requerido | Notas frontend |
|---|---|---|---|---|
| contenidoBase | String | string (HTML) | Sí | HTML con placeholders `{{cliente_nombre}}`, `{{fecha_evento}}`, `{{precio_total}}`, etc. |
| camposEditables | String | string (JSON) | No | JSON con nombres de secciones editables |

### 3.2 DTOs Response

#### DocumentoResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| idDocumento | Long | number | No | entidad | |
| idEvento | Long | number | No | entidad | |
| docuTipo | String | string | No | entidad | CONFIRMACION_RESERVA, COTIZACION_EVENTO, SALDO_PENDIENTE, AVISO_SALDO_PENDIENTE |
| docuFechaGeneracion | LocalDateTime | ISO datetime | No | entidad | |
| archivoUrl | String | string (URL) | Sí | entidad | URL en cloud |
| docuEstadoEnvio | String | string | No | entidad | PENDIENTE, ENVIADO, FALLIDO, REENVIADO |
| docuFechaEnvio | LocalDateTime | ISO datetime | Sí | entidad | null si no enviado |
| docuIntentosEnvio | Integer | number | No | entidad | Contador de intentos |
| clienteNombre | String | string | No | entidad.evento.cliente | Denormalizado |
| estadoEventoNombre | String | string | No | entidad.evento.estadoEvento | |

#### PlantillaResponseDTO

| Campo | Tipo Java | JSON esperado | Nullable | Origen | Notas frontend |
|---|---|---|---|---|---|
| idPlantilla | Long | number | No | entidad | |
| tipoDocumento | String | string | No | entidad | |
| contenidoBase | String | string (HTML) | No | entidad | HTML con placeholders |
| camposEditables | String | string (JSON) | Sí | entidad | |
| activo | Boolean | boolean | No | entidad | |
| fechaCreacion | LocalDateTime | ISO datetime | No | entidad | |

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Documento` | `documento` | Tabla operativa | Documentos generados | `docu_estado_envio`: CHECK PENDIENTE/ENVIADO/FALLIDO/REENVIADO. `docu_intentos_envio`: contador |
| `PlantillaDocumento` | `plantilla_documento` | Catálogo | Plantillas HTML base | `contenido_base`: HTML con placeholders. `activo`: solo una activa por tipo |

**Tipos de documento** (`DocumentoTipo` enum): `CONFIRMACION_RESERVA`, `COTIZACION_EVENTO`, `SALDO_PENDIENTE`, `AVISO_SALDO_PENDIENTE`.

## 5. Services y lógica de negocio

| Flujo | Método | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Generar documento | `generarDocumento` | Evento existe, estado permitido, no duplicado, plantilla activa | Crea Documento (PENDIENTE), genera PDF, envía email, actualiza estado envío | DocumentoResponseDTO |
| Generar aviso saldo | `generarAvisoSaldoPendiente` | Evento existe, estado permitido, no duplicado, saldo > 0, plantilla activa | Crea Documento (PENDIENTE), genera PDF con datos financieros, envía | DocumentoResponseDTO |
| Reenviar | `reenviarDocumento` | Documento existe, reintentos disponibles | Incrementa intentos, reenvía, actualiza estado | DocumentoResponseDTO |

### Comportamiento transaccional

`generarDocumento` usa `@Transactional(noRollbackFor = RuntimeException.class)`. Si el envío de email falla, el documento se guarda igual con estado FALLIDO (no se pierde). El PDF se genera con `PdfGeneratorService` a partir del HTML de la plantilla rellenando placeholders.

### Control de duplicados

- `generarDocumento`: `existsByEventoIdAndTipoDocumento` + `DocumentoPolicy.validarSinDocumentoDuplicado`
- `generarAvisoSaldoPendiente`: mismo control (agregado recientemente)

### Límite de reintentos

`MAX_INTENTOS_ENVIO = 3`. `DocumentoPolicy.validarReenvioPermitido` bloquea si se excede.

## 6. Estados y transiciones

### Estados de envío

| Estado | Significado | Visible en frontend | Notas |
|---|---|---|---|
| PENDIENTE | Generado, no enviado (cliente sin correo) | Sí (badge warning) | |
| ENVIADO | Enviado exitosamente | Sí (badge success) | |
| FALLIDO | Error al enviar | Sí (badge danger) | Reenviable |
| REENVIADO | Reenviado exitosamente | Sí (badge info) | |

## 7. Excepciones y respuestas de error

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado |
|---|---|---|---|
| `EventoNotFoundException` | Evento no existe | 404 | "Evento no encontrado: X" |
| `DocumentoNotFoundException` | Documento no existe | 404 | "Documento no encontrado: X" |
| `PlantillaDocumentoNotFoundException` | Plantilla no existe | 404 | "Plantilla no encontrada: X" |
| `DocumentoOperacionNoPermitidaException` | Regla de negocio | 400 | Variable (duplicado, límite reintentos, sin plantilla) |
| `DataIntegrityViolationException` | Duplicado por constraint DB | 409 | "Ya existe un documento de este tipo para el evento especificado" |

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa |
|---|---|---|---|
| `evento` (EventoRepository) | Repository | Generar documentos | Leer evento y cliente |
| `comunicacion` (EmailService) | Service | Envío | Enviar email con PDF adjunto |
| `comunicacion` (ComunicacionClienteService) | Service | Aviso saldo | Enviar aviso específico |
| `pago` (VistaPagosEventoRepository) | Repository | Aviso saldo | Obtener datos financieros |
| `common` (ConfiguracionService) | Service | General | NEGOCIO_NOMBRE, NEGOCIO_CORREO, etc. |
| `common` (AuditoriaService) | Service | General | Registrar generación/reenvío |

```text
Dependencia circular:
- Detectada: No
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| POST generar | Genera PDF, envía email, `event_cliente_notificado=true`, auditoría | comunicacion, evento, common | documentos del evento, detalle evento |
| POST aviso saldo | Igual que generar + datos financieros | comunicacion, common | documentos del evento |
| POST reenviar | Reenvía email, incrementa intentos, auditoría | comunicacion, common | documento, listado |

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando |
|---|---|---|
| Generar documento | No existe documento de ese tipo para el evento | Ya existe (duplicado) |
| Generar aviso saldo | Hay saldo pendiente > 0, no existe aviso previo | Sin saldo o ya generado |
| Reenviar | `docuIntentosEnvio < 3`, estado no PENDIENTE | Límite alcanzado |

### 10.2 Reglas importantes

- El tipo de documento se pasa como query param enum: `?tipo=CONFIRMACION_RESERVA`. El frontend debe usar los valores exactos del enum `DocumentoTipo`.
- Si el cliente no tiene correo, el documento se crea pero queda PENDIENTE. No se envía.
- Si el envío falla, el documento se guarda con estado FALLIDO. Se puede reintentar hasta 3 veces.
- El control de duplicados aplica tanto para `generarDocumento` como para `generarAvisoSaldoPendiente`.
- `archivoUrl` es la URL del PDF en almacenamiento cloud. Puede ser null si el documento está PENDIENTE.

---

## Resumen para frontend

- Generación de documentos: POST con `?tipo=` como query param. Tipos: CONFIRMACION_RESERVA, COTIZACION_EVENTO, SALDO_PENDIENTE, AVISO_SALDO_PENDIENTE.
- Aviso de saldo pendiente: endpoint dedicado `POST /evento/{id}/aviso-saldo-pendiente`.
- Ambos endpoints bloquean duplicados.
- Reenvío con límite de 3 intentos.
- Plantillas gestionables: GET listar/obtener, PUT actualizar HTML.
- El documento se guarda aunque falle el envío (FALLIDO).
