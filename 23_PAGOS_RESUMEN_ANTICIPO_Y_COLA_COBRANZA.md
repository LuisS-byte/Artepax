# Pagos — Nuevos Endpoints: Resumen con Anticipo y Cola de Cobranza

Este documento describe los dos nuevos endpoints agregados al módulo de Pagos para soportar la bandeja operativa de cobranza y la alerta de anticipo mínimo.

## 1. Resumen de Pagos con Anticipo

### Endpoint

`GET /api/pagos/evento/{eventoId}/resumen`

### Cambio

El DTO `ResumenPagoEventoDTO` ahora incluye un bloque `anticipo` con información calculada por el backend.

### Nuevo campo: `anticipo` (objeto `AnticipoInfoDTO`)

| Campo | Tipo | Origen | Ejemplo |
|---|---|---|---|
| `porcentajeAnticipoMinimo` | `BigDecimal` | Configuración `PORCENTAJE_ANTICIPO_MINIMO` | `50.00` |
| `montoMinimoAnticipo` | `BigDecimal` | `precioTotal * porcentaje / 100` | `255.00` |
| `anticipoConfirmado` | `BigDecimal` | Suma de TODOS los pagos en estado `PAGADO` | `180.00` |
| `anticipoPorConcepto` | `BigDecimal` | Suma de pagos `PAGADO` con concepto `ANTICIPO` | `180.00` |
| `faltanteParaAnticipo` | `BigDecimal` | `max(montoMinimo - anticipoConfirmado, 0)` | `75.00` |
| `anticipoCumplido` | `Boolean` | `anticipoConfirmado >= montoMinimoAnticipo` | `false` |
| `puedeConfirmarsePorAnticipo` | `Boolean` | `anticipoCumplido && estadoEvento == PROGRAMADO` | `false` |

### Semántica de `anticipoConfirmado`

**Importante:** `anticipoConfirmado` suma TODOS los pagos en estado `PAGADO`, sin filtrar por concepto. Esto es coherente con la regla de negocio actual implementada en `EventoPagoValidator.tieneAnticipoSuficiente()`, que también considera el total cobrado.

El campo `anticipoPorConcepto` existe como información adicional para que el usuario pueda ver cuánto de ese total corresponde específicamente al concepto `ANTICIPO`.

### Ejemplo de response

```json
{
  "idEvento": 124,
  "precioTotal": 850.00,
  "totalCobrado": 300.00,
  "saldoPendiente": 550.00,
  "cobroCompleto": false,
  "totalDevolucionPendiente": 0.00,
  "tieneDevolucionPendiente": false,
  "anticipo": {
    "porcentajeAnticipoMinimo": 50.00,
    "montoMinimoAnticipo": 425.00,
    "anticipoConfirmado": 300.00,
    "anticipoPorConcepto": 300.00,
    "faltanteParaAnticipo": 125.00,
    "anticipoCumplido": false,
    "puedeConfirmarsePorAnticipo": false
  }
}
```

### Reglas de UI

| Escenario | Qué mostrar |
|---|---|
| `anticipoCumplido = false` | "Anticipo requerido: {porcentaje}% (${montoMinimo}). Confirmado: ${anticipoConfirmado}. Faltan ${faltante}." |
| `anticipoCumplido = true` | "Anticipo mínimo cumplido. El evento ya puede quedar confirmado." |
| Evento en `PROGRAMADO` + `anticipoCumplido = true` | `puedeConfirmarsePorAnticipo = true`. El evento pasará automáticamente a `CONFIRMADO` al confirmar el siguiente pago. |
| Evento en `CONFIRMADO` o posterior | `puedeConfirmarsePorAnticipo = false` porque ya no está en `PROGRAMADO`; mostrar como información histórica. |

### Qué NO debe hacer el frontend

- No hardcodear el porcentaje mínimo (viene del backend).
- No asumir que `totalCobrado` es lo mismo que `anticipoConfirmado` (son iguales en la práctica, pero conceptualmente `anticipoConfirmado` es la fuente para la regla de anticipo).
- No usar `anticipoCumplido` como fuente final para habilitar confirmaciones manuales; la decisión final la toma el backend.

---

## 2. Cola Operativa de Cobranza

### Endpoint

`GET /api/pagos/eventos-cobranza`

### Propósito

Devolver una lista priorizada de eventos con información financiera resumida, lista para renderizar en `Pagos > Por Evento` sin requests adicionales por fila.

### Query params

| Param | Tipo | Requerido | Descripción |
|---|---|---|---|
| `estado` | `String[]` | No | Filtrar por estados de evento (ej. `PROGRAMADO,CONFIRMADO`) |
| `soloConSaldo` | `Boolean` | No | Solo eventos con `saldoPendiente > 0` |
| `soloConDevolucion` | `Boolean` | No | Solo eventos con `tieneDevolucionPendiente = true` |
| `soloConPagosPendientes` | `Boolean` | No | Solo eventos con `cantidadPagosPendientes > 0` |
| `q` | `String` | No | Buscar por nombre de cliente o ID de evento |
| `limit` | `Integer` | No | Límite de resultados |

### Response

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

### Qué eventos incluye

Solo eventos en estados: `PROGRAMADO`, `CONFIRMADO`, `EN_MONTAJE`, `CANCELACION_PENDIENTE`.

No incluye `COTIZACION`, `FINALIZADO` ni `CANCELADO`.

### Ordenamiento

El backend devuelve los resultados ya ordenados por prioridad:

1. Eventos con `tieneDevolucionPendiente = true` (primero, más urgente)
2. Eventos con `saldoPendiente > 0`
3. Eventos con `cantidadPagosPendientes > 0`
4. Resto de eventos

Dentro de cada grupo, se ordena por mayor saldo pendiente (descendente).

### Ejemplos de uso

| Caso | URL |
|---|---|
| Todos los eventos de cobranza | `GET /api/pagos/eventos-cobranza` |
| Solo con saldo pendiente | `GET /api/pagos/eventos-cobranza?soloConSaldo=true` |
| Solo con devolución pendiente | `GET /api/pagos/eventos-cobranza?soloConDevolucion=true` |
| Solo con pagos por confirmar | `GET /api/pagos/eventos-cobranza?soloConPagosPendientes=true` |
| Buscar por cliente | `GET /api/pagos/eventos-cobranza?q=Ana` |
| Filtrar por estado + límite | `GET /api/pagos/eventos-cobranza?estado=PROGRAMADO&limit=10` |

### Reglas de UI

| Filtro UI | Query param sugerido |
|---|---|
| Tab "Con saldo pendiente" | `soloConSaldo=true` |
| Tab "Con devolución pendiente" | `soloConDevolucion=true` |
| Tab "Pagos por confirmar" | `soloConPagosPendientes=true` |
| Búsqueda en barra | `q={texto}` |
| Selector de estado | `estado={estado}` |

---

## 3. Archivos modificados

| Archivo | Cambio |
|---|---|
| `pago/dto/AnticipoInfoDTO.java` | **Nuevo** — DTO con campos de anticipo |
| `pago/dto/EventoCobranzaDTO.java` | **Nuevo** — DTO para cola de cobranza |
| `pago/dto/ResumenPagoEventoDTO.java` | Agregado campo `anticipo` + constructor nuevo |
| `pago/repository/PagoRepository.java` | Agregada query `findEventosCobranzaRaw()` |
| `pago/service/PagoService.java` | Agregado método `getEventosCobranza()` |
| `pago/service/impl/PagoServiceImpl.java` | Inyectado `ConfiguracionService`, implementados `calcularAnticipo()` y `getEventosCobranza()` |
| `pago/controller/PagoController.java` | Agregado endpoint `GET /eventos-cobranza` |
