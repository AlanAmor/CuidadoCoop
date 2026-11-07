package com.cooperativa.cuidado.servicio;

import java.math.BigDecimal;

public enum TramoHorarioTarifa {

    MENOS_DE_12_HS("Menos de 12 hs (< 12 hs)"),
    DOCE_HS_O_MAS("12 hs o más (>= 12 hs)");

    public static final BigDecimal LIMITE_DOCE_HORAS = new BigDecimal("12.00");

    private final String etiqueta;

    TramoHorarioTarifa(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static TramoHorarioTarifa desdeHoras(BigDecimal horasTotales) {
        if (horasTotales != null && horasTotales.compareTo(LIMITE_DOCE_HORAS) >= 0) {
            return DOCE_HS_O_MAS;
        }
        return MENOS_DE_12_HS;
    }
}

