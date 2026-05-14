# Integración API Propuesta para Alerta de Anticipo en Pagos

Este documento define qué dato le falta hoy al frontend para mostrar una alerta real de anticipo mínimo dentro del módulo `Pagos`, sin duplicar lógica crítica del backend ni depender de cálculos parciales en UI.

## Quick Path

1. Exponer desde backend el estado del anticipo ya calculado para un evento.
2. Consumir ese resumen desde `Pagos > Por Evento` y `Detalle de Evento`.
3. Mostrar una alerta operativa con porcentaje requerido, porcentaje cubierto y monto faltante.

## Respuesta corta

Hoy el frontend NO tiene un contrato suficiente para mostrar bien la regla de anticipo mínimo en `Pagos`.

Sí sabe que existe la configuración `PORCENTAJE_ANTICIPO_MINIMO`, pero el contrato actual de pagos no expone un resumen listo para UI con:

- cuánto anticipo mínimo se requiere
- cuánto anticipo confirmado ya se pagó
- cuánto falta para cumplir
- si el anticipo ya está cumplido o no

## Qué existe hoy

| Fuente | Qué aporta | Qué falta |
|---|---|---|
| `02_PAGO_CONTRATO_BACKEND.md` | Dice que confirmar pago puede confirmar el evento si alcanza anticipo mínimo | No expone datos concretos de anticipo en el DTO de resumen |
| `01_EVENTO_CONTRATO_BACKEND.md` | Confirma que la validación depende de `PORCENTAJE_ANTICIPO_MINIMO` | No garantiza que `Pagos` reciba ese cálculo listo para UI |
| `13_CONFIGURACION_CONTRATO_BACKEND.md` | Expone la clave `PORCENTAJE_ANTICIPO_MINIMO` | Solo da la configuración, no el estado financiero del anticipo por evento |
| `modules/configuracion/configuracion.js` | Ya reconoce la clave en frontend | No conecta ese dato con el resumen de pagos |

## Problema actual de UX

El usuario puede estar en `Pagos > Por Evento` y ver:

- total del evento
- total cobrado
- saldo pendiente

Pero no puede ver claramente:

- si el evento ya alcanzó el anticipo mínimo para confirmarse
- cuánto % de anticipo exige la regla actual
- cuánto dinero falta para llegar al umbral

Entonces la UI hoy comunica la idea de anticipo solo como texto genérico, no como un estado de negocio verificable.

## Regla funcional que debe reflejar la UI

La fuente final sigue siendo el backend.

El frontend NO debe decidir por su cuenta si un evento cumple o no el anticipo mínimo como fuente de verdad.

Pero sí debe poder mostrar una alerta de negocio consistente usando datos ya calculados por backend.

## Opción recomendada

## Extender `GET /api/pagos/evento/{eventoId}/resumen`

La opción más simple para este módulo es enriquecer `ResumenPagoEventoDTO` con un bloque de anticipo.

### Campos nuevos sugeridos

| Campo | Tipo | Uso en UI |
|---|---|---|
| `porcentajeAnticipoMinimo` | `BigDecimal` | Mostrar `%` requerido |
| `montoMinimoAnticipo` | `BigDecimal` | Mostrar umbral en dinero |
| `anticipoConfirmado` | `BigDecimal` | Mostrar cuánto anticipo confirmado lleva el evento |
| `faltanteParaAnticipo` | `BigDecimal` | Mostrar cuánto falta para llegar al mínimo |
| `anticipoCumplido` | `Boolean` | Badge/alerta principal |
| `puedeConfirmarsePorAnticipo` | `Boolean` | Comunicar si, con el estado actual, el evento ya podría pasar a `CONFIRMADO` |

### Ejemplo de response enriquecida

```json
{
  "idEvento": 124,
  "precioTotal": 850.00,
  "totalCobrado": 300.00,
  "saldoPendiente": 550.00,
  "cobroCompleto": false,
  "totalDevolucionPendiente": 0.00,
  "tieneDevolucionPendiente": false,
  "porcentajeAnticipoMinimo": 30.00,
  "montoMinimoAnticipo": 255.00,
  "anticipoConfirmado": 180.00,
  "faltanteParaAnticipo": 75.00,
  "anticipoCumplido": false,
  "puedeConfirmarsePorAnticipo": false
}
```

## Semántica esperada de los campos

| Campo | Regla |
|---|---|
| `porcentajeAnticipoMinimo` | Valor vigente de `PORCENTAJE_ANTICIPO_MINIMO` |
| `montoMinimoAnticipo` | `precioTotal * porcentajeAnticipoMinimo / 100` |
| `anticipoConfirmado` | Suma confirmada de pagos cuyo concepto aplique como anticipo |
| `faltanteParaAnticipo` | `max(montoMinimoAnticipo - anticipoConfirmado, 0)` |
| `anticipoCumplido` | `anticipoConfirmado >= montoMinimoAnticipo` |
| `puedeConfirmarsePorAnticipo` | `true` solo si además el evento está en estado que permita confirmación automática/manual |

## Importante

`anticipoConfirmado` NO debería mezclar cualquier pago confirmado si la regla real del backend distingue conceptos como `ANTICIPO`.

Si backend hoy calcula anticipo usando solo pagos de concepto `ANTICIPO`, la respuesta para frontend debe respetar exactamente esa misma semántica.

## Alternativa aceptable

Si no se quiere tocar `ResumenPagoEventoDTO`, otra opción es agregar un endpoint específico:

## GET `/api/pagos/evento/{eventoId}/anticipo`

### Response sugerida

```json
{
  "eventoId": 124,
  "porcentajeAnticipoMinimo": 30.00,
  "montoMinimoAnticipo": 255.00,
  "anticipoConfirmado": 180.00,
  "faltanteParaAnticipo": 75.00,
  "anticipoCumplido": false,
  "puedeConfirmarsePorAnticipo": false
}
```

### Tradeoff

| Opción | Ventaja | Costo |
|---|---|---|
| Extender `resumen` | Un solo request para la UI | Cambia DTO existente |
| Endpoint `anticipo` separado | Mantiene `resumen` más acotado | Requiere request adicional |

Para `Pagos > Por Evento`, conviene MÁS extender `resumen`.

## Qué debería mostrar la UI con ese contrato

### Caso 1: anticipo no cumplido

```text
Anticipo requerido: 30% ($255.00)
Anticipo confirmado: $180.00
Faltan $75.00 para que el evento pueda confirmarse.
```

### Caso 2: anticipo cumplido

```text
Anticipo requerido: 30% ($255.00)
Anticipo confirmado: $300.00
Anticipo mínimo cumplido. El evento ya puede quedar confirmado según reglas de backend.
```

### Caso 3: evento no operativo para pagos

```text
El evento ya no admite nuevos pagos, aunque se muestre el historial financiero.
```

## Reglas para frontend

- No hardcodear el porcentaje mínimo.
- No asumir que `totalCobrado` es lo mismo que `anticipoConfirmado`.
- No usar el cálculo local como fuente final para habilitar confirmaciones críticas.
- Sí usar el resumen del backend para mostrar alertas, progreso y mensajes de negocio.

## Checklist

- [ ] `Pagos > Por Evento` muestra el porcentaje mínimo requerido.
- [ ] La UI muestra el monto de anticipo ya confirmado.
- [ ] La UI muestra el faltante para alcanzar el umbral.
- [ ] La UI diferencia `saldo pendiente total` de `faltante para anticipo`.
- [ ] El frontend no duplica la regla crítica como fuente final.

## Next Step

Pedir al backend una de estas dos implementaciones:

1. Extender `GET /api/pagos/evento/{eventoId}/resumen` con datos de anticipo.
2. Crear `GET /api/pagos/evento/{eventoId}/anticipo` si se quiere separar responsabilidades.

Para este proyecto, recomiendo la opción 1 porque resuelve mejor la UX del módulo `Pagos` con menos fricción de integración.
