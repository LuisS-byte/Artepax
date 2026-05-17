-- =============================================
-- Fix plantilla_documento templates
-- Run this against your PostgreSQL database
-- =============================================

-- 1) CONFIRMACION_RESERVA: fix spacing + remove duplicate $
UPDATE public.plantilla_documento
SET contenido_base = '<h1><strong>Confirmación de Reserva - {{negocio_nombre}}</strong></h1><h2>Confirmación de Reserva de Evento</h2><p><strong>Cliente:</strong> {{cliente_nombre}}</p><p><strong>Tipo de evento:</strong> {{tipo_evento}}</p><p><strong>Fecha:</strong> {{fecha_evento}}</p><p><strong>Hora:</strong> {{hora_inicio}} - {{hora_fin}}</p><p><strong>Lugar:</strong> {{ubicacion}}</p><p><strong>Decoración:</strong> {{decoracion}}</p><p><strong>Precio total:</strong> {{precio_total}}</p><p><strong>Anticipo pagado:</strong> {{anticipo_pagado}}</p><p><strong>Saldo pendiente:</strong> {{saldo_pendiente}}</p><p>{{condiciones_cancelacion}}</p><p>{{texto_adicional}}</p><p>Generado el: {{fecha_generacion}}</p>'
WHERE tipo_documento = 'CONFIRMACION_RESERVA';

-- 2) AVISO_SALDO_PENDIENTE: fix spacing + remove duplicate $
UPDATE public.plantilla_documento
SET contenido_base = '<h1>Aviso de Saldo Pendiente - {{negocio_nombre}}</h1><h2>Aviso de Saldo Pendiente</h2><p>Estimado/a {{cliente_nombre}},</p><p>Le informamos que su evento aún registra un saldo pendiente. A continuación se detallan los datos principales:</p><h3>Detalles del evento</h3><p><strong>Tipo de evento:</strong> {{tipo_evento}}</p><p><strong>Fecha:</strong> {{fecha_evento}}</p><p><strong>Hora:</strong> {{hora_inicio}} - {{hora_fin}}</p><p><strong>Lugar:</strong> {{ubicacion}}</p><p><strong>Decoración:</strong> {{decoracion}}</p><h3>Resumen financiero</h3><p><strong>Precio total:</strong> {{precio_total}}</p><p><strong>Total pagado:</strong> {{total_pagado}}</p><p><strong>Saldo pendiente:</strong> {{saldo_pendiente}}</p><p><strong>Fecha límite sugerida de pago:</strong> {{fecha_limite_pago}}</p><h3>Instrucciones de pago</h3><p>{{instrucciones_pago}}</p><p>{{mensaje_adicional}}</p><p>Si ya realizó el pago, puede ignorar este aviso o comunicarse con nosotros para confirmar la actualización.</p><p><strong>Contacto:</strong> {{negocio_correo}} | {{negocio_telefono}}</p><p>Generado el: {{fecha_generacion}}</p><p><br></p><p>{{condiciones_cancelacion}}</p>'
WHERE tipo_documento = 'AVISO_SALDO_PENDIENTE';

-- 3) COTIZACION_EVENTO: remove duplicate $ only (spacing already OK)
UPDATE public.plantilla_documento
SET contenido_base = '<h1>Cotización - {{negocio_nombre}}</h1><h2>Cotización de Evento</h2><p>Estimado/a <strong>{{cliente_nombre}}</strong>,</p><p>Gracias por considerar nuestros servicios. A continuación se presenta la cotización preliminar del evento solicitado.</p><h3>Datos del evento</h3><p><strong>Tipo de evento:</strong> {{tipo_evento}}</p><p><strong>Fecha:</strong> {{fecha_evento}}</p><p><strong>Hora:</strong> {{hora_inicio}} - {{hora_fin}}</p><p><strong>Lugar:</strong> {{ubicacion}}</p><p><strong>Decoración:</strong> {{decoracion}}</p><h3>Resumen económico</h3><p><strong>Precio total estimado:</strong> {{precio_total}}</p><p><strong>Anticipo requerido:</strong> {{anticipo_pagado}}</p><p><strong>Saldo pendiente estimado:</strong> {{saldo_pendiente}}</p><h3>Condiciones</h3><p>{{condiciones_cancelacion}}</p><p>{{texto_adicional}}</p><p><strong>Contacto:</strong> {{negocio_correo}} | {{negocio_telefono}}</p><p><em>Generado el: {{fecha_generacion}}</em></p>'
WHERE tipo_documento = 'COTIZACION_EVENTO';
