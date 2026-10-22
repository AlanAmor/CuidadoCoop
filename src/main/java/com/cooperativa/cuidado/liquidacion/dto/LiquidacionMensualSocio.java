package com.cooperativa.cuidado.liquidacion.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record LiquidacionMensualSocio(
        Long socioId,
        String nombreSocio,
        YearMonth periodo,
        BigDecimal totalHoras,
        BigDecimal totalRetorno,
        List<ResumenDomicilioLiquidacion> domicilios) {
}
