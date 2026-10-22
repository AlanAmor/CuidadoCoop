package com.cooperativa.cuidado.estadistica.service;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.estadistica.EstadisticaRepository;
import com.cooperativa.cuidado.estadistica.dto.PanelEstadistico;
import com.cooperativa.cuidado.estadistica.dto.PanelEstadistico.Barra;
import com.cooperativa.cuidado.estadistica.dto.PanelEstadistico.CeldaMes;
import com.cooperativa.cuidado.estadistica.dto.PanelEstadistico.Comparacion;
import com.cooperativa.cuidado.estadistica.dto.PanelEstadistico.FilaFinanzas;
import com.cooperativa.cuidado.estadistica.dto.PanelEstadistico.FilaSocio;
import com.cooperativa.cuidado.estadistica.dto.PanelEstadistico.PuntoSerie;
import com.cooperativa.cuidado.estadistica.dto.PanelEstadistico.Resumen;
import com.cooperativa.cuidado.socio.Socio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EstadisticaService {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int LIMITE_RANKING = 10;

    private final EstadisticaRepository estadisticaRepository;

    public EstadisticaService(EstadisticaRepository estadisticaRepository) {
        this.estadisticaRepository = estadisticaRepository;
    }

    /**
     * Arma el panel completo. Las semanas van de lunes a domingo y se numeran desde la que contiene el 1 de enero.
     * Si {@code mes} o {@code semana} son nulos se eligen según la fecha de hoy (o el cierre/inicio del año).
     */
    @Transactional(readOnly = true)
    public PanelEstadistico generarPanel(int anio, Integer mesSolicitado, Integer semanaSolicitada) {
        if (anio < 1900 || anio > 9998) {
            throw new IllegalArgumentException("El año debe estar entre 1900 y 9998");
        }
        LocalDate hoy = LocalDate.now();
        int mes = mesSolicitado != null ? mesSolicitado
                : anio < hoy.getYear() ? 12 : anio > hoy.getYear() ? 1 : hoy.getMonthValue();
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }

        LocalDate inicioAnio = LocalDate.of(anio, 1, 1);
        LocalDate finAnio = LocalDate.of(anio, 12, 31);
        LocalDate primerLunes = inicioAnio.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate ultimoDomingo = finAnio.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        int totalSemanas = (int) (ChronoUnit.DAYS.between(primerLunes, ultimoDomingo) + 1) / 7;
        int semana = semanaSolicitada != null ? semanaSolicitada
                : anio < hoy.getYear() ? totalSemanas
                : anio > hoy.getYear() ? 1
                : (int) (ChronoUnit.DAYS.between(primerLunes, hoy) / 7) + 1;
        semana = Math.max(1, Math.min(semana, totalSemanas));

        LocalDate inicioSemana = primerLunes.plusWeeks(semana - 1L);
        LocalDate finSemana = inicioSemana.plusDays(6);
        YearMonth periodoMes = YearMonth.of(anio, mes);

        List<Asistencia> todas = estadisticaRepository.asistenciasEntre(inicioAnio.minusYears(1), ultimoDomingo);
        List<Asistencia> delAnio = todas.stream().filter(a -> a.getFecha().getYear() == anio).toList();
        List<Asistencia> delMes = entre(todas, periodoMes.atDay(1), periodoMes.atEndOfMonth());
        List<Asistencia> delaSemana = entre(todas, inicioSemana, finSemana);
        List<Socio> sociosActivos = estadisticaRepository.sociosActivos();

        BigDecimal horasAnio = horas(delAnio);
        List<PuntoSerie> mensual = serieMensual(todas, anio, mes);
        List<FilaFinanzas> finanzas = finanzasMensuales(delAnio);
        List<FilaSocio> filas = filasSocios(todas, delAnio, sociosActivos, anio, periodoMes, inicioSemana, finSemana);

        PuntoSerie mejor = mensual.stream()
                .max(Comparator.comparing(PuntoSerie::horas)).orElseThrow();
        boolean hayHoras = mejor.horas().signum() > 0;
        BigDecimal facturado = finanzas.stream().map(FilaFinanzas::facturado).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pagado = finanzas.stream().map(FilaFinanzas::pagadoSocios).reduce(BigDecimal.ZERO, BigDecimal::add);
        long sociosConHoras = filas.stream().filter(f -> f.horasAnio().signum() > 0).count();

        Resumen resumen = new Resumen(
                comparar(horasAnio, horas(todas.stream()
                        .filter(a -> a.getFecha().getYear() == anio - 1).toList())),
                comparar(horas(delMes), horas(entre(todas,
                        periodoMes.minusMonths(1).atDay(1), periodoMes.minusMonths(1).atEndOfMonth()))),
                comparar(horas(delaSemana), horas(entre(todas, inicioSemana.minusWeeks(1), inicioSemana.minusDays(1)))),
                delAnio.size(),
                sociosConHoras,
                sociosActivos.size(),
                delAnio.stream().map(a -> a.getCliente().getId()).distinct().count(),
                sociosConHoras == 0 ? dos(BigDecimal.ZERO) : dividir(horasAnio, BigDecimal.valueOf(sociosConHoras)),
                delAnio.isEmpty() ? dos(BigDecimal.ZERO) : dividir(horasAnio, BigDecimal.valueOf(delAnio.size())),
                hayHoras ? mejor.titulo() : "—",
                mejor.horas(),
                facturado,
                pagado,
                facturado.subtract(pagado));

        List<String> sinHoras = sociosActivos.stream()
                .filter(s -> delMes.stream().noneMatch(a -> a.getSocio().getId().equals(s.getId())))
                .map(EstadisticaService::nombre)
                .toList();

        return new PanelEstadistico(
                anio, mes, semana, totalSemanas,
                capitalizar(periodoMes.getMonth().getDisplayName(TextStyle.FULL, ES)),
                DIA_MES.format(inicioSemana) + " al " + DIA_MES.format(finSemana),
                resumen,
                mensual,
                serieSemanal(todas, primerLunes, totalSemanas, semana),
                filas,
                rankingSocios(delMes),
                rankingSocios(delaSemana),
                rankingClientes(delAnio),
                rankingServicios(delAnio),
                rankingDias(delAnio),
                rankingFranjas(delAnio),
                finanzas,
                sinHoras);
    }

    private List<PuntoSerie> serieMensual(List<Asistencia> todas, int anio, int mesDestacado) {
        List<BigDecimal> actuales = new ArrayList<>();
        List<BigDecimal> anteriores = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            actuales.add(horas(entre(todas, LocalDate.of(anio, m, 1), YearMonth.of(anio, m).atEndOfMonth())));
            anteriores.add(horas(entre(todas, LocalDate.of(anio - 1, m, 1), YearMonth.of(anio - 1, m).atEndOfMonth())));
        }
        BigDecimal maximo = actuales.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO)
                .max(anteriores.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO));
        List<PuntoSerie> serie = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String nombre = capitalizar(java.time.Month.of(m).getDisplayName(TextStyle.FULL, ES));
            serie.add(new PuntoSerie(
                    capitalizar(java.time.Month.of(m).getDisplayName(TextStyle.SHORT, ES).replace(".", "")),
                    nombre,
                    actuales.get(m - 1),
                    anteriores.get(m - 1),
                    porcentaje(actuales.get(m - 1), maximo),
                    porcentaje(anteriores.get(m - 1), maximo),
                    m == mesDestacado));
        }
        return serie;
    }

    private List<PuntoSerie> serieSemanal(List<Asistencia> todas, LocalDate primerLunes, int total, int destacada) {
        List<BigDecimal> valores = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            LocalDate inicio = primerLunes.plusWeeks(i);
            valores.add(horas(entre(todas, inicio, inicio.plusDays(6))));
        }
        BigDecimal maximo = valores.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        List<PuntoSerie> serie = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            LocalDate inicio = primerLunes.plusWeeks(i);
            serie.add(new PuntoSerie(
                    String.valueOf(i + 1),
                    "Semana " + (i + 1) + " (" + DIA_MES.format(inicio) + " al " + DIA_MES.format(inicio.plusDays(6)) + ")",
                    valores.get(i),
                    BigDecimal.ZERO,
                    porcentaje(valores.get(i), maximo),
                    0,
                    i + 1 == destacada));
        }
        return serie;
    }

    private List<FilaSocio> filasSocios(
            List<Asistencia> todas, List<Asistencia> delAnio, List<Socio> activos, int anio,
            YearMonth mes, LocalDate inicioSemana, LocalDate finSemana) {
        Map<Long, String> nombres = new LinkedHashMap<>();
        activos.forEach(s -> nombres.put(s.getId(), nombre(s)));
        delAnio.forEach(a -> nombres.putIfAbsent(a.getSocio().getId(), nombre(a.getSocio())));
        Map<Long, List<Asistencia>> porSocio = todas.stream()
                .collect(Collectors.groupingBy(a -> a.getSocio().getId()));
        BigDecimal totalAnio = horas(delAnio);

        List<Long> ids = new ArrayList<>(nombres.keySet());
        List<List<BigDecimal>> mensualesPorSocio = new ArrayList<>();
        BigDecimal maximoCelda = BigDecimal.ZERO;
        for (Long id : ids) {
            List<Asistencia> propias = porSocio.getOrDefault(id, List.of());
            List<BigDecimal> meses = new ArrayList<>();
            for (int m = 1; m <= 12; m++) {
                BigDecimal h = horas(entre(propias, LocalDate.of(anio, m, 1), YearMonth.of(anio, m).atEndOfMonth()));
                meses.add(h);
                maximoCelda = maximoCelda.max(h);
            }
            mensualesPorSocio.add(meses);
        }

        List<FilaSocio> filas = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            List<Asistencia> propias = porSocio.getOrDefault(id, List.of());
            List<Asistencia> propiasAnio = propias.stream().filter(a -> a.getFecha().getYear() == anio).toList();
            BigDecimal horasAnio = horas(propiasAnio);
            final BigDecimal maximo = maximoCelda;
            List<CeldaMes> celdas = mensualesPorSocio.get(i).stream()
                    .map(h -> new CeldaMes(h, nivel(h, maximo)))
                    .toList();
            filas.add(new FilaSocio(
                    id,
                    nombres.get(id),
                    horas(entre(propias, inicioSemana, finSemana)),
                    horas(entre(propias, mes.atDay(1), mes.atEndOfMonth())),
                    horasAnio,
                    totalAnio.signum() == 0 ? dos(BigDecimal.ZERO)
                            : dividir(horasAnio.multiply(CIEN), totalAnio),
                    propiasAnio.size(),
                    propiasAnio.isEmpty() ? dos(BigDecimal.ZERO) : dividir(horasAnio, BigDecimal.valueOf(propiasAnio.size())),
                    celdas));
        }
        filas.sort(Comparator.comparing(FilaSocio::horasAnio).reversed()
                .thenComparing(FilaSocio::nombre, String.CASE_INSENSITIVE_ORDER));
        return filas;
    }

    private List<FilaFinanzas> finanzasMensuales(List<Asistencia> delAnio) {
        List<FilaFinanzas> filas = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            final int mes = m;
            List<Asistencia> delMes = delAnio.stream().filter(a -> a.getFecha().getMonthValue() == mes).toList();
            BigDecimal facturado = delMes.stream()
                    .map(a -> multiplicar(a.getHorasTotales(), a.getValorHoraAplicado()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal pagado = delMes.stream()
                    .map(a -> multiplicar(a.getHorasTotales(), a.getValorHoraPagoSocio()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            filas.add(new FilaFinanzas(
                    capitalizar(java.time.Month.of(m).getDisplayName(TextStyle.FULL, ES)),
                    horas(delMes), dos(facturado), dos(pagado), dos(facturado.subtract(pagado))));
        }
        return filas;
    }

    private List<Barra> rankingSocios(List<Asistencia> asistencias) {
        return ranking(asistencias, a -> a.getSocio().getId(), a -> nombre(a.getSocio()), false, LIMITE_RANKING);
    }

    private List<Barra> rankingClientes(List<Asistencia> asistencias) {
        return ranking(asistencias, a -> a.getCliente().getId(), a -> a.getCliente().getNombrePaciente(), false, LIMITE_RANKING);
    }

    private List<Barra> rankingServicios(List<Asistencia> asistencias) {
        return ranking(asistencias, a -> a.getServicioDomicilio().getTipoServicio().getId(),
                a -> a.getServicioDomicilio().getTipoServicio().getEtiqueta(), true, LIMITE_RANKING);
    }

    private List<Barra> rankingDias(List<Asistencia> asistencias) {
        Map<DayOfWeek, List<Asistencia>> grupos = asistencias.stream()
                .collect(Collectors.groupingBy(a -> a.getFecha().getDayOfWeek()));
        List<Map.Entry<String, List<Asistencia>>> orden = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            orden.add(Map.entry(capitalizar(dia.getDisplayName(TextStyle.FULL, ES)), grupos.getOrDefault(dia, List.of())));
        }
        return barras(orden, false);
    }

    private List<Barra> rankingFranjas(List<Asistencia> asistencias) {
        String[] nombres = {"Madrugada (0 a 6 h)", "Mañana (6 a 12 h)", "Tarde (12 a 18 h)", "Noche (18 a 24 h)"};
        List<List<Asistencia>> grupos = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (Asistencia a : asistencias) {
            LocalTime inicio = a.getHoraInicio();
            grupos.get(inicio.getHour() / 6).add(a);
        }
        List<Map.Entry<String, List<Asistencia>>> orden = new ArrayList<>();
        for (int i = 0; i < nombres.length; i++) {
            orden.add(Map.entry(nombres[i], grupos.get(i)));
        }
        return barras(orden, false);
    }

    private <K> List<Barra> ranking(
            List<Asistencia> asistencias, Function<Asistencia, K> clave,
            Function<Asistencia, String> etiqueta, boolean porTurnos, int limite) {
        Map<K, List<Asistencia>> grupos = asistencias.stream()
                .collect(Collectors.groupingBy(clave, LinkedHashMap::new, Collectors.toList()));
        Comparator<List<Asistencia>> orden = porTurnos
                ? Comparator.comparingInt(List::size)
                : Comparator.comparing(EstadisticaService::horas);
        List<Map.Entry<String, List<Asistencia>>> ordenados = grupos.values().stream()
                .sorted(orden.reversed())
                .limit(limite)
                .map(lista -> Map.entry(etiqueta.apply(lista.get(0)), lista))
                .toList();
        return barras(ordenados, porTurnos);
    }

    private List<Barra> barras(List<Map.Entry<String, List<Asistencia>>> grupos, boolean porTurnos) {
        BigDecimal total = grupos.stream().map(g -> medida(g.getValue(), porTurnos))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maximo = grupos.stream().map(g -> medida(g.getValue(), porTurnos))
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        return grupos.stream()
                .map(g -> new Barra(
                        g.getKey(),
                        horas(g.getValue()),
                        g.getValue().size(),
                        porcentaje(medida(g.getValue(), porTurnos), maximo),
                        total.signum() == 0 ? dos(BigDecimal.ZERO)
                                : dividir(medida(g.getValue(), porTurnos).multiply(CIEN), total)))
                .toList();
    }

    private static BigDecimal medida(List<Asistencia> lista, boolean porTurnos) {
        return porTurnos ? BigDecimal.valueOf(lista.size()) : horas(lista);
    }

    private static List<Asistencia> entre(List<Asistencia> lista, LocalDate desde, LocalDate hasta) {
        return lista.stream()
                .filter(a -> !a.getFecha().isBefore(desde) && !a.getFecha().isAfter(hasta))
                .toList();
    }

    private static BigDecimal horas(List<Asistencia> lista) {
        return dos(lista.stream()
                .map(Asistencia::getHorasTotales)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal multiplicar(BigDecimal horas, BigDecimal valor) {
        return horas == null || valor == null ? BigDecimal.ZERO : horas.multiply(valor);
    }

    private static Comparacion comparar(BigDecimal actual, BigDecimal anterior) {
        BigDecimal variacion = anterior.signum() == 0 ? null
                : actual.subtract(anterior).multiply(CIEN).divide(anterior, 1, RoundingMode.HALF_UP);
        return new Comparacion(actual, anterior, variacion);
    }

    private static int porcentaje(BigDecimal valor, BigDecimal maximo) {
        if (maximo.signum() == 0 || valor.signum() == 0) {
            return 0;
        }
        return Math.max(2, valor.multiply(CIEN).divide(maximo, 0, RoundingMode.HALF_UP).intValue());
    }

    private static int nivel(BigDecimal valor, BigDecimal maximo) {
        if (maximo.signum() == 0 || valor.signum() == 0) {
            return 0;
        }
        return Math.max(1, Math.min(4, valor.multiply(BigDecimal.valueOf(4)).divide(maximo, 0, RoundingMode.CEILING).intValue()));
    }

    private static BigDecimal dividir(BigDecimal numerador, BigDecimal divisor) {
        return numerador.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static String nombre(Socio socio) {
        return socio.getNombre() + " " + socio.getApellido();
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase(ES) + texto.substring(1);
    }
}
