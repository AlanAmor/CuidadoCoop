package com.cooperativa.cuidado.web.calendario;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EventoCalendario(
        Long asistenciaId,
        Long servicioId,
        Long socioId,
        Long clienteId,
        LocalDate fecha,
        LocalDateTime inicio,
        LocalDateTime fin,
        String nombreSocio,
        String dniSocio,
        String paciente,
        String domicilio,
        String tipoServicio) {
}
