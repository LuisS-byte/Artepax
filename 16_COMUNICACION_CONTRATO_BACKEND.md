# Comunicación — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

El módulo `comunicacion` centraliza el envío de correos electrónicos a clientes. No expone endpoints REST propios. Funciona como infraestructura interna consumida por otros módulos, principalmente documentos, jobs, pagos y eventos.

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Enviar correos simples y correos con PDF adjunto al cliente |
| Controller principal | No aplica |
| Service principal | `ComunicacionClienteService`, `EmailService` |
| Entidades principales | No tiene entidades propias |
| Tablas/vistas implicadas | No aplica directamente |
| Módulos relacionados | Evento, Pago, Documento, Configuración, Jobs |
| Tipo de módulo | Infraestructura / Comunicación |

Este módulo no administra persistencia propia. Usa datos de `Evento`, `Pago` y configuración global para construir mensajes HTML y delega el envío técnico a `JavaMailSender`.

## 2. Controllers y endpoints completos

No existen controllers ni endpoints públicos en este módulo.

| Método | Ruta | Acción | Request | Response | Service llamado | Auth requerida | Notas |
|---|---|---|---|---|---|---|---|
| NO APLICA | NO APLICA | No expone API REST | NO APLICA | NO APLICA | NO APLICA | NO APLICA | Módulo interno |

El frontend no debe consumir este módulo directamente. Los correos se disparan indirectamente mediante acciones de otros módulos, por ejemplo generación de documentos, confirmaciones, avisos de saldo o jobs programados.

## 3. DTOs request/response

No existen DTOs propios del módulo.

### 3.1 DTOs Request

No aplica. El módulo recibe objetos de dominio internos (`Evento`, `Pago`, `byte[]`) desde services de otros módulos.

### 3.2 DTOs Response

No aplica. Los métodos son `void`; si el envío falla, `EmailService` lanza una excepción runtime.

## 4. Entidades y tablas implicadas

| Entidad Java | Tabla/Vista SQL | Tipo | Propósito | Notas |
|---|---|---|---|---|
| `Evento` | `evento` | Entidad externa | Fuente de datos del evento, cliente, tipo, decoración, fecha, horario y ubicación | Usada para construir cuerpo de email |
| `Pago` | `pago` | Entidad externa | Fuente de datos del pago confirmado | Usada para comprobante de pago |
| `Configuracion` | `configuracion` | Entidad externa | Fuente de nombre/teléfono del negocio | Acceso mediante `ConfiguracionService` |
| `MailProperties` | No aplica | Configuración Spring | Configura remitente técnico | Usa prefijo `app.mail` |

### Configuración de correo

`MailProperties` carga propiedades con prefijo:

```properties
app.mail.from=
app.mail.from-name=
```

Campos:

| Propiedad | Uso |
|---|---|
| `app.mail.from` | Correo remitente usado en `MimeMessageHelper.setFrom(...)` |
| `app.mail.from-name` | Nombre visible del remitente |

## 5. Services y lógica de negocio

| Flujo | Método del service | Qué valida | Qué modifica | Qué retorna |
|---|---|---|---|---|
| Enviar confirmación de reserva | `ComunicacionClienteService.enviarConfirmacionReserva(Evento, byte[])` | Si cliente tiene correo; si no, solo registra warning y retorna | No modifica BD | `void` |
| Enviar recordatorio de evento | `enviarRecordatorioEvento(Evento)` | Si cliente tiene correo; si no, warning y retorna | No modifica BD | `void` |
| Enviar aviso de saldo pendiente con PDF | `enviarAvisoSaldoPendienteConDocumento(Evento, byte[])` | Si cliente tiene correo; si no, lanza `IllegalArgumentException` | No modifica BD | `void` |
| Enviar aviso de saldo pendiente simple | `enviarAvisoSaldoPendiente(Evento, BigDecimal)` | Si cliente tiene correo; si no, warning y retorna | No modifica BD | `void` |
| Enviar comprobante de pago | `enviarComprobantePago(Evento, Pago)` | Si cliente tiene correo; si no, warning y retorna | No modifica BD | `void` |
| Enviar email con adjunto | `EmailService.enviarConAdjunto(...)` | Configuración de mail válida y envío SMTP exitoso | No modifica BD | `void` |
| Enviar email simple | `EmailService.enviarSimple(...)` | Configuración de mail válida y envío SMTP exitoso | No modifica BD | `void` |

### Flujo: confirmación de reserva con PDF

1. Recibe `Evento` y bytes del PDF.
2. Lee el correo del cliente desde `evento.getCliente().getCorreo()`.
3. Si el correo está vacío, registra warning y no envía.
4. Consulta `NEGOCIO_NOMBRE` desde `ConfiguracionService`.
5. Construye asunto y cuerpo HTML.
6. Define nombre de archivo `confirmacion_evento_{id}.pdf`.
7. Llama a `EmailService.enviarConAdjunto(...)`.

### Flujo: recordatorio de evento

1. Recibe `Evento`.
2. Si el cliente no tiene correo, registra warning y retorna.
3. Consulta `NEGOCIO_NOMBRE`.
4. Construye cuerpo HTML con fecha, horario y ubicación.
5. Llama a `EmailService.enviarSimple(...)`.

### Flujo: aviso de saldo pendiente con documento

1. Recibe `Evento` y PDF.
2. Si el cliente no tiene correo, lanza `IllegalArgumentException` con mensaje `El cliente no tiene correo registrado.`.
3. Consulta `NEGOCIO_NOMBRE`.
4. Construye cuerpo HTML.
5. Envía PDF `aviso_saldo_pendiente_{id}.pdf` mediante `EmailService.enviarConAdjunto(...)`.

### Flujo: aviso de saldo pendiente simple

1. Recibe `Evento` y `saldoPendiente`.
2. Si el cliente no tiene correo, registra warning y retorna.
3. Consulta `NEGOCIO_NOMBRE` y `NEGOCIO_TELEFONO` para el cuerpo.
4. Llama a `EmailService.enviarSimple(...)`.

### Flujo: comprobante de pago

1. Recibe `Evento` y `Pago`.
2. Si el cliente no tiene correo, registra warning y retorna.
3. Consulta `NEGOCIO_NOMBRE`.
4. Construye cuerpo HTML con monto, fecha y evento.
5. Llama a `EmailService.enviarSimple(...)`.

### Flujo técnico: envío con adjunto

1. Crea `MimeMessage` mediante `JavaMailSender`.
2. Configura remitente desde `MailProperties`.
3. Configura destinatario, asunto y cuerpo HTML.
4. Adjunta el PDF como `application/pdf`.
5. Ejecuta `mailSender.send(...)`.
6. Si ocurre `MessagingException` o `UnsupportedEncodingException`, lanza `RuntimeException`.

## 6. Estados y transiciones

No aplica. Este módulo no maneja estados propios.

Los estados de negocio relacionados pertenecen a otros módulos:

| Estado relacionado | Módulo dueño | Uso en comunicación |
|---|---|---|
| Evento confirmado | Evento / Documento | Puede generar confirmación de reserva |
| Evento próximo | Jobs / Evento | Puede generar recordatorio |
| Saldo pendiente | Pago / Jobs / Documento | Puede generar aviso de saldo |
| Pago confirmado | Pago | Puede generar comprobante de pago |

## 7. Excepciones y respuestas de error

Este módulo no define excepciones custom propias.

| Excepción | Cuándo ocurre | HTTP esperado | Mensaje esperado | Acción sugerida en frontend |
|---|---|---|---|---|
| `IllegalArgumentException` | `enviarAvisoSaldoPendienteConDocumento()` recibe evento cuyo cliente no tiene correo | 400 si atraviesa `GlobalExceptionHandler` | `El cliente no tiene correo registrado.` | Mostrar mensaje claro: el cliente necesita correo registrado |
| `RuntimeException` | Falla técnica al enviar correo en `EmailService` | 500 si no es capturada por el módulo llamador | `Error al enviar el email: ...` o genérico según handler global | Mostrar error general y permitir reintento |
| `ConfiguracionNotFoundException` | Falta `NEGOCIO_NOMBRE` o `NEGOCIO_TELEFONO` | 404 si atraviesa `GlobalExceptionHandler` | `Configuración no encontrada: {clave}` | Reportar configuración faltante al admin |

Comportamiento relevante:

- En varios métodos, si el cliente no tiene correo, el servicio solo registra warning y retorna sin error.
- En `enviarAvisoSaldoPendienteConDocumento()`, si falta correo sí se lanza excepción.
- `EmailService` lanza `RuntimeException` ante errores de envío SMTP o codificación.
- Si estas excepciones llegan al `GlobalExceptionHandler`, las respuestas siguen el formato global del sistema.

## 8. Dependencias con otros módulos

| Depende de | Tipo de dependencia | Dónde se usa | Para qué se usa | Riesgo |
|---|---|---|---|---|
| `JavaMailSender` | Infraestructura Spring Mail | `EmailService` | Enviar correos SMTP | Alto si está mal configurado |
| `MailProperties` | Configuración externa `app.mail` | `EmailService` | Remitente y nombre visible | Medio; si falta, puede fallar envío |
| `ConfiguracionService` | Service de common | `ComunicacionClienteService` | Obtener `NEGOCIO_NOMBRE` y `NEGOCIO_TELEFONO` | Medio; configuración faltante rompe algunos flujos |
| `Evento` | Entidad de evento | `ComunicacionClienteService` | Datos del cliente/evento para emails | Medio; relaciones lazy deben estar disponibles |
| `Pago` | Entidad de pago | `enviarComprobantePago()` | Monto y fecha de pago | Bajo |
| Módulo Documento | Consumidor externo | Indirecto | Puede usar confirmación con PDF y saldo con PDF | Medio |
| Jobs | Consumidor externo | Indirecto | Recordatorios y avisos de saldo | Medio |

```text
Dependencia circular:
- Detectada: No en el módulo comunicación.
- Detalle: Comunicación depende de entidades/services externos, pero no se observa que esos services dependan obligatoriamente de Comunicación dentro de este ZIP.
- Riesgo: Bajo.
- Cómo se evita o cómo debería evitarse: Mantener Comunicación como servicio de infraestructura sin llamar services de negocio de vuelta.
```

## 9. Efectos secundarios

| Acción | Efecto secundario | Módulo afectado | Frontend debe refrescar |
|---|---|---|---|
| Enviar confirmación de reserva | Envía email con PDF al cliente | Documento / Evento | Documentos si el módulo llamador actualiza estado de envío |
| Enviar recordatorio | Envía email simple al cliente | Jobs / Evento | No necesariamente; depende del job |
| Enviar aviso saldo pendiente con documento | Envía email con PDF | Documento / Pago | Documentos y resumen de pagos si el llamador modifica estado |
| Enviar aviso saldo pendiente simple | Envía email simple | Pago / Jobs | No necesariamente |
| Enviar comprobante de pago | Envía email simple | Pago | Resumen de pagos si el llamador cambia pago |
| Falla técnica de email | Lanza excepción runtime | Módulo llamador | Mostrar error si la acción era iniciada por usuario |

Este módulo por sí solo no persiste datos de envío, no actualiza estado de documento y no registra auditoría. Esos efectos dependen del módulo que lo invoca.

## 10. Reglas que debe respetar el frontend

### 10.1 Acciones habilitadas/deshabilitadas

| Acción UI | Permitir cuando | Bloquear cuando | Mensaje sugerido |
|---|---|---|---|
| Enviar documento al cliente | El módulo Documento indique que se puede enviar y el cliente tenga correo | Cliente sin correo o documento no generado | `El cliente no tiene correo registrado.` |
| Reenviar documento | Documento permita reenvío y no haya superado límite | Documento no reenviable o cliente sin correo | `No se puede reenviar este documento.` |
| Avisar saldo pendiente | Exista saldo pendiente y cliente tenga correo | Cliente sin correo | `Debe registrar un correo del cliente antes de enviar avisos.` |
| Comprobante de pago | Pago confirmado y cliente con correo | Pago no confirmado o cliente sin correo | `Solo se puede enviar comprobante de pagos confirmados.` |

### 10.2 Campos editables

No aplica directamente. El módulo no expone formularios propios.

Campos de otros módulos que impactan comunicación:

| Campo | Editable cuando | No editable cuando | Notas |
|---|---|---|---|
| `cliente.correo` | Según reglas del módulo Cliente | Si Cliente lo bloquea | Necesario para envíos al cliente |
| `configuracion.NEGOCIO_NOMBRE` | Según módulo Configuración | Si usuario no tiene permisos | Usado en asunto y cuerpo |
| `configuracion.NEGOCIO_TELEFONO` | Según módulo Configuración | Si usuario no tiene permisos | Usado en cuerpos de recordatorio/saldo |
| `app.mail.from` | Configuración de aplicación | No editable desde frontend salvo panel técnico | Remitente SMTP |
| `app.mail.from-name` | Configuración de aplicación | No editable desde frontend salvo panel técnico | Nombre visible del remitente |

### 10.3 Datos que debe refrescar

| Después de acción | Refrescar |
|---|---|
| Generar/enviar documento | Detalle de documento, listado de documentos, estado de envío si existe |
| Reenviar documento | Documento y contador/intentos si se expone |
| Confirmar pago | Resumen de pagos, detalle de evento, notificaciones si aplica |
| Job o acción automática | No aplica desde UI salvo que se muestre historial |

### 10.4 Badges o estados visuales

No hay estados propios de comunicación. Los badges los debe tomar de otros módulos.

| Valor backend | Significado | Uso frontend |
|---|---|---|
| `docu_estado_envio` | Estado de envío de documento | Mostrar badge en Documentos |
| `cliente.correo == null/blank` | Cliente sin email | Mostrar advertencia antes de acciones de envío |
| Error 400 por falta de correo | No se puede enviar al cliente | Mostrar mensaje de validación |
| Error 500 por email | Fallo técnico de envío | Mostrar error general y permitir reintento |

### 10.5 Reglas importantes

- El frontend no consume `ComunicacionClienteService` directamente.
- Toda acción de envío debe pasar por el módulo dueño: Documento, Pago, Evento o Job correspondiente.
- Antes de permitir acciones de envío, la UI debe mostrar si el cliente tiene correo registrado.
- El frontend no debe asumir que un email fue enviado si el endpoint del módulo llamador falló.
- Si el backend responde error por falta de correo, debe mostrarse como problema corregible del cliente.
- Si el backend responde error genérico de email, debe tratarse como fallo técnico y permitir reintento según el módulo llamador.
- Los contenidos HTML de email están hardcodeados en backend; el frontend no los edita en este módulo.
- La configuración `NEGOCIO_NOMBRE` y `NEGOCIO_TELEFONO` impacta los emails. Si falta, puede fallar el flujo.

## Resumen para frontend

- Comunicación no tiene endpoints propios.
- El frontend no envía correos directamente; dispara acciones en Documento, Pago, Evento o Jobs.
- El campo crítico para cualquier envío al cliente es el correo del cliente.
- Si el cliente no tiene correo, algunos flujos solo omiten envío y otros lanzan error.
- Los correos pueden ser simples o con PDF adjunto.
- La confirmación de reserva y el aviso formal de saldo usan PDF adjunto.
- Los asuntos y cuerpos usan `NEGOCIO_NOMBRE` y en algunos casos `NEGOCIO_TELEFONO`.
- Los errores técnicos de correo salen como `RuntimeException` si no son capturados por el llamador.
- El estado visible para frontend normalmente pertenece a Documento, no a Comunicación.
- Este módulo debe tratarse como infraestructura interna del backend.
