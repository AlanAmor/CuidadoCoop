package com.cooperativa.cuidado.documento;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Desglose de un presupuesto. El descuento por pago en efectivo se aplica sobre el
 * subtotal y el IVA se calcula sobre el importe ya descontado.
 */
public record CalculoPresupuesto(
        BigDecimal horas,
        BigDecimal valorHora,
        AlicuotaIva alicuota,
        boolean pagoEfectivo,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal neto,
        BigDecimal iva,
        BigDecimal total) {

    public static final BigDecimal PORCENTAJE_DESCUENTO_EFECTIVO = new BigDecimal("10");

    public static CalculoPresupuesto calcular(
            BigDecimal horas, BigDecimal valorHora, AlicuotaIva alicuota, boolean pagoEfectivo) {
        if (horas == null || horas.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Las horas estimadas deben ser mayores que cero");
        }
        if (horas.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Las horas estimadas admiten hasta dos decimales");
        }
        if (valorHora == null || valorHora.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor hora debe ser mayor que cero");
        }
        AlicuotaIva tasa = alicuota == null ? AlicuotaIva.SIN_IVA : alicuota;

        BigDecimal subtotal = horas.multiply(valorHora).setScale(2, RoundingMode.HALF_UP);
        BigDecimal descuento = pagoEfectivo
                ? subtotal.multiply(PORCENTAJE_DESCUENTO_EFECTIVO)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);
        BigDecimal neto = subtotal.subtract(descuento);
        BigDecimal importeIva = neto.multiply(tasa.getPorcentaje())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new CalculoPresupuesto(
                horas, valorHora, tasa, pagoEfectivo, subtotal, descuento, neto, importeIva,
                neto.add(importeIva));
    }
}
