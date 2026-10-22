package com.cooperativa.cuidado.web.asignacion;

import com.cooperativa.cuidado.exception.DatosInvalidosException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Calcula las fechas de un cuidado fijo: desde un día de inicio, durante N días/semanas/meses, en los días elegidos. */
public final class CalculoRepeticion {

    private CalculoRepeticion() {
    }

    public static List<LocalDate> fechas(
            LocalDate inicio, UnidadDuracion unidad, Integer cantidad, Collection<DayOfWeek> dias) {
        if (inicio == null) {
            throw new DatosInvalidosException("Ingresá la fecha de inicio del cuidado.");
        }
        if (unidad == null || cantidad == null || cantidad < 1) {
            throw new DatosInvalidosException("Indicá la duración del cuidado (por ejemplo, 4 semanas).");
        }
        if (cantidad > unidad.getMaximo()) {
            throw new DatosInvalidosException("La duración máxima por asignación es de un año ("
                    + unidad.getMaximo() + " " + unidad.getEtiqueta()
                    + "). Para cuidados más largos, renová la asignación al finalizar.");
        }
        if (dias == null || dias.isEmpty()) {
            throw new DatosInvalidosException("Elegí al menos un día de la semana para repetir el turno.");
        }

        LocalDate finExclusivo = switch (unidad) {
            case DIAS -> inicio.plusDays(cantidad);
            case SEMANAS -> inicio.plusWeeks(cantidad);
            case MESES -> inicio.plusMonths(cantidad);
        };
        List<LocalDate> fechas = new ArrayList<>();
        for (LocalDate dia = inicio; dia.isBefore(finExclusivo); dia = dia.plusDays(1)) {
            if (dias.contains(dia.getDayOfWeek())) {
                fechas.add(dia);
            }
        }
        if (fechas.isEmpty()) {
            throw new DatosInvalidosException(
                    "Con esa duración y esos días de la semana no hay ningún turno para crear. Ampliá la duración o elegí otros días.");
        }
        return fechas;
    }
}
