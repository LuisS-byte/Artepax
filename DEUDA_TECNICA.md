# Deuda Técnica — Frontend Artepax

Documento vivo. Agregar items nuevos al final.

---

## 1. Endpoint implementado: GET /api/eventos/{id}/productos

**Fecha**: 2026-05-10
**Estado**: ✅ RESUELTO — listo para consumo frontend

### Descripción

Devuelve la lista de productos asociados a un evento. El comportamiento cambia según el estado del evento:

- **COTIZACION**: devuelve la composición de la decoración (lo que se reservará si se programa). No hay reservas reales todavía.
- **PROGRAMADO, CONFIRMADO, EN_MONTAJE, FINALIZADO, CANCELADO, CANCELACION_PENDIENTE**: devuelve las reservas efectivas de la tabla `evento_producto`.

### Contrato del endpoint

| Propiedad | Valor |
|---|---|
| Método | `GET` |
| Ruta | `/api/eventos/{id}/productos` |
| Auth requerida | `ADMIN` (heredada del controller) |
| Path param | `id` (Long) — ID del evento |
| Query params | Ninguno |
| Request body | Ninguno |
| HTTP esperado | `200 OK` |
| Response type | `Array<EventoProductoDTO>` |

### DTO de respuesta: EventoProductoDTO

| Campo | Tipo | JSON | Nullable | Descripción |
|---|---|---|---|---|
| `productoId` | `Long` | `productoId` | No | ID del producto. |
| `nombre` | `String` | `nombre` | No | Nombre del producto (ej. "Silla Tiffany"). |
| `cantidad` | `Integer` | `cantidad` | No | Cantidad reservada o requerida. |
| `costoUnitario` | `BigDecimal` | `costoUnitario` | No | Costo interno por unidad (`produ_precio_unitario`). **No es precio de venta.** |
| `costoTotal` | `BigDecimal` | `costoTotal` | No | `cantidad × costoUnitario`. Calculado en backend. |
| `estadoReserva` | `String` | `estadoReserva` | No | Estado de la reserva. Ver tabla de estados abajo. |
| `origen` | `String` | `origen` | No | `"DECORACION"` o `"EXTRA"`. Ver sección de origen. |

### Estados de reserva (`estadoReserva`)

| Valor | Cuándo aparece | Significado |
|---|---|---|
| `COTIZADO` | Solo en estado COTIZACION del evento | Proyección. No hay reserva real en DB. |
| `ACTIVA` | PROGRAMADO, CONFIRMADO, EN_MONTAJE, CANCELACION_PENDIENTE | Reserva vigente que bloquea stock. |
| `CONSUMIDA` | FINALIZADO | Reserva consumida al finalizar el evento. |
| `LIBERADA` | CANCELADO | Reserva liberada por cancelación. |

### Origen del producto (`origen`)

| Valor | Significado | Cómo se determina |
|---|---|---|
| `DECORACION` | Producto que viene de la composición de la decoración | Existe en `decoracionxproducto` para la decoración del evento. |
| `EXTRA` | Producto agregado manualmente vía PATCH `/api/eventos/{id}/extras` | No existe en `decoracionxproducto` para esa decoración. |

### Ejemplo de respuesta — Evento en COTIZACION

```json
[
  {
    "productoId": 1,
    "nombre": "Silla Tiffany",
    "cantidad": 20,
    "costoUnitario": 15.00,
    "costoTotal": 300.00,
    "estadoReserva": "COTIZADO",
    "origen": "DECORACION"
  },
  {
    "productoId": 2,
    "nombre": "Mesa redonda 1.5m",
    "cantidad": 5,
    "costoUnitario": 40.00,
    "costoTotal": 200.00,
    "estadoReserva": "COTIZADO",
    "origen": "DECORACION"
  }
]
```

En COTIZACION **todos** los productos son `DECORACION` y `COTIZADO`. No puede haber extras porque el endpoint `/extras` solo funciona en PROGRAMADO o CONFIRMADO.

### Ejemplo de respuesta — Evento en PROGRAMADO con extras

```json
[
  {
    "productoId": 1,
    "nombre": "Silla Tiffany",
    "cantidad": 20,
    "costoUnitario": 15.00,
    "costoTotal": 300.00,
    "estadoReserva": "ACTIVA",
    "origen": "DECORACION"
  },
  {
    "productoId": 2,
    "nombre": "Mesa redonda 1.5m",
    "cantidad": 5,
    "costoUnitario": 40.00,
    "costoTotal": 200.00,
    "estadoReserva": "ACTIVA",
    "origen": "DECORACION"
  },
  {
    "productoId": 5,
    "nombre": "Centro de mesa floral",
    "cantidad": 10,
    "costoUnitario": 25.00,
    "costoTotal": 250.00,
    "estadoReserva": "ACTIVA",
    "origen": "EXTRA"
  }
]
```

### Ejemplo de respuesta — Evento FINALIZADO

```json
[
  {
    "productoId": 1,
    "nombre": "Silla Tiffany",
    "cantidad": 20,
    "costoUnitario": 15.00,
    "costoTotal": 300.00,
    "estadoReserva": "CONSUMIDA",
    "origen": "DECORACION"
  },
  {
    "productoId": 5,
    "nombre": "Centro de mesa floral",
    "cantidad": 10,
    "costoUnitario": 25.00,
    "costoTotal": 250.00,
    "estadoReserva": "CONSUMIDA",
    "origen": "EXTRA"
  }
]
```

### Ejemplo de respuesta — Evento CANCELADO

```json
[
  {
    "productoId": 1,
    "nombre": "Silla Tiffany",
    "cantidad": 20,
    "costoUnitario": 15.00,
    "costoTotal": 300.00,
    "estadoReserva": "LIBERADA",
    "origen": "DECORACION"
  }
]
```

### Respuesta vacía

Si el evento no existe, el backend lanza `EventoNotFoundException` con HTTP 404. No devuelve array vacío.

Si el evento existe pero no tiene productos (caso teórico imposible porque toda decoración tiene al menos 1 producto), devuelve `[]`.

### Errores posibles

| HTTP | Causa | Mensaje esperado |
|---|---|---|
| 404 | Evento no encontrado | `Evento no encontrado con ID: {id}` |
| 403 | Sin rol ADMIN | Mensaje de Spring Security |
| 500 | Error interno | `Error interno del servidor` |

### Cómo integrarlo en el frontend

#### 1. Agregar en `js/shared/api.js`

```javascript
// Dentro del objeto API.eventos:
productos: (id) => fetchGet(`/api/eventos/${id}/productos`),
```

O la convención que use el frontend para hacer requests autenticados.

#### 2. Dónde consumirlo

En la **modal de detalle del evento**, agregar una sección acordeón o tabla "Productos del Evento" que:

1. Llame a `API.eventos.productos(eventoId)` al abrir la modal
2. Renderice una tabla con columnas: Producto, Cantidad, Costo Unitario, Costo Total, Estado, Origen
3. Agrupe visualmente por `origen` (DECORACION primero, EXTRA después — el backend ya ordena así)

#### 3. Renderizado sugerido por estado del evento

| Estado del evento | Qué mostrar |
|---|---|
| COTIZACION | Tabla con encabezado "Productos incluidos en la cotización". Badge "COTIZADO". No mostrar sección de extras porque no puede haber. |
| PROGRAMADO | Tabla con encabezado "Productos reservados". Mostrar badges ACTIVA. Si hay productos con origen EXTRA, mostrar sección separada "Extras agregados". |
| CONFIRMADO | Igual que PROGRAMADO. |
| EN_MONTAJE | Igual que PROGRAMADO. |
| FINALIZADO | Tabla con encabezado "Productos consumidos". Badge CONSUMIDA. |
| CANCELADO | Tabla con encabezado "Productos liberados". Badge LIBERADA. |
| CANCELACION_PENDIENTE | Igual que PROGRAMADO (las reservas siguen ACTIVA hasta confirmar cancelación). |

#### 4. Cálculos que NO debe hacer el frontend

- **NO sumar `costoTotal` para obtener el precio del evento.** El precio total viene en `evento.precioTotal` y es `precioBase + precioAdicional`. La suma de `costoTotal` de productos es el **costo interno de materiales**, no lo que se cobra al cliente.
- **NO calcular `costoTotal` manualmente.** Ya viene calculado del backend como `cantidad × costoUnitario`.
- **NO asumir que `costoUnitario` es el precio de venta.** Es el costo interno de reposición del producto.

#### 5. Relación con el precio adicional

El `precioAdicional` del evento (que ya viene en `EventoResponseDTO`) es el monto global que se cobra por los extras. **No hay relación directa** entre `costoTotal` de los productos EXTRA y el `precioAdicional`. El operador decide cuánto cobrar por los extras; el costo interno es solo referencia.

Ejemplo:
- 10 centros de mesa floral → `costoTotal` = $250 (costo interno)
- El operador cobró $400 por esos extras → `precioAdicional` = $400
- La diferencia ($150) es margen, pero no se calcula en el frontend.

#### 6. Cuándo refrescar la lista de productos

| Acción del usuario | Refrescar productos? |
|---|---|
| Abrir modal de detalle | Sí |
| Programar evento (COTIZACION → PROGRAMADO) | Sí — pasa de COTIZADO a ACTIVA |
| Agregar extras (PATCH `/extras`) | Sí — aparecen nuevos productos EXTRA |
| Finalizar evento | Sí — pasa de ACTIVA a CONSUMIDA |
| Cancelar evento (confirmar cancelación) | Sí — pasa de ACTIVA a LIBERADA |
| Revertir cancelación | Sí — vuelve a ACTIVA |
| Editar evento (PUT) | No — los productos no cambian |

### Notas importantes para el frontend

1. **No inventar acciones por estado.** Usar `estadoEventoNombre` del evento para determinar qué mostrar.
2. **El orden de la respuesta** ya viene del backend: primero DECORACION, luego EXTRA, ambos ordenados alfabéticamente por nombre.
3. **En COTIZACION**, todos los productos tienen `estadoReserva: "COTIZADO"`. Este estado no existe en la tabla `estado_reserva` — es un valor virtual que solo usa este endpoint para indicar que es una proyección.
4. **La lista puede estar vacía** solo si el evento no tiene decoración asignada (caso borde). En la práctica, toda decoración tiene al menos 1 producto.
5. **No confundir `costoTotal` con `precioTotal`.** Son conceptos distintos: costo interno vs precio de venta.
6. **El endpoint es de solo lectura.** No modifica nada en la DB.

---

## 2. Pendiente

_(agregar aquí)_
