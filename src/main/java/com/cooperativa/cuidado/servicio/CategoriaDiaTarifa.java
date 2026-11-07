package com.cooperativa.cuidado.servicio;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum CategoriaDiaTarifa {

    DIA_SEMANA("Día de semana (Lun a Vie)"),
    FIN_DE_SEMANA("Fin de semana (Sáb y Dom)"),
    FERIADO_NACIONAL("Feriado nacional");

    private final String etiqueta;

    CategoriaDiaTarifa(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static CategoriaDiaTarifa desde(LocalDate fecha, boolean esFeriadoNacional) {
        if (esFeriadoNacional) {
            return FERIADO_NACIONAL;
        }
        if (fecha != null) {
            DayOfWeek dia = fecha.getDayOfWeek();
            if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
                return FIN_DE_SEMANA;
            }
        }
        return DIA_SEMANA;
    }
}

