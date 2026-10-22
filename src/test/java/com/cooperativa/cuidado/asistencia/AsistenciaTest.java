package com.cooperativa.cuidado.asistencia;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsistenciaTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void calculaTurnoQueCruzaMedianoche() {
        Asistencia asistencia = new Asistencia();
        asistencia.setHoraInicio(LocalTime.of(22, 30));
        asistencia.setHoraFin(LocalTime.of(6, 15));

        asistencia.calcularHorasTotales();

        assertEquals(new BigDecimal("7.75"), asistencia.getHorasTotales());
    }

    @Test
    void rechazaTurnoConMismaHoraDeInicioYFin() {
        Asistencia asistencia = new Asistencia();
        asistencia.setHoraInicio(LocalTime.of(8, 0));
        asistencia.setHoraFin(LocalTime.of(8, 0));

        assertTrue(validator.validate(asistencia).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("rangoHorarioValido")));
    }
}
