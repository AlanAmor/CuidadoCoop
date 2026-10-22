package com.cooperativa.cuidado.web.calendario;

import java.math.BigDecimal;

public record ServicioOpcionCalendario(
        Long servicioId,
        String tipoServicio,
        BigDecimal valorHora,
        BigDecimal valorHoraSocio) {
}
