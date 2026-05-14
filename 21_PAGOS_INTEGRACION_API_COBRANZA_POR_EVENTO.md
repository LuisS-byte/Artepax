# Integración API Propuesta para Cobranza por Evento

Este documento define la integración backend/frontend necesaria para rediseñar `Pagos > Por Evento` como una cola operativa de cobranza. El objetivo es que el usuario vea primero los eventos que requieren acción, en vez de tener que buscarlos manualmente en un selector.

## Objetivo

La nueva entrada de `Por Evento` debe responder en un solo flujo:

1. Qué eventos tienen saldo pendiente.
2. Qué eventos tienen devolución pendiente.
3. Qué eventos tienen pagos pendientes de confirmar.
4. Qué evento conviene abrir primero.

## Problema Actual

Con la API actual, el frontend puede listar eventos y abrir el resumen de un evento puntual, pero no tiene un endpoint optimizado para construir una cola de cobranza.

Hoy existen:

- `GET /api/eventos`
- `GET /api/pagos/evento/{eventoId}`
- `GET /api/pagos/evento/{eventoId}/resumen`

Eso permite construir la experiencia, pero obliga a hacer múltiples requests por evento para detectar cuáles tienen saldo pendiente o devoluciones pendientes.

## Necesidad de Frontend

El frontend necesita una lista operativa que, por cada evento relevante, incluya al menos:

| Campo | Uso en UI |
|---|---|
| `eventoId` | navegación al detalle |
| `clienteNombre` | reconocimiento humano |
| `estadoEventoNombre` | badge y reglas de acción |
| `precioTotal` | contexto financiero |
| `totalCobrado` | progreso de cobro |
| `saldoPendiente` | priorización |
| `tieneDevolucionPendiente` | alerta crítica |
| `totalDevolucionPendiente` | monto a resolver |
| `cantidadPagosPendientes` | acción de confirmación |

## Opción Recomendada

## GET `/api/pagos/eventos-cobranza`

Propósito: devolver una cola operativa de eventos lista para renderizar en `Pagos > Por Evento`.

### Query params sugeridos

| Param | Tipo | Requerido | Descripción |
|---|---|---|---|
| `estado` | `String[]` | No | Filtrar por estados de evento |
| `soloConSaldo` | `Boolean` | No | Solo eventos con `saldoPendiente > 0` |
| `soloConDevolucion` | `Boolean` | No | Solo eventos con devolución pendiente |
| `soloConPagosPendientes` | `Boolean` | No | Solo eventos con pagos `PENDIENTE` |
| `q` | `String` | No | Buscar por cliente o número de evento |
| `limit` | `Integer` | No | Límite de resultados |

### Response sugerida

```json
[
  {
    "eventoId": 124,
    "clienteNombre": "Ana Torres",
    "estadoEventoNombre": "PROGRAMADO",
    "precioTotal": 850.00,
    "totalCobrado": 300.00,
    "saldoPendiente": 550.00,
    "cobroCompleto": false,
    "tieneDevolucionPendiente": false,
    "totalDevolucionPendiente": 0.00,
    "cantidadPagosPendientes": 1
  }
]
```

## Orden sugerido del backend

Para ayudar a la UX, el backend debería devolver los datos ya priorizados así:

1. eventos con `tieneDevolucionPendiente = true`
2. eventos con `saldoPendiente > 0`
3. eventos con `cantidadPagosPendientes > 0`
4. resto de eventos

Dentro de cada grupo, ordenar por fecha próxima o por mayor saldo pendiente.

## Opción Alternativa

Si no se quiere crear un endpoint nuevo, otra opción aceptable es enriquecer `GET /api/eventos` con datos financieros resumidos.

Campos agregados sugeridos:

- `totalCobrado`
- `saldoPendiente`
- `cobroCompleto`
- `tieneDevolucionPendiente`
- `totalDevolucionPendiente`
- `cantidadPagosPendientes`

Ventaja:

- menos cambios en frontend

Desventaja:

- mezcla responsabilidades del módulo Evento con necesidades operativas de Pagos

## Opción Temporal Solo Frontend

Mientras no exista endpoint nuevo, el frontend puede resolver una primera versión así:

1. llamar `GET /api/eventos`
2. iterar eventos
3. llamar `GET /api/pagos/evento/{eventoId}/resumen` por cada evento
4. opcionalmente llamar `GET /api/pagos/evento/{eventoId}` para contar pagos pendientes
5. construir una lista local y ordenarla

## Riesgo de esta opción

- genera múltiples requests
- escala mal con muchos eventos
- puede cargar lento la bandeja operativa

Esta opción sirve para validar UX, no como solución final de rendimiento.

## Contrato Mínimo que Necesita la Nueva UI

Para que `Pagos > Por Evento` deje de depender de un selector manual, la UI necesita poder renderizar una lista como esta sin requests adicionales por fila:

| Dato | Obligatorio |
|---|---|
| `eventoId` | Sí |
| `clienteNombre` | Sí |
| `estadoEventoNombre` | Sí |
| `saldoPendiente` | Sí |
| `tieneDevolucionPendiente` | Sí |
| `cantidadPagosPendientes` | Sí |
| `precioTotal` | Recomendado |
| `totalCobrado` | Recomendado |
| `totalDevolucionPendiente` | Recomendado |

## Reglas de UI que dependen de este contrato

- La bandeja inicial debe poder mostrar `Con saldo pendiente`.
- La bandeja inicial debe poder mostrar `Con devolución pendiente`.
- La bandeja inicial debe poder destacar `pagos pendientes de confirmar`.
- El usuario debe poder elegir un evento sin recordar manualmente su nombre.

## Recomendación Final

La mejor integración para esta mejora es un endpoint específico de cola operativa de cobranza.

Si eso no se implementa todavía, el frontend puede avanzar con una versión temporal basada en `GET /api/eventos` + resúmenes por evento, pero debería considerarse una solución transitoria.

## Siguiente Paso

1. Confirmar con backend si se acepta `GET /api/pagos/eventos-cobranza`.
2. Si no, implementar una primera versión temporal en frontend con agregación local.
3. Diseñar la nueva vista `Por Evento` en torno a esa cola operativa.
