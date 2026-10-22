package com.cooperativa.cuidado.web.calendario;

import java.time.LocalDate;
import java.util.List;

public record CalendarioRespuesta(
        LocalDate desde,
        LocalDate hasta,
        List<EventoCalendario> eventos,
        List<SocioDisponibleCalendario> sociosDisponibles,
        List<ClienteCalendario> clientesSinTurno) {
}
