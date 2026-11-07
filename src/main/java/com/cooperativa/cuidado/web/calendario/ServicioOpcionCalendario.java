package com.cooperativa.cuidado.web.calendario;

import java.math.BigDecimal;

public record ServicioOpcionCalendario(
        Long servicioId,
        String tipoServicio,
        BigDecimal valorHora,
        BigDecimal valorHoraSocio,
        BigDecimal valorHoraClienteSemanaMas12,
        BigDecimal valorHoraClienteFindeMenos12,
        BigDecimal valorHoraClienteFindeMas12,
        BigDecimal valorHoraClienteFeriadoMenos12,
        BigDecimal valorHoraClienteFeriadoMas12,
        BigDecimal valorHoraSocioSemanaMas12,
        BigDecimal valorHoraSocioFindeMenos12,
        BigDecimal valorHoraSocioFindeMas12,
        BigDecimal valorHoraSocioFeriadoMenos12,
        BigDecimal valorHoraSocioFeriadoMas12) {
}
