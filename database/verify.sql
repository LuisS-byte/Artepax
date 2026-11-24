-- Validación del esquema de demostración antes del bootstrap.
DO $verify$
BEGIN
    IF (SELECT count(*) FROM information_schema.tables
        WHERE table_schema = 'public' AND table_type = 'BASE TABLE') <> 27 THEN
        RAISE EXCEPTION 'Se esperaban 27 tablas en public';
    END IF;
    IF (SELECT count(*) FROM information_schema.views WHERE table_schema = 'public') <> 5 THEN
        RAISE EXCEPTION 'Se esperaban cinco vistas en public';
    END IF;
    IF EXISTS (SELECT FROM public.usuario) THEN
        RAISE EXCEPTION 'El seed debe comenzar sin cuentas';
    END IF;
    IF NOT EXISTS (SELECT FROM public.rol WHERE rol_nombre = 'ADMIN')
       OR NOT EXISTS (SELECT FROM public.estado_evento WHERE estado_nombre = 'PENDIENTE_COBRO')
       OR (SELECT count(*) FROM public.producto) <> 8
       OR (SELECT count(*) FROM public.decoracion) <> 3
       OR (SELECT count(*) FROM public.decoracion_tipo_evento) <> 7
       OR (SELECT count(*) FROM public.decoracionxproducto) <> 12
       OR (SELECT count(*) FROM public.plantilla_documento) <> 3
       OR (SELECT count(*) FROM public.configuracion) <> 12 THEN
        RAISE EXCEPTION 'Faltan datos base para la demo';
    END IF;
END
$verify$;

SELECT count(*) AS stock FROM public.vista_stock_producto;
SELECT count(*) AS pagos FROM public.vista_pagos_evento;
SELECT count(*) AS cotizaciones FROM public.vista_cotizaciones_expiradas;
SELECT count(*) AS eventos FROM public.vista_eventos_periodo;
SELECT count(*) AS rentabilidad FROM public.vista_rentabilidad_evento;
