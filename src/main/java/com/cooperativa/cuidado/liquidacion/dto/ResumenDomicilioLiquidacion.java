package com.cooperativa.cuidado.liquidacion.dto;

import com.cooperativa.cuidado.servicio.TipoServicio;

import java.math.BigDecimal;

public record ResumenDomicilioLiquidacion(
        Long servicioDomicilioId,
        String domicilio,
        TipoServicio tipoServicio,
        BigDecimal horas,
        BigDecimal importe) {
}
