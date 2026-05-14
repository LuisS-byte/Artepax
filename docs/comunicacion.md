# comunicacion — Contrato Backend para Frontend

## 1. Responsabilidad del módulo

Servicios de envío de email: emails genéricos con adjuntos y comunicación específica con clientes (avisos de saldo pendiente, confirmaciones).

| Elemento | Detalle |
|---|---|
| Responsabilidad principal | Envío de emails (sin endpoints REST propios) |
| Services | `EmailService`, `ComunicacionClienteService` |
| Config | `MailProperties` |
| Tipo de módulo | Infraestructura (sin endpoints) |

## 2. Endpoints

**No aplica.** Este módulo no expone endpoints REST. Es usado internamente por otros módulos (`documento`, `evento`, `pago`).

## 3. Servicios expuestos a otros módulos

| Servicio | Método | Usado por |
|---|---|---|
| `EmailService` | `enviarConAdjunto(correo, asunto, cuerpo, pdfBytes, nombreArchivo)` | `DocumentoServiceImpl` |
| `ComunicacionClienteService` | `enviarAvisoSaldoPendienteConDocumento(evento, pdfBytes)` | `DocumentoServiceImpl` |

## 4. Reglas para frontend

- No hay endpoints que consumir directamente.
- La configuración de email (SMTP) se maneja vía variables de entorno: `MAIL_USERNAME`, `MAIL_PASSWORD`.
- Los correos se envían automáticamente al generar documentos o al ocurrir eventos del sistema.

---

## Resumen para frontend

- Sin endpoints. Solo infraestructura interna.
