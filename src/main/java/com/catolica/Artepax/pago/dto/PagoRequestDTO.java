package com.catolica.Artepax.pago.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request DTO for registering a new payment.
 * <p>
 * Requires the target event ID, the payment amount (must be positive),
 * the payment method ID, and the payment concept ID. The amount must not
 * exceed the event's remaining balance when combined with prior payments.
 */
public class PagoRequestDTO {
    @NotNull(message = "El evento es obligatorio")
    private Long eventoId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "El método de pago es obligatorio")
    private Long metodoPagoId;

    @NotNull(message = "El concepto de pago es obligatorio")
    private Long conceptoPagoId;

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public Long getMetodoPagoId() { return metodoPagoId; }
    public void setMetodoPagoId(Long metodoPagoId) { this.metodoPagoId = metodoPagoId; }

    public Long getConceptoPagoId() { return conceptoPagoId; }
    public void setConceptoPagoId(Long conceptoPagoId) { this.conceptoPagoId = conceptoPagoId; }
}
