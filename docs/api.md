# API REST

El inventario recoge 103 métodos HTTP declarados en los controladores de la versión revisada. La columna de retorno muestra el tipo Java declarado. Para consultar esquemas, parámetros y operaciones en ejecución, abre `/swagger-ui/index.html` o `/v3/api-docs` en el servicio local. Consulta [configuración](setup.md) para los requisitos de arranque.

## Autenticación

La configuración permite el acceso público a `/auth/**`, `/error` y a las rutas Swagger/OpenAPI. Las peticiones restantes requieren autenticación. Las rutas administrativas del inventario añaden el requisito `ADMIN`. `GET /api/usuarios/me` admite una identidad autenticada sin una restricción adicional de rol en el método.

```http
POST /auth/login
Content-Type: application/json

{"email":"usuario@example.com","password":"<CONTRASEÑA>"}
```

Para `POST /auth/google`, envía `{"idToken":"<GOOGLE_ID_TOKEN>"}`. Ambos devuelven HTTP 200 con `{"token":"<TOKEN>"}` al autenticar correctamente. Envía ese valor después en `Authorization: Bearer <TOKEN>`.

El correo del login local debe tener formato válido y ambos campos son obligatorios. La validación de DTO puede devolver 400. El login local devuelve 401 ante credenciales inválidas y 403 para una cuenta inactiva o sin los permisos exigidos por el servicio. Las reglas del flujo pueden devolver otros estados definidos por sus excepciones de dominio.

## Acceso y usuarios

| Método | Ruta | Operación | Acceso | Retorno Java |
| --- | --- | --- | --- | --- |
| POST | `/auth/google` | Autenticar con Google | Público | `ResponseEntity<AuthResponse>` |
| POST | `/auth/login` | Autenticar con credenciales locales | Público | `ResponseEntity<AuthResponse>` |
| GET | `/api/usuarios/me` | Consultar la identidad actual | Autenticado | `ResponseEntity<Map<String, Object>>` |
| POST | `/api/usuarios` | Crear usuario | ADMIN | `ResponseEntity<UsuarioResponseDTO>` |
| GET | `/api/usuarios` | Listar usuarios | ADMIN | `ResponseEntity<List<UsuarioResponseDTO>>` |
| PATCH | `/api/usuarios/{id}/estado` | Cambiar estado | ADMIN | `ResponseEntity<UsuarioResponseDTO>` |

## Clientes

| Método | Ruta | Operación | Acceso | Retorno Java |
| --- | --- | --- | --- | --- |
| POST | `/api/clientes` | Crear cliente | ADMIN | `ResponseEntity<ClienteResponseDTO>` |
| GET | `/api/clientes` | Obtener todos | ADMIN | `ResponseEntity<List<ClienteResponseDTO>>` |
| GET | `/api/clientes/{id}` | Obtener por id | ADMIN | `ResponseEntity<ClienteResponseDTO>` |
| PUT | `/api/clientes/{id}` | Actualizar cliente | ADMIN | `ResponseEntity<ClienteResponseDTO>` |
| DELETE | `/api/clientes/{id}` | Eliminar cliente | ADMIN | `ResponseEntity<Void>` |
| GET | `/api/clientes/{id}/eventos` | Obtener eventos por cliente | ADMIN | `ResponseEntity<List<EventoResponseDTO>>` |
| GET | `/api/clientes/buscar` | Buscar por nombre | ADMIN | `ResponseEntity<List<ClienteResponseDTO>>` |
| GET | `/api/clientes/inactivos` | Listar inactivos | ADMIN | `ResponseEntity<List<ClienteResponseDTO>>` |
| PATCH | `/api/clientes/{id}/reactivar` | Reactivar | ADMIN | `ResponseEntity<ClienteResponseDTO>` |

## Configuración y auditoría

| Método | Ruta | Operación | Acceso | Retorno Java |
| --- | --- | --- | --- | --- |
| GET | `/api/auditoria` | Obtener todo | ADMIN | `ResponseEntity<List<AuditoriaResponseDTO>>` |
| GET | `/api/auditoria/tabla/{tabla}` | Por tabla | ADMIN | `ResponseEntity<List<AuditoriaResponseDTO>>` |
| GET | `/api/auditoria/tabla/{tabla}/registro/{id}` | Por registro | ADMIN | `ResponseEntity<List<AuditoriaResponseDTO>>` |
| GET | `/api/configuracion` | Listar | ADMIN | `ResponseEntity<List<ConfiguracionResponseDTO>>` |
| GET | `/api/configuracion/{clave}` | Obtener | ADMIN | `ResponseEntity<ConfiguracionResponseDTO>` |
| PUT | `/api/configuracion/{clave}` | Actualizar | ADMIN | `ResponseEntity<ConfiguracionResponseDTO>` |

## Cronogramas

| Método | Ruta | Operación | Acceso | Retorno Java |
| --- | --- | --- | --- | --- |
| POST | `/api/cronogramas` | Crear | ADMIN | `ResponseEntity<CronogramaResponseDTO>` |
| GET | `/api/cronogramas/evento/{idEvento}` | Obtener por evento | ADMIN | `ResponseEntity<List<CronogramaResponseDTO>>` |
| GET | `/api/cronogramas/{id}` | Obtener por id | ADMIN | `ResponseEntity<CronogramaResponseDTO>` |
| PUT | `/api/cronogramas/{id}` | Actualizar | ADMIN | `ResponseEntity<CronogramaResponseDTO>` |
| PATCH | `/api/cronogramas/{id}/estado` | Cambiar estado | ADMIN | `ResponseEntity<CronogramaResponseDTO>` |
| PATCH | `/api/cronogramas/{id}/cancelar` | Cancelar | ADMIN | `ResponseEntity<Void>` |
| POST | `/api/cronogramas/{id}/usuarios` | Asignar usuario | ADMIN | `ResponseEntity<CronogramaUsuarioResponseDTO>` |
| GET | `/api/cronogramas/{id}/usuarios` | Obtener usuarios | ADMIN | `ResponseEntity<List<CronogramaUsuarioResponseDTO>>` |
| DELETE | `/api/cronogramas/{id}/usuarios/{usuarioId}` | Remover usuario | ADMIN | `ResponseEntity<Void>` |
| GET | `/api/cronogramas` | Obtener todos | ADMIN | `ResponseEntity<Page<CronogramaResponseDTO>>` |

## Documentos

| Método | Ruta | Operación | Acceso | Retorno Java |
| --- | --- | --- | --- | --- |
| POST | `/api/documentos/evento/{idEvento}/generar` | Generar documento | ADMIN | `ResponseEntity<DocumentoResponseDTO>` |
| POST | `/api/documentos/evento/{idEvento}/aviso-saldo-pendiente` | Generar aviso saldo pendiente | ADMIN | `ResponseEntity<DocumentoResponseDTO>` |
| GET | `/api/documentos/evento/{idEvento}` | Listar por evento | ADMIN | `ResponseEntity<List<DocumentoResponseDTO>>` |
| GET | `/api/documentos/{id}` | Obtener documento | ADMIN | `ResponseEntity<DocumentoResponseDTO>` |
| POST | `/api/documentos/{id}/reenviar` | Reenviar documento | ADMIN | `ResponseEntity<DocumentoResponseDTO>` |
| GET | `/api/documentos/plantillas` | Listar plantillas | ADMIN | `ResponseEntity<List<PlantillaResponseDTO>>` |
| GET | `/api/documentos/plantillas/{id}` | Obtener plantilla | ADMIN | `ResponseEntity<PlantillaResponseDTO>` |
| PUT | `/api/documentos/plantillas/{id}` | Actualizar plantilla | ADMIN | `ResponseEntity<PlantillaResponseDTO>` |
| GET | `/api/documentos` | Obtener todos | ADMIN | `ResponseEntity<Page<DocumentoResponseDTO>>` |
| GET | `/api/documentos/{id}/descarga` | Descargar pdf | ADMIN | `ResponseEntity<Resource>` |

## Eventos y gastos

| Método | Ruta | Operación | Acceso | Retorno Java |
| --- | --- | --- | --- | --- |
| POST | `/api/eventos` | Crear evento | ADMIN | `ResponseEntity<EventoResponseDTO>` |
| GET | `/api/eventos` | Obtener eventos | ADMIN | `ResponseEntity<Page<EventoResponseDTO>>` |
| GET | `/api/eventos/{id}` | Obtener evento por id | ADMIN | `ResponseEntity<EventoResponseDTO>` |
| PUT | `/api/eventos/{id}` | Actualizar evento | ADMIN | `ResponseEntity<EventoResponseDTO>` |
| PATCH | `/api/eventos/{id}/extras` | Agregar extras | ADMIN | `ResponseEntity<EventoResponseDTO>` |
| PATCH | `/api/eventos/{id}/programar` | Programar evento | ADMIN | `ResponseEntity<EventoResponseDTO>` |
| PATCH | `/api/eventos/{id}/confirmar` | Confirmar evento | ADMIN | `ResponseEntity<EventoResponseDTO>` |
| PATCH | `/api/eventos/{id}/iniciar-montaje` | Iniciar montaje | ADMIN | `ResponseEntity<EventoResponseDTO>` |
| PATCH | `/api/eventos/{id}/finalizar` | Finalizar evento | ADMIN | `ResponseEntity<EventoResponseDTO>` |
| PATCH | `/api/eventos/{id}/cancelar` | Iniciar cancelacion | ADMIN | `ResponseEntity<EventoResponseDTO>` |
| PATCH | `/api/eventos/{id}/revertir-cancelacion` | Revertir cancelacion | ADMIN | `ResponseEntity<EventoResponseDTO>` |
| PATCH | `/api/eventos/{id}/confirmar-cancelacion` | Confirmar cancelacion | ADMIN | `ResponseEntity<Void>` |
| GET | `/api/eventos/disponibilidad` | Verificar disponibilidad | ADMIN | `ResponseEntity<Boolean>` |
| GET | `/api/eventos/calendario` | Obtener calendario | ADMIN | `ResponseEntity<List<CalendarioEventoDTO>>` |
| GET | `/api/eventos/{id}/rentabilidad` | Get rentabilidad | ADMIN | `ResponseEntity<RentabilidadEventoDTO>` |
| GET | `/api/eventos/{id}/detalle` | Get detalle | ADMIN | `ResponseEntity<EventoResumenInternoDTO>` |
| GET | `/api/eventos/{id}/productos` | Obtener productos | ADMIN | `ResponseEntity<List<EventoProductoDTO>>` |
| POST | `/api/gastos` | Crear | ADMIN | `ResponseEntity<GastoEventoResponseDTO>` |
| GET | `/api/gastos/evento/{eventoId}` | Obtener por evento | ADMIN | `ResponseEntity<List<GastoEventoResponseDTO>>` |
| GET | `/api/gastos/evento/{eventoId}/resumen` | Obtener resumen | ADMIN | `ResponseEntity<ResumenGastoEventoDTO>` |
| GET | `/api/gastos/eventos-control` | Obtener eventos control | ADMIN | `ResponseEntity<List<EventoGastoControlDTO>>` |
| GET | `/api/gastos/{id}` | Obtener por id | ADMIN | `ResponseEntity<GastoEventoResponseDTO>` |
| PATCH | `/api/gastos/{id}/desembolsar` | Desembolsar | ADMIN | `ResponseEntity<GastoEventoResponseDTO>` |
| DELETE | `/api/gastos/{id}` | Eliminar | ADMIN | `ResponseEntity<Void>` |
| GET | `/api/gastos` | Obtener todos | ADMIN | `ResponseEntity<Page<GastoEventoResponseDTO>>` |
| GET | `/api/tipos-evento` | Listar | ADMIN | `List<TipoEventoResponseDTO>` |
| POST | `/api/tipos-evento` | Crear | ADMIN | `ResponseEntity<TipoEventoResponseDTO>` |
| PATCH | `/api/tipos-evento/{id}/toggle` | Toggle | ADMIN | `ResponseEntity<TipoEventoResponseDTO>` |

## Inventario

| Método | Ruta | Operación | Acceso | Retorno Java |
| --- | --- | --- | --- | --- |
| POST | `/api/decoraciones` | Crear decoracion | ADMIN | `ResponseEntity<DecoracionResponseDTO>` |
| GET | `/api/decoraciones` | Obtener todas | ADMIN | `ResponseEntity<List<DecoracionResponseDTO>>` |
| GET | `/api/decoraciones/{id}` | Obtener por id | ADMIN | `ResponseEntity<DecoracionResponseDTO>` |
| PUT | `/api/decoraciones/{id}` | Actualizar | ADMIN | `ResponseEntity<DecoracionResponseDTO>` |
| DELETE | `/api/decoraciones/{id}` | Eliminar | ADMIN | `ResponseEntity<Void>` |
| PATCH | `/api/decoraciones/{id}/reactivar` | Reactivar | ADMIN | `ResponseEntity<DecoracionResponseDTO>` |
| GET | `/api/decoraciones/compatibles` | Obtener compatibles | ADMIN | `ResponseEntity<List<DecoracionResponseDTO>>` |
| POST | `/api/inventario/entrada` | Entrada | ADMIN | `ResponseEntity<Void>` |
| POST | `/api/inventario/salida` | Salida | ADMIN | `ResponseEntity<Void>` |
| POST | `/api/inventario/ajuste/{productoId}` | Ajustar stock | ADMIN | `ResponseEntity<Void>` |
| GET | `/api/inventario/productos/{id}/stock` | Obtener stock | ADMIN | `ResponseEntity<StockProductoResponseDTO>` |
| GET | `/api/inventario/productos/{id}/movimientos` | Obtener movimientos | ADMIN | `ResponseEntity<Page<MovimientoResponseDTO>>` |
| GET | `/api/inventario/movimientos` | Obtener todos movimientos | ADMIN | `ResponseEntity<Page<MovimientoResponseDTO>>` |
| POST | `/api/productos` | Crear producto | ADMIN | `ResponseEntity<ProductoResponseDTO>` |
| GET | `/api/productos` | Obtener todos | ADMIN | `ResponseEntity<List<ProductoResponseDTO>>` |
| GET | `/api/productos/{id}` | Obtener por id | ADMIN | `ResponseEntity<ProductoResponseDTO>` |
| PUT | `/api/productos/{id}` | Actualizar producto | ADMIN | `ResponseEntity<ProductoResponseDTO>` |
| GET | `/api/productos/buscar` | Buscar por nombre | ADMIN | `ResponseEntity<List<ProductoResponseDTO>>` |
| DELETE | `/api/productos/{id}` | Eliminar producto | ADMIN | `ResponseEntity<Void>` |
| PATCH | `/api/productos/{id}/reactivar` | Reactivar | ADMIN | `ResponseEntity<ProductoResponseDTO>` |

## Notificaciones

| Método | Ruta | Operación | Acceso | Retorno Java |
| --- | --- | --- | --- | --- |
| GET | `/api/notificaciones/mias` | Obtener mias | ADMIN | `ResponseEntity<List<NotificacionResponseDTO>>` |
| PATCH | `/api/notificaciones/{id}/leer` | Marcar leida | ADMIN | `ResponseEntity<Void>` |

## Pagos y catálogos

| Método | Ruta | Operación | Acceso | Retorno Java |
| --- | --- | --- | --- | --- |
| GET | `/api/catalogos/estados-pago` | Listar estados | ADMIN | `List<EstadoPagoResponseDTO>` |
| GET | `/api/catalogos/conceptos-pago` | Listar conceptos | ADMIN | `List<ConceptoPagoResponseDTO>` |
| GET | `/api/catalogos/metodos-pago` | Listar metodos | ADMIN | `List<MetodoPagoResponseDTO>` |
| POST | `/api/catalogos/metodos-pago` | Crear metodo | ADMIN | `ResponseEntity<MetodoPagoResponseDTO>` |
| PATCH | `/api/catalogos/metodos-pago/{id}/toggle` | Toggle metodo | ADMIN | `ResponseEntity<MetodoPagoResponseDTO>` |
| POST | `/api/pagos` | Registrar | ADMIN | `ResponseEntity<PagoResponseDTO>` |
| PATCH | `/api/pagos/{id}/confirmar` | Confirmar | ADMIN | `ResponseEntity<PagoResponseDTO>` |
| PATCH | `/api/pagos/{id}/anular` | Anular | ADMIN | `ResponseEntity<PagoResponseDTO>` |
| GET | `/api/pagos/evento/{eventoId}` | Por evento | ADMIN | `ResponseEntity<List<PagoResponseDTO>>` |
| GET | `/api/pagos/evento/{eventoId}/resumen` | Get resumen pagos | ADMIN | `ResponseEntity<ResumenPagoEventoDTO>` |
| GET | `/api/pagos` | Obtener todos | ADMIN | `ResponseEntity<Page<PagoResponseDTO>>` |
| GET | `/api/pagos/eventos-cobranza` | Get cobranza | ADMIN | `ResponseEntity<List<EventoCobranzaDTO>>` |

## Relación con OpenSpec

Los listados globales incluidos en esta guía tienen mappings en los controladores actuales. El [checklist de OpenSpec](../openspec/changes/endpoints-globales/tasks.md) conserva su estado original y documenta un alcance más amplio que la disponibilidad de las rutas.
