package com.cooperativa.cuidado.web.calendario;

public record SocioDisponibleCalendario(
        Long socioId,
        String nombre,
        String especialidad,
        boolean tieneTurnoMismoDia) {

    public SocioDisponibleCalendario(Long socioId, String nombre, String especialidad) {
        this(socioId, nombre, especialidad, false);
    }
}
