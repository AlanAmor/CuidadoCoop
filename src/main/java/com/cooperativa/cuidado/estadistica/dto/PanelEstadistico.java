package com.cooperativa.cuidado.estadistica.dto;

import java.math.BigDecimal;
import java.util.List;

/** Datos del panel de estadísticas para un año, un mes y una semana de referencia. */
public record PanelEstadistico(
        int anio,
        int mes,
        int semana,
        int totalSemanas,
        String nombreMes,
        String rangoSemana,
        Resumen resumen,
        List<PuntoSerie> horasMensuales,
        List<PuntoSerie> horasSemanales,
        List<FilaSocio> socios,
        List<Barra> topSociosMes,
        List<Barra> topSociosSemana,
        List<Barra> topClientes,
        List<Barra> servicios,
        List<Barra> diasSemana,
        List<Barra> franjasHorarias,
        List<FilaFinanzas> finanzasMensuales,
        List<String> sociosSinHorasMes) {

    /** Valor del período actual junto al del período anterior y su variación porcentual (nula si no hay base). */
    public record Comparacion(BigDecimal actual, BigDecimal anterior, BigDecimal variacion) {
    }

    public record Resumen(
            Comparacion anio,
            Comparacion mes,
            Comparacion semana,
            long turnosAnio,
            long sociosConHoras,
            long sociosActivos,
            long clientesAtendidos,
            BigDecimal promedioHorasPorSocio,
            BigDecimal promedioHorasPorTurno,
            String mejorMes,
            BigDecimal horasMejorMes,
            BigDecimal facturado,
            BigDecimal pagadoSocios,
            BigDecimal margen) {
    }

    /** Punto de un gráfico de columnas; las alturas son porcentajes (0-100) respecto del máximo de la serie. */
    public record PuntoSerie(
            String etiqueta,
            String titulo,
            BigDecimal horas,
            BigDecimal horasAnterior,
            int altura,
            int alturaAnterior,
            boolean destacado) {
    }

    public record CeldaMes(BigDecimal horas, int nivel) {
    }

    public record FilaSocio(
            Long id,
            String nombre,
            BigDecimal horasSemana,
            BigDecimal horasMes,
            BigDecimal horasAnio,
            BigDecimal participacionAnio,
            long turnosAnio,
            BigDecimal promedioPorTurno,
            List<CeldaMes> meses) {
    }

    /** Barra horizontal de un ranking; {@code ancho} es un porcentaje (0-100) respecto de la mayor barra. */
    public record Barra(
            String etiqueta,
            BigDecimal horas,
            long turnos,
            int ancho,
            BigDecimal participacion) {
    }

    public record FilaFinanzas(
            String mes,
            BigDecimal horas,
            BigDecimal facturado,
            BigDecimal pagadoSocios,
            BigDecimal margen) {
    }
}
