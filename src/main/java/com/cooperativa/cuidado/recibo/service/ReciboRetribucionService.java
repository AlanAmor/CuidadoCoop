package com.cooperativa.cuidado.recibo.service;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.recibo.ItemRetencionRecibo;
import com.cooperativa.cuidado.recibo.ReciboRetribucion;
import com.cooperativa.cuidado.recibo.ReciboRetribucionRepository;
import com.cooperativa.cuidado.recibo.dto.EmisionReciboForm;
import com.cooperativa.cuidado.recibo.dto.PrevisualizacionReciboDto;
import com.cooperativa.cuidado.retencion.RetencionSocio;
import com.cooperativa.cuidado.retencion.TipoRetencion;
import com.cooperativa.cuidado.retencion.service.RetencionService;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ReciboRetribucionService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal UNO = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);

    private final Object secuenciaLock = new Object();

    private final ReciboRetribucionRepository reciboRepository;
    private final SocioRepository socioRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final RetencionService retencionService;

    public ReciboRetribucionService(
            ReciboRetribucionRepository reciboRepository,
            SocioRepository socioRepository,
            AsistenciaRepository asistenciaRepository,
            RetencionService retencionService) {
        this.reciboRepository = reciboRepository;
        this.socioRepository = socioRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.retencionService = retencionService;
    }

    @Transactional(readOnly = true)
    public long obtenerProximoNumero() {
        Long ultimo = reciboRepository.obtenerUltimoNumero();
        return (ultimo == null ? 0L : ultimo) + 1L;
    }

    @Transactional(readOnly = true)
    public List<ReciboRetribucion> listarEmitidos() {
        return reciboRepository.findAllByOrderByNumeroReciboDesc();
    }

    @Transactional(readOnly = true)
    public ReciboRetribucion obtener(Long id) {
        return reciboRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el recibo de retribución solicitado."));
    }

    @Transactional(readOnly = true)
    public PrevisualizacionReciboDto previsualizar(
            Long socioId,
            String mes,
            LocalDate desde,
            LocalDate hasta) {
        if (socioId == null) {
            throw new DatosInvalidosException("Seleccioná un socio.");
        }
        Socio socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio seleccionado."));

        YearMonth ym = parsearMesODefecto(mes);
        LocalDate pDesde = desde != null ? desde : ym.atDay(1);
        LocalDate pHasta = hasta != null ? hasta : ym.atEndOfMonth();
        if (pHasta.isBefore(pDesde)) {
            throw new DatosInvalidosException("La fecha de fin del período no puede ser anterior a la fecha de inicio.");
        }

        CalculoCuidados cuidados = calcularCuidados(socioId, pDesde, pHasta);
        List<RetencionSocio> retenciones = retencionService.listarPorSocioYPeriodo(socioId, ym.toString());
        List<PrevisualizacionReciboDto.RetencionPreviaDto> retencionesDto = retenciones.stream()
                .map(r -> new PrevisualizacionReciboDto.RetencionPreviaDto(
                        r.getId(),
                        r.getTipo().name(),
                        r.getTipo().getEtiquetaRecibo(),
                        r.getMotivo(),
                        r.getUnidad(),
                        r.getValorUnidad(),
                        r.getTotal()))
                .toList();

        long proximo = obtenerProximoNumero();
        return new PrevisualizacionReciboDto(
                proximo,
                String.format("N° %05d", proximo),
                socio.getId(),
                formatearNombreSocio(socio),
                textoONullFallback(socio.getCuil(), socio.getDni()),
                textoONullFallback(socio.getDireccion(), "-"),
                textoONullFallback(socio.getLocalidad(), "-"),
                textoONullFallback(socio.getCodigoPostal(), "-"),
                textoONullFallback(socio.getCbu(), "-"),
                ym.toString(),
                pDesde,
                pHasta,
                cuidados.horas(),
                cuidados.valorHora(),
                cuidados.subtotalHoras(),
                cuidados.unidadesSanatorio(),
                cuidados.valorSanatorio(),
                cuidados.subtotalSanatorio(),
                cuidados.unidadesViaticos(),
                cuidados.valorViatico(),
                cuidados.subtotalViaticos(),
                cuidados.subtotalCuidados(),
                retencionesDto);
    }

    @Transactional
    public ReciboRetribucion emitirRecibo(EmisionReciboForm form) {
        if (form == null || form.getSocioId() == null) {
            throw new DatosInvalidosException("Seleccioná un socio para generar el recibo de retribución.");
        }
        Socio socio = socioRepository.findById(form.getSocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio seleccionado."));

        YearMonth ym = parsearMesODefecto(form.getMesSeleccionado());
        LocalDate fechaEmision = form.getFechaEmision() != null ? form.getFechaEmision() : LocalDate.now();
        LocalDate periodoDesde = form.getPeriodoDesde() != null ? form.getPeriodoDesde() : ym.atDay(1);
        LocalDate periodoHasta = form.getPeriodoHasta() != null ? form.getPeriodoHasta() : ym.atEndOfMonth();
        if (periodoHasta.isBefore(periodoDesde)) {
            throw new DatosInvalidosException("El período del recibo es inválido: la fecha final es anterior a la inicial.");
        }
        LocalDate fechaPago = form.getFechaPago() != null ? form.getFechaPago() : fechaEmision;

        CalculoCuidados cuidados = calcularCuidados(socio.getId(), periodoDesde, periodoHasta);

        synchronized (secuenciaLock) {
            long siguienteNumero = obtenerProximoNumero();

            ReciboRetribucion recibo = new ReciboRetribucion();
            recibo.setNumeroRecibo(siguienteNumero);
            recibo.setSocio(socio);
            recibo.setNombreApellidoSocio(formatearNombreSocio(socio));
            recibo.setCuilSocio(textoONullFallback(socio.getCuil(), socio.getDni()));
            recibo.setDomicilioSocio(textoONullFallback(socio.getDireccion(), "-"));
            recibo.setLocalidadSocio(textoONullFallback(socio.getLocalidad(), "-"));
            recibo.setCpSocio(textoONullFallback(socio.getCodigoPostal(), "-"));
            recibo.setCbuSocio(textoONullFallback(socio.getCbu(), "-"));

            recibo.setFechaEmision(fechaEmision);
            recibo.setMesSeleccionado(ym.toString());
            recibo.setPeriodoDesde(periodoDesde);
            recibo.setPeriodoHasta(periodoHasta);
            recibo.setFechaPago(fechaPago);
            recibo.setComprobanteTransferencia(textoONullFallback(form.getComprobanteTransferencia(), "-"));

            recibo.setHorasCuidados(cuidados.horas());
            recibo.setValorHoraCuidados(cuidados.valorHora());
            recibo.setSubtotalHoras(cuidados.subtotalHoras());
            recibo.setUnidadesSanatorio(cuidados.unidadesSanatorio());
            recibo.setValorSanatorio(cuidados.valorSanatorio());
            recibo.setSubtotalSanatorio(cuidados.subtotalSanatorio());
            recibo.setUnidadesViaticos(cuidados.unidadesViaticos());
            recibo.setValorViatico(cuidados.valorViatico());
            recibo.setSubtotalViaticos(cuidados.subtotalViaticos());
            recibo.setSubtotalCuidados(cuidados.subtotalCuidados());

            if (form.isIncluirOtrosConceptos()) {
                recibo.setOtrosConcepto(textoONullFallback(form.getOtrosConcepto(), "-"));
                recibo.setOtrosDetalle(textoONullFallback(form.getOtrosDetalle(), "-"));
                recibo.setTotalOtrosConceptos(noNegativo(form.getTotalOtrosConceptos()));
            } else {
                recibo.setOtrosConcepto("-");
                recibo.setOtrosDetalle("-");
                recibo.setTotalOtrosConceptos(CERO);
            }

            BigDecimal totalRetribucion = recibo.getSubtotalCuidados()
                    .add(recibo.getTotalOtrosConceptos())
                    .setScale(2, RoundingMode.HALF_UP);
            recibo.setTotalRetribucion(totalRetribucion);

            recibo.setExcedenteConcepto("Retiro de Excedente Anual");
            recibo.setExcedenteDetalle("Adelanto Excedente Anual por Asociado Cooperativo");
            if (form.isIncluirExcedenteAnual()) {
                BigDecimal unidadExc = form.getExcedenteUnidad() != null && form.getExcedenteUnidad().compareTo(BigDecimal.ZERO) > 0
                        ? form.getExcedenteUnidad().setScale(2, RoundingMode.HALF_UP)
                        : UNO;
                BigDecimal valorExc = noNegativo(form.getExcedenteValorUnidad());
                BigDecimal totalExc = form.getTotalExcedente() != null && form.getTotalExcedente().compareTo(BigDecimal.ZERO) > 0
                        ? form.getTotalExcedente().setScale(2, RoundingMode.HALF_UP)
                        : unidadExc.multiply(valorExc).setScale(2, RoundingMode.HALF_UP);
                if (valorExc.compareTo(BigDecimal.ZERO) == 0 && totalExc.compareTo(BigDecimal.ZERO) > 0) {
                    valorExc = totalExc.divide(unidadExc, 2, RoundingMode.HALF_UP);
                }
                recibo.setExcedenteUnidad(unidadExc);
                recibo.setExcedenteValorUnidad(valorExc);
                recibo.setTotalExcedente(totalExc);
            } else {
                recibo.setExcedenteUnidad(UNO);
                recibo.setExcedenteValorUnidad(CERO);
                recibo.setTotalExcedente(CERO);
            }

            List<ItemRetencionRecibo> items = construirItemsRetencion(form);
            recibo.setItemsRetencion(items);

            BigDecimal totalRetenciones = items.stream()
                    .map(ItemRetencionRecibo::getTotal)
                    .reduce(CERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            recibo.setTotalRetenciones(totalRetenciones);

            BigDecimal retribucionNeta = totalRetribucion
                    .add(recibo.getTotalExcedente())
                    .subtract(totalRetenciones)
                    .setScale(2, RoundingMode.HALF_UP);
            recibo.setRetribucionNeta(retribucionNeta);

            return reciboRepository.saveAndFlush(recibo);
        }
    }

    private List<ItemRetencionRecibo> construirItemsRetencion(EmisionReciboForm form) {
        List<ItemRetencionRecibo> items = new ArrayList<>();

        // Fila fija 1: RETENCION MONOTRIBUTO
        if (form.isIncluirMonotributo()) {
            BigDecimal u = positivoODefecto(form.getMonotributoUnidad(), UNO);
            BigDecimal v = noNegativo(form.getMonotributoValorUnidad());
            BigDecimal tot = u.multiply(v).setScale(2, RoundingMode.HALF_UP);
            items.add(new ItemRetencionRecibo(
                    TipoRetencion.RETENCION_MONOTRIBUTO.getEtiquetaRecibo(),
                    textoONullFallback(form.getMonotributoDetalle(), TipoRetencion.RETENCION_MONOTRIBUTO.getDetallePorDefecto()),
                    u, v, tot));
        } else {
            items.add(new ItemRetencionRecibo(
                    TipoRetencion.RETENCION_MONOTRIBUTO.getEtiquetaRecibo(),
                    TipoRetencion.RETENCION_MONOTRIBUTO.getDetallePorDefecto(),
                    CERO, CERO, CERO));
        }

        // Fila fija 2: PRÉSTAMO COOPERATIVA
        if (form.isIncluirPrestamo()) {
            BigDecimal u = positivoODefecto(form.getPrestamoUnidad(), UNO);
            BigDecimal v = noNegativo(form.getPrestamoValorUnidad());
            BigDecimal tot = u.multiply(v).setScale(2, RoundingMode.HALF_UP);
            items.add(new ItemRetencionRecibo(
                    TipoRetencion.PRESTAMO_COOPERATIVA.getEtiquetaRecibo(),
                    textoONullFallback(form.getPrestamoDetalle(), TipoRetencion.PRESTAMO_COOPERATIVA.getDetallePorDefecto()),
                    u, v, tot));
        } else {
            items.add(new ItemRetencionRecibo(
                    TipoRetencion.PRESTAMO_COOPERATIVA.getEtiquetaRecibo(),
                    TipoRetencion.PRESTAMO_COOPERATIVA.getDetallePorDefecto(),
                    CERO, CERO, CERO));
        }

        // Filas dinámicas: OTRAS RETENCIONES: (1 o más filas; si no se modificó, queda la fila por defecto de la planilla)
        List<ItemRetencionRecibo> otras = new ArrayList<>();
        if (form.isIncluirOtrasRetenciones()) {
            List<String> detalles = form.getOtrasRetencionesDetalle();
            List<BigDecimal> unidades = form.getOtrasRetencionesUnidad();
            List<BigDecimal> valores = form.getOtrasRetencionesValorUnidad();
            int max = Math.max(detalles.size(), Math.max(unidades.size(), valores.size()));
            for (int i = 0; i < max; i++) {
                String det = i < detalles.size() ? detalles.get(i) : null;
                BigDecimal uni = i < unidades.size() ? unidades.get(i) : null;
                BigDecimal val = i < valores.size() ? valores.get(i) : null;
                if ((val == null || val.compareTo(BigDecimal.ZERO) <= 0)
                        && (det == null || det.isBlank())) {
                    continue;
                }
                BigDecimal u = positivoODefecto(uni, UNO);
                BigDecimal v = noNegativo(val);
                BigDecimal tot = u.multiply(v).setScale(2, RoundingMode.HALF_UP);
                otras.add(new ItemRetencionRecibo(
                        TipoRetencion.OTRAS_RETENCIONES.getEtiquetaRecibo(),
                        textoONullFallback(det, TipoRetencion.OTRAS_RETENCIONES.getDetallePorDefecto()),
                        u, v, tot));
            }
        }
        if (otras.isEmpty()) {
            otras.add(new ItemRetencionRecibo(
                    TipoRetencion.OTRAS_RETENCIONES.getEtiquetaRecibo(),
                    TipoRetencion.OTRAS_RETENCIONES.getDetallePorDefecto(),
                    CERO, CERO, CERO));
        }
        items.addAll(otras);
        return items;
    }

    private CalculoCuidados calcularCuidados(Long socioId, LocalDate desde, LocalDate hasta) {
        List<Asistencia> asistencias = asistenciaRepository.findAllBySocioIdAndFechaBetween(socioId, desde, hasta);
        BigDecimal horas = CERO;
        BigDecimal subtotalHoras = CERO;
        int unidadesSanatorio = 0;
        BigDecimal subtotalSanatorio = CERO;
        int unidadesViaticos = 0;
        BigDecimal subtotalViaticos = CERO;

        for (Asistencia a : asistencias) {
            BigDecimal h = a.getHorasTotales() != null ? a.getHorasTotales() : BigDecimal.ZERO;
            BigDecimal vh = a.getValorHoraPagoSocio() != null ? a.getValorHoraPagoSocio() : BigDecimal.ZERO;
            horas = horas.add(h);
            subtotalHoras = subtotalHoras.add(h.multiply(vh).setScale(2, RoundingMode.HALF_UP));

            BigDecimal san = a.getImporteSanatorio() != null ? a.getImporteSanatorio() : BigDecimal.ZERO;
            if (san.compareTo(BigDecimal.ZERO) > 0) {
                unidadesSanatorio++;
                subtotalSanatorio = subtotalSanatorio.add(san);
            }

            BigDecimal via = a.getImporteViatico() != null ? a.getImporteViatico() : BigDecimal.ZERO;
            if (via.compareTo(BigDecimal.ZERO) > 0) {
                unidadesViaticos++;
                subtotalViaticos = subtotalViaticos.add(via);
            }
        }

        horas = horas.setScale(2, RoundingMode.HALF_UP);
        subtotalHoras = subtotalHoras.setScale(2, RoundingMode.HALF_UP);
        subtotalSanatorio = subtotalSanatorio.setScale(2, RoundingMode.HALF_UP);
        subtotalViaticos = subtotalViaticos.setScale(2, RoundingMode.HALF_UP);

        BigDecimal valorHora = horas.compareTo(BigDecimal.ZERO) > 0
                ? subtotalHoras.divide(horas, 2, RoundingMode.HALF_UP)
                : CERO;
        BigDecimal valorSanatorio = unidadesSanatorio > 0
                ? subtotalSanatorio.divide(BigDecimal.valueOf(unidadesSanatorio), 2, RoundingMode.HALF_UP)
                : CERO;
        BigDecimal valorViatico = unidadesViaticos > 0
                ? subtotalViaticos.divide(BigDecimal.valueOf(unidadesViaticos), 2, RoundingMode.HALF_UP)
                : CERO;
        BigDecimal subtotalCuidados = subtotalHoras
                .add(subtotalSanatorio)
                .add(subtotalViaticos)
                .setScale(2, RoundingMode.HALF_UP);

        return new CalculoCuidados(
                horas,
                valorHora,
                subtotalHoras,
                unidadesSanatorio,
                valorSanatorio,
                subtotalSanatorio,
                unidadesViaticos,
                valorViatico,
                subtotalViaticos,
                subtotalCuidados);
    }

    private YearMonth parsearMesODefecto(String mes) {
        if (mes == null || mes.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(mes.trim());
        } catch (DateTimeParseException ex) {
            throw new DatosInvalidosException("El mes debe tener el formato AAAA-MM.");
        }
    }

    private static String formatearNombreSocio(Socio socio) {
        String completo = ((socio.getNombre() != null ? socio.getNombre().trim() : "")
                + " "
                + (socio.getApellido() != null ? socio.getApellido().trim() : "")).trim();
        return completo.toUpperCase(Locale.forLanguageTag("es-AR"));
    }

    private static String textoONullFallback(String valor, String fallback) {
        return valor != null && !valor.isBlank() ? valor.trim() : fallback;
    }

    private static BigDecimal noNegativo(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            return CERO;
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal positivoODefecto(BigDecimal valor, BigDecimal defecto) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            return defecto;
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private record CalculoCuidados(
            BigDecimal horas,
            BigDecimal valorHora,
            BigDecimal subtotalHoras,
            int unidadesSanatorio,
            BigDecimal valorSanatorio,
            BigDecimal subtotalSanatorio,
            int unidadesViaticos,
            BigDecimal valorViatico,
            BigDecimal subtotalViaticos,
            BigDecimal subtotalCuidados) {
    }
}

