package com.cooperativa.cuidado.asistencia.service;

import com.cooperativa.cuidado.socio.Especialidad;
import com.cooperativa.cuidado.socio.Socio;

public record SocioDisponible(
        Socio socio,
        boolean tieneTurnoMismoDia
) {
    public Long getId() {
        return socio != null ? socio.getId() : null;
    }

    public String getNombre() {
        return socio != null ? socio.getNombre() : null;
    }

    public String getApellido() {
        return socio != null ? socio.getApellido() : null;
    }

    public String getDni() {
        return socio != null ? socio.getDni() : null;
    }

    public Especialidad getEspecialidad() {
        return socio != null ? socio.getEspecialidad() : null;
    }

    public boolean isTieneTurnoMismoDia() {
        return tieneTurnoMismoDia;
    }
}
