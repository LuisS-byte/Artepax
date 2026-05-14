# PENDIENTE_COBRO — Nuevo Estado de Evento para Cobranza Post-Montaje

Este documento describe el nuevo estado `PENDIENTE_COBRO` agregado al ciclo de vida del evento para resolver el problema de eventos finalizados con saldo pendiente.

## Problema resuelto

Antes de este cambio, `finalizarEvento()` permitía transicionar de `EN_MONTAJE → FINALIZADO` sin validar que el evento estuviera completamente pago. Si existía saldo pendiente, solo se notificaba pero no se bloqueaba. Una vez en `FINALIZADO`, el módulo de Pagos bloqueaba cualquier operación de pago. Resultado: **deuda incobrable por el sistema**.

## Solución

Se bifurca la transición desde `EN_MONTAJE`:

```
EN_MONTAJE ──┬── cobroCompleto = true ──→ FINALIZADO
             │
             └── cobroCompleto = false ─→ PENDIENTE_COBRO
                                               │
                                               └── último pago → FINALIZADO (automático)
```

### Semántica de PENDIENTE_COBRO

- **Operativamente**: el evento ya ocurrió. El stock fue consumido. El cronograma está cerrado.
- **Financieramente**: el evento admite pagos (registro y confirmación). Se comporta como un estado activo para cobranza.
- **Transición de salida**: automática cuando se completa el cobro total (disparada desde `confirmarPago()`).

## Flujo detallado

### finalizarEvento() — EventoServiceImpl

1. Valida que el evento esté en `EN_MONTAJE`.
2. Consulta `cobroCompleto` desde `vista_pagos_evento`.
3. **Siempre** consume el stock (`stockHandler.consumirStock`).
4. Si `cobroCompleto = true` → transiciona a `FINALIZADO`.
5. Si `cobroCompleto = false` → transiciona a `PENDIENTE_COBRO` + notifica saldo pendiente.

### finalizarDesdeCobroCompleto() — EventoServiceImpl (nuevo)

- Disparado desde `PagoServiceImpl.confirmarPago()` después de confirmar un pago.
- **Idempotente**: si el evento no está en `PENDIENTE_COBRO` o el cobro no está completo, simplemente retorna sin hacer nada.
- Si el evento está en `PENDIENTE_COBRO` y `cobroCompleto = true` → transiciona a `FINALIZADO`.

### Validaciones de pago

| Operación | ¿Permitida en PENDIENTE_COBRO? | Validación de exceso |
|---|---|---|
| `registrarPago()` | ✅ Sí | `totalPagado + nuevoMonto <= precioTotal` |
| `confirmarPago()` | ✅ Sí | `totalConfirmado + estePago <= precioTotal` |
| `anularPago()` | ✅ Sí (solo pagos PENDIENTE) | N/A |

Las mismas guardas de exceso que aplican a `PROGRAMADO`, `CONFIRMADO` y `EN_MONTAJE` aplican a `PENDIENTE_COBRO`. No se puede exceder el precio total del evento.

## Archivos modificados

| Archivo | Cambio |
|---|---|
| `evento/model/EstadoEventoNames.java` | +`PENDIENTE_COBRO` constante. Sets sin cambios. |
| `evento/service/EventoService.java` | +`finalizarDesdeCobroCompleto()` en interfaz. Javadoc actualizado. |
| `evento/service/impl/EventoServiceImpl.java` | `finalizarEvento()` bifurcado. +`finalizarDesdeCobroCompleto()`. |
| `pago/service/impl/PagoServiceImpl.java` | +`PENDIENTE_COBRO` en `ESTADOS_EVENTO_PERMITIDOS_PARA_CONFIRMAR`. +llamada a `finalizarDesdeCobroCompleto()` en `confirmarPago()`. |
| `pago/repository/PagoRepository.java` | +`'PENDIENTE_COBRO'` en `WHERE IN` de `findEventosCobranzaRaw()`. |
| `documento/service/policy/DocumentoPolicy.java` | +`PENDIENTE_COBRO` en `ESTADOS_AVISO_SALDO_PENDIENTE`. |
| `common/job/JobRecordatorioEvento.java` | +`"PENDIENTE_COBRO"` en blacklist de estados inactivos. |

## Decisiones de diseño

### ¿Qué NO se incluyó?

| Área | Decisión | Motivo |
|---|---|---|
| `ESTADOS_ACTIVOS_RESERVA` | NO incluir | El stock ya fue consumido al salir de EN_MONTAJE |
| `ESTADOS_PERMITIDOS_CANCELACION` | NO incluir | El evento ya ocurrió; cancelar acá no tiene sentido operativo |
| `ESTADOS_PERMITIDOS_GASTOS` | NO incluir | El evento ya ocurrió; no se registran más gastos |
| `CronogramaPolicy` | NO incluir | El cronograma es histórico; no se edita post-evento |
| `ESTADOS_CONFIRMACION_RESERVA` (DocumentoPolicy) | NO incluir | La reserva ya existió; el documento de confirmación aplica a eventos futuros |

### ¿Por qué no se agregó un flag `permitirPagos` en vez de un estado nuevo?

Porque el estado refleja una fase real del negocio: "el evento ya pasó pero la cobranza sigue activa". Un flag sería un parche técnico. Un estado modela la realidad y permite que todas las capas (jobs, políticas, UI) reaccionen consistentemente sin preguntar "¿este evento permite pagos?".

### Estados del ciclo de vida (actualizado)

```
COTIZACION → PROGRAMADO → CONFIRMADO → EN_MONTAJE ──┬──→ FINALIZADO
                                                     │
                                                     └──→ PENDIENTE_COBRO → FINALIZADO
                                                              (acepta pagos)
```

## DB requerido

El registro `PENDIENTE_COBRO` debe existir en `public.estado_evento`:

```sql
INSERT INTO public.estado_evento (estado_nombre, estado_descripcion)
VALUES ('PENDIENTE_COBRO', 'Evento completado operativamente con pagos pendientes de cobro.');
```

La secuencia `estado_evento_id_estadoevento_seq` se actualiza automáticamente.

## Tests

169 tests ejecutados. 168 pasan, 1 error pre-existente (`ArtepaxApplicationTests` requiere PostgreSQL). Cero regresiones introducidas.
