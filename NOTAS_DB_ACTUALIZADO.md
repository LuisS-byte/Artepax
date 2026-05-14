# ARTEPAX — Documentación Completa de Base de Datos (Actualizada)

> Sistema de gestión de eventos y decoraciones  
> Motor observado: PostgreSQL 18.1  
> Estructura esperada: `Artepax_DDL_v4_2.sql`  
> Seed histórico de referencia: `Artepax_seed_v4_2.sql`  
> Estado real observado: `ArtepaxMaster.sql` (dump iniciado el 2026-05-02 13:34:01)  
> Documento generado: 2026-05-02

---

## Tabla de Contenidos

1. [Visión General](#1-visión-general)
2. [Contexto del Negocio](#2-contexto-del-negocio)
3. [Esquema Completo de Tablas](#3-esquema-completo-de-tablas)
4. [Relaciones entre Entidades](#4-relaciones-entre-entidades)
5. [Flujos de Estado](#5-flujos-de-estado)
6. [Reglas de Negocio por Módulo](#6-reglas-de-negocio-por-módulo)
7. [Vistas Calculadas](#7-vistas-calculadas)
8. [Configuración Global](#8-configuración-global)
9. [Lógica de Stock](#9-lógica-de-stock)
10. [Lógica de Pagos](#10-lógica-de-pagos)
11. [Lógica de Cancelación](#11-lógica-de-cancelación)
12. [Auditoría](#12-auditoría)
13. [Notificaciones](#13-notificaciones)
14. [Decisiones de Diseño y Su Razón](#14-decisiones-de-diseño-y-su-razón)
15. [Lo que el Backend DEBE validar](#15-lo-que-el-backend-debe-validar)
16. [Lo que el Backend NO necesita validar](#16-lo-que-el-backend-no-necesita-validar)
17. [Diferencias entre el NOTAS_DB anterior y la BD Actual](#17-diferencias-entre-el-notas_db-anterior-y-la-bd-actual)
18. [Riesgos Técnicos y Observaciones](#18-riesgos-técnicos-y-observaciones)
19. [Veredicto Final](#19-veredicto-final)

---

## 1. Visión General

Artepax es una base de datos relacional para operar un negocio de decoraciones y eventos. El modelo cubre clientes, cotizaciones, reservas, cronograma, pagos, gastos, inventario, documentos, notificaciones, auditoría y configuración global.

**Resumen estructural observado:**
- 25 tablas en el dump actual.
- 5 vistas calculadas en el dump actual.
- PKs con secuencias `BIGSERIAL` o equivalentes `bigint + sequence` observados en el dump.
- Checks e índices alineados con el DDL v4.2.
- Dos FKs con `ON DELETE SET NULL` observadas realmente: `auditoria.id_usuario` y `notificacion.id_evento`.

**Fuentes utilizadas y precedencia aplicada:**

| Fuente | Rol en el análisis | Prioridad |
|---|---|---|
| `Artepax_DDL_v4_2.sql` | Fuente de verdad de la estructura física esperada | 1 |
| `ArtepaxMaster.sql` | Fuente de verdad del estado real actual | 2 |
| `NOTAS_DB.md` | Base editorial, narrativa y semántica documental | 3 |
| `Artepax_seed_v4_2.sql` | Referencia histórica de catálogo y configuración inicial | 4 |

**Estado general del documento:**
- La estructura base del `NOTAS_DB.md` anterior sigue siendo útil.
- El esquema real actual está ampliamente alineado con lo documentado.
- La BD real NO está en seed v4.2 puro: contiene ampliaciones equivalentes a una semilla v4.3 y un usuario adicional de desarrollo.
- La BD actual no contiene aún datos transaccionales de operación real: no hay eventos, pagos, cronogramas, gastos, documentos emitidos, movimientos de inventario, notificaciones ni auditoría cargados.

---

## 2. Contexto del Negocio

El sistema sostiene el flujo administrativo interno de Artepax. No es una base pensada para autoservicio del cliente final.

**Flujo operativo resumido:**
1. Se registra un cliente y se crea una cotización.
2. Si la propuesta avanza, el evento pasa a `PROGRAMADO` y se reservan productos en `evento_producto`.
3. Con un anticipo suficiente, el evento puede pasar a `CONFIRMADO`.
4. Desde ahí se habilita el cronograma y la asignación de personal.
5. El evento puede entrar en `EN_MONTAJE`.
6. Al finalizar, se registran salidas reales de inventario.
7. Si se cancela, existe un estado intermedio `CANCELACION_PENDIENTE` con tiempo de gracia.

**Módulos principales sostenidos por la BD:**
- Seguridad y usuarios.
- Clientes.
- Catálogos del negocio.
- Eventos y estados.
- Inventario y reservas.
- Cronograma y personal.
- Pagos y gastos.
- Documentos.
- Notificaciones.
- Auditoría.
- Configuración global.

**Estado real observado hoy:**
- El modelo está listo para operar.
- La base actual parece ambiente de desarrollo temprano o de validación estructural.
- El inventario y catálogos sí están sembrados.
- La operación comercial real todavía no dejó trazas en las tablas transaccionales.

---

## 3. Esquema Completo de Tablas

### 3.1 `rol`
**Propósito funcional:** catálogo de roles del sistema.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_rol` | BIGINT | PK, sequence | |
| `rol_nombre` | VARCHAR(50) | NOT NULL, UNIQUE | Base para autorización |
| `rol_descripcion` | VARCHAR(255) | nullable | |

**Índices relevantes:** unicidad sobre `rol_nombre`.

**Notas para backend:** el backend puede mapearlo a `ROLE_{nombre}` sin tabla puente adicional.

**Observación real:** existen 2 registros: `ADMIN`, `EMPLEADO`.

**Trazabilidad:** sigue igual en DDL, dump y documento previo.

---

### 3.2 `usuario`
**Propósito funcional:** usuarios administrativos del sistema.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_usuario` | BIGINT | PK, sequence | |
| `usu_nombre` | VARCHAR(100) | NOT NULL | |
| `usu_correo` | VARCHAR(100) | NOT NULL | Unicidad real vía índice funcional |
| `usu_password` | VARCHAR(255) | NOT NULL | |
| `usu_telefono` | VARCHAR(20) | nullable | |
| `usu_tipo_auth` | VARCHAR(10) | NOT NULL, DEFAULT `LOCAL` | CHECK `LOCAL`, `GOOGLE` |
| `estado` | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| `usu_fechacreacion` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| `id_rol` | BIGINT | NOT NULL, FK -> `rol` | |

**Restricciones relevantes:** `chk_usuario_tipo_auth`.

**Índices relevantes:**
- `idx_usuario_correo_lower` UNIQUE sobre `LOWER(usu_correo)`.
- `idx_usuario_rol`.

**Notas para backend:**
- La unicidad de correo es case-insensitive en DB.
- `estado = FALSE` preserva historial.
- La gestión de permisos depende del rol y del backend, no del esquema.

**Observación real de uso:**
- El seed histórico v4.2 tenía 3 usuarios.
- El dump actual tiene 4 usuarios: los 3 originales y `Ernesto Mendoza` (`id_usuario = 7`).
- El registro adicional aparece como agregado de desarrollo.

**Hallazgo de datos reales:**
- En `id_usuario = 7`, `usu_tipo_auth = 'LOCAL'` pero `usu_password = 'GOOGLE_AUTH'`.
- Hecho observado: el valor no viola el esquema.
- Riesgo técnico: su semántica parece inconsistente con la intención de autenticación.

**Trazabilidad:** cambió en datos reales respecto del seed histórico; estructura sigue igual.

---

### 3.3 `cliente`
**Propósito funcional:** personas o empresas que contratan eventos.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_cliente` | BIGINT | PK, sequence | |
| `cli_nombre` | VARCHAR(100) | NOT NULL | |
| `cli_telefono` | VARCHAR(20) | nullable | No único |
| `cli_correo` | VARCHAR(100) | nullable | Unicidad parcial case-insensitive |
| `cli_direccion` | VARCHAR(255) | nullable | |
| `cli_activo` | BOOLEAN | NOT NULL, DEFAULT TRUE | Soft delete |
| `cli_fechacreacion` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |

**Índices relevantes:**
- `idx_cliente_correo_unique` UNIQUE parcial sobre `LOWER(cli_correo)` cuando no es NULL.
- `idx_cliente_telefono` parcial cuando no es NULL.

**Notas para backend:**
- El teléfono puede repetirse.
- `cli_activo` debe usarse como filtro funcional para nuevas asignaciones.

**Observación real:** hay 3 clientes y coinciden con la semilla histórica.

**Trazabilidad:** sigue igual.

---

### 3.4 `tipo_evento`
**Propósito funcional:** catálogo editable de tipos de evento.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_tipo_evento` | BIGINT | PK, sequence | |
| `tipo_nombre` | VARCHAR(50) | NOT NULL, UNIQUE | |
| `tipo_descripcion` | VARCHAR(255) | nullable | |
| `tipo_activo` | BOOLEAN | NOT NULL, DEFAULT TRUE | |

**Notas para backend:** el modelo permite desactivar tipos sin borrar historial.

**Observación real:** 7 tipos activos, iguales al seed v4.2.

**Trazabilidad:** sigue igual.

---

### 3.5 `estado_evento`
**Propósito funcional:** catálogo del ciclo de vida del evento.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_estadoevento` | BIGINT | PK, sequence | |
| `estado_nombre` | VARCHAR(30) | NOT NULL, UNIQUE | Sin CHECK de transición |
| `estado_descripcion` | VARCHAR(255) | nullable | |

**Estados observados:** `COTIZACION`, `PROGRAMADO`, `CONFIRMADO`, `EN_MONTAJE`, `FINALIZADO`, `CANCELADO`, `CANCELACION_PENDIENTE`.

**Notas para backend:** las transiciones válidas NO están en DB; el backend debe orquestarlas.

**Observación real:** catálogo completo presente.

**Trazabilidad:** sigue igual.

---

### 3.6 `producto`
**Propósito funcional:** inventario físico del negocio.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_producto` | BIGINT | PK, sequence | |
| `produ_nombre` | VARCHAR(100) | NOT NULL, UNIQUE | |
| `produ_descripcion` | VARCHAR(255) | nullable | |
| `produ_cantidad_total` | INT | NOT NULL, DEFAULT 0, CHECK >= 0 | Stock físico |
| `produ_precio_unitario` | NUMERIC(10,2) | NOT NULL, DEFAULT 0, CHECK >= 0 | Costo unitario |
| `produ_stock_minimo` | INT | NOT NULL, DEFAULT 0, CHECK >= 0 | Umbral fijo por producto |
| `produ_estado` | VARCHAR(10) | NOT NULL, DEFAULT `ACTIVO` | CHECK `ACTIVO`, `INACTIVO` |
| `produ_creacion` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |

**Notas para backend:**
- `produ_cantidad_total` no expresa disponibilidad real si hay reservas.
- Para disponibilidad operativa debe consultarse `vista_stock_producto`.
- La lógica de fallback con `PORCENTAJE_STOCK_MINIMO_GLOBAL` NO existe en DB; si se usa, es responsabilidad del backend.

**Observación real:** 8 productos, todos activos, iguales al seed v4.2.

**Trazabilidad:** sigue igual.

---

### 3.7 `decoracion`
**Propósito funcional:** paquetes de decoración ofrecidos por el negocio.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_decoracion` | BIGINT | PK, sequence | |
| `deco_nombre` | VARCHAR(100) | NOT NULL, UNIQUE | |
| `decor_descripcion` | VARCHAR(255) | nullable | |
| `decor_precio` | NUMERIC(10,2) | NOT NULL, DEFAULT 0, CHECK >= 0 | Base para `event_precio_base` |
| `decor_imagen_url` | VARCHAR(500) | nullable | |
| `decor_estado` | VARCHAR(10) | NOT NULL, DEFAULT `ACTIVO` | CHECK `ACTIVO`, `INACTIVO` |
| `decor_creacion` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |

**Notas para backend:** el precio se copia al evento; no hay sincronización automática posterior.

**Observación real:** 3 decoraciones activas, sin URL de imagen cargada.

**Trazabilidad:** sigue igual.

---

### 3.8 `decoracionxproducto`
**Propósito funcional:** tabla puente que define la composición de cada decoración.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_decoracion` | BIGINT | PK compuesta, FK -> `decoracion` | |
| `id_producto` | BIGINT | PK compuesta, FK -> `producto` | |
| `cantidad_requerida` | INT | NOT NULL, CHECK > 0 | |

**Índices relevantes:** `idx_dxp_producto`.

**Notas para backend:** esta tabla es la plantilla inicial de reserva cuando el evento avanza a `PROGRAMADO`.

**Observación real:** 12 relaciones decoración-producto, iguales al seed v4.2.

**Trazabilidad:** sigue igual.

---

### 3.9 `estado_pago`
**Propósito funcional:** catálogo del ciclo de vida del pago.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_estado_pago` | BIGINT | PK, sequence | |
| `estado_nombre` | VARCHAR(20) | NOT NULL, UNIQUE | Sin CHECK |
| `estado_descripcion` | VARCHAR(255) | nullable | |

**Observación real:** `PENDIENTE`, `PAGADO`, `CANCELADO`.

**Trazabilidad:** sigue igual.

---

### 3.10 `concepto_pago`
**Propósito funcional:** catálogo del propósito del pago.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_concepto_pago` | BIGINT | PK, sequence | |
| `concepto_nombre` | VARCHAR(30) | NOT NULL, UNIQUE | |
| `concepto_descripcion` | VARCHAR(255) | nullable | |

**Observación real:** `ANTICIPO`, `ABONO`, `SALDO_TOTAL`, `PENALIZACION`.

**Trazabilidad:** sigue igual.

---

### 3.11 `metodo_pago`
**Propósito funcional:** catálogo de medios de pago.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_metodo_pago` | BIGINT | PK, sequence | |
| `metodo_nombre` | VARCHAR(30) | NOT NULL, UNIQUE | |
| `metodo_descripcion` | VARCHAR(255) | nullable | |
| `metodo_activo` | BOOLEAN | NOT NULL, DEFAULT TRUE | |

**Observación real:** `EFECTIVO`, `TRANSFERENCIA`, `TARJETA`, todos activos.

**Trazabilidad:** sigue igual.

---

### 3.12 `evento`
**Propósito funcional:** entidad central del negocio.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_evento` | BIGINT | PK, sequence | |
| `id_cliente` | BIGINT | NOT NULL, FK -> `cliente` | |
| `id_decoracion` | BIGINT | NOT NULL, FK -> `decoracion` | |
| `id_tipo_evento` | BIGINT | NOT NULL, FK -> `tipo_evento` | |
| `id_estadoevento` | BIGINT | NOT NULL, FK -> `estado_evento` | |
| `id_estado_anterior` | BIGINT | nullable, FK -> `estado_evento` | |
| `event_fechaevento` | DATE | NOT NULL | |
| `event_horainicio` | TIME | nullable | |
| `event_horafin` | TIME | nullable, CHECK horario | |
| `event_ubicacion` | VARCHAR(255) | nullable | |
| `event_precio_base` | NUMERIC(10,2) | NOT NULL, DEFAULT 0, CHECK >= 0 | |
| `event_precio_adicional` | NUMERIC(10,2) | NOT NULL, DEFAULT 0, CHECK >= 0 | |
| `event_precio_total` | NUMERIC(10,2) | GENERATED ALWAYS AS STORED | Base + adicional |
| `event_nota_precio` | VARCHAR(255) | nullable, CHECK contextual | Obligatoria si adicional > 0 |
| `event_nota` | VARCHAR(255) | nullable | |
| `motivo_cancelacion` | VARCHAR(255) | nullable | |
| `event_cliente_notificado` | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| `event_fecha_confirmacion` | TIMESTAMP | nullable | |
| `event_fecha_cancelacion` | TIMESTAMP | nullable | |
| `event_fecha_expiracion` | DATE | nullable | |
| `event_creacion` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |

**Restricciones relevantes:**
- `chk_evento_precio_base`.
- `chk_evento_precio_adicional`.
- `chk_evento_nota_precio`.
- `chk_evento_horario`.

**Índices relevantes:**
- `idx_evento_cliente`, `idx_evento_decoracion`, `idx_evento_tipo`, `idx_evento_estado`.
- `idx_evento_estado_fecha`.
- `idx_evento_fecha`.

**Notas para backend:**
- `event_precio_total` no debe escribirse manualmente.
- El backend debe validar transiciones, solapamientos y reglas de anticipo.
- `motivo_cancelacion` NO es obligatorio por esquema; debe imponerlo el backend.

**Observación real:** no existen eventos cargados en la BD actual.

**Trazabilidad:** estructura sigue igual; falta evidencia operativa real en datos.

---

### 3.13 `evento_producto`
**Propósito funcional:** reserva de stock por evento.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_evento` | BIGINT | PK compuesta, FK -> `evento` | |
| `id_producto` | BIGINT | PK compuesta, FK -> `producto` | |
| `cantidad_reservada` | INT | NOT NULL, CHECK > 0 | |

**Índices relevantes:** `idx_ep_evento`, `idx_ep_producto`.

**Notas para backend:** se usa como reserva activa, no como consumo histórico.

**Observación real:** tabla vacía.

**Trazabilidad:** sigue igual en estructura; sin evidencia de uso real.

---

### 3.14 `cronograma`
**Propósito funcional:** actividades operativas asociadas al evento.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_cronograma` | BIGINT | PK, sequence | |
| `id_evento` | BIGINT | NOT NULL, FK -> `evento` | |
| `actividad` | VARCHAR(255) | NOT NULL | |
| `fecha_inicio` | TIMESTAMP | NOT NULL | |
| `fecha_fin` | TIMESTAMP | NOT NULL, CHECK `fecha_fin > fecha_inicio` | |
| `crono_estado` | VARCHAR(15) | NOT NULL, DEFAULT `PENDIENTE` | CHECK fijo |
| `crono_observacion` | VARCHAR(255) | nullable | |

**Índices relevantes:** `idx_cronograma_evento`.

**Observación real:** tabla vacía.

**Trazabilidad:** sigue igual; sin evidencia operativa actual.

---

### 3.15 `cronograma_usuario`
**Propósito funcional:** tabla puente entre actividad y usuario con pago asociado.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_cronograma` | BIGINT | PK compuesta, FK -> `cronograma` | |
| `id_usuario` | BIGINT | PK compuesta, FK -> `usuario` | |
| `pago_asignado` | NUMERIC(10,2) | NOT NULL, DEFAULT 0, CHECK >= 0 | |
| `pago_tipo` | VARCHAR(10) | NOT NULL, DEFAULT `FIJO` | CHECK `FIJO`, `POR_HORA` |

**Índices relevantes:** `idx_cu_usuario`.

**Observación real:** tabla vacía.

**Trazabilidad:** sigue igual.

---

### 3.16 `gasto_evento`
**Propósito funcional:** gastos variables atribuibles al evento.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_gasto` | BIGINT | PK, sequence | |
| `id_evento` | BIGINT | NOT NULL, FK -> `evento` | |
| `gasto_concepto` | VARCHAR(100) | NOT NULL | |
| `gasto_monto` | NUMERIC(10,2) | NOT NULL, CHECK > 0 | |
| `gasto_estado` | VARCHAR(15) | NOT NULL, DEFAULT `PROYECTADO` | CHECK fijo |
| `gasto_fecha` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| `gasto_nota` | VARCHAR(255) | nullable | |

**Índices relevantes:** `idx_gasto_evento`.

**Observación real:** tabla vacía.

**Trazabilidad:** sigue igual.

---

### 3.17 `pago`
**Propósito funcional:** pagos del cliente por evento.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_pago` | BIGINT | PK, sequence | |
| `id_evento` | BIGINT | NOT NULL, FK -> `evento` | |
| `id_estado_pago` | BIGINT | NOT NULL, FK -> `estado_pago` | |
| `id_concepto_pago` | BIGINT | NOT NULL, FK -> `concepto_pago` | |
| `id_metodo_pago` | BIGINT | NOT NULL, FK -> `metodo_pago` | |
| `pago_monto` | NUMERIC(10,2) | NOT NULL, CHECK > 0 | |
| `pago_fecha` | TIMESTAMP | nullable | |

**Índices relevantes:**
- `idx_pago_evento`, `idx_pago_estado`, `idx_pago_concepto`, `idx_pago_metodo`.
- `idx_pago_evento_estado`.

**Notas para backend:** la DB no evita sobrepago ni fuerza anticipo mínimo.

**Observación real:** tabla vacía.

**Trazabilidad:** sigue igual.

---

### 3.18 `plantilla_documento`
**Propósito funcional:** plantillas base para generar documentos.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_plantilla` | BIGINT | PK, sequence | |
| `tipo_documento` | VARCHAR(50) | NOT NULL, UNIQUE | |
| `contenido_base` | TEXT | NOT NULL | HTML o texto con placeholders |
| `campos_editables` | TEXT | nullable | JSON serializado |
| `activo` | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| `fecha_creacion` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |

**Observación real:** existe 1 plantilla activa `CONFIRMACION_RESERVA`.

**Trazabilidad:** sigue igual.

---

### 3.19 `documento`
**Propósito funcional:** documentos generados por evento.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_documento` | BIGINT | PK, sequence | |
| `id_evento` | BIGINT | NOT NULL, FK -> `evento` | |
| `id_plantilla` | BIGINT | nullable, FK -> `plantilla_documento` | |
| `docu_tipo` | VARCHAR(50) | NOT NULL | |
| `docu_fechageneracion` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| `archivo_url` | VARCHAR(500) | nullable | |
| `docu_estado_envio` | VARCHAR(15) | NOT NULL, DEFAULT `PENDIENTE` | CHECK fijo |
| `docu_fecha_envio` | TIMESTAMP | nullable | |
| `docu_intentos_envio` | INT | NOT NULL, DEFAULT 0 | |

**Índices relevantes:** `idx_documento_evento`.

**Observación real:** tabla vacía.

**Trazabilidad:** sigue igual.

---

### 3.20 `movimiento_inventario`
**Propósito funcional:** historial de entradas y salidas reales de stock.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_movimiento` | BIGINT | PK, sequence | |
| `id_producto` | BIGINT | NOT NULL, FK -> `producto` | |
| `id_evento` | BIGINT | nullable, FK -> `evento` | Obligatorio solo si origen es `EVENTO` |
| `registrado_por` | BIGINT | nullable, FK -> `usuario` | |
| `tipo_movimiento` | VARCHAR(10) | NOT NULL | CHECK `ENTRADA`, `SALIDA` |
| `tipo_origen` | VARCHAR(20) | NOT NULL, DEFAULT `REPOSICION` | CHECK fijo |
| `cantidad` | INT | NOT NULL, CHECK > 0 | |
| `fecha` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| `motivo` | VARCHAR(255) | nullable | |

**Restricciones relevantes:**
- `chk_movimiento_tipo`.
- `chk_movimiento_origen`.
- `chk_movimiento_cantidad_positiva`.
- `chk_movimiento_evento_requerido`.

**Índices relevantes:**
- `idx_movimiento_producto`.
- `idx_movimiento_evento` parcial.
- `idx_movimiento_fecha` descendente.

**Observación real:** tabla vacía.

**Trazabilidad:** sigue igual.

---

### 3.21 `tipo_notificacion`
**Propósito funcional:** catálogo de tipos de alerta del sistema.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_tipo_notificacion` | BIGINT | PK, sequence | |
| `nombre_tipo` | VARCHAR(50) | NOT NULL, UNIQUE | |
| `descripcion` | VARCHAR(255) | nullable | |

**Observación real:** 8 tipos:
- `RECORDATORIO_EVENTO`
- `ALERTA_STOCK`
- `CONFIRMACION_RESERVA`
- `INICIO_MONTAJE`
- `CANCELACION_EVENTO`
- `PAGO_RECIBIDO`
- `COTIZACION_VENCIDA`
- `SALDO_PENDIENTE`

**Trazabilidad:** cambió respecto del seed v4.2; la BD real ya incorpora `SALDO_PENDIENTE`.

---

### 3.22 `estado_notificacion`
**Propósito funcional:** estados técnicos del proceso de notificación.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_estado_notificacion` | BIGINT | PK, sequence | |
| `nombre_estado` | VARCHAR(20) | NOT NULL, UNIQUE | CHECK fijo |
| `descripcion` | VARCHAR(255) | nullable | |

**Observación real:** `PENDIENTE`, `ENVIADA`, `FALLIDA`, `LEIDA`.

**Trazabilidad:** sigue igual.

---

### 3.23 `notificacion`
**Propósito funcional:** notificaciones generadas hacia usuarios internos.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_notificacion` | BIGINT | PK, sequence | |
| `id_usuario` | BIGINT | NOT NULL, FK -> `usuario` | |
| `id_tipo_notificacion` | BIGINT | NOT NULL, FK -> `tipo_notificacion` | |
| `id_estado_notificacion` | BIGINT | NOT NULL, FK -> `estado_notificacion` | |
| `id_evento` | BIGINT | nullable, FK -> `evento` | `ON DELETE SET NULL` |
| `mensaje` | VARCHAR(500) | NOT NULL | |
| `fecha_envio` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |

**Índices relevantes:** `idx_notificacion_usuario`, `idx_notificacion_tipo`, `idx_notificacion_estado`, `idx_notificacion_evento` parcial.

**Notas para backend:** el vínculo con evento es opcional y puede desaparecer si el evento se elimina.

**Observación real:** tabla vacía.

**Trazabilidad:** sigue igual estructuralmente; el documento anterior omite relevancia comparativa de `ON DELETE SET NULL` frente a la frase “sin cascade en ninguna FK”.

---

### 3.24 `auditoria`
**Propósito funcional:** registro histórico de acciones relevantes.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `id_auditoria` | BIGINT | PK, sequence | |
| `id_usuario` | BIGINT | nullable, FK -> `usuario` | `ON DELETE SET NULL` |
| `accion` | VARCHAR(20) | NOT NULL | Sin CHECK |
| `tabla_afectada` | VARCHAR(50) | NOT NULL | Sin CHECK |
| `id_registro` | BIGINT | nullable | |
| `valor_anterior` | JSONB | nullable | |
| `valor_nuevo` | JSONB | nullable | |
| `descripcion` | VARCHAR(255) | nullable | |
| `fecha` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |

**Índices relevantes:**
- `idx_auditoria_usuario`.
- `idx_auditoria_tabla_fecha`.
- `idx_auditoria_fecha`.
- `idx_auditoria_valor_anterior` GIN.
- `idx_auditoria_valor_nuevo` GIN.

**Notas para backend:** la taxonomía de acciones y tablas la gobierna el servicio, no la DB.

**Observación real:** tabla vacía.

**Trazabilidad:** estructura sigue igual; el documento anterior usa por momentos el nombre viejo `idx_auditoria_tabla`, pero el esquema real actual tiene `idx_auditoria_tabla_fecha`.

---

### 3.25 `configuracion`
**Propósito funcional:** parámetros globales editables por administración.

| Campo | Tipo | Restricción | Notas |
|---|---|---|---|
| `config_clave` | VARCHAR(100) | PK | CHECK uppercase |
| `config_valor` | TEXT | NOT NULL | |
| `config_tipo` | VARCHAR(10) | NOT NULL, DEFAULT `STRING` | CHECK `INT`, `DECIMAL`, `BOOLEAN`, `STRING`, `JSON` |
| `config_descripcion` | VARCHAR(255) | nullable | |

**Restricciones relevantes:**
- `chk_configuracion_tipo`.
- `chk_configuracion_clave_upper`.

**Claves observadas realmente en la BD actual:**
- `PORCENTAJE_ANTICIPO_MINIMO`
- `TIEMPO_GRACIA_CANCELACION_MINUTOS`
- `POLITICA_CANCELACION_MONTAJE`
- `PORCENTAJE_STOCK_MINIMO_GLOBAL`
- `DIAS_EXPIRACION_COTIZACION`
- `NEGOCIO_NOMBRE`
- `NEGOCIO_CORREO`
- `NEGOCIO_TELEFONO`
- `DIAS_AVISO_SALDO_PENDIENTE`
- `DIAS_RECORDATORIO_EVENTO`

**Observación real:** la BD actual ya contiene 10 claves; el seed histórico v4.2 tenía solo 8.

**Trazabilidad:** cambió en datos reales; la BD actual incluye extensiones posteriores al seed v4.2.

---

## 4. Relaciones entre Entidades

```text
rol <- usuario

cliente <- evento -> decoracion
                 -> tipo_evento
                 -> estado_evento
                 -> estado_evento (id_estado_anterior)
                 -> evento_producto -> producto
                 -> cronograma -> cronograma_usuario -> usuario
                 -> gasto_evento
                 -> pago -> estado_pago
                         -> concepto_pago
                         -> metodo_pago
                 -> documento -> plantilla_documento
                 -> notificacion

decoracion <- decoracionxproducto -> producto <- movimiento_inventario

usuario <- auditoria
usuario <- notificacion
usuario <- movimiento_inventario
```

**Mapa relacional explicativo:**
- `evento` concentra casi todo el dominio operativo.
- `evento_producto` y `decoracionxproducto` son tablas puente con significado distinto:
  - `decoracionxproducto` define plantilla de composición.
  - `evento_producto` define reserva efectiva por evento.
- `cronograma_usuario` resuelve asignación many-to-many entre actividad y usuario con metadatos de pago.
- `pago` depende de tres catálogos para mantener extensibilidad del negocio.
- `documento`, `notificacion` y `auditoria` funcionan como satélites operativos y de trazabilidad.

**Dependencias importantes:**
- No hay `ON DELETE CASCADE` en cascada destructiva.
- Sí hay `ON DELETE SET NULL` en:
  - `auditoria.id_usuario`
  - `notificacion.id_evento`

**Impacto práctico:**
- El backend es el responsable de orquestar borrados lógicos, liberación de reservas, y cancelaciones en cascada funcional.
- La BD prioriza preservación histórica y flexibilidad de negocio.

---

## 5. Flujos de Estado

### 5.1 Eventos

```text
COTIZACION -> PROGRAMADO -> CONFIRMADO -> EN_MONTAJE -> FINALIZADO
     |             |             |             |
     +-------> CANCELACION_PENDIENTE ----------+
                        |
                        +-> CANCELADO
                        +-> volver a id_estado_anterior
```

**Soporte estructural real:**
- El catálogo `estado_evento` existe.
- `evento.id_estadoevento` y `evento.id_estado_anterior` soportan transición y reversión.
- `event_fecha_cancelacion` soporta el cómputo del tiempo de gracia.

**Lo que depende del backend:**
- Orden válido de transiciones.
- Bloqueo de saltos de estado.
- Reversión de cancelación.
- Liberación de reservas y cancelación del cronograma.

### 5.2 Pagos

```text
PENDIENTE -> PAGADO
     |
     +-> CANCELADO
```

**Soporte estructural real:** catálogo `estado_pago`.

**Lo que depende del backend:**
- Confirmar el pago.
- Llenar `pago_fecha` al confirmar.
- Evitar sobrepago.
- Validar anticipo mínimo.

### 5.3 Cronograma

```text
PENDIENTE -> EN_PROGRESO -> COMPLETADO
     |
     +-> CANCELADO
```

**Soporte estructural real:** `crono_estado` con CHECK.

**Lo que depende del backend:**
- Habilitar cronograma solo desde estados válidos.
- Cancelación en cascada al cancelar evento.

### 5.4 Documentos

```text
PENDIENTE -> ENVIADO
     |
     +-> FALLIDO -> REENVIADO
```

**Soporte estructural real:** `docu_estado_envio` con CHECK.

**Lo que depende del backend:** generación de archivo, subida a nube, envío, reintentos y actualización del evento notificado.

### 5.5 Notificaciones

```text
PENDIENTE -> ENVIADA -> LEIDA
     |
     +-> FALLIDA
```

**Soporte estructural real:** `estado_notificacion` con CHECK.

**Lo que depende del backend:** generación por jobs o eventos de dominio, y cambio de estado de lectura.

---

## 6. Reglas de Negocio por Módulo

### 6.1 Clientes
- El correo debe ser único cuando existe: garantizado por índice único parcial.
- El teléfono puede repetirse: hecho observado y sustentado por el diseño.
- `cli_activo` soporta baja lógica.
- Bloquear uso de clientes inactivos en nuevos eventos depende del backend.

### 6.2 Eventos
- El precio base del evento debe nacer desde `decoracion.decor_precio`.
- `event_precio_total` queda siempre sincronizado por columna generada.
- Si `event_precio_adicional > 0`, `event_nota_precio` es obligatoria por esquema.
- La validación de solapamientos y de estados permitidos es backend.
- `event_fecha_expiracion` es soportada por estructura, pero su cálculo depende del servicio.

### 6.3 Stock
- La decoración define requerimientos teóricos vía `decoracionxproducto`.
- La reserva efectiva vive en `evento_producto`.
- La salida real solo queda históricamente en `movimiento_inventario`.
- Liberar o consumir reserva depende del backend.

### 6.4 Pagos
- El esquema soporta múltiples pagos por evento.
- El esquema NO impide sobrepago.
- El esquema NO exige que el anticipo alcance un porcentaje mínimo.
- La semántica de `ANTICIPO`, `ABONO`, `SALDO_TOTAL`, `PENALIZACION` viene del catálogo, no de un CHECK.

### 6.5 Cronograma
- El modelo soporta actividades y asignaciones con pago fijo o por hora.
- El rango temporal del cronograma respecto al evento NO está garantizado por la DB.

### 6.6 Gastos
- Solo existen dos estados: `PROYECTADO`, `DESEMBOLSADO`.
- La restricción de en qué estados del evento se puede registrar un gasto es backend.

### 6.7 Documentos
- Existe una plantilla base para confirmación de reserva.
- El esquema no valida placeholders ni consistencia del HTML.
- Verificar correo del cliente y generar el PDF es backend.

### 6.8 Usuarios
- El esquema distingue autenticación `LOCAL` y `GOOGLE`.
- La regla “no desactivar al último admin” no está en DB.
- La relación con cronograma, auditoría y movimientos preserva historial.

### 6.9 Configuración
- La tabla soporta tipos heterogéneos con casting delegado al backend.
- La convención de claves en mayúscula sí está garantizada por CHECK.

### 6.10 Notificaciones
- La BD soporta tipos y estados.
- La generación de recordatorios, alertas y avisos depende enteramente del backend o jobs.

---

## 7. Vistas Calculadas

### 7.1 `vista_stock_producto`

**Propósito:** mostrar stock disponible real descontando reservas activas.

**Existe en DDL:** Sí.  
**Existe en la BD actual:** Sí.

**Tablas origen:** `producto`, `evento_producto`, `evento`, `estado_evento`.

**Columnas reales expuestas:**
- `id_producto`
- `produ_nombre`
- `produ_estado`
- `produ_stock_minimo`
- `cantidad_total`
- `cantidad_reservada`
- `cantidad_disponible`
- `produ_precio_unitario`
- `stock_bajo_alerta`

**Definición lógica observada:**
- Usa `LEFT JOIN` desde `producto`.
- Usa `SUM(CASE WHEN ...)` para sumar reservas solo cuando el estado del evento está en `PROGRAMADO`, `CONFIRMADO`, `EN_MONTAJE` o `CANCELACION_PENDIENTE`.
- `stock_bajo_alerta` se calcula comparando `cantidad_disponible` contra `produ_stock_minimo` del producto.

**Utilidad práctica:**
- Backend: validación de reservas y alertas de stock.
- Frontend: dashboard de inventario y alertas visuales.

**Diferencia contra `NOTAS_DB.md`:**
- Sigue alineada casi por completo.
- El documento anterior sugiere un fallback con `PORCENTAJE_STOCK_MINIMO_GLOBAL`; la vista real NO usa esa configuración. Eso es lógica externa al esquema.

**Impacto en backend:** no confiar en `producto.produ_cantidad_total` para disponibilidad.

**Impacto en frontend:** usar esta vista para existencia operativa, no la tabla base.

**Observaciones importantes:** con la BD actual, como no hay reservas, `cantidad_reservada` es 0 para todos los productos.

**Riesgo técnico si aplica:** bajo. La vista es consistente con el modelo de reservas.

---

### 7.2 `vista_pagos_evento`

**Propósito:** resumir cobros por evento.

**Existe en DDL:** Sí.  
**Existe en la BD actual:** Sí.

**Tablas origen:** `evento`, `pago`, `estado_pago`.

**Columnas reales expuestas:**
- `id_evento`
- `precio_total`
- `total_cobrado`
- `saldo_pendiente`
- `cobro_completo`

**Definición lógica observada:**
- Suma solo pagos cuyo estado es `PAGADO`.
- `saldo_pendiente = event_precio_total - total_cobrado`.
- `cobro_completo` es booleano.

**Utilidad práctica:**
- Backend: validaciones de saldo y confirmación.
- Frontend: tarjetas de cobro, saldo y estado financiero.

**Diferencia contra `NOTAS_DB.md`:** alineada. El alias correcto observado es `estpago`.

**Impacto en backend:** evita repetir agregaciones para cada consulta de resumen.

**Impacto en frontend:** útil para dashboards y listados de eventos.

**Observaciones importantes:** actualmente la vista no devuelve filas porque no hay eventos.

**Riesgo técnico si aplica:** bajo.

---

### 7.3 `vista_rentabilidad_evento`

**Propósito:** estimar rentabilidad real por evento.

**Existe en DDL:** Sí.  
**Existe en la BD actual:** Sí.

**Tablas origen:** `evento`, `estado_evento`, `pago`, `estado_pago`, `evento_producto`, `producto`, `cronograma`, `cronograma_usuario`, `gasto_evento`.

**Columnas reales expuestas:**
- `id_evento`
- `event_fechaevento`
- `estado`
- `precio_acordado`
- `ingreso_cobrado`
- `costo_materiales`
- `costo_mano_obra`
- `gastos_desembolsados`
- `gastos_proyectados`
- `ganancia_neta`

**Definición lógica observada:**
- Ingreso: suma de pagos `PAGADO`.
- Materiales: suma de `evento_producto.cantidad_reservada * producto.produ_precio_unitario`.
- Mano de obra: suma de `cronograma_usuario.pago_asignado` por evento.
- Gastos: separa `DESEMBOLSADO` de `PROYECTADO`.
- Ganancia neta: ingreso menos materiales, mano de obra y gastos desembolsados.

**Utilidad práctica:**
- Backend: reportes y métricas.
- Frontend: dashboards de rentabilidad.

**Diferencia contra `NOTAS_DB.md`:** la fórmula documentada coincide con lo observado.

**Impacto en backend:** cuidado al interpretar “rentabilidad real” como dato histórico definitivo.

**Impacto en frontend:** la vista puede mostrar números inconsistentes si el backend elimina reservas al finalizar.

**Observaciones importantes:**
- Hoy no devuelve filas por falta de eventos.
- La vista depende de `evento_producto` para costo de materiales.

**Riesgo técnico si aplica:** alto. Si al finalizar el evento se eliminan filas de `evento_producto` como indica el flujo documentado, la vista perdería costo histórico de materiales y la rentabilidad quedaría distorsionada.

---

### 7.4 `vista_eventos_periodo`

**Propósito:** vista consolidada de eventos para dashboard y reportes.

**Existe en DDL:** Sí.  
**Existe en la BD actual:** Sí.

**Tablas origen:** `evento`, `cliente`, `tipo_evento`, `estado_evento`, `decoracion`, `vista_pagos_evento`, `cronograma`.

**Columnas reales expuestas:**
- `id_evento`
- `cli_nombre`
- `cli_telefono`
- `cli_correo`
- `tipo_evento`
- `estado`
- `decoracion`
- `decoracion_imagen`
- `event_fechaevento`
- `event_horainicio`
- `event_horafin`
- `event_ubicacion`
- `event_precio_base`
- `event_precio_adicional`
- `event_precio_total`
- `event_cliente_notificado`
- `event_fecha_confirmacion`
- `event_fecha_expiracion`
- `event_creacion`
- `total_cobrado`
- `saldo_pendiente`
- `cobro_completo`
- `tiene_cronograma`

**Definición lógica observada:**
- Integra datos comerciales y financieros.
- `tiene_cronograma` usa `EXISTS`.

**Utilidad práctica:**
- Backend: endpoint de listado enriquecido.
- Frontend: tablero principal, filtros, cards y reportes.

**Diferencia contra `NOTAS_DB.md`:**
- El documento anterior dice “todas las de evento + datos...”. Eso NO es literal.
- La vista no expone todos los campos de `evento`; por ejemplo no expone `id_estado_anterior`, `event_nota`, `motivo_cancelacion` ni `event_fecha_cancelacion`.

**Impacto en backend:** no asumir que esta vista reemplaza una lectura completa de `evento`.

**Impacto en frontend:** sirve para overview, no para detalle exhaustivo de cancelación o seguimiento interno.

**Observaciones importantes:** hoy no devuelve filas.

**Riesgo técnico si aplica:** medio si se usa como sustituto total de la tabla `evento`.

---

### 7.5 `vista_cotizaciones_expiradas`

**Propósito:** detectar cotizaciones vencidas sin cambio de estado.

**Existe en DDL:** Sí.  
**Existe en la BD actual:** Sí.

**Tablas origen:** `evento`, `cliente`, `decoracion`, `tipo_evento`, `estado_evento`.

**Columnas reales expuestas:**
- `id_evento`
- `cli_nombre`
- `cli_telefono`
- `cli_correo`
- `event_fechaevento`
- `event_fecha_expiracion`
- `event_creacion`
- `decoracion`
- `tipo_evento`
- `event_precio_total`
- `dias_vencida`

**Definición lógica observada:**
- Filtra solo eventos en `COTIZACION`.
- Requiere `event_fecha_expiracion` no nula.
- Considera vencidas las fechas menores a `CURRENT_DATE`.
- Ordena por `dias_vencida` descendente.

**Utilidad práctica:**
- Backend: jobs de seguimiento y alertas.
- Frontend: listas de cotizaciones vencidas.

**Diferencia contra `NOTAS_DB.md`:** alineada.

**Impacto en backend:** útil para no recalcular vencimiento en cada query.

**Impacto en frontend:** habilita dashboards de seguimiento comercial.

**Observaciones importantes:** hoy no devuelve filas.

**Riesgo técnico si aplica:** bajo.

---

## 8. Configuración Global

**Claves documentadas y observadas realmente:**

| Clave | En seed v4.2 | En BD actual | Uso esperado | Impacto en backend |
|---|---|---|---|---|
| `PORCENTAJE_ANTICIPO_MINIMO` | Sí | Sí | Definir mínimo de anticipo | Confirmación automática / validación de pagos |
| `TIEMPO_GRACIA_CANCELACION_MINUTOS` | Sí | Sí | Tiempo para revertir cancelación | Job de cancelaciones pendientes |
| `POLITICA_CANCELACION_MONTAJE` | Sí | Sí | Política comercial al cancelar en montaje | Lógica de devoluciones o penalización |
| `PORCENTAJE_STOCK_MINIMO_GLOBAL` | Sí | Sí | Fallback lógico externo | Servicio de alertas, no vista SQL |
| `DIAS_EXPIRACION_COTIZACION` | Sí | Sí | Vencimiento de cotizaciones | Alta de evento y jobs |
| `NEGOCIO_NOMBRE` | Sí | Sí | Identidad documental | Render de documentos y mensajes |
| `NEGOCIO_CORREO` | Sí | Sí | Remitente documental | Envío de correos |
| `NEGOCIO_TELEFONO` | Sí | Sí | Contacto documental | Render de documentos |
| `DIAS_AVISO_SALDO_PENDIENTE` | No | Sí | Aviso previo de saldo | Job `saldo pendiente` |
| `DIAS_RECORDATORIO_EVENTO` | No | Sí | Aviso previo al evento | Job `recordatorio evento` |

**Lectura crítica:**
- La BD actual ya resolvió una carencia del seed v4.2.
- El `NOTAS_DB.md` anterior trataba esto como v4.3; esa lectura coincide con el estado real del dump, aunque el archivo de seed entregado siga siendo v4.2.

**Uso esperado:** el backend debe centralizar casteo por `config_tipo`.

**No definido en base de datos:** periodicidad exacta de jobs y políticas de caché de configuración.

---

## 9. Lógica de Stock

### Flujo de reserva
1. La decoración define requerimientos en `decoracionxproducto`.
2. Al pasar a `PROGRAMADO`, el backend debe copiar esos requerimientos a `evento_producto`.
3. `vista_stock_producto` resta esas reservas solo en estados activos.

### Disponibilidad real
- `producto.produ_cantidad_total` = stock físico actual.
- `vista_stock_producto.cantidad_disponible` = stock operativo disponible.

### Ajuste manual
- La estructura lo soporta vía `movimiento_inventario` con `tipo_origen = 'AJUSTE_MANUAL'`.
- El cálculo de diferencia lo hace el backend.

### Movimientos

| Situación | tipo_movimiento | tipo_origen | Soporte en DB |
|---|---|---|---|
| Reposición | `ENTRADA` | `REPOSICION` | Sí |
| Ajuste manual positivo | `ENTRADA` | `AJUSTE_MANUAL` | Sí |
| Ajuste manual negativo | `SALIDA` | `AJUSTE_MANUAL` | Sí |
| Daño | `SALIDA` | `DAÑO` | Sí |
| Pérdida | `SALIDA` | `PERDIDA` | Sí |
| Consumo por evento finalizado | `SALIDA` | `EVENTO` | Sí |

### Observaciones importantes
- La BD actual no tiene movimientos cargados.
- No hay evidencia transaccional real todavía para validar el flujo completo de stock en datos observados.
- El cálculo de `stock_bajo_alerta` de la vista depende solo de `produ_stock_minimo`, no del porcentaje global de configuración.

---

## 10. Lógica de Pagos

### Anticipo mínimo
- Está soportado por configuración, no por restricción de base.
- La validación debe usar `PORCENTAJE_ANTICIPO_MINIMO`.

### Sobrepago
- La BD NO impide `SUM(pagos PAGADO) > event_precio_total`.
- Debe controlarlo el backend.

### Confirmación
- `pago_fecha` permite registrar el momento de confirmación.
- El cambio automático de evento a `CONFIRMADO` no existe en DB; es lógica de aplicación.

### Restricciones de estado
- El esquema no prohíbe insertar pagos para eventos cancelados o finalizados.
- Esa regla es responsabilidad del servicio.

### Observaciones reales detectadas
- No existen pagos en la BD actual.
- No hay evidencia de cómo quedó implementada en datos la relación entre `ANTICIPO` y transición a `CONFIRMADO`.
- No definido en base de datos: política exacta de redondeo del anticipo mínimo en divisiones decimales.

---

## 11. Lógica de Cancelación

### Flujo
1. El backend guarda `id_estado_anterior`.
2. El backend guarda `motivo_cancelacion`.
3. El backend guarda `event_fecha_cancelacion`.
4. El backend mueve a `CANCELACION_PENDIENTE`.
5. Un job decide si revierte o consuma la cancelación.

### Tiempo de gracia
- Soportado estructuralmente por `event_fecha_cancelacion` y `TIEMPO_GRACIA_CANCELACION_MINUTOS`.

### Efectos en stock y cronograma
- El esquema permite borrar reservas y marcar cronograma como cancelado.
- La cascada funcional NO está automatizada en DB.

### Observaciones reales detectadas
- No hay eventos cancelados en la BD actual.
- No existe evidencia real en datos de ejecuciones del job de cancelación.
- No definido en base de datos: política efectiva de pagos o penalización al cancelar según estado del evento.

---

## 12. Auditoría

**Propósito:** preservar trazabilidad de acciones relevantes.

**Estructura real:** tabla `auditoria` con usuario opcional, acción, tabla, id afectado, snapshots JSONB y timestamp.

**Uso esperado:**
- Registrar altas, ediciones, cancelaciones, confirmaciones, finalizaciones y bajas lógicas.
- Permitir análisis posterior y reconstrucción de cambios.

**Decisiones relevantes observadas:**
- `valor_anterior` y `valor_nuevo` son `JSONB`, no `TEXT`.
- Existen índices GIN para análisis.
- `id_usuario` usa `ON DELETE SET NULL`, lo cual favorece preservar historial aunque cambie la relación de usuario.

**Observación real:** tabla vacía. No hay evidencia todavía de auditoría operativa ejecutándose.

---

## 13. Notificaciones

**Tipos observados:**
- `RECORDATORIO_EVENTO`
- `ALERTA_STOCK`
- `CONFIRMACION_RESERVA`
- `INICIO_MONTAJE`
- `CANCELACION_EVENTO`
- `PAGO_RECIBIDO`
- `COTIZACION_VENCIDA`
- `SALDO_PENDIENTE`

**Estados observados:** `PENDIENTE`, `ENVIADA`, `FALLIDA`, `LEIDA`.

**Uso esperado:**
- Jobs automáticos.
- Alertas internas operativas.
- Deep link opcional a `evento`.

**Observaciones reales:**
- La tabla `notificacion` está vacía.
- El catálogo ya incluye `SALDO_PENDIENTE`, que en v4.2 histórico no existía.

---

## 14. Decisiones de Diseño y Su Razón

### Decisiones confirmadas por DDL / dump

| Decisión | Razón observable |
|---|---|
| `estado_evento` sin CHECK de transición | Mantener flexibilidad del flujo en backend |
| `pago` apoyado en catálogos y no enums rígidos por CHECK | Permite expansión sin `ALTER TABLE` |
| `event_precio_total` como columna generada | Evita desincronización entre base y adicional |
| `configuracion` con `config_tipo` y valor `TEXT` | Permite parámetros heterogéneos |
| Índices funcionales `LOWER(...)` para correos | Garantizan unicidad case-insensitive |
| `auditoria` con `JSONB` e índices GIN | Facilita trazabilidad consultable |
| `CANCELACION_PENDIENTE` e `id_estado_anterior` | Modelan reversión de cancelación |
| `vista_stock_producto` con `SUM(CASE WHEN)` | Hace explícita la suma solo en estados activos |
| `ON DELETE SET NULL` selectivo | Preserva historial sin cascadas destructivas |

### Decisiones inferidas con sustento parcial

| Decisión inferida | Sustento | Observación |
|---|---|---|
| El backend centraliza toda la orquestación de reglas de negocio complejas | DDL con pocos triggers y muchos comentarios orientados a servicio | Inferencia razonable, no imposición de DB |
| El sistema prioriza preservación histórica sobre limpieza automática | Ausencia de cascadas destructivas y uso de soft flags | Coherente con auditoría y notificaciones |
| La BD fue preparada primero para soportar backend Spring Boot | Comentarios del DDL mencionan Spring Boot / Spring Security | Es contexto documental, no restricción física |

---

## 15. Lo que el Backend DEBE validar

- Transiciones válidas entre estados del evento.
- Solapamiento de horarios entre eventos.
- Disponibilidad real de stock al reservar.
- Copia de `decoracionxproducto` hacia `evento_producto`.
- `motivo_cancelacion` obligatorio al iniciar cancelación.
- Anticipo mínimo según configuración.
- Evitar sobrepago.
- Prohibir pagos y gastos en estados de evento no permitidos.
- Fechas del cronograma dentro del rango lógico del evento.
- Uso de clientes activos y decoraciones activas para nuevas operaciones.
- Envío de documentos solo si el cliente tiene correo.
- Política de reintentos y actualización de `docu_intentos_envio`.
- Generación de movimientos de inventario al finalizar evento.
- Fallback de stock mínimo global si se quiere aplicar esa regla.
- Consistencia semántica entre `usu_tipo_auth` y credenciales utilizadas.

---

## 16. Lo que el Backend NO necesita validar

- Existencia de claves foráneas referenciadas.
- Unicidad case-insensitive de `usuario.usu_correo`.
- Unicidad case-insensitive de `cliente.cli_correo` cuando no es NULL.
- Positividad de `pago_monto`.
- Positividad de `gasto_monto`.
- Positividad de `cantidad_requerida` y `cantidad_reservada`.
- No negatividad de `produ_cantidad_total`, `produ_precio_unitario`, `produ_stock_minimo`, `event_precio_base`, `event_precio_adicional`, `pago_asignado`.
- Valores permitidos en `usu_tipo_auth`, `crono_estado`, `gasto_estado`, `docu_estado_envio`, `tipo_movimiento`, `tipo_origen`, `config_tipo`, `estado_notificacion.nombre_estado`.
- `event_horafin > event_horainicio` cuando ambos existen.
- `fecha_fin > fecha_inicio` en cronograma.
- `event_precio_total = event_precio_base + event_precio_adicional`.
- `event_nota_precio` obligatoria cuando hay precio adicional.
- Uppercase de `config_clave`.
- Requisito de `id_evento` cuando `movimiento_inventario.tipo_origen = 'EVENTO'`.

---

## 17. Diferencias entre el NOTAS_DB anterior y la BD Actual

| elemento | estado en `NOTAS_DB.md` anterior | estado real actual | clasificación | evidencia |
|---|---|---|---|---|
| Conteo estructural general | 25 tablas y 5 vistas | 25 tablas y 5 vistas | Sigue igual | `CREATE TABLE public...` x25 y `CREATE VIEW public...` x5 en dump |
| Motor PostgreSQL | 18.1 | 18.1 | Sigue igual | Encabezado de `ArtepaxMaster.sql` |
| `tipo_notificacion.SALDO_PENDIENTE` | Documentado como agregado de v4.3 | Existe realmente en BD actual | Sigue igual | `COPY public.tipo_notificacion` línea 2270 |
| `configuracion.DIAS_AVISO_SALDO_PENDIENTE` | Documentado como agregado de v4.3 | Existe realmente en BD actual | Sigue igual | `COPY public.configuracion` línea 2011 |
| `configuracion.DIAS_RECORDATORIO_EVENTO` | Documentado como agregado de v4.3 | Existe realmente en BD actual | Sigue igual | `COPY public.configuracion` línea 2012 |
| Base actual como “dump real verificado” | Afirmado | Se confirma | Sigue igual | `ArtepaxMaster.sql` observado completo |
| `usuario` | Seed original de 3 usuarios descrito indirectamente | 4 usuarios, con uno agregado en desarrollo | 🆕 Agregado en desarrollo | `COPY public.usuario` línea 2284 |
| Usuario `Ernesto Mendoza` | No documentado | Existe con `id_usuario = 7` | 🆕 Agregado en desarrollo | `COPY public.usuario` línea 2284 |
| Semántica del usuario agregado | No documentada | `usu_tipo_auth='LOCAL'` y password `GOOGLE_AUTH` | ❌ Inconsistencia documental | `COPY public.usuario` línea 2284 |
| Datos transaccionales de eventos | El documento describe flujo operativo completo | No hay eventos en la BD actual | Cambió | `COPY public.evento` vacío |
| Datos transaccionales de pagos | Descritos funcionalmente | No hay pagos en la BD actual | Cambió | `COPY public.pago` vacío |
| Datos transaccionales de cronograma | Descritos funcionalmente | No hay cronograma ni asignaciones | Cambió | `COPY public.cronograma` y `cronograma_usuario` vacíos |
| Datos transaccionales de gastos | Descritos funcionalmente | No hay gastos | Cambió | `COPY public.gasto_evento` vacío |
| Datos transaccionales de documentos | Descritos funcionalmente | No hay documentos emitidos | Cambió | `COPY public.documento` vacío |
| Datos transaccionales de movimientos | Descritos funcionalmente | No hay movimientos | Cambió | `COPY public.movimiento_inventario` vacío |
| Datos transaccionales de notificaciones | Descritos funcionalmente | No hay notificaciones | Cambió | `COPY public.notificacion` vacío |
| Datos transaccionales de auditoría | Descritos funcionalmente | No hay auditoría registrada | Cambió | `COPY public.auditoria` vacío |
| Frase “sin ON DELETE CASCADE en ninguna FK” | Documentada como resumen global | Hay FKs con `ON DELETE SET NULL` | ❌ Inconsistencia documental | `fk_auditoria_usuario`, `fk_notificacion_evento` |
| `vista_eventos_periodo` | Documentada como “todas las de evento + datos...” | No expone todos los campos de `evento` | ❌ Inconsistencia documental | Definición real de la vista en dump |
| `vista_stock_producto` | Correctamente descrita con `SUM(CASE WHEN)` | Coincide | Sigue igual | Definición real de la vista |
| `vista_pagos_evento` | Correctamente descrita con alias `estpago` | Coincide | Sigue igual | Definición real de la vista |
| `vista_rentabilidad_evento` | Fórmula documentada | Coincide estructuralmente | Sigue igual | Definición real de la vista |
| Rentabilidad histórica usando `evento_producto` | No explicitado como riesgo | La vista depende de una tabla que el flujo dice que puede vaciarse al finalizar | ❌ Inconsistencia documental | DDL de vista + flujo documentado de `evento_producto` |
| `configuracion` de 10 claves | Documentada en versión previa | Coincide con dump real | Sigue igual | `COPY public.configuracion` líneas 2003-2012 |
| `idx_auditoria_tabla_fecha` | Documentado en historial v4.1 | Coincide con dump | Sigue igual | `CREATE INDEX idx_auditoria_tabla_fecha` |

---

## 18. Riesgos Técnicos y Observaciones

| categoría | severidad | tipo | descripción | impacto | evidencia | recomendación breve |
|---|---|---|---|---|---|---|
| Rentabilidad | Alta | Riesgo técnico | `vista_rentabilidad_evento` calcula materiales desde `evento_producto`, pero el flujo documentado indica que esa reserva se elimina al finalizar o cancelar | La ganancia histórica puede quedar subestimada o en cero para materiales | Vista en DDL/dump + ciclo de vida de `evento_producto` | Persistir costo material histórico al finalizar o no borrar esa evidencia |
| Autenticación / datos | Media | Hecho observado | Existe un usuario real con `usu_tipo_auth='LOCAL'` y valor de password `GOOGLE_AUTH` | Puede romper login o confundir la capa de autenticación | `COPY public.usuario` línea 2284 | Revisar si el usuario debe ser `GOOGLE` o si falta hash válido |
| Documentación | Media | Hecho observado | El documento anterior resume “sin ON DELETE CASCADE en ninguna FK”, pero hay `ON DELETE SET NULL` en dos FKs | Puede inducir decisiones erróneas en backend o migraciones | `fk_auditoria_usuario`, `fk_notificacion_evento` | Corregir lenguaje: no hay cascadas destructivas, pero sí nullificación selectiva |
| Documentación | Media | Hecho observado | `vista_eventos_periodo` no expone todos los campos de `evento` pese a la formulación anterior | Riesgo de asumir campos inexistentes en DTOs o consultas | Definición real de la vista | Tratarla como vista de overview, no como espejo de `evento` |
| Evidencia operativa | Baja | Hecho observado | La BD actual no tiene datos transaccionales | Varias reglas del documento son correctas conceptualmente, pero aún no están probadas con uso real | Tablas transaccionales vacías en dump | Validar flujos con datos de prueba integrales |
| Configuración | Baja | Hecho observado | El dump actual incluye dos claves y un tipo de notificación ausentes del seed v4.2 entregado | Un entorno reconstruido solo con seed v4.2 quedaría incompleto | `Artepax_seed_v4_2.sql` vs `COPY` reales | Crear seed consolidado actualizado o un delta de migración |
| Stock mínimo | Baja | Riesgo técnico | La vista de stock no usa `PORCENTAJE_STOCK_MINIMO_GLOBAL` | Si frontend o backend asumen que el umbral sale de SQL, habrá discrepancias | Definición real de `vista_stock_producto` | Centralizar la regla de fallback solo en backend y documentarlo así |

---

## 19. Veredicto Final

**¿El `NOTAS_DB.md` anterior sigue vigente?:** Parcial.

**¿La BD actual está alineada con el documento anterior?:** Parcial.

**¿Qué cambió realmente?:**
- La BD real ya no corresponde al seed v4.2 puro; incorpora `SALDO_PENDIENTE` y dos claves de configuración adicionales.
- Existe un usuario adicional de desarrollo no reflejado en la semilla histórica.
- La BD actual carece de datos transaccionales, por lo que varias secciones operativas siguen siendo diseño validado por esquema, no comportamiento observado en uso real.

**¿Qué no cambió?:**
- La estructura central de 25 tablas y 5 vistas.
- Relaciones, checks, índices y columnas principales del DDL v4.2.
- El modelo conceptual de clientes, eventos, stock, pagos, cronograma y configuración.

**¿Qué debe actualizarse en backend si aplica?:**
- Verificar consistencia del usuario adicional y su tipo de autenticación.
- Si se usa `vista_rentabilidad_evento`, revisar cómo se preserva el costo histórico de materiales tras finalizar eventos.
- Mantener seed o migración actualizados con las claves y tipos de notificación ya requeridos por jobs.

**¿Qué debe vigilarse en frontend si aplica?:**
- No asumir que `vista_eventos_periodo` expone todos los campos de `evento`.
- Preparar estados vacíos para dashboards mientras la base no tenga operación real.
- Tomar disponibilidad desde `vista_stock_producto`, no desde `producto`.

**Recomendación final:**
El documento anterior era una muy buena base, pero YA no alcanza como fuente única sin matices. La estructura sigue bien, pero había que separar mejor tres cosas: lo que garantiza la DB, lo que depende del backend y lo que la BD actual realmente contiene hoy. Este archivo debe pasar a ser la referencia vigente para backend, frontend, validación y auditoría futura.
