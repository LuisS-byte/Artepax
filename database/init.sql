-- Esquema PostgreSQL 18 y datos de demostración de Artepax.
-- Ejecutar mediante psql con ON_ERROR_STOP en una base vacía.

BEGIN;

--
--


SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 219 (class 1259 OID 106518)
-- Name: auditoria; Type: TABLE; Schema: public
--

CREATE TABLE public.auditoria (
    id_auditoria bigint NOT NULL,
    id_usuario bigint,
    accion character varying(20) NOT NULL,
    tabla_afectada character varying(50) NOT NULL,
    id_registro bigint,
    valor_anterior jsonb,
    valor_nuevo jsonb,
    descripcion character varying(255),
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- TOC entry 5442 (class 0 OID 0)
-- Dependencies: 219
-- Name: TABLE auditoria; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.auditoria IS 'Registro de acciones del sistema. Llenado automáticamente con AOP en Spring Boot.';


--
-- TOC entry 5443 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN auditoria.id_usuario; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.auditoria.id_usuario IS 'NULL si usuario eliminado (ON DELETE SET NULL preserva el historial).';


--
-- TOC entry 5444 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN auditoria.accion; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.auditoria.accion IS 'Sin CHECK: valores controlados en backend (CREAR, EDITAR, ELIMINAR, CANCELAR, CONFIRMAR, FINALIZAR, etc.)';


--
-- TOC entry 5445 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN auditoria.tabla_afectada; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.auditoria.tabla_afectada IS 'Sin CHECK: nombre de tabla afectada, controlado en backend para evitar mantenimiento de constraint.';


--
-- TOC entry 5446 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN auditoria.valor_anterior; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.auditoria.valor_anterior IS 'JSONB: estado del registro antes del cambio. Indexable y consultable con operadores ->, ->>.';


--
-- TOC entry 5447 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN auditoria.valor_nuevo; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.auditoria.valor_nuevo IS 'JSONB: estado del registro después del cambio. Permite queries como precio anterior > 500.';


--
-- TOC entry 220 (class 1259 OID 106528)
-- Name: auditoria_id_auditoria_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.auditoria_id_auditoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5448 (class 0 OID 0)
-- Dependencies: 220
-- Name: auditoria_id_auditoria_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.auditoria_id_auditoria_seq OWNED BY public.auditoria.id_auditoria;


--
-- TOC entry 221 (class 1259 OID 106529)
-- Name: cliente; Type: TABLE; Schema: public
--

CREATE TABLE public.cliente (
    id_cliente bigint NOT NULL,
    cli_nombre character varying(100) NOT NULL,
    cli_telefono character varying(20),
    cli_correo character varying(100),
    cli_direccion character varying(255),
    cli_activo boolean DEFAULT true NOT NULL,
    cli_fechacreacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- TOC entry 5449 (class 0 OID 0)
-- Dependencies: 221
-- Name: TABLE cliente; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.cliente IS 'Clientes que contratan eventos de decoración';


--
-- TOC entry 5450 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN cliente.cli_telefono; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.cliente.cli_telefono IS 'No único: familias/empresas pueden compartir número.';


--
-- TOC entry 5451 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN cliente.cli_correo; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.cliente.cli_correo IS 'Único cuando se proporciona. NULL permitido.';


--
-- TOC entry 5452 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN cliente.cli_activo; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.cliente.cli_activo IS 'Soft delete: FALSE = cliente inactivo. Sus eventos históricos se preservan.';


--
-- TOC entry 222 (class 1259 OID 106538)
-- Name: cliente_id_cliente_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.cliente_id_cliente_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5453 (class 0 OID 0)
-- Dependencies: 222
-- Name: cliente_id_cliente_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.cliente_id_cliente_seq OWNED BY public.cliente.id_cliente;


--
-- TOC entry 223 (class 1259 OID 106539)
-- Name: concepto_pago; Type: TABLE; Schema: public
--

CREATE TABLE public.concepto_pago (
    id_concepto_pago bigint NOT NULL,
    concepto_nombre character varying(30) NOT NULL,
    concepto_descripcion character varying(255)
);


--
-- TOC entry 5454 (class 0 OID 0)
-- Dependencies: 223
-- Name: TABLE concepto_pago; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.concepto_pago IS 'Catálogo de conceptos de pago (ANTICIPO, ABONO, SALDO_TOTAL, PENALIZACION). Editable por ADMIN.';


--
-- TOC entry 224 (class 1259 OID 106544)
-- Name: concepto_pago_id_concepto_pago_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.concepto_pago_id_concepto_pago_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5455 (class 0 OID 0)
-- Dependencies: 224
-- Name: concepto_pago_id_concepto_pago_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.concepto_pago_id_concepto_pago_seq OWNED BY public.concepto_pago.id_concepto_pago;


--
-- TOC entry 225 (class 1259 OID 106545)
-- Name: configuracion; Type: TABLE; Schema: public
--

CREATE TABLE public.configuracion (
    config_clave character varying(100) NOT NULL,
    config_valor text NOT NULL,
    config_tipo character varying(10) DEFAULT 'STRING'::character varying NOT NULL,
    config_descripcion character varying(255),
    CONSTRAINT chk_configuracion_clave_upper CHECK (((config_clave)::text = upper((config_clave)::text))),
    CONSTRAINT chk_configuracion_tipo CHECK (((config_tipo)::text = ANY (ARRAY[('INT'::character varying)::text, ('DECIMAL'::character varying)::text, ('BOOLEAN'::character varying)::text, ('STRING'::character varying)::text, ('JSON'::character varying)::text])))
);


--
-- TOC entry 5456 (class 0 OID 0)
-- Dependencies: 225
-- Name: TABLE configuracion; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.configuracion IS 'Parámetros globales del sistema. Editables por ADMIN sin modificar código.';


--
-- TOC entry 5457 (class 0 OID 0)
-- Dependencies: 225
-- Name: COLUMN configuracion.config_valor; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.configuracion.config_valor IS 'TEXT: soporta números, porcentajes, cadenas y JSON según config_tipo.';


--
-- TOC entry 5458 (class 0 OID 0)
-- Dependencies: 225
-- Name: COLUMN configuracion.config_tipo; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.configuracion.config_tipo IS 'Tipo del valor: INT, DECIMAL, BOOLEAN, STRING, JSON. El backend hace el cast correspondiente.';


--
-- TOC entry 226 (class 1259 OID 106556)
-- Name: cronograma; Type: TABLE; Schema: public
--

CREATE TABLE public.cronograma (
    id_cronograma bigint NOT NULL,
    id_evento bigint NOT NULL,
    actividad character varying(255) NOT NULL,
    fecha_inicio timestamp without time zone NOT NULL,
    fecha_fin timestamp without time zone NOT NULL,
    crono_estado character varying(15) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    crono_observacion character varying(255),
    CONSTRAINT chk_cronograma_estado CHECK (((crono_estado)::text = ANY (ARRAY[('PENDIENTE'::character varying)::text, ('EN_PROGRESO'::character varying)::text, ('COMPLETADO'::character varying)::text, ('CANCELADO'::character varying)::text]))),
    CONSTRAINT chk_cronograma_fechas CHECK ((fecha_fin > fecha_inicio))
);


--
-- TOC entry 5459 (class 0 OID 0)
-- Dependencies: 226
-- Name: TABLE cronograma; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.cronograma IS 'Actividades de montaje/desmontaje. Disponible desde CONFIRMADO. Se cancela en cascada con el evento.';


--
-- TOC entry 5460 (class 0 OID 0)
-- Dependencies: 226
-- Name: COLUMN cronograma.crono_estado; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.cronograma.crono_estado IS 'PENDIENTE→EN_PROGRESO→COMPLETADO | CANCELADO (cascada al cancelar evento)';


--
-- TOC entry 227 (class 1259 OID 106570)
-- Name: cronograma_id_cronograma_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.cronograma_id_cronograma_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5461 (class 0 OID 0)
-- Dependencies: 227
-- Name: cronograma_id_cronograma_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.cronograma_id_cronograma_seq OWNED BY public.cronograma.id_cronograma;


--
-- TOC entry 228 (class 1259 OID 106571)
-- Name: cronograma_usuario; Type: TABLE; Schema: public
--

CREATE TABLE public.cronograma_usuario (
    id_cronograma bigint NOT NULL,
    id_usuario bigint NOT NULL,
    pago_asignado numeric(10,2) DEFAULT 0.00 NOT NULL,
    pago_tipo character varying(10) DEFAULT 'FIJO'::character varying NOT NULL,
    CONSTRAINT chk_cu_pago_asignado CHECK ((pago_asignado >= (0)::numeric)),
    CONSTRAINT chk_cu_pago_tipo CHECK (((pago_tipo)::text = ANY (ARRAY[('FIJO'::character varying)::text, ('POR_HORA'::character varying)::text])))
);


--
-- TOC entry 5462 (class 0 OID 0)
-- Dependencies: 228
-- Name: TABLE cronograma_usuario; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.cronograma_usuario IS 'Usuarios asignados a actividades del cronograma con pago individual';


--
-- TOC entry 5463 (class 0 OID 0)
-- Dependencies: 228
-- Name: COLUMN cronograma_usuario.pago_tipo; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.cronograma_usuario.pago_tipo IS 'FIJO = total por actividad. POR_HORA = tarifa/hora (v2: agregar horas_trabajadas).';


--
-- TOC entry 229 (class 1259 OID 106582)
-- Name: decoracion; Type: TABLE; Schema: public
--

CREATE TABLE public.decoracion (
    id_decoracion bigint NOT NULL,
    deco_nombre character varying(100) NOT NULL,
    decor_descripcion character varying(255),
    decor_precio numeric(10,2) DEFAULT 0.00 NOT NULL,
    decor_imagen_url character varying(500),
    decor_estado character varying(10) DEFAULT 'ACTIVO'::character varying NOT NULL,
    decor_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_decoracion_estado CHECK (((decor_estado)::text = ANY (ARRAY[('ACTIVO'::character varying)::text, ('INACTIVO'::character varying)::text]))),
    CONSTRAINT chk_decoracion_precio CHECK ((decor_precio >= (0)::numeric))
);


--
-- TOC entry 5464 (class 0 OID 0)
-- Dependencies: 229
-- Name: TABLE decoracion; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.decoracion IS 'Paquetes de decoración ofrecidos por el negocio';


--
-- TOC entry 5465 (class 0 OID 0)
-- Dependencies: 229
-- Name: COLUMN decoracion.decor_precio; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.decoracion.decor_precio IS 'Precio de venta. Se copia a evento.event_precio_base al crear el evento.';


--
-- TOC entry 5466 (class 0 OID 0)
-- Dependencies: 229
-- Name: COLUMN decoracion.decor_imagen_url; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.decoracion.decor_imagen_url IS 'URL de imagen en Cloudinary u otro servicio. VARCHAR(500) por URLs largas con transformaciones.';


--
-- TOC entry 230 (class 1259 OID 106597)
-- Name: decoracion_id_decoracion_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.decoracion_id_decoracion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5467 (class 0 OID 0)
-- Dependencies: 230
-- Name: decoracion_id_decoracion_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.decoracion_id_decoracion_seq OWNED BY public.decoracion.id_decoracion;


--
-- TOC entry 231 (class 1259 OID 106598)
-- Name: decoracion_tipo_evento; Type: TABLE; Schema: public
--

CREATE TABLE public.decoracion_tipo_evento (
    id_decoracion bigint NOT NULL,
    id_tipo_evento bigint NOT NULL,
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- TOC entry 232 (class 1259 OID 106605)
-- Name: decoracionxproducto; Type: TABLE; Schema: public
--

CREATE TABLE public.decoracionxproducto (
    id_decoracion bigint NOT NULL,
    id_producto bigint NOT NULL,
    cantidad_requerida integer NOT NULL,
    CONSTRAINT chk_dxp_cantidad_positiva CHECK ((cantidad_requerida > 0))
);


--
-- TOC entry 5468 (class 0 OID 0)
-- Dependencies: 232
-- Name: TABLE decoracionxproducto; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.decoracionxproducto IS 'Composición de cada paquete: qué productos y en qué cantidad';


--
-- TOC entry 233 (class 1259 OID 106612)
-- Name: documento; Type: TABLE; Schema: public
--

CREATE TABLE public.documento (
    id_documento bigint NOT NULL,
    id_evento bigint NOT NULL,
    id_plantilla bigint,
    docu_tipo character varying(50) NOT NULL,
    docu_fechageneracion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    archivo_url character varying(500),
    docu_estado_envio character varying(15) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    docu_fecha_envio timestamp without time zone,
    docu_intentos_envio integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_documento_estado_envio CHECK (((docu_estado_envio)::text = ANY (ARRAY[('PENDIENTE'::character varying)::text, ('ENVIADO'::character varying)::text, ('FALLIDO'::character varying)::text, ('REENVIADO'::character varying)::text])))
);


--
-- TOC entry 5469 (class 0 OID 0)
-- Dependencies: 233
-- Name: TABLE documento; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.documento IS 'Documentos generados y enviados al cliente: confirmaciones, contratos, comprobantes';


--
-- TOC entry 5470 (class 0 OID 0)
-- Dependencies: 233
-- Name: COLUMN documento.archivo_url; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.documento.archivo_url IS 'URL en almacenamiento en nube (ej. Cloudinary). VARCHAR(500) por URLs largas.';


--
-- TOC entry 5471 (class 0 OID 0)
-- Dependencies: 233
-- Name: COLUMN documento.docu_estado_envio; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.documento.docu_estado_envio IS 'PENDIENTE=generado sin enviar. ENVIADO=entregado. FALLIDO=error. REENVIADO=reintento exitoso.';


--
-- TOC entry 5472 (class 0 OID 0)
-- Dependencies: 233
-- Name: COLUMN documento.docu_intentos_envio; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.documento.docu_intentos_envio IS 'Contador de intentos de envío. Útil para controlar reintentos automáticos.';


--
-- TOC entry 234 (class 1259 OID 106627)
-- Name: documento_id_documento_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.documento_id_documento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5473 (class 0 OID 0)
-- Dependencies: 234
-- Name: documento_id_documento_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.documento_id_documento_seq OWNED BY public.documento.id_documento;


--
-- TOC entry 235 (class 1259 OID 106628)
-- Name: estado_evento; Type: TABLE; Schema: public
--

CREATE TABLE public.estado_evento (
    id_estadoevento bigint NOT NULL,
    estado_nombre character varying(30) NOT NULL,
    estado_descripcion character varying(255)
);


--
-- TOC entry 5474 (class 0 OID 0)
-- Dependencies: 235
-- Name: TABLE estado_evento; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.estado_evento IS 'Catálogo de estados del ciclo de vida de un evento. Sin CHECK: transiciones controladas en backend.';


--
-- TOC entry 5475 (class 0 OID 0)
-- Dependencies: 235
-- Name: COLUMN estado_evento.estado_nombre; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.estado_evento.estado_nombre IS 'COTIZACION→PROGRAMADO→CONFIRMADO→EN_MONTAJE→FINALIZADO | CANCELACION_PENDIENTE→CANCELADO';


--
-- TOC entry 236 (class 1259 OID 106633)
-- Name: estado_evento_id_estadoevento_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.estado_evento_id_estadoevento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5476 (class 0 OID 0)
-- Dependencies: 236
-- Name: estado_evento_id_estadoevento_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.estado_evento_id_estadoevento_seq OWNED BY public.estado_evento.id_estadoevento;


--
-- TOC entry 237 (class 1259 OID 106634)
-- Name: estado_notificacion; Type: TABLE; Schema: public
--

CREATE TABLE public.estado_notificacion (
    id_estado_notificacion bigint NOT NULL,
    nombre_estado character varying(20) NOT NULL,
    descripcion character varying(255),
    CONSTRAINT chk_estado_notificacion CHECK (((nombre_estado)::text = ANY (ARRAY[('PENDIENTE'::character varying)::text, ('ENVIADA'::character varying)::text, ('FALLIDA'::character varying)::text, ('LEIDA'::character varying)::text])))
);


--
-- TOC entry 5477 (class 0 OID 0)
-- Dependencies: 237
-- Name: TABLE estado_notificacion; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.estado_notificacion IS 'Estados técnicos de notificación. CHECK mantenido: son estados fijos del proceso, no del negocio.';


--
-- TOC entry 238 (class 1259 OID 106640)
-- Name: estado_notificacion_id_estado_notificacion_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.estado_notificacion_id_estado_notificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5478 (class 0 OID 0)
-- Dependencies: 238
-- Name: estado_notificacion_id_estado_notificacion_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.estado_notificacion_id_estado_notificacion_seq OWNED BY public.estado_notificacion.id_estado_notificacion;


--
-- TOC entry 239 (class 1259 OID 106641)
-- Name: estado_pago; Type: TABLE; Schema: public
--

CREATE TABLE public.estado_pago (
    id_estado_pago bigint NOT NULL,
    estado_nombre character varying(20) NOT NULL,
    estado_descripcion character varying(255)
);


--
-- TOC entry 5479 (class 0 OID 0)
-- Dependencies: 239
-- Name: TABLE estado_pago; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.estado_pago IS 'Catálogo de estados de pago (PENDIENTE, PAGADO, CANCELADO). Editable por ADMIN.';


--
-- TOC entry 240 (class 1259 OID 106646)
-- Name: estado_pago_id_estado_pago_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.estado_pago_id_estado_pago_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5480 (class 0 OID 0)
-- Dependencies: 240
-- Name: estado_pago_id_estado_pago_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.estado_pago_id_estado_pago_seq OWNED BY public.estado_pago.id_estado_pago;


--
-- TOC entry 241 (class 1259 OID 106647)
-- Name: estado_reserva; Type: TABLE; Schema: public
--

CREATE TABLE public.estado_reserva (
    id_estado_reserva bigint NOT NULL,
    estado_nombre character varying(20) NOT NULL,
    estado_descripcion character varying(255)
);


--
-- TOC entry 5481 (class 0 OID 0)
-- Dependencies: 241
-- Name: TABLE estado_reserva; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.estado_reserva IS 'Catálogo de estados de la reserva de productos por evento.';


--
-- TOC entry 5482 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN estado_reserva.estado_nombre; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.estado_reserva.estado_nombre IS 'ACTIVA bloquea stock, LIBERADA conserva histórico sin bloquear, CONSUMIDA representa material usado al finalizar.';


--
-- TOC entry 242 (class 1259 OID 106652)
-- Name: estado_reserva_id_estado_reserva_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.estado_reserva_id_estado_reserva_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5483 (class 0 OID 0)
-- Dependencies: 242
-- Name: estado_reserva_id_estado_reserva_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.estado_reserva_id_estado_reserva_seq OWNED BY public.estado_reserva.id_estado_reserva;


--
-- TOC entry 243 (class 1259 OID 106653)
-- Name: evento; Type: TABLE; Schema: public
--

CREATE TABLE public.evento (
    id_evento bigint NOT NULL,
    id_cliente bigint NOT NULL,
    id_decoracion bigint NOT NULL,
    id_tipo_evento bigint NOT NULL,
    id_estadoevento bigint NOT NULL,
    id_estado_anterior bigint,
    event_fechaevento date NOT NULL,
    event_horainicio time without time zone,
    event_horafin time without time zone,
    event_ubicacion character varying(255),
    event_precio_base numeric(10,2) DEFAULT 0.00 NOT NULL,
    event_precio_adicional numeric(10,2) DEFAULT 0.00 NOT NULL,
    event_precio_total numeric(10,2) GENERATED ALWAYS AS ((event_precio_base + event_precio_adicional)) STORED,
    event_nota_precio character varying(255),
    event_nota character varying(255),
    motivo_cancelacion character varying(255),
    event_cliente_notificado boolean DEFAULT false NOT NULL,
    event_fecha_confirmacion timestamp without time zone,
    event_fecha_cancelacion timestamp without time zone,
    event_fecha_expiracion date,
    event_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_evento_horario CHECK (((event_horafin IS NULL) OR (event_horainicio IS NULL) OR (event_horafin > event_horainicio))),
    CONSTRAINT chk_evento_nota_precio CHECK (((event_precio_adicional = (0)::numeric) OR (event_nota_precio IS NOT NULL))),
    CONSTRAINT chk_evento_precio_adicional CHECK ((event_precio_adicional >= (0)::numeric)),
    CONSTRAINT chk_evento_precio_base CHECK ((event_precio_base >= (0)::numeric))
);


--
-- TOC entry 5484 (class 0 OID 0)
-- Dependencies: 243
-- Name: COLUMN evento.id_estado_anterior; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento.id_estado_anterior IS 'Estado guardado antes de CANCELACION_PENDIENTE. Permite revertir sin perder historial.';


--
-- TOC entry 5485 (class 0 OID 0)
-- Dependencies: 243
-- Name: COLUMN evento.event_precio_base; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento.event_precio_base IS 'Copiado de decoracion.decor_precio al crear. No cambia si la decoración cambia después.';


--
-- TOC entry 5486 (class 0 OID 0)
-- Dependencies: 243
-- Name: COLUMN evento.event_precio_adicional; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento.event_precio_adicional IS 'Cargos extra: mano de obra, transporte. Solo ADMIN puede modificar.';


--
-- TOC entry 5487 (class 0 OID 0)
-- Dependencies: 243
-- Name: COLUMN evento.event_precio_total; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento.event_precio_total IS 'Generada automáticamente: precio_base + precio_adicional. No editar directamente.';


--
-- TOC entry 5488 (class 0 OID 0)
-- Dependencies: 243
-- Name: COLUMN evento.event_nota_precio; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento.event_nota_precio IS 'Justificación obligatoria cuando event_precio_adicional > 0.';


--
-- TOC entry 5489 (class 0 OID 0)
-- Dependencies: 243
-- Name: COLUMN evento.motivo_cancelacion; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento.motivo_cancelacion IS 'Obligatorio al iniciar CANCELACION_PENDIENTE. Capturado en el modal del frontend.';


--
-- TOC entry 5490 (class 0 OID 0)
-- Dependencies: 243
-- Name: COLUMN evento.event_cliente_notificado; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento.event_cliente_notificado IS 'TRUE cuando se generó y envió el documento de confirmación al cliente.';


--
-- TOC entry 5491 (class 0 OID 0)
-- Dependencies: 243
-- Name: COLUMN evento.event_fecha_cancelacion; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento.event_fecha_cancelacion IS 'TIMESTAMP de inicio de CANCELACION_PENDIENTE. Job del backend calcula si expiró el tiempo de gracia.';


--
-- TOC entry 5492 (class 0 OID 0)
-- Dependencies: 243
-- Name: COLUMN evento.event_fecha_expiracion; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento.event_fecha_expiracion IS 'Fecha límite para que una COTIZACION avance. Backend notifica o archiva si vence.';


--
-- TOC entry 244 (class 1259 OID 106677)
-- Name: evento_id_evento_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.evento_id_evento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5493 (class 0 OID 0)
-- Dependencies: 244
-- Name: evento_id_evento_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.evento_id_evento_seq OWNED BY public.evento.id_evento;


--
-- TOC entry 245 (class 1259 OID 106678)
-- Name: evento_producto; Type: TABLE; Schema: public
--

CREATE TABLE public.evento_producto (
    id_evento bigint NOT NULL,
    id_producto bigint NOT NULL,
    cantidad_reservada integer NOT NULL,
    id_estado_reserva bigint NOT NULL,
    fecha_reserva timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_liberacion timestamp without time zone,
    motivo_liberacion character varying(255),
    CONSTRAINT chk_ep_cantidad_positiva CHECK ((cantidad_reservada > 0))
);


--
-- TOC entry 5494 (class 0 OID 0)
-- Dependencies: 245
-- Name: TABLE evento_producto; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.evento_producto IS 'Reserva histórica de productos por evento. ACTIVA bloquea stock, LIBERADA deja de bloquear por cancelación, CONSUMIDA conserva evidencia de uso al finalizar.';


--
-- TOC entry 5495 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN evento_producto.id_estado_reserva; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento_producto.id_estado_reserva IS 'Estado funcional de la reserva: ACTIVA, LIBERADA o CONSUMIDA.';


--
-- TOC entry 5496 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN evento_producto.fecha_reserva; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento_producto.fecha_reserva IS 'Fecha en que se creó la reserva de producto para el evento.';


--
-- TOC entry 5497 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN evento_producto.fecha_liberacion; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento_producto.fecha_liberacion IS 'Fecha en que la reserva fue liberada por cancelación definitiva.';


--
-- TOC entry 5498 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN evento_producto.motivo_liberacion; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.evento_producto.motivo_liberacion IS 'Motivo funcional de liberación de la reserva.';


--
-- TOC entry 246 (class 1259 OID 106688)
-- Name: gasto_evento; Type: TABLE; Schema: public
--

CREATE TABLE public.gasto_evento (
    id_gasto bigint NOT NULL,
    id_evento bigint NOT NULL,
    gasto_concepto character varying(100) NOT NULL,
    gasto_monto numeric(10,2) NOT NULL,
    gasto_estado character varying(15) DEFAULT 'PROYECTADO'::character varying NOT NULL,
    gasto_fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gasto_nota character varying(255),
    CONSTRAINT chk_gasto_estado CHECK (((gasto_estado)::text = ANY (ARRAY[('PROYECTADO'::character varying)::text, ('DESEMBOLSADO'::character varying)::text]))),
    CONSTRAINT chk_gasto_monto_positivo CHECK ((gasto_monto > (0)::numeric))
);


--
-- TOC entry 5499 (class 0 OID 0)
-- Dependencies: 246
-- Name: TABLE gasto_evento; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.gasto_evento IS 'Gastos variables por evento. Usados en cálculo de rentabilidad.';


--
-- TOC entry 5500 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN gasto_evento.id_evento; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.gasto_evento.id_evento IS 'Solo permitido en eventos PROGRAMADO, CONFIRMADO o EN_MONTAJE (validado en backend).';


--
-- TOC entry 5501 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN gasto_evento.gasto_estado; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.gasto_evento.gasto_estado IS 'PROYECTADO = estimado, aún no pagado. DESEMBOLSADO = ya salió del bolsillo.';


--
-- TOC entry 247 (class 1259 OID 106701)
-- Name: gasto_evento_id_gasto_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.gasto_evento_id_gasto_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5502 (class 0 OID 0)
-- Dependencies: 247
-- Name: gasto_evento_id_gasto_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.gasto_evento_id_gasto_seq OWNED BY public.gasto_evento.id_gasto;


--
-- TOC entry 248 (class 1259 OID 106702)
-- Name: metodo_pago; Type: TABLE; Schema: public
--

CREATE TABLE public.metodo_pago (
    id_metodo_pago bigint NOT NULL,
    metodo_nombre character varying(30) NOT NULL,
    metodo_descripcion character varying(255),
    metodo_activo boolean DEFAULT true NOT NULL
);


--
-- TOC entry 5503 (class 0 OID 0)
-- Dependencies: 248
-- Name: TABLE metodo_pago; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.metodo_pago IS 'Formas de pago aceptadas. metodo_activo=FALSE oculta la opción sin borrar el historial.';


--
-- TOC entry 5504 (class 0 OID 0)
-- Dependencies: 248
-- Name: COLUMN metodo_pago.metodo_activo; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.metodo_pago.metodo_activo IS 'FALSE = método desactivado, no aparece como opción pero sus pagos históricos se conservan.';


--
-- TOC entry 249 (class 1259 OID 106709)
-- Name: metodo_pago_id_metodo_pago_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.metodo_pago_id_metodo_pago_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5505 (class 0 OID 0)
-- Dependencies: 249
-- Name: metodo_pago_id_metodo_pago_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.metodo_pago_id_metodo_pago_seq OWNED BY public.metodo_pago.id_metodo_pago;


--
-- TOC entry 250 (class 1259 OID 106710)
-- Name: movimiento_inventario; Type: TABLE; Schema: public
--

CREATE TABLE public.movimiento_inventario (
    id_movimiento bigint NOT NULL,
    id_producto bigint NOT NULL,
    id_evento bigint,
    registrado_por bigint,
    tipo_movimiento character varying(10) NOT NULL,
    tipo_origen character varying(20) DEFAULT 'REPOSICION'::character varying NOT NULL,
    cantidad integer NOT NULL,
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    motivo character varying(255),
    CONSTRAINT chk_movimiento_cantidad_positiva CHECK ((cantidad > 0)),
    CONSTRAINT chk_movimiento_evento_requerido CHECK ((((tipo_origen)::text <> 'EVENTO'::text) OR (id_evento IS NOT NULL))),
    CONSTRAINT chk_movimiento_origen CHECK (((tipo_origen)::text = ANY (ARRAY[('EVENTO'::character varying)::text, ('REPOSICION'::character varying)::text, ('AJUSTE_MANUAL'::character varying)::text, ('DAÑO'::character varying)::text, ('PERDIDA'::character varying)::text]))),
    CONSTRAINT chk_movimiento_tipo CHECK (((tipo_movimiento)::text = ANY (ARRAY[('ENTRADA'::character varying)::text, ('SALIDA'::character varying)::text])))
);


--
-- TOC entry 5506 (class 0 OID 0)
-- Dependencies: 250
-- Name: TABLE movimiento_inventario; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.movimiento_inventario IS 'Historial completo de stock. Toda modificación de produ_cantidad_total pasa por aquí.';


--
-- TOC entry 5507 (class 0 OID 0)
-- Dependencies: 250
-- Name: COLUMN movimiento_inventario.id_evento; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.movimiento_inventario.id_evento IS 'Solo cuando tipo_origen=EVENTO (obligatorio por constraint).';


--
-- TOC entry 5508 (class 0 OID 0)
-- Dependencies: 250
-- Name: COLUMN movimiento_inventario.tipo_origen; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.movimiento_inventario.tipo_origen IS 'EVENTO=fin de evento, REPOSICION=compra, AJUSTE_MANUAL=conteo, DAÑO/PERDIDA=baja.';


--
-- TOC entry 251 (class 1259 OID 106725)
-- Name: movimiento_inventario_id_movimiento_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.movimiento_inventario_id_movimiento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5509 (class 0 OID 0)
-- Dependencies: 251
-- Name: movimiento_inventario_id_movimiento_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.movimiento_inventario_id_movimiento_seq OWNED BY public.movimiento_inventario.id_movimiento;


--
-- TOC entry 252 (class 1259 OID 106726)
-- Name: notificacion; Type: TABLE; Schema: public
--

CREATE TABLE public.notificacion (
    id_notificacion bigint NOT NULL,
    id_usuario bigint NOT NULL,
    id_tipo_notificacion bigint NOT NULL,
    id_estado_notificacion bigint NOT NULL,
    id_evento bigint,
    mensaje character varying(500) NOT NULL,
    fecha_envio timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- TOC entry 5510 (class 0 OID 0)
-- Dependencies: 252
-- Name: COLUMN notificacion.id_evento; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.notificacion.id_evento IS 'FK opcional. Permite al frontend navegar directamente al evento relacionado.';


--
-- TOC entry 253 (class 1259 OID 106738)
-- Name: notificacion_id_notificacion_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.notificacion_id_notificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5511 (class 0 OID 0)
-- Dependencies: 253
-- Name: notificacion_id_notificacion_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.notificacion_id_notificacion_seq OWNED BY public.notificacion.id_notificacion;


--
-- TOC entry 254 (class 1259 OID 106739)
-- Name: pago; Type: TABLE; Schema: public
--

CREATE TABLE public.pago (
    id_pago bigint NOT NULL,
    id_evento bigint NOT NULL,
    id_estado_pago bigint NOT NULL,
    id_concepto_pago bigint NOT NULL,
    id_metodo_pago bigint NOT NULL,
    pago_monto numeric(10,2) NOT NULL,
    pago_fecha timestamp without time zone,
    CONSTRAINT chk_pago_monto_positivo CHECK ((pago_monto > (0)::numeric))
);


--
-- TOC entry 5512 (class 0 OID 0)
-- Dependencies: 254
-- Name: TABLE pago; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.pago IS 'Pagos del cliente por evento. Múltiples pagos por evento soportados.';


--
-- TOC entry 5513 (class 0 OID 0)
-- Dependencies: 254
-- Name: COLUMN pago.pago_fecha; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.pago.pago_fecha IS 'NULL mientras estado sea PENDIENTE. Se llena al confirmar el pago.';


--
-- TOC entry 255 (class 1259 OID 106749)
-- Name: pago_id_pago_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.pago_id_pago_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5514 (class 0 OID 0)
-- Dependencies: 255
-- Name: pago_id_pago_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.pago_id_pago_seq OWNED BY public.pago.id_pago;


--
-- TOC entry 256 (class 1259 OID 106750)
-- Name: plantilla_documento; Type: TABLE; Schema: public
--

CREATE TABLE public.plantilla_documento (
    id_plantilla bigint NOT NULL,
    tipo_documento character varying(50) NOT NULL,
    contenido_base text NOT NULL,
    campos_editables text,
    activo boolean DEFAULT true NOT NULL,
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- TOC entry 5515 (class 0 OID 0)
-- Dependencies: 256
-- Name: TABLE plantilla_documento; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.plantilla_documento IS 'Plantillas para generación de documentos PDF enviados al cliente';


--
-- TOC entry 5516 (class 0 OID 0)
-- Dependencies: 256
-- Name: COLUMN plantilla_documento.contenido_base; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.plantilla_documento.contenido_base IS 'HTML o texto plano con placeholders: {{cliente_nombre}}, {{fecha_evento}}, {{precio_total}}, etc.';


--
-- TOC entry 5517 (class 0 OID 0)
-- Dependencies: 256
-- Name: COLUMN plantilla_documento.campos_editables; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.plantilla_documento.campos_editables IS 'JSON con nombres de secciones que el admin puede modificar. Campos obligatorios protegidos en backend.';


--
-- TOC entry 257 (class 1259 OID 106762)
-- Name: plantilla_documento_id_plantilla_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.plantilla_documento_id_plantilla_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5518 (class 0 OID 0)
-- Dependencies: 257
-- Name: plantilla_documento_id_plantilla_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.plantilla_documento_id_plantilla_seq OWNED BY public.plantilla_documento.id_plantilla;


--
-- TOC entry 258 (class 1259 OID 106763)
-- Name: producto; Type: TABLE; Schema: public
--

CREATE TABLE public.producto (
    id_producto bigint NOT NULL,
    produ_nombre character varying(100) NOT NULL,
    produ_descripcion character varying(255),
    produ_cantidad_total integer DEFAULT 0 NOT NULL,
    produ_precio_unitario numeric(10,2) DEFAULT 0.00 NOT NULL,
    produ_stock_minimo integer DEFAULT 0 NOT NULL,
    produ_estado character varying(10) DEFAULT 'ACTIVO'::character varying NOT NULL,
    produ_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_producto_cantidad_no_negativa CHECK ((produ_cantidad_total >= 0)),
    CONSTRAINT chk_producto_estado CHECK (((produ_estado)::text = ANY (ARRAY[('ACTIVO'::character varying)::text, ('INACTIVO'::character varying)::text]))),
    CONSTRAINT chk_producto_precio_unitario CHECK ((produ_precio_unitario >= (0)::numeric)),
    CONSTRAINT chk_producto_stock_minimo CHECK ((produ_stock_minimo >= 0))
);


--
-- TOC entry 5519 (class 0 OID 0)
-- Dependencies: 258
-- Name: TABLE producto; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.producto IS 'Inventario físico de artículos del negocio';


--
-- TOC entry 5520 (class 0 OID 0)
-- Dependencies: 258
-- Name: COLUMN producto.produ_cantidad_total; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.producto.produ_cantidad_total IS 'Stock físico real. Disponibilidad real en vista_stock_producto.';


--
-- TOC entry 5521 (class 0 OID 0)
-- Dependencies: 258
-- Name: COLUMN producto.produ_precio_unitario; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.producto.produ_precio_unitario IS 'Costo por unidad. Usado para calcular costo de materiales por evento.';


--
-- TOC entry 5522 (class 0 OID 0)
-- Dependencies: 258
-- Name: COLUMN producto.produ_stock_minimo; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.producto.produ_stock_minimo IS 'Umbral de alerta. Notificación ALERTA_STOCK cuando disponible <= este valor.';


--
-- TOC entry 259 (class 1259 OID 106782)
-- Name: producto_id_producto_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.producto_id_producto_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5523 (class 0 OID 0)
-- Dependencies: 259
-- Name: producto_id_producto_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.producto_id_producto_seq OWNED BY public.producto.id_producto;


--
-- TOC entry 260 (class 1259 OID 106783)
-- Name: rol; Type: TABLE; Schema: public
--

CREATE TABLE public.rol (
    id_rol bigint NOT NULL,
    rol_nombre character varying(50) NOT NULL,
    rol_descripcion character varying(255)
);


--
-- TOC entry 5524 (class 0 OID 0)
-- Dependencies: 260
-- Name: TABLE rol; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.rol IS 'Catálogo de roles del sistema (ADMIN, EMPLEADO)';


--
-- TOC entry 5525 (class 0 OID 0)
-- Dependencies: 260
-- Name: COLUMN rol.rol_nombre; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.rol.rol_nombre IS 'Nombre único del rol. Usado por Spring Security como ROLE_{nombre}';


--
-- TOC entry 261 (class 1259 OID 106788)
-- Name: rol_id_rol_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.rol_id_rol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5526 (class 0 OID 0)
-- Dependencies: 261
-- Name: rol_id_rol_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.rol_id_rol_seq OWNED BY public.rol.id_rol;


--
-- TOC entry 262 (class 1259 OID 106789)
-- Name: tipo_evento; Type: TABLE; Schema: public
--

CREATE TABLE public.tipo_evento (
    id_tipo_evento bigint NOT NULL,
    tipo_nombre character varying(50) NOT NULL,
    tipo_descripcion character varying(255),
    tipo_activo boolean DEFAULT true NOT NULL
);


--
-- TOC entry 5527 (class 0 OID 0)
-- Dependencies: 262
-- Name: TABLE tipo_evento; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.tipo_evento IS 'Catálogo de tipos de evento, editable por el administrador';


--
-- TOC entry 263 (class 1259 OID 106796)
-- Name: tipo_evento_id_tipo_evento_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.tipo_evento_id_tipo_evento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5528 (class 0 OID 0)
-- Dependencies: 263
-- Name: tipo_evento_id_tipo_evento_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.tipo_evento_id_tipo_evento_seq OWNED BY public.tipo_evento.id_tipo_evento;


--
-- TOC entry 264 (class 1259 OID 106797)
-- Name: tipo_notificacion; Type: TABLE; Schema: public
--

CREATE TABLE public.tipo_notificacion (
    id_tipo_notificacion bigint NOT NULL,
    nombre_tipo character varying(50) NOT NULL,
    descripcion character varying(255)
);


--
-- TOC entry 5529 (class 0 OID 0)
-- Dependencies: 264
-- Name: TABLE tipo_notificacion; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.tipo_notificacion IS 'Catálogo de tipos de notificación del sistema';


--
-- TOC entry 265 (class 1259 OID 106802)
-- Name: tipo_notificacion_id_tipo_notificacion_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.tipo_notificacion_id_tipo_notificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5530 (class 0 OID 0)
-- Dependencies: 265
-- Name: tipo_notificacion_id_tipo_notificacion_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.tipo_notificacion_id_tipo_notificacion_seq OWNED BY public.tipo_notificacion.id_tipo_notificacion;


--
-- TOC entry 266 (class 1259 OID 106803)
-- Name: usuario; Type: TABLE; Schema: public
--

CREATE TABLE public.usuario (
    id_usuario bigint NOT NULL,
    usu_nombre character varying(100) NOT NULL,
    usu_correo character varying(100) NOT NULL,
    usu_password character varying(255) NOT NULL,
    usu_telefono character varying(20),
    usu_tipo_auth character varying(10) DEFAULT 'LOCAL'::character varying NOT NULL,
    estado boolean DEFAULT true NOT NULL,
    usu_fechacreacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    id_rol bigint NOT NULL,
    CONSTRAINT chk_usuario_tipo_auth CHECK (((usu_tipo_auth)::text = ANY (ARRAY[('LOCAL'::character varying)::text, ('GOOGLE'::character varying)::text])))
);


--
-- TOC entry 5531 (class 0 OID 0)
-- Dependencies: 266
-- Name: TABLE usuario; Type: COMMENT; Schema: public
--

COMMENT ON TABLE public.usuario IS 'Usuarios del sistema administrativo';


--
-- TOC entry 5532 (class 0 OID 0)
-- Dependencies: 266
-- Name: COLUMN usuario.usu_tipo_auth; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.usuario.usu_tipo_auth IS 'LOCAL = creado por ADMIN con password BCrypt. GOOGLE = autenticado via OAuth2';


--
-- TOC entry 5533 (class 0 OID 0)
-- Dependencies: 266
-- Name: COLUMN usuario.estado; Type: COMMENT; Schema: public
--

COMMENT ON COLUMN public.usuario.estado IS 'FALSE = cuenta desactivada. El historial (auditoría, cronogramas) se preserva.';


--
-- TOC entry 267 (class 1259 OID 106818)
-- Name: usuario_id_usuario_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.usuario_id_usuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5534 (class 0 OID 0)
-- Dependencies: 267
-- Name: usuario_id_usuario_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.usuario_id_usuario_seq OWNED BY public.usuario.id_usuario;


--
-- TOC entry 268 (class 1259 OID 106819)
-- Name: vista_cotizaciones_expiradas; Type: VIEW; Schema: public
--

CREATE VIEW public.vista_cotizaciones_expiradas AS
 SELECT e.id_evento,
    c.cli_nombre,
    c.cli_telefono,
    c.cli_correo,
    e.event_fechaevento,
    e.event_fecha_expiracion,
    e.event_creacion,
    d.deco_nombre AS decoracion,
    te.tipo_nombre AS tipo_evento,
    e.event_precio_total,
    (EXTRACT(day FROM (now() - ((e.event_fecha_expiracion)::timestamp without time zone)::timestamp with time zone)))::integer AS dias_vencida
   FROM ((((public.evento e
     JOIN public.cliente c ON ((c.id_cliente = e.id_cliente)))
     JOIN public.decoracion d ON ((d.id_decoracion = e.id_decoracion)))
     JOIN public.tipo_evento te ON ((te.id_tipo_evento = e.id_tipo_evento)))
     JOIN public.estado_evento ee ON ((ee.id_estadoevento = e.id_estadoevento)))
  WHERE (((ee.estado_nombre)::text = 'COTIZACION'::text) AND (e.event_fecha_expiracion IS NOT NULL) AND (e.event_fecha_expiracion < CURRENT_DATE))
  ORDER BY ((EXTRACT(day FROM (now() - ((e.event_fecha_expiracion)::timestamp without time zone)::timestamp with time zone)))::integer) DESC;


--
-- TOC entry 5535 (class 0 OID 0)
-- Dependencies: 268
-- Name: VIEW vista_cotizaciones_expiradas; Type: COMMENT; Schema: public
--

COMMENT ON VIEW public.vista_cotizaciones_expiradas IS 'Cotizaciones vencidas sin avanzar. Consultada por job @Scheduled en backend para notificaciones y archivado automático.';


--
-- TOC entry 269 (class 1259 OID 106824)
-- Name: vista_pagos_evento; Type: VIEW; Schema: public
--

CREATE VIEW public.vista_pagos_evento AS
 SELECT e.id_evento,
    e.event_precio_total AS precio_total,
    COALESCE(sum(
        CASE
            WHEN ((ep.estado_nombre)::text = 'PAGADO'::text) THEN p.pago_monto
            ELSE (0)::numeric
        END), (0)::numeric) AS total_cobrado,
    (e.event_precio_total - COALESCE(sum(
        CASE
            WHEN ((ep.estado_nombre)::text = 'PAGADO'::text) THEN p.pago_monto
            ELSE (0)::numeric
        END), (0)::numeric)) AS saldo_pendiente,
    (COALESCE(sum(
        CASE
            WHEN ((ep.estado_nombre)::text = 'PAGADO'::text) THEN p.pago_monto
            ELSE (0)::numeric
        END), (0)::numeric) >= e.event_precio_total) AS cobro_completo,
    COALESCE(sum(
        CASE
            WHEN ((ep.estado_nombre)::text = 'DEVOLUCION_PENDIENTE'::text) THEN p.pago_monto
            ELSE (0)::numeric
        END), (0)::numeric) AS total_devolucion_pendiente,
    (COALESCE(sum(
        CASE
            WHEN ((ep.estado_nombre)::text = 'DEVOLUCION_PENDIENTE'::text) THEN p.pago_monto
            ELSE (0)::numeric
        END), (0)::numeric) > (0)::numeric) AS tiene_devolucion_pendiente
   FROM ((public.evento e
     LEFT JOIN public.pago p ON ((p.id_evento = e.id_evento)))
     LEFT JOIN public.estado_pago ep ON ((ep.id_estado_pago = p.id_estado_pago)))
  GROUP BY e.id_evento, e.event_precio_total;


--
-- TOC entry 5536 (class 0 OID 0)
-- Dependencies: 269
-- Name: VIEW vista_pagos_evento; Type: COMMENT; Schema: public
--

COMMENT ON VIEW public.vista_pagos_evento IS 'Cobros por evento: total cobrado, saldo pendiente y si el cobro está completo. JOIN correcto a estado_pago.';


--
-- TOC entry 270 (class 1259 OID 106829)
-- Name: vista_eventos_periodo; Type: VIEW; Schema: public
--

CREATE VIEW public.vista_eventos_periodo AS
 SELECT e.id_evento,
    c.cli_nombre,
    c.cli_telefono,
    c.cli_correo,
    te.tipo_nombre AS tipo_evento,
    ee.estado_nombre AS estado,
    d.deco_nombre AS decoracion,
    d.decor_imagen_url AS decoracion_imagen,
    e.event_fechaevento,
    e.event_horainicio,
    e.event_horafin,
    e.event_ubicacion,
    e.event_precio_base,
    e.event_precio_adicional,
    e.event_precio_total,
    e.event_cliente_notificado,
    e.event_fecha_confirmacion,
    e.event_fecha_expiracion,
    e.event_creacion,
    vpe.total_cobrado,
    vpe.saldo_pendiente,
    vpe.cobro_completo,
    (EXISTS ( SELECT 1
           FROM public.cronograma cr
          WHERE (cr.id_evento = e.id_evento))) AS tiene_cronograma
   FROM (((((public.evento e
     JOIN public.cliente c ON ((c.id_cliente = e.id_cliente)))
     JOIN public.tipo_evento te ON ((te.id_tipo_evento = e.id_tipo_evento)))
     JOIN public.estado_evento ee ON ((ee.id_estadoevento = e.id_estadoevento)))
     JOIN public.decoracion d ON ((d.id_decoracion = e.id_decoracion)))
     JOIN public.vista_pagos_evento vpe ON ((vpe.id_evento = e.id_evento)));


--
-- TOC entry 5537 (class 0 OID 0)
-- Dependencies: 270
-- Name: VIEW vista_eventos_periodo; Type: COMMENT; Schema: public
--

COMMENT ON VIEW public.vista_eventos_periodo IS 'Vista completa de eventos para reportes y dashboard. Filtros de fecha aplicados en backend con parámetros.';


--
-- TOC entry 271 (class 1259 OID 106834)
-- Name: vista_rentabilidad_evento; Type: VIEW; Schema: public
--

CREATE VIEW public.vista_rentabilidad_evento AS
 WITH pagos AS (
         SELECT p_1.id_evento,
            COALESCE(sum(p_1.pago_monto) FILTER (WHERE ((ep.estado_nombre)::text = 'PAGADO'::text)), (0)::numeric) AS ingreso_cobrado
           FROM (public.pago p_1
             JOIN public.estado_pago ep ON ((ep.id_estado_pago = p_1.id_estado_pago)))
          GROUP BY p_1.id_evento
        ), materiales AS (
         SELECT ep.id_evento,
            COALESCE(sum(((ep.cantidad_reservada)::numeric * pr.produ_precio_unitario)), (0)::numeric) AS costo_materiales
           FROM ((public.evento_producto ep
             JOIN public.producto pr ON ((pr.id_producto = ep.id_producto)))
             JOIN public.estado_reserva er ON ((er.id_estado_reserva = ep.id_estado_reserva)))
          WHERE ((er.estado_nombre)::text = 'CONSUMIDA'::text)
          GROUP BY ep.id_evento
        ), mano_obra AS (
         SELECT c.id_evento,
            COALESCE(sum(
                CASE
                    WHEN ((cu.pago_tipo)::text = 'POR_HORA'::text) THEN (cu.pago_asignado * (EXTRACT(epoch FROM (c.fecha_fin - c.fecha_inicio)) / (3600)::numeric))
                    ELSE cu.pago_asignado
                END), (0)::numeric) AS costo_mano_obra
           FROM (public.cronograma c
             JOIN public.cronograma_usuario cu ON ((cu.id_cronograma = c.id_cronograma)))
          WHERE ((c.crono_estado)::text <> 'CANCELADO'::text)
          GROUP BY c.id_evento
        ), gastos AS (
         SELECT g_1.id_evento,
            COALESCE(sum(g_1.gasto_monto) FILTER (WHERE ((g_1.gasto_estado)::text = 'DESEMBOLSADO'::text)), (0)::numeric) AS gastos_desembolsados,
            COALESCE(sum(g_1.gasto_monto) FILTER (WHERE ((g_1.gasto_estado)::text = 'PROYECTADO'::text)), (0)::numeric) AS gastos_proyectados
           FROM public.gasto_evento g_1
          GROUP BY g_1.id_evento
        )
 SELECT e.id_evento,
    e.event_fechaevento,
    ee.estado_nombre AS estado,
    e.event_precio_total AS precio_acordado,
    COALESCE(p.ingreso_cobrado, (0)::numeric) AS ingreso_cobrado,
    COALESCE(m.costo_materiales, (0)::numeric) AS costo_materiales,
    COALESCE(mo.costo_mano_obra, (0)::numeric) AS costo_mano_obra,
    COALESCE(g.gastos_desembolsados, (0)::numeric) AS gastos_desembolsados,
    COALESCE(g.gastos_proyectados, (0)::numeric) AS gastos_proyectados,
    (((COALESCE(p.ingreso_cobrado, (0)::numeric) - COALESCE(m.costo_materiales, (0)::numeric)) - COALESCE(mo.costo_mano_obra, (0)::numeric)) - COALESCE(g.gastos_desembolsados, (0)::numeric)) AS ganancia_neta
   FROM (((((public.evento e
     LEFT JOIN public.estado_evento ee ON ((ee.id_estadoevento = e.id_estadoevento)))
     LEFT JOIN pagos p ON ((p.id_evento = e.id_evento)))
     LEFT JOIN materiales m ON ((m.id_evento = e.id_evento)))
     LEFT JOIN mano_obra mo ON ((mo.id_evento = e.id_evento)))
     LEFT JOIN gastos g ON ((g.id_evento = e.id_evento)));


--
-- TOC entry 5538 (class 0 OID 0)
-- Dependencies: 271
-- Name: VIEW vista_rentabilidad_evento; Type: COMMENT; Schema: public
--

COMMENT ON VIEW public.vista_rentabilidad_evento IS 'Rentabilidad real por evento: ingreso cobrado - materiales CONSUMIDA - mano de obra - gastos desembolsados. Significativa en FINALIZADOS.';


--
-- TOC entry 272 (class 1259 OID 106839)
-- Name: vista_stock_producto; Type: VIEW; Schema: public
--

CREATE VIEW public.vista_stock_producto AS
 SELECT p.id_producto,
    p.produ_nombre,
    p.produ_estado,
    p.produ_stock_minimo,
    p.produ_cantidad_total AS cantidad_total,
    COALESCE(sum(
        CASE
            WHEN (((er.estado_nombre)::text = 'ACTIVA'::text) AND ((ee.estado_nombre)::text = ANY (ARRAY[('PROGRAMADO'::character varying)::text, ('CONFIRMADO'::character varying)::text, ('EN_MONTAJE'::character varying)::text, ('CANCELACION_PENDIENTE'::character varying)::text]))) THEN ep.cantidad_reservada
            ELSE 0
        END), (0)::bigint) AS cantidad_reservada,
    (p.produ_cantidad_total - COALESCE(sum(
        CASE
            WHEN (((er.estado_nombre)::text = 'ACTIVA'::text) AND ((ee.estado_nombre)::text = ANY (ARRAY[('PROGRAMADO'::character varying)::text, ('CONFIRMADO'::character varying)::text, ('EN_MONTAJE'::character varying)::text, ('CANCELACION_PENDIENTE'::character varying)::text]))) THEN ep.cantidad_reservada
            ELSE 0
        END), (0)::bigint)) AS cantidad_disponible,
    p.produ_precio_unitario,
        CASE
            WHEN ((p.produ_cantidad_total - COALESCE(sum(
            CASE
                WHEN (((er.estado_nombre)::text = 'ACTIVA'::text) AND ((ee.estado_nombre)::text = ANY (ARRAY[('PROGRAMADO'::character varying)::text, ('CONFIRMADO'::character varying)::text, ('EN_MONTAJE'::character varying)::text, ('CANCELACION_PENDIENTE'::character varying)::text]))) THEN ep.cantidad_reservada
                ELSE 0
            END), (0)::bigint)) <= p.produ_stock_minimo) THEN true
            ELSE false
        END AS stock_bajo_alerta
   FROM ((((public.producto p
     LEFT JOIN public.evento_producto ep ON ((ep.id_producto = p.id_producto)))
     LEFT JOIN public.estado_reserva er ON ((er.id_estado_reserva = ep.id_estado_reserva)))
     LEFT JOIN public.evento e ON ((e.id_evento = ep.id_evento)))
     LEFT JOIN public.estado_evento ee ON ((ee.id_estadoevento = e.id_estadoevento)))
  GROUP BY p.id_producto, p.produ_nombre, p.produ_estado, p.produ_stock_minimo, p.produ_cantidad_total, p.produ_precio_unitario;


--
-- TOC entry 5539 (class 0 OID 0)
-- Dependencies: 272
-- Name: VIEW vista_stock_producto; Type: COMMENT; Schema: public
--

COMMENT ON VIEW public.vista_stock_producto IS 'Stock disponible real. Solo reservas ACTIVA en eventos PROGRAMADO/CONFIRMADO/EN_MONTAJE/CANCELACION_PENDIENTE suman como reservado.';


--
-- TOC entry 5001 (class 2604 OID 106844)
-- Name: auditoria id_auditoria; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.auditoria ALTER COLUMN id_auditoria SET DEFAULT nextval('public.auditoria_id_auditoria_seq'::regclass);


--
-- TOC entry 5003 (class 2604 OID 106845)
-- Name: cliente id_cliente; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.cliente ALTER COLUMN id_cliente SET DEFAULT nextval('public.cliente_id_cliente_seq'::regclass);


--
-- TOC entry 5006 (class 2604 OID 106846)
-- Name: concepto_pago id_concepto_pago; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.concepto_pago ALTER COLUMN id_concepto_pago SET DEFAULT nextval('public.concepto_pago_id_concepto_pago_seq'::regclass);


--
-- TOC entry 5008 (class 2604 OID 106847)
-- Name: cronograma id_cronograma; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.cronograma ALTER COLUMN id_cronograma SET DEFAULT nextval('public.cronograma_id_cronograma_seq'::regclass);


--
-- TOC entry 5012 (class 2604 OID 106848)
-- Name: decoracion id_decoracion; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.decoracion ALTER COLUMN id_decoracion SET DEFAULT nextval('public.decoracion_id_decoracion_seq'::regclass);


--
-- TOC entry 5017 (class 2604 OID 106849)
-- Name: documento id_documento; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.documento ALTER COLUMN id_documento SET DEFAULT nextval('public.documento_id_documento_seq'::regclass);


--
-- TOC entry 5021 (class 2604 OID 106850)
-- Name: estado_evento id_estadoevento; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.estado_evento ALTER COLUMN id_estadoevento SET DEFAULT nextval('public.estado_evento_id_estadoevento_seq'::regclass);


--
-- TOC entry 5022 (class 2604 OID 106851)
-- Name: estado_notificacion id_estado_notificacion; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.estado_notificacion ALTER COLUMN id_estado_notificacion SET DEFAULT nextval('public.estado_notificacion_id_estado_notificacion_seq'::regclass);


--
-- TOC entry 5023 (class 2604 OID 106852)
-- Name: estado_pago id_estado_pago; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.estado_pago ALTER COLUMN id_estado_pago SET DEFAULT nextval('public.estado_pago_id_estado_pago_seq'::regclass);


--
-- TOC entry 5024 (class 2604 OID 106853)
-- Name: estado_reserva id_estado_reserva; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.estado_reserva ALTER COLUMN id_estado_reserva SET DEFAULT nextval('public.estado_reserva_id_estado_reserva_seq'::regclass);


--
-- TOC entry 5025 (class 2604 OID 106854)
-- Name: evento id_evento; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.evento ALTER COLUMN id_evento SET DEFAULT nextval('public.evento_id_evento_seq'::regclass);


--
-- TOC entry 5032 (class 2604 OID 106855)
-- Name: gasto_evento id_gasto; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.gasto_evento ALTER COLUMN id_gasto SET DEFAULT nextval('public.gasto_evento_id_gasto_seq'::regclass);


--
-- TOC entry 5035 (class 2604 OID 106856)
-- Name: metodo_pago id_metodo_pago; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.metodo_pago ALTER COLUMN id_metodo_pago SET DEFAULT nextval('public.metodo_pago_id_metodo_pago_seq'::regclass);


--
-- TOC entry 5037 (class 2604 OID 106857)
-- Name: movimiento_inventario id_movimiento; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.movimiento_inventario ALTER COLUMN id_movimiento SET DEFAULT nextval('public.movimiento_inventario_id_movimiento_seq'::regclass);


--
-- TOC entry 5040 (class 2604 OID 106858)
-- Name: notificacion id_notificacion; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.notificacion ALTER COLUMN id_notificacion SET DEFAULT nextval('public.notificacion_id_notificacion_seq'::regclass);


--
-- TOC entry 5042 (class 2604 OID 106859)
-- Name: pago id_pago; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.pago ALTER COLUMN id_pago SET DEFAULT nextval('public.pago_id_pago_seq'::regclass);


--
-- TOC entry 5043 (class 2604 OID 106860)
-- Name: plantilla_documento id_plantilla; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.plantilla_documento ALTER COLUMN id_plantilla SET DEFAULT nextval('public.plantilla_documento_id_plantilla_seq'::regclass);


--
-- TOC entry 5046 (class 2604 OID 106861)
-- Name: producto id_producto; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.producto ALTER COLUMN id_producto SET DEFAULT nextval('public.producto_id_producto_seq'::regclass);


--
-- TOC entry 5052 (class 2604 OID 106862)
-- Name: rol id_rol; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.rol ALTER COLUMN id_rol SET DEFAULT nextval('public.rol_id_rol_seq'::regclass);


--
-- TOC entry 5053 (class 2604 OID 106863)
-- Name: tipo_evento id_tipo_evento; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.tipo_evento ALTER COLUMN id_tipo_evento SET DEFAULT nextval('public.tipo_evento_id_tipo_evento_seq'::regclass);


--
-- TOC entry 5055 (class 2604 OID 106864)
-- Name: tipo_notificacion id_tipo_notificacion; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.tipo_notificacion ALTER COLUMN id_tipo_notificacion SET DEFAULT nextval('public.tipo_notificacion_id_tipo_notificacion_seq'::regclass);


--
-- TOC entry 5056 (class 2604 OID 106865)
-- Name: usuario id_usuario; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.usuario ALTER COLUMN id_usuario SET DEFAULT nextval('public.usuario_id_usuario_seq'::regclass);


--
-- TOC entry 5388 (class 0 OID 106518)
-- Dependencies: 219
-- Data for Name: auditoria; Type: TABLE DATA; Schema: public
--

COPY public.auditoria (id_auditoria, id_usuario, accion, tabla_afectada, id_registro, valor_anterior, valor_nuevo, descripcion, fecha) FROM stdin;
\.


--
-- TOC entry 5390 (class 0 OID 106529)
-- Dependencies: 221
-- Data for Name: cliente; Type: TABLE DATA; Schema: public
--

COPY public.cliente (id_cliente, cli_nombre, cli_telefono, cli_correo, cli_direccion, cli_activo, cli_fechacreacion) FROM stdin;
\.


--
-- TOC entry 5392 (class 0 OID 106539)
-- Dependencies: 223
-- Data for Name: concepto_pago; Type: TABLE DATA; Schema: public
--

COPY public.concepto_pago (id_concepto_pago, concepto_nombre, concepto_descripcion) FROM stdin;
1	ANTICIPO	Primer pago al momento de reservar el evento
2	ABONO	Pago parcial intermedio
3	SALDO_TOTAL	Pago que cierra el balance completo del evento
4	PENALIZACION	Cargo por cancelación tardía según política del negocio
\.


--
-- TOC entry 5394 (class 0 OID 106545)
-- Dependencies: 225
-- Data for Name: configuracion; Type: TABLE DATA; Schema: public
--

COPY public.configuracion (config_clave, config_valor, config_tipo, config_descripcion) FROM stdin;
POLITICA_CANCELACION_MONTAJE	Política de cancelación de demostración.	STRING	Qué hacer con pagos al cancelar en EN_MONTAJE: RETENER_ANTICIPO o DEVOLVER_TODO
NEGOCIO_NOMBRE	Artepax Demo	STRING	Nombre del negocio usado en documentos y notificaciones generadas
NEGOCIO_CORREO	contacto@example.invalid	STRING	Correo remitente para envío de documentos y notificaciones al cliente
NEGOCIO_TELEFONO	0000-0000	STRING	Teléfono de contacto incluido en documentos generados
PORCENTAJE_ANTICIPO_MINIMO	50	INT	Porcentaje mínimo del precio total requerido como anticipo para confirmar un evento
TIEMPO_GRACIA_CANCELACION_MINUTOS	1	INT	Minutos que tiene el admin para revertir una cancelación antes de que se ejecuten los efectos en cascada
PORCENTAJE_STOCK_MINIMO_GLOBAL	24	INT	Porcentaje sobre cantidad_total usado como stock_minimo cuando produ_stock_minimo=0
DIAS_EXPIRACION_COTIZACION	1	INT	Días máximos que puede estar una cotización sin avanzar antes de notificar o archivar
INSTRUCCIONES_PAGO	Usar únicamente datos ficticios en el entorno de demostración.	STRING	Instrucciones generales de pago usadas en documentos y comunicaciones al cliente.
MINUTOS_GAP_ENTRE_EVENTOS	30	INT	Minutos de separación obligatoria entre eventos en la misma ubicación
DIAS_RECORDATORIO_EVENTO	1	INT	Días de anticipación para enviar recordatorio del evento
DIAS_AVISO_SALDO_PENDIENTE	1	INT	Días de anticipación para alertar saldo pendiente
\.


--
-- TOC entry 5395 (class 0 OID 106556)
-- Dependencies: 226
-- Data for Name: cronograma; Type: TABLE DATA; Schema: public
--

COPY public.cronograma (id_cronograma, id_evento, actividad, fecha_inicio, fecha_fin, crono_estado, crono_observacion) FROM stdin;
\.


--
-- TOC entry 5397 (class 0 OID 106571)
-- Dependencies: 228
-- Data for Name: cronograma_usuario; Type: TABLE DATA; Schema: public
--

COPY public.cronograma_usuario (id_cronograma, id_usuario, pago_asignado, pago_tipo) FROM stdin;
\.


--
-- TOC entry 5398 (class 0 OID 106582)
-- Dependencies: 229
-- Data for Name: decoracion; Type: TABLE DATA; Schema: public
--

COPY public.decoracion (id_decoracion, deco_nombre, decor_descripcion, decor_precio, decor_imagen_url, decor_estado, decor_creacion) FROM stdin;
2	Infantil Festiva	Decoración colorida con globos y personajes para niños	280.00	\N	ACTIVO	2026-04-26 15:40:12
1	Romántica Clásica	Decoración en tonos blancos y dorados con flores y velas	450.00	\N	ACTIVO	2026-04-26 15:40:12
3	Elegante Corporativa	Decoración sobria en negro y plateado para eventos formales	550.00	\N	ACTIVO	2026-04-26 15:40:12
\.


--
-- TOC entry 5400 (class 0 OID 106598)
-- Dependencies: 231
-- Data for Name: decoracion_tipo_evento; Type: TABLE DATA; Schema: public
--

COPY public.decoracion_tipo_evento (id_decoracion, id_tipo_evento, fecha_creacion) FROM stdin;
1	1	2026-05-17 17:28:26.610004
1	3	2026-05-17 17:28:26.610004
2	3	2026-05-17 17:28:26.610004
2	5	2026-05-17 17:28:26.610004
3	3	2026-05-17 17:28:26.610004
3	4	2026-05-17 17:28:26.610004
3	6	2026-05-17 17:28:26.610004
\.


--
-- TOC entry 5401 (class 0 OID 106605)
-- Dependencies: 232
-- Data for Name: decoracionxproducto; Type: TABLE DATA; Schema: public
--

COPY public.decoracionxproducto (id_decoracion, id_producto, cantidad_requerida) FROM stdin;
1	3	20
1	4	10
1	5	50
1	6	5
1	7	8
2	1	100
2	2	30
2	8	10
3	2	50
3	4	20
3	6	10
3	7	15
\.


--
-- TOC entry 5402 (class 0 OID 106612)
-- Dependencies: 233
-- Data for Name: documento; Type: TABLE DATA; Schema: public
--

COPY public.documento (id_documento, id_evento, id_plantilla, docu_tipo, docu_fechageneracion, archivo_url, docu_estado_envio, docu_fecha_envio, docu_intentos_envio) FROM stdin;
\.


--
-- TOC entry 5404 (class 0 OID 106628)
-- Dependencies: 235
-- Data for Name: estado_evento; Type: TABLE DATA; Schema: public
--

COPY public.estado_evento (id_estadoevento, estado_nombre, estado_descripcion) FROM stdin;
1	COTIZACION	Solicitud inicial. Sin compromiso formal. Stock no reservado.
2	PROGRAMADO	Fecha y decoración definidas. Stock reservado en evento_producto.
3	CONFIRMADO	Anticipo recibido. Evento comprometido formalmente.
4	EN_MONTAJE	El equipo está ejecutando el montaje del evento.
5	FINALIZADO	Evento completado. Movimientos de salida de stock generados.
6	CANCELADO	Evento cancelado definitivamente. Stock liberado.
7	CANCELACION_PENDIENTE	Cancelación iniciada. Stock intacto durante el tiempo de gracia.
8	PENDIENTE_COBRO	Evento ejecutado con saldo pendiente de cobro
\.


--
-- TOC entry 5406 (class 0 OID 106634)
-- Dependencies: 237
-- Data for Name: estado_notificacion; Type: TABLE DATA; Schema: public
--

COPY public.estado_notificacion (id_estado_notificacion, nombre_estado, descripcion) FROM stdin;
1	PENDIENTE	Generada pero aún no enviada
2	ENVIADA	Enviada exitosamente
3	FALLIDA	El envío falló
4	LEIDA	El usuario la leyó en el sistema
\.


--
-- TOC entry 5408 (class 0 OID 106641)
-- Dependencies: 239
-- Data for Name: estado_pago; Type: TABLE DATA; Schema: public
--

COPY public.estado_pago (id_estado_pago, estado_nombre, estado_descripcion) FROM stdin;
1	PENDIENTE	Pago registrado pero aún no recibido físicamente
2	PAGADO	Pago recibido y confirmado por el administrador
3	CANCELADO	Pago anulado, no aplica al total del evento
4	DEVOLUCION_PENDIENTE	Pago recibido y confirmado, pendiente de devolución por cancelación del evento
\.


--
-- TOC entry 5410 (class 0 OID 106647)
-- Dependencies: 241
-- Data for Name: estado_reserva; Type: TABLE DATA; Schema: public
--

COPY public.estado_reserva (id_estado_reserva, estado_nombre, estado_descripcion) FROM stdin;
1	ACTIVA	Reserva vigente que bloquea disponibilidad de stock
2	LIBERADA	Reserva liberada por cancelación definitiva del evento
3	CONSUMIDA	Reserva consumida o consolidada al finalizar el evento
\.


--
-- TOC entry 5412 (class 0 OID 106653)
-- Dependencies: 243
-- Data for Name: evento; Type: TABLE DATA; Schema: public
--

COPY public.evento (id_evento, id_cliente, id_decoracion, id_tipo_evento, id_estadoevento, id_estado_anterior, event_fechaevento, event_horainicio, event_horafin, event_ubicacion, event_precio_base, event_precio_adicional, event_nota_precio, event_nota, motivo_cancelacion, event_cliente_notificado, event_fecha_confirmacion, event_fecha_cancelacion, event_fecha_expiracion, event_creacion) FROM stdin;
\.


--
-- TOC entry 5414 (class 0 OID 106678)
-- Dependencies: 245
-- Data for Name: evento_producto; Type: TABLE DATA; Schema: public
--

COPY public.evento_producto (id_evento, id_producto, cantidad_reservada, id_estado_reserva, fecha_reserva, fecha_liberacion, motivo_liberacion) FROM stdin;
\.


--
-- TOC entry 5415 (class 0 OID 106688)
-- Dependencies: 246
-- Data for Name: gasto_evento; Type: TABLE DATA; Schema: public
--

COPY public.gasto_evento (id_gasto, id_evento, gasto_concepto, gasto_monto, gasto_estado, gasto_fecha, gasto_nota) FROM stdin;
\.


--
-- TOC entry 5417 (class 0 OID 106702)
-- Dependencies: 248
-- Data for Name: metodo_pago; Type: TABLE DATA; Schema: public
--

COPY public.metodo_pago (id_metodo_pago, metodo_nombre, metodo_descripcion, metodo_activo) FROM stdin;
1	EFECTIVO	Pago en efectivo entregado directamente	t
2	TRANSFERENCIA	Transferencia bancaria o depósito	t
3	TARJETA	Pago con tarjeta de crédito o débito	t
\.


--
-- TOC entry 5419 (class 0 OID 106710)
-- Dependencies: 250
-- Data for Name: movimiento_inventario; Type: TABLE DATA; Schema: public
--

COPY public.movimiento_inventario (id_movimiento, id_producto, id_evento, registrado_por, tipo_movimiento, tipo_origen, cantidad, fecha, motivo) FROM stdin;
\.


--
-- TOC entry 5421 (class 0 OID 106726)
-- Dependencies: 252
-- Data for Name: notificacion; Type: TABLE DATA; Schema: public
--

COPY public.notificacion (id_notificacion, id_usuario, id_tipo_notificacion, id_estado_notificacion, id_evento, mensaje, fecha_envio) FROM stdin;
\.


--
-- TOC entry 5423 (class 0 OID 106739)
-- Dependencies: 254
-- Data for Name: pago; Type: TABLE DATA; Schema: public
--

COPY public.pago (id_pago, id_evento, id_estado_pago, id_concepto_pago, id_metodo_pago, pago_monto, pago_fecha) FROM stdin;
\.


--
-- TOC entry 5425 (class 0 OID 106750)
-- Dependencies: 256
-- Data for Name: plantilla_documento; Type: TABLE DATA; Schema: public
--

COPY public.plantilla_documento (id_plantilla, tipo_documento, contenido_base, campos_editables, activo, fecha_creacion) FROM stdin;
1	CONFIRMACION_RESERVA	<h1><strong>Confirmación de Reserva - {{negocio_nombre}}</strong></h1>\n<h2>Confirmación de Reserva de Evento</h2>\n\n<p>Estimado/a <strong>{{cliente_nombre}}</strong>,</p>\n\n<p>Por este medio confirmamos la reserva de su evento con los siguientes datos:</p>\n\n<h3>Datos del evento</h3>\n<p><strong>Cliente:</strong> {{cliente_nombre}}</p>\n<p><strong>Tipo de evento:</strong> {{tipo_evento}}</p>\n<p><strong>Fecha:</strong> {{fecha_evento}}</p>\n<p><strong>Hora:</strong> {{hora_inicio}} - {{hora_fin}}</p>\n<p><strong>Lugar:</strong> {{ubicacion}}</p>\n<p><strong>Decoración:</strong> {{decoracion}}</p>\n\n<h3>Resumen financiero</h3>\n<p><strong>Precio total:</strong> ${{precio_total}}</p>\n<p><strong>Anticipo pagado:</strong> ${{anticipo_pagado}}</p>\n<p><strong>Saldo pendiente:</strong> ${{saldo_pendiente}}</p>\n\n<h3>Condiciones de la reserva</h3>\n<p>La reserva queda confirmada para la fecha y horario indicados. Cualquier cambio de fecha, horario, ubicación o decoración deberá coordinarse previamente con {{negocio_nombre}} y estará sujeto a disponibilidad.</p>\n\n<p>El saldo pendiente deberá ser cancelado según las condiciones acordadas antes de la realización del evento.</p>\n\n<p><strong>Contacto:</strong> {{negocio_correo}} | {{negocio_telefono}}</p>\n\n<p><em>Generado el: {{fecha_generacion}}</em></p>	["condiciones_reserva", "mensaje_adicional"]	t	2026-04-26 15:40:12
2	AVISO_SALDO_PENDIENTE	<h1>Aviso de Saldo Pendiente - {{negocio_nombre}}</h1>\n<h2>Aviso de Saldo Pendiente</h2>\n\n<p>Estimado/a <strong>{{cliente_nombre}}</strong>,</p>\n\n<p>Le informamos que su evento aún registra un saldo pendiente. A continuación se detallan los datos principales:</p>\n\n<h3>Detalles del evento</h3>\n<p><strong>Tipo de evento:</strong> {{tipo_evento}}</p>\n<p><strong>Fecha:</strong> {{fecha_evento}}</p>\n<p><strong>Hora:</strong> {{hora_inicio}} - {{hora_fin}}</p>\n<p><strong>Lugar:</strong> {{ubicacion}}</p>\n<p><strong>Decoración:</strong> {{decoracion}}</p>\n\n<h3>Resumen financiero</h3>\n<p><strong>Precio total:</strong> {{precio_total}}</p>\n<p><strong>Total pagado:</strong> {{total_pagado}}</p>\n<p><strong>Saldo pendiente:</strong> {{saldo_pendiente}}</p>\n\n<h3>Instrucciones de pago</h3>\n<p>Le solicitamos realizar el pago del saldo pendiente antes de la fecha acordada para evitar inconvenientes en la preparación del evento.</p>\n\n<p>Si ya realizó el pago, puede ignorar este aviso o comunicarse con nosotros para confirmar la actualización.</p>\n\n<p><strong>Contacto:</strong> {{negocio_correo}} | {{negocio_telefono}}</p>\n\n<p><em>Generado el: {{fecha_generacion}}</em></p>	[]	t	2026-05-09 11:37:32
3	COTIZACION_EVENTO	<h1><strong>Cotización - {{negocio_nombre}}</strong></h1>\n<h2>Cotización de Evento</h2>\n\n<p>Estimado/a <strong>{{cliente_nombre}}</strong>,</p>\n\n<p>Gracias por considerar nuestros servicios. A continuación se presenta la cotización preliminar para el evento solicitado.</p>\n\n<h3>Datos del evento</h3>\n<p><strong>Tipo de evento:</strong> {{tipo_evento}}</p>\n<p><strong>Fecha:</strong> {{fecha_evento}}</p>\n<p><strong>Hora:</strong> {{hora_inicio}} - {{hora_fin}}</p>\n<p><strong>Lugar:</strong> {{ubicacion}}</p>\n<p><strong>Decoración:</strong> {{decoracion}}</p>\n\n<h3>Resumen económico</h3>\n<p><strong>Precio total estimado:</strong> ${{precio_total}}</p>\n\n<h3>Condiciones</h3>\n<p>Esta cotización es preliminar y está sujeta a disponibilidad de fecha, horario, decoración y productos al momento de programar el evento.</p>\n\n<p>Para reservar formalmente el evento, será necesario programarlo y cumplir con el anticipo mínimo requerido según las políticas vigentes de {{negocio_nombre}}.</p>\n\n<p>Los precios pueden variar si se agregan productos extra, cambios de decoración, modificaciones de ubicación u otros servicios adicionales.</p>\n\n<p><strong>Contacto:</strong> {{negocio_correo}} | {{negocio_telefono}}</p>\n\n<p><em>Generado el: {{fecha_generacion}}</em></p>	["condiciones_cotizacion", "mensaje_adicional"]	t	2026-05-17 00:45:02
\.


--
-- TOC entry 5427 (class 0 OID 106763)
-- Dependencies: 258
-- Data for Name: producto; Type: TABLE DATA; Schema: public
--

COPY public.producto (id_producto, produ_nombre, produ_descripcion, produ_cantidad_total, produ_precio_unitario, produ_stock_minimo, produ_estado, produ_creacion) FROM stdin;
1	Globos de látex	Globos de colores variados	500	0.15	50	ACTIVO	2026-05-17 17:29:15.930756
2	Globos metálicos	Globos metálicos dorados y plateados	200	0.50	30	ACTIVO	2026-05-17 17:29:15.930756
3	Velas decorativas	Velas blancas para centros de mesa	300	0.75	40	ACTIVO	2026-05-17 17:29:15.930756
4	Telas organza	Telas para drapeados, por metro	100	2.50	20	ACTIVO	2026-05-17 17:29:15.930756
5	Flores artificiales	Rosas artificiales de seda	400	1.20	50	ACTIVO	2026-05-17 17:29:15.930756
6	Luces LED	Cadenas de luces LED cálidas 5m	50	8.00	10	ACTIVO	2026-05-17 17:29:15.930756
7	Centros de mesa base	Bases metálicas para centros de mesa	30	12.00	5	ACTIVO	2026-05-17 17:29:15.930756
8	Globos gigantes	Globos de 90cm para sesión de fotos	80	2.00	10	ACTIVO	2026-05-17 17:29:15.930756
\.


--
-- TOC entry 5429 (class 0 OID 106783)
-- Dependencies: 260
-- Data for Name: rol; Type: TABLE DATA; Schema: public
--

COPY public.rol (id_rol, rol_nombre, rol_descripcion) FROM stdin;
1	ADMIN	Administrador del sistema. Acceso completo.
2	EMPLEADO	Empleado del negocio. Acceso a sus actividades asignadas.
\.


--
-- TOC entry 5431 (class 0 OID 106789)
-- Dependencies: 262
-- Data for Name: tipo_evento; Type: TABLE DATA; Schema: public
--

COPY public.tipo_evento (id_tipo_evento, tipo_nombre, tipo_descripcion, tipo_activo) FROM stdin;
1	Boda	Ceremonia y/o recepción de matrimonio	t
2	Quinceañera	Celebración de quince años	t
3	Cumpleaños	Fiesta de cumpleaños	t
4	Corporativo	Evento empresarial o institucional	t
5	Baby Shower	Celebración de bienvenida de bebé	t
6	Graduación	Celebración de graduación	t
7	Otro	Tipo de evento no categorizado	t
\.


--
-- TOC entry 5433 (class 0 OID 106797)
-- Dependencies: 264
-- Data for Name: tipo_notificacion; Type: TABLE DATA; Schema: public
--

COPY public.tipo_notificacion (id_tipo_notificacion, nombre_tipo, descripcion) FROM stdin;
1	RECORDATORIO_EVENTO	Recordatorio automático antes de la fecha del evento
2	ALERTA_STOCK	Producto con stock disponible en o bajo el mínimo
3	CONFIRMACION_RESERVA	Confirmación de reserva generada para el cliente
4	INICIO_MONTAJE	Alerta de inicio de actividad del cronograma
5	CANCELACION_EVENTO	Aviso de cancelación pendiente o definitiva de un evento
6	PAGO_RECIBIDO	Confirmación de registro de un pago
7	COTIZACION_VENCIDA	Una cotización superó su fecha de expiración sin avanzar
8	SALDO_PENDIENTE	Alerta de saldo pendiente antes del evento
\.


--
-- TOC entry 5435 (class 0 OID 106803)
-- Dependencies: 266
-- Data for Name: usuario; Type: TABLE DATA; Schema: public
--


--
-- TOC entry 5540 (class 0 OID 0)
-- Dependencies: 220
-- Name: auditoria_id_auditoria_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.auditoria_id_auditoria_seq', 1, false);


--
-- TOC entry 5541 (class 0 OID 0)
-- Dependencies: 222
-- Name: cliente_id_cliente_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.cliente_id_cliente_seq', 1, false);


--
-- TOC entry 5542 (class 0 OID 0)
-- Dependencies: 224
-- Name: concepto_pago_id_concepto_pago_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.concepto_pago_id_concepto_pago_seq', 4, true);


--
-- TOC entry 5543 (class 0 OID 0)
-- Dependencies: 227
-- Name: cronograma_id_cronograma_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.cronograma_id_cronograma_seq', 1, false);


--
-- TOC entry 5544 (class 0 OID 0)
-- Dependencies: 230
-- Name: decoracion_id_decoracion_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.decoracion_id_decoracion_seq', 3, true);


--
-- TOC entry 5545 (class 0 OID 0)
-- Dependencies: 234
-- Name: documento_id_documento_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.documento_id_documento_seq', 1, false);


--
-- TOC entry 5546 (class 0 OID 0)
-- Dependencies: 236
-- Name: estado_evento_id_estadoevento_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.estado_evento_id_estadoevento_seq', 8, true);


--
-- TOC entry 5547 (class 0 OID 0)
-- Dependencies: 238
-- Name: estado_notificacion_id_estado_notificacion_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.estado_notificacion_id_estado_notificacion_seq', 4, true);


--
-- TOC entry 5548 (class 0 OID 0)
-- Dependencies: 240
-- Name: estado_pago_id_estado_pago_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.estado_pago_id_estado_pago_seq', 4, true);


--
-- TOC entry 5549 (class 0 OID 0)
-- Dependencies: 242
-- Name: estado_reserva_id_estado_reserva_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.estado_reserva_id_estado_reserva_seq', 3, true);


--
-- TOC entry 5550 (class 0 OID 0)
-- Dependencies: 244
-- Name: evento_id_evento_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.evento_id_evento_seq', 1, false);


--
-- TOC entry 5551 (class 0 OID 0)
-- Dependencies: 247
-- Name: gasto_evento_id_gasto_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.gasto_evento_id_gasto_seq', 1, false);


--
-- TOC entry 5552 (class 0 OID 0)
-- Dependencies: 249
-- Name: metodo_pago_id_metodo_pago_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.metodo_pago_id_metodo_pago_seq', 3, true);


--
-- TOC entry 5553 (class 0 OID 0)
-- Dependencies: 251
-- Name: movimiento_inventario_id_movimiento_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.movimiento_inventario_id_movimiento_seq', 1, false);


--
-- TOC entry 5554 (class 0 OID 0)
-- Dependencies: 253
-- Name: notificacion_id_notificacion_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.notificacion_id_notificacion_seq', 1, false);


--
-- TOC entry 5555 (class 0 OID 0)
-- Dependencies: 255
-- Name: pago_id_pago_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.pago_id_pago_seq', 1, false);


--
-- TOC entry 5556 (class 0 OID 0)
-- Dependencies: 257
-- Name: plantilla_documento_id_plantilla_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.plantilla_documento_id_plantilla_seq', 3, true);


--
-- TOC entry 5557 (class 0 OID 0)
-- Dependencies: 259
-- Name: producto_id_producto_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.producto_id_producto_seq', 8, true);


--
-- TOC entry 5558 (class 0 OID 0)
-- Dependencies: 261
-- Name: rol_id_rol_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.rol_id_rol_seq', 2, true);


--
-- TOC entry 5559 (class 0 OID 0)
-- Dependencies: 263
-- Name: tipo_evento_id_tipo_evento_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.tipo_evento_id_tipo_evento_seq', 7, true);


--
-- TOC entry 5560 (class 0 OID 0)
-- Dependencies: 265
-- Name: tipo_notificacion_id_tipo_notificacion_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.tipo_notificacion_id_tipo_notificacion_seq', 8, true);


--
-- TOC entry 5561 (class 0 OID 0)
-- Dependencies: 267
-- Name: usuario_id_usuario_seq; Type: SEQUENCE SET; Schema: public
--

SELECT pg_catalog.setval('public.usuario_id_usuario_seq', 1, true);


--
-- TOC entry 5089 (class 2606 OID 106867)
-- Name: auditoria auditoria_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.auditoria
    ADD CONSTRAINT auditoria_pkey PRIMARY KEY (id_auditoria);


--
-- TOC entry 5096 (class 2606 OID 106869)
-- Name: cliente cliente_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.cliente
    ADD CONSTRAINT cliente_pkey PRIMARY KEY (id_cliente);


--
-- TOC entry 5100 (class 2606 OID 106871)
-- Name: concepto_pago concepto_pago_concepto_nombre_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.concepto_pago
    ADD CONSTRAINT concepto_pago_concepto_nombre_key UNIQUE (concepto_nombre);


--
-- TOC entry 5102 (class 2606 OID 106873)
-- Name: concepto_pago concepto_pago_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.concepto_pago
    ADD CONSTRAINT concepto_pago_pkey PRIMARY KEY (id_concepto_pago);


--
-- TOC entry 5104 (class 2606 OID 106875)
-- Name: configuracion configuracion_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.configuracion
    ADD CONSTRAINT configuracion_pkey PRIMARY KEY (config_clave);


--
-- TOC entry 5106 (class 2606 OID 106877)
-- Name: cronograma cronograma_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.cronograma
    ADD CONSTRAINT cronograma_pkey PRIMARY KEY (id_cronograma);


--
-- TOC entry 5109 (class 2606 OID 106879)
-- Name: cronograma_usuario cronograma_usuario_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.cronograma_usuario
    ADD CONSTRAINT cronograma_usuario_pkey PRIMARY KEY (id_cronograma, id_usuario);


--
-- TOC entry 5112 (class 2606 OID 106881)
-- Name: decoracion decoracion_deco_nombre_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.decoracion
    ADD CONSTRAINT decoracion_deco_nombre_key UNIQUE (deco_nombre);


--
-- TOC entry 5114 (class 2606 OID 106883)
-- Name: decoracion decoracion_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.decoracion
    ADD CONSTRAINT decoracion_pkey PRIMARY KEY (id_decoracion);


--
-- TOC entry 5120 (class 2606 OID 106885)
-- Name: decoracionxproducto decoracionxproducto_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.decoracionxproducto
    ADD CONSTRAINT decoracionxproducto_pkey PRIMARY KEY (id_decoracion, id_producto);


--
-- TOC entry 5123 (class 2606 OID 106887)
-- Name: documento documento_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.documento
    ADD CONSTRAINT documento_pkey PRIMARY KEY (id_documento);


--
-- TOC entry 5126 (class 2606 OID 106889)
-- Name: estado_evento estado_evento_estado_nombre_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.estado_evento
    ADD CONSTRAINT estado_evento_estado_nombre_key UNIQUE (estado_nombre);


--
-- TOC entry 5128 (class 2606 OID 106891)
-- Name: estado_evento estado_evento_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.estado_evento
    ADD CONSTRAINT estado_evento_pkey PRIMARY KEY (id_estadoevento);


--
-- TOC entry 5130 (class 2606 OID 106893)
-- Name: estado_notificacion estado_notificacion_nombre_estado_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.estado_notificacion
    ADD CONSTRAINT estado_notificacion_nombre_estado_key UNIQUE (nombre_estado);


--
-- TOC entry 5132 (class 2606 OID 106895)
-- Name: estado_notificacion estado_notificacion_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.estado_notificacion
    ADD CONSTRAINT estado_notificacion_pkey PRIMARY KEY (id_estado_notificacion);


--
-- TOC entry 5134 (class 2606 OID 106897)
-- Name: estado_pago estado_pago_estado_nombre_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.estado_pago
    ADD CONSTRAINT estado_pago_estado_nombre_key UNIQUE (estado_nombre);


--
-- TOC entry 5136 (class 2606 OID 106899)
-- Name: estado_pago estado_pago_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.estado_pago
    ADD CONSTRAINT estado_pago_pkey PRIMARY KEY (id_estado_pago);


--
-- TOC entry 5138 (class 2606 OID 106901)
-- Name: estado_reserva estado_reserva_estado_nombre_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.estado_reserva
    ADD CONSTRAINT estado_reserva_estado_nombre_key UNIQUE (estado_nombre);


--
-- TOC entry 5140 (class 2606 OID 106903)
-- Name: estado_reserva estado_reserva_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.estado_reserva
    ADD CONSTRAINT estado_reserva_pkey PRIMARY KEY (id_estado_reserva);


--
-- TOC entry 5142 (class 2606 OID 106905)
-- Name: evento evento_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.evento
    ADD CONSTRAINT evento_pkey PRIMARY KEY (id_evento);


--
-- TOC entry 5150 (class 2606 OID 106907)
-- Name: evento_producto evento_producto_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.evento_producto
    ADD CONSTRAINT evento_producto_pkey PRIMARY KEY (id_evento, id_producto);


--
-- TOC entry 5157 (class 2606 OID 106909)
-- Name: gasto_evento gasto_evento_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.gasto_evento
    ADD CONSTRAINT gasto_evento_pkey PRIMARY KEY (id_gasto);


--
-- TOC entry 5160 (class 2606 OID 106911)
-- Name: metodo_pago metodo_pago_metodo_nombre_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.metodo_pago
    ADD CONSTRAINT metodo_pago_metodo_nombre_key UNIQUE (metodo_nombre);


--
-- TOC entry 5162 (class 2606 OID 106913)
-- Name: metodo_pago metodo_pago_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.metodo_pago
    ADD CONSTRAINT metodo_pago_pkey PRIMARY KEY (id_metodo_pago);


--
-- TOC entry 5167 (class 2606 OID 106915)
-- Name: movimiento_inventario movimiento_inventario_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.movimiento_inventario
    ADD CONSTRAINT movimiento_inventario_pkey PRIMARY KEY (id_movimiento);


--
-- TOC entry 5173 (class 2606 OID 106917)
-- Name: notificacion notificacion_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.notificacion
    ADD CONSTRAINT notificacion_pkey PRIMARY KEY (id_notificacion);


--
-- TOC entry 5180 (class 2606 OID 106919)
-- Name: pago pago_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.pago
    ADD CONSTRAINT pago_pkey PRIMARY KEY (id_pago);


--
-- TOC entry 5118 (class 2606 OID 106921)
-- Name: decoracion_tipo_evento pk_decoracion_tipo_evento; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.decoracion_tipo_evento
    ADD CONSTRAINT pk_decoracion_tipo_evento PRIMARY KEY (id_decoracion, id_tipo_evento);


--
-- TOC entry 5182 (class 2606 OID 106923)
-- Name: plantilla_documento plantilla_documento_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.plantilla_documento
    ADD CONSTRAINT plantilla_documento_pkey PRIMARY KEY (id_plantilla);


--
-- TOC entry 5184 (class 2606 OID 106925)
-- Name: plantilla_documento plantilla_documento_tipo_documento_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.plantilla_documento
    ADD CONSTRAINT plantilla_documento_tipo_documento_key UNIQUE (tipo_documento);


--
-- TOC entry 5186 (class 2606 OID 106927)
-- Name: producto producto_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.producto
    ADD CONSTRAINT producto_pkey PRIMARY KEY (id_producto);


--
-- TOC entry 5188 (class 2606 OID 106929)
-- Name: producto producto_produ_nombre_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.producto
    ADD CONSTRAINT producto_produ_nombre_key UNIQUE (produ_nombre);


--
-- TOC entry 5190 (class 2606 OID 106931)
-- Name: rol rol_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.rol
    ADD CONSTRAINT rol_pkey PRIMARY KEY (id_rol);


--
-- TOC entry 5192 (class 2606 OID 106933)
-- Name: rol rol_rol_nombre_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.rol
    ADD CONSTRAINT rol_rol_nombre_key UNIQUE (rol_nombre);


--
-- TOC entry 5194 (class 2606 OID 106935)
-- Name: tipo_evento tipo_evento_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.tipo_evento
    ADD CONSTRAINT tipo_evento_pkey PRIMARY KEY (id_tipo_evento);


--
-- TOC entry 5196 (class 2606 OID 106937)
-- Name: tipo_evento tipo_evento_tipo_nombre_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.tipo_evento
    ADD CONSTRAINT tipo_evento_tipo_nombre_key UNIQUE (tipo_nombre);


--
-- TOC entry 5198 (class 2606 OID 106939)
-- Name: tipo_notificacion tipo_notificacion_nombre_tipo_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.tipo_notificacion
    ADD CONSTRAINT tipo_notificacion_nombre_tipo_key UNIQUE (nombre_tipo);


--
-- TOC entry 5200 (class 2606 OID 106941)
-- Name: tipo_notificacion tipo_notificacion_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.tipo_notificacion
    ADD CONSTRAINT tipo_notificacion_pkey PRIMARY KEY (id_tipo_notificacion);


--
-- TOC entry 5204 (class 2606 OID 106943)
-- Name: usuario usuario_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id_usuario);


--
-- TOC entry 5090 (class 1259 OID 106944)
-- Name: idx_auditoria_fecha; Type: INDEX; Schema: public
--

CREATE INDEX idx_auditoria_fecha ON public.auditoria USING btree (fecha DESC);


--
-- TOC entry 5091 (class 1259 OID 106945)
-- Name: idx_auditoria_tabla_fecha; Type: INDEX; Schema: public
--

CREATE INDEX idx_auditoria_tabla_fecha ON public.auditoria USING btree (tabla_afectada, fecha DESC);


--
-- TOC entry 5092 (class 1259 OID 106946)
-- Name: idx_auditoria_usuario; Type: INDEX; Schema: public
--

CREATE INDEX idx_auditoria_usuario ON public.auditoria USING btree (id_usuario);


--
-- TOC entry 5093 (class 1259 OID 106947)
-- Name: idx_auditoria_valor_anterior; Type: INDEX; Schema: public
--

CREATE INDEX idx_auditoria_valor_anterior ON public.auditoria USING gin (valor_anterior);


--
-- TOC entry 5094 (class 1259 OID 106948)
-- Name: idx_auditoria_valor_nuevo; Type: INDEX; Schema: public
--

CREATE INDEX idx_auditoria_valor_nuevo ON public.auditoria USING gin (valor_nuevo);


--
-- TOC entry 5097 (class 1259 OID 106949)
-- Name: idx_cliente_correo_unique; Type: INDEX; Schema: public
--

CREATE UNIQUE INDEX idx_cliente_correo_unique ON public.cliente USING btree (lower((cli_correo)::text)) WHERE (cli_correo IS NOT NULL);


--
-- TOC entry 5098 (class 1259 OID 106950)
-- Name: idx_cliente_telefono; Type: INDEX; Schema: public
--

CREATE INDEX idx_cliente_telefono ON public.cliente USING btree (cli_telefono) WHERE (cli_telefono IS NOT NULL);


--
-- TOC entry 5107 (class 1259 OID 106951)
-- Name: idx_cronograma_evento; Type: INDEX; Schema: public
--

CREATE INDEX idx_cronograma_evento ON public.cronograma USING btree (id_evento);


--
-- TOC entry 5110 (class 1259 OID 106952)
-- Name: idx_cu_usuario; Type: INDEX; Schema: public
--

CREATE INDEX idx_cu_usuario ON public.cronograma_usuario USING btree (id_usuario);


--
-- TOC entry 5124 (class 1259 OID 106953)
-- Name: idx_documento_evento; Type: INDEX; Schema: public
--

CREATE INDEX idx_documento_evento ON public.documento USING btree (id_evento);


--
-- TOC entry 5115 (class 1259 OID 106954)
-- Name: idx_dte_decoracion; Type: INDEX; Schema: public
--

CREATE INDEX idx_dte_decoracion ON public.decoracion_tipo_evento USING btree (id_decoracion);


--
-- TOC entry 5116 (class 1259 OID 106955)
-- Name: idx_dte_tipo_evento; Type: INDEX; Schema: public
--

CREATE INDEX idx_dte_tipo_evento ON public.decoracion_tipo_evento USING btree (id_tipo_evento);


--
-- TOC entry 5121 (class 1259 OID 106956)
-- Name: idx_dxp_producto; Type: INDEX; Schema: public
--

CREATE INDEX idx_dxp_producto ON public.decoracionxproducto USING btree (id_producto);


--
-- TOC entry 5151 (class 1259 OID 106957)
-- Name: idx_ep_estado_reserva; Type: INDEX; Schema: public
--

CREATE INDEX idx_ep_estado_reserva ON public.evento_producto USING btree (id_estado_reserva);


--
-- TOC entry 5152 (class 1259 OID 106958)
-- Name: idx_ep_evento; Type: INDEX; Schema: public
--

CREATE INDEX idx_ep_evento ON public.evento_producto USING btree (id_evento);


--
-- TOC entry 5153 (class 1259 OID 106959)
-- Name: idx_ep_evento_estado; Type: INDEX; Schema: public
--

CREATE INDEX idx_ep_evento_estado ON public.evento_producto USING btree (id_evento, id_estado_reserva);


--
-- TOC entry 5154 (class 1259 OID 106960)
-- Name: idx_ep_producto; Type: INDEX; Schema: public
--

CREATE INDEX idx_ep_producto ON public.evento_producto USING btree (id_producto);


--
-- TOC entry 5155 (class 1259 OID 106961)
-- Name: idx_ep_producto_estado; Type: INDEX; Schema: public
--

CREATE INDEX idx_ep_producto_estado ON public.evento_producto USING btree (id_producto, id_estado_reserva);


--
-- TOC entry 5143 (class 1259 OID 106962)
-- Name: idx_evento_cliente; Type: INDEX; Schema: public
--

CREATE INDEX idx_evento_cliente ON public.evento USING btree (id_cliente);


--
-- TOC entry 5144 (class 1259 OID 106963)
-- Name: idx_evento_decoracion; Type: INDEX; Schema: public
--

CREATE INDEX idx_evento_decoracion ON public.evento USING btree (id_decoracion);


--
-- TOC entry 5145 (class 1259 OID 106964)
-- Name: idx_evento_estado; Type: INDEX; Schema: public
--

CREATE INDEX idx_evento_estado ON public.evento USING btree (id_estadoevento);


--
-- TOC entry 5146 (class 1259 OID 106965)
-- Name: idx_evento_estado_fecha; Type: INDEX; Schema: public
--

CREATE INDEX idx_evento_estado_fecha ON public.evento USING btree (id_estadoevento, event_fechaevento);


--
-- TOC entry 5147 (class 1259 OID 106966)
-- Name: idx_evento_fecha; Type: INDEX; Schema: public
--

CREATE INDEX idx_evento_fecha ON public.evento USING btree (event_fechaevento);


--
-- TOC entry 5148 (class 1259 OID 106967)
-- Name: idx_evento_tipo; Type: INDEX; Schema: public
--

CREATE INDEX idx_evento_tipo ON public.evento USING btree (id_tipo_evento);


--
-- TOC entry 5158 (class 1259 OID 106968)
-- Name: idx_gasto_evento; Type: INDEX; Schema: public
--

CREATE INDEX idx_gasto_evento ON public.gasto_evento USING btree (id_evento);


--
-- TOC entry 5163 (class 1259 OID 106969)
-- Name: idx_movimiento_evento; Type: INDEX; Schema: public
--

CREATE INDEX idx_movimiento_evento ON public.movimiento_inventario USING btree (id_evento) WHERE (id_evento IS NOT NULL);


--
-- TOC entry 5164 (class 1259 OID 106970)
-- Name: idx_movimiento_fecha; Type: INDEX; Schema: public
--

CREATE INDEX idx_movimiento_fecha ON public.movimiento_inventario USING btree (fecha DESC);


--
-- TOC entry 5165 (class 1259 OID 106971)
-- Name: idx_movimiento_producto; Type: INDEX; Schema: public
--

CREATE INDEX idx_movimiento_producto ON public.movimiento_inventario USING btree (id_producto);


--
-- TOC entry 5168 (class 1259 OID 106972)
-- Name: idx_notificacion_estado; Type: INDEX; Schema: public
--

CREATE INDEX idx_notificacion_estado ON public.notificacion USING btree (id_estado_notificacion);


--
-- TOC entry 5169 (class 1259 OID 106973)
-- Name: idx_notificacion_evento; Type: INDEX; Schema: public
--

CREATE INDEX idx_notificacion_evento ON public.notificacion USING btree (id_evento) WHERE (id_evento IS NOT NULL);


--
-- TOC entry 5170 (class 1259 OID 106974)
-- Name: idx_notificacion_tipo; Type: INDEX; Schema: public
--

CREATE INDEX idx_notificacion_tipo ON public.notificacion USING btree (id_tipo_notificacion);


--
-- TOC entry 5171 (class 1259 OID 106975)
-- Name: idx_notificacion_usuario; Type: INDEX; Schema: public
--

CREATE INDEX idx_notificacion_usuario ON public.notificacion USING btree (id_usuario);


--
-- TOC entry 5174 (class 1259 OID 106976)
-- Name: idx_pago_concepto; Type: INDEX; Schema: public
--

CREATE INDEX idx_pago_concepto ON public.pago USING btree (id_concepto_pago);


--
-- TOC entry 5175 (class 1259 OID 106977)
-- Name: idx_pago_estado; Type: INDEX; Schema: public
--

CREATE INDEX idx_pago_estado ON public.pago USING btree (id_estado_pago);


--
-- TOC entry 5176 (class 1259 OID 106978)
-- Name: idx_pago_evento; Type: INDEX; Schema: public
--

CREATE INDEX idx_pago_evento ON public.pago USING btree (id_evento);


--
-- TOC entry 5177 (class 1259 OID 106979)
-- Name: idx_pago_evento_estado; Type: INDEX; Schema: public
--

CREATE INDEX idx_pago_evento_estado ON public.pago USING btree (id_evento, id_estado_pago);


--
-- TOC entry 5178 (class 1259 OID 106980)
-- Name: idx_pago_metodo; Type: INDEX; Schema: public
--

CREATE INDEX idx_pago_metodo ON public.pago USING btree (id_metodo_pago);


--
-- TOC entry 5201 (class 1259 OID 106981)
-- Name: idx_usuario_correo_lower; Type: INDEX; Schema: public
--

CREATE UNIQUE INDEX idx_usuario_correo_lower ON public.usuario USING btree (lower((usu_correo)::text));


--
-- TOC entry 5202 (class 1259 OID 106982)
-- Name: idx_usuario_rol; Type: INDEX; Schema: public
--

CREATE INDEX idx_usuario_rol ON public.usuario USING btree (id_rol);


--
-- TOC entry 5205 (class 2606 OID 106983)
-- Name: auditoria fk_auditoria_usuario; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.auditoria
    ADD CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuario(id_usuario) ON DELETE SET NULL;


--
-- TOC entry 5206 (class 2606 OID 106988)
-- Name: cronograma fk_cronograma_evento; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.cronograma
    ADD CONSTRAINT fk_cronograma_evento FOREIGN KEY (id_evento) REFERENCES public.evento(id_evento);


--
-- TOC entry 5207 (class 2606 OID 106993)
-- Name: cronograma_usuario fk_cu_cronograma; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.cronograma_usuario
    ADD CONSTRAINT fk_cu_cronograma FOREIGN KEY (id_cronograma) REFERENCES public.cronograma(id_cronograma);


--
-- TOC entry 5208 (class 2606 OID 106998)
-- Name: cronograma_usuario fk_cu_usuario; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.cronograma_usuario
    ADD CONSTRAINT fk_cu_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuario(id_usuario);


--
-- TOC entry 5213 (class 2606 OID 107003)
-- Name: documento fk_documento_evento; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.documento
    ADD CONSTRAINT fk_documento_evento FOREIGN KEY (id_evento) REFERENCES public.evento(id_evento);


--
-- TOC entry 5214 (class 2606 OID 107008)
-- Name: documento fk_documento_plantilla; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.documento
    ADD CONSTRAINT fk_documento_plantilla FOREIGN KEY (id_plantilla) REFERENCES public.plantilla_documento(id_plantilla);


--
-- TOC entry 5209 (class 2606 OID 107013)
-- Name: decoracion_tipo_evento fk_dte_decoracion; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.decoracion_tipo_evento
    ADD CONSTRAINT fk_dte_decoracion FOREIGN KEY (id_decoracion) REFERENCES public.decoracion(id_decoracion);


--
-- TOC entry 5210 (class 2606 OID 107018)
-- Name: decoracion_tipo_evento fk_dte_tipo_evento; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.decoracion_tipo_evento
    ADD CONSTRAINT fk_dte_tipo_evento FOREIGN KEY (id_tipo_evento) REFERENCES public.tipo_evento(id_tipo_evento);


--
-- TOC entry 5211 (class 2606 OID 107023)
-- Name: decoracionxproducto fk_dxp_decoracion; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.decoracionxproducto
    ADD CONSTRAINT fk_dxp_decoracion FOREIGN KEY (id_decoracion) REFERENCES public.decoracion(id_decoracion);


--
-- TOC entry 5212 (class 2606 OID 107028)
-- Name: decoracionxproducto fk_dxp_producto; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.decoracionxproducto
    ADD CONSTRAINT fk_dxp_producto FOREIGN KEY (id_producto) REFERENCES public.producto(id_producto);


--
-- TOC entry 5220 (class 2606 OID 107033)
-- Name: evento_producto fk_ep_evento; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.evento_producto
    ADD CONSTRAINT fk_ep_evento FOREIGN KEY (id_evento) REFERENCES public.evento(id_evento);


--
-- TOC entry 5221 (class 2606 OID 107038)
-- Name: evento_producto fk_ep_producto; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.evento_producto
    ADD CONSTRAINT fk_ep_producto FOREIGN KEY (id_producto) REFERENCES public.producto(id_producto);


--
-- TOC entry 5215 (class 2606 OID 107043)
-- Name: evento fk_evento_cliente; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.evento
    ADD CONSTRAINT fk_evento_cliente FOREIGN KEY (id_cliente) REFERENCES public.cliente(id_cliente);


--
-- TOC entry 5216 (class 2606 OID 107048)
-- Name: evento fk_evento_decoracion; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.evento
    ADD CONSTRAINT fk_evento_decoracion FOREIGN KEY (id_decoracion) REFERENCES public.decoracion(id_decoracion);


--
-- TOC entry 5217 (class 2606 OID 107053)
-- Name: evento fk_evento_estado; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.evento
    ADD CONSTRAINT fk_evento_estado FOREIGN KEY (id_estadoevento) REFERENCES public.estado_evento(id_estadoevento);


--
-- TOC entry 5218 (class 2606 OID 107058)
-- Name: evento fk_evento_estado_anterior; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.evento
    ADD CONSTRAINT fk_evento_estado_anterior FOREIGN KEY (id_estado_anterior) REFERENCES public.estado_evento(id_estadoevento);


--
-- TOC entry 5222 (class 2606 OID 107063)
-- Name: evento_producto fk_evento_producto_estado_reserva; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.evento_producto
    ADD CONSTRAINT fk_evento_producto_estado_reserva FOREIGN KEY (id_estado_reserva) REFERENCES public.estado_reserva(id_estado_reserva);


--
-- TOC entry 5219 (class 2606 OID 107068)
-- Name: evento fk_evento_tipo; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.evento
    ADD CONSTRAINT fk_evento_tipo FOREIGN KEY (id_tipo_evento) REFERENCES public.tipo_evento(id_tipo_evento);


--
-- TOC entry 5223 (class 2606 OID 107073)
-- Name: gasto_evento fk_gasto_evento; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.gasto_evento
    ADD CONSTRAINT fk_gasto_evento FOREIGN KEY (id_evento) REFERENCES public.evento(id_evento);


--
-- TOC entry 5224 (class 2606 OID 107078)
-- Name: movimiento_inventario fk_movimiento_evento; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.movimiento_inventario
    ADD CONSTRAINT fk_movimiento_evento FOREIGN KEY (id_evento) REFERENCES public.evento(id_evento);


--
-- TOC entry 5225 (class 2606 OID 107083)
-- Name: movimiento_inventario fk_movimiento_producto; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.movimiento_inventario
    ADD CONSTRAINT fk_movimiento_producto FOREIGN KEY (id_producto) REFERENCES public.producto(id_producto);


--
-- TOC entry 5226 (class 2606 OID 107088)
-- Name: movimiento_inventario fk_movimiento_usuario; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.movimiento_inventario
    ADD CONSTRAINT fk_movimiento_usuario FOREIGN KEY (registrado_por) REFERENCES public.usuario(id_usuario);


--
-- TOC entry 5227 (class 2606 OID 107093)
-- Name: notificacion fk_notificacion_estado; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.notificacion
    ADD CONSTRAINT fk_notificacion_estado FOREIGN KEY (id_estado_notificacion) REFERENCES public.estado_notificacion(id_estado_notificacion);


--
-- TOC entry 5228 (class 2606 OID 107098)
-- Name: notificacion fk_notificacion_evento; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.notificacion
    ADD CONSTRAINT fk_notificacion_evento FOREIGN KEY (id_evento) REFERENCES public.evento(id_evento) ON DELETE SET NULL;


--
-- TOC entry 5229 (class 2606 OID 107103)
-- Name: notificacion fk_notificacion_tipo; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.notificacion
    ADD CONSTRAINT fk_notificacion_tipo FOREIGN KEY (id_tipo_notificacion) REFERENCES public.tipo_notificacion(id_tipo_notificacion);


--
-- TOC entry 5230 (class 2606 OID 107108)
-- Name: notificacion fk_notificacion_usuario; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.notificacion
    ADD CONSTRAINT fk_notificacion_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuario(id_usuario);


--
-- TOC entry 5231 (class 2606 OID 107113)
-- Name: pago fk_pago_concepto; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.pago
    ADD CONSTRAINT fk_pago_concepto FOREIGN KEY (id_concepto_pago) REFERENCES public.concepto_pago(id_concepto_pago);


--
-- TOC entry 5232 (class 2606 OID 107118)
-- Name: pago fk_pago_estado; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.pago
    ADD CONSTRAINT fk_pago_estado FOREIGN KEY (id_estado_pago) REFERENCES public.estado_pago(id_estado_pago);


--
-- TOC entry 5233 (class 2606 OID 107123)
-- Name: pago fk_pago_evento; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.pago
    ADD CONSTRAINT fk_pago_evento FOREIGN KEY (id_evento) REFERENCES public.evento(id_evento);


--
-- TOC entry 5234 (class 2606 OID 107128)
-- Name: pago fk_pago_metodo; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.pago
    ADD CONSTRAINT fk_pago_metodo FOREIGN KEY (id_metodo_pago) REFERENCES public.metodo_pago(id_metodo_pago);


--
-- TOC entry 5235 (class 2606 OID 107133)
-- Name: usuario fk_usuario_rol; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES public.rol(id_rol);


--
--

COMMIT;
