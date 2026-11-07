package com.cooperativa.cuidado.web.calendario;

import java.math.BigDecimal;
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
        String tipoServicio,
        BigDecimal importeViatico,
        BigDecimal importeSanatorio,
        LocalDateTime marcacionEntrada,
        LocalDateTime marcacionSalida,
        String aclaracionTurno,
        boolean diaDelTurnoHoy,
        boolean feriadoNacional) {
}
