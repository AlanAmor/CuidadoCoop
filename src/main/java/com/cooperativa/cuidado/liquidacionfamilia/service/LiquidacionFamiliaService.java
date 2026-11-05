package com.cooperativa.cuidado.liquidacionfamilia.service;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.liquidacionfamilia.LineaCuidadorFamilia;
import com.cooperativa.cuidado.liquidacionfamilia.LiquidacionFamilia;
import com.cooperativa.cuidado.liquidacionfamilia.LiquidacionFamiliaRepository;
import com.cooperativa.cuidado.liquidacionfamilia.dto.EmisionLiquidacionFamiliaForm;
import com.cooperativa.cuidado.liquidacionfamilia.dto.PrevisualizacionFamiliaDto;
import com.cooperativa.cuidado.socio.Socio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
public class LiquidacionFamiliaService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final String[] MESES_ES = {
            "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
            "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"
    };

    private final LiquidacionFamiliaRepository liquidacionRepository;
    private final ClienteRepository clienteRepository;
    private final AsistenciaRepository asistenciaRepository;

    public LiquidacionFamiliaService(
            LiquidacionFamiliaRepository liquidacionRepository,
            ClienteRepository clienteRepository,
            AsistenciaRepository asistenciaRepository) {
        this.liquidacionRepository = liquidacionRepository;
        this.clienteRepository = clienteRepository;
        this.asistenciaRepository = asistenciaRepository;
    }

    @Transactional(readOnly = true)
    public List<LiquidacionFamilia> listarEmitidas() {
        return liquidacionRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public LiquidacionFamilia obtener(Long id) {
        return liquidacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la liquidación familiar solicitada."));
    }

    @Transactional(readOnly = true)
    public PrevisualizacionFamiliaDto previsualizar(Long clienteId, Integer mes, Integer anio, String periodo) {
        if (clienteId == null) {
            throw new DatosInvalidosException("Seleccioná un paciente.");
        }
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el paciente seleccionado."));

        YearMonth ym = resolverPeriodo(mes, anio, periodo);
        List<LineaCuidadorFamilia> lineas = calcularLineasDesdeTurnos(clienteId, ym);
        List<PrevisualizacionFamiliaDto.LineaCuidadorDto> lineasDto = lineas.stream()
                .map(l -> new PrevisualizacionFamiliaDto.LineaCuidadorDto(
                        l.getNombreCuidador(),
                        l.getHoras(),
                        l.getValorHoraCliente(),
                        l.getTotalLinea()))
                .toList();

        BigDecimal subtotal = lineas.stream()
                .map(LineaCuidadorFamilia::getTotalLinea)
                .reduce(CERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        String etiquetaPeriodo = "PERIODO " + MESES_ES[ym.getMonthValue() - 1] + " " + ym.getYear();
        return new PrevisualizacionFamiliaDto(
                cliente.getId(),
                textoLimpio(cliente.getNombrePaciente(), ""),
                textoLimpio(cliente.getDniPaciente(), ""),
                textoLimpio(cliente.getDomicilio(), ""),
                textoLimpio(cliente.getCiudad(), ""),
                textoLimpio(cliente.getLocalidad(), ""),
                textoLimpio(cliente.getCodigoPostal(), ""),
                textoLimpio(cliente.getTelefono(), ""),
                ym.toString(),
                etiquetaPeriodo,
                lineasDto,
                subtotal);
    }

    @Transactional
    public LiquidacionFamilia emitir(EmisionLiquidacionFamiliaForm form) {
        if (form == null || form.getClienteId() == null) {
            throw new DatosInvalidosException("Seleccioná un paciente para generar la Liquidación Familia.");
        }
        Cliente cliente = clienteRepository.findById(form.getClienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el paciente seleccionado."));

        YearMonth ym = resolverPeriodo(form.getMes(), form.getAnio(), form.getPeriodo());

        List<LineaCuidadorFamilia> lineas = construirLineasFormulario(form);
        if (lineas.isEmpty()) {
            lineas = calcularLineasDesdeTurnos(cliente.getId(), ym);
        }
        if (lineas.isEmpty()) {
            lineas = List.of(new LineaCuidadorFamilia("-", CERO, CERO, CERO));
        }

        BigDecimal subtotal = lineas.stream()
                .map(LineaCuidadorFamilia::getTotalLinea)
                .reduce(CERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal porcentajeIva = noNegativo(form.getPorcentajeIva());
        BigDecimal importeIva = subtotal.multiply(porcentajeIva)
                .divide(CIEN, 2, RoundingMode.HALF_UP);
        BigDecimal importeImpuestos = noNegativo(form.getImporteImpuestos());

        BigDecimal totalMensual = subtotal
                .add(importeIva)
                .add(importeImpuestos)
                .setScale(2, RoundingMode.HALF_UP);

        LiquidacionFamilia liq = new LiquidacionFamilia();
        liq.setCliente(cliente);
        liq.setNombrePaciente(preferir(form.getNombrePaciente(), cliente.getNombrePaciente(), "-"));
        liq.setDniPaciente(preferir(form.getDniPaciente(), cliente.getDniPaciente(), ""));
        liq.setDireccion(preferir(form.getDireccion(), cliente.getDomicilio(), "-"));
        liq.setCiudad(preferir(form.getCiudad(), cliente.getCiudad(), ""));
        liq.setLocalidad(preferir(form.getLocalidad(), cliente.getLocalidad(), ""));
        liq.setCodigoPostal(preferir(form.getCodigoPostal(), cliente.getCodigoPostal(), ""));
        liq.setTelefonoContacto(preferir(form.getTelefonoContacto(), cliente.getTelefono(), ""));
        liq.setPeriodo(ym.toString());
        liq.setFechaEmision(LocalDate.now());
        liq.setLineas(new ArrayList<>(lineas));
        liq.setSubtotal(subtotal);
        liq.setPorcentajeIva(porcentajeIva);
        liq.setImporteIva(importeIva);
        liq.setImporteImpuestos(importeImpuestos);
        liq.setTotalMensual(totalMensual);
        liq.setSaldoATransferir(totalMensual);
        liq.setVariacionIpcSalud(conSignoPermitido(form.getVariacionIpcSalud()));
        liq.setTransportePublico(conSignoPermitido(form.getTransportePublico()));
        liq.setAumentoParitaria(conSignoPermitido(form.getAumentoParitaria()));

        return liquidacionRepository.saveAndFlush(liq);
    }

    private List<LineaCuidadorFamilia> calcularLineasDesdeTurnos(Long clienteId, YearMonth ym) {
        List<Asistencia> asistencias = asistenciaRepository
                .findAllByClienteIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(
                        clienteId, ym.atDay(1), ym.atEndOfMonth());
        if (asistencias.isEmpty()) {
            return List.of();
        }

        Map<String, Set<Long>> sociosPorNombrePila = new HashMap<>();
        for (Asistencia a : asistencias) {
            Socio s = a.getSocio();
            if (s != null && s.getNombre() != null) {
                String claveNombre = s.getNombre().trim().toLowerCase(Locale.ROOT);
                sociosPorNombrePila.computeIfAbsent(claveNombre, ignored -> new HashSet<>()).add(s.getId());
            }
        }

        Map<String, AcumuladorCuidador> agrupado = new LinkedHashMap<>();
        for (Asistencia a : asistencias) {
            Socio s = a.getSocio();
            BigDecimal horas = a.getHorasTotales() != null ? a.getHorasTotales() : BigDecimal.ZERO;
            BigDecimal valorHoraCliente = (a.getValorHoraAplicado() != null ? a.getValorHoraAplicado() : BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);

            String nombrePila = s != null && s.getNombre() != null ? s.getNombre().trim() : "Cuidador";
            boolean nombreRepetido = sociosPorNombrePila
                    .getOrDefault(nombrePila.toLowerCase(Locale.ROOT), Set.of()).size() > 1;
            String etiquetaCuidador = nombreRepetido && s != null && s.getApellido() != null
                    ? nombrePila + " " + s.getApellido().trim()
                    : nombrePila;

            Long socioId = s != null ? s.getId() : 0L;
            String clave = socioId + "|" + valorHoraCliente.toPlainString();
            agrupado.computeIfAbsent(clave, ignored -> new AcumuladorCuidador(etiquetaCuidador, valorHoraCliente))
                    .sumarHoras(horas);
        }

        return agrupado.values().stream()
                .map(AcumuladorCuidador::construir)
                .toList();
    }

    private List<LineaCuidadorFamilia> construirLineasFormulario(EmisionLiquidacionFamiliaForm form) {
        List<String> nombres = form.getCuidadorNombre();
        List<BigDecimal> horasList = form.getCuidadorHoras();
        List<BigDecimal> valoresList = form.getCuidadorValorHora();
        int max = Math.max(nombres.size(), Math.max(horasList.size(), valoresList.size()));
        List<LineaCuidadorFamilia> resultado = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            String nom = i < nombres.size() ? nombres.get(i) : null;
            BigDecimal h = i < horasList.size() ? horasList.get(i) : null;
            BigDecimal v = i < valoresList.size() ? valoresList.get(i) : null;
            if ((nom == null || nom.isBlank()) && (h == null || h.compareTo(BigDecimal.ZERO) <= 0)) {
                continue;
            }
            BigDecimal horas = noNegativo(h);
            BigDecimal valor = noNegativo(v);
            BigDecimal total = horas.multiply(valor).setScale(2, RoundingMode.HALF_UP);
            resultado.add(new LineaCuidadorFamilia(
                    textoLimpio(nom, "-"),
                    horas,
                    valor,
                    total));
        }
        return resultado;
    }

    public YearMonth resolverPeriodo(Integer mes, Integer anio, String periodo) {
        if (mes != null && anio != null) {
            if (mes < 1 || mes > 12 || anio < 2000 || anio > 2100) {
                throw new DatosInvalidosException("Seleccioná un mes y año válidos.");
            }
            return YearMonth.of(anio, mes);
        }
        if (periodo != null && !periodo.isBlank()) {
            try {
                return YearMonth.parse(periodo.trim());
            } catch (DateTimeParseException ex) {
                throw new DatosInvalidosException("El período debe tener formato AAAA-MM.");
            }
        }
        return YearMonth.now();
    }

    private static String textoLimpio(String val, String defecto) {
        return val != null && !val.isBlank() ? val.trim() : defecto;
    }

    private static String preferir(String primario, String respaldo, String defecto) {
        if (primario != null && !primario.isBlank()) {
            return primario.trim();
        }
        if (respaldo != null && !respaldo.isBlank()) {
            return respaldo.trim();
        }
        return defecto;
    }

    private static BigDecimal noNegativo(BigDecimal val) {
        if (val == null || val.compareTo(BigDecimal.ZERO) < 0) {
            return CERO;
        }
        return val.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal conSignoPermitido(BigDecimal val) {
        if (val == null) {
            return CERO;
        }
        return val.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class AcumuladorCuidador {
        private final String nombre;
        private final BigDecimal valorHoraCliente;
        private BigDecimal horas = CERO;

        private AcumuladorCuidador(String nombre, BigDecimal valorHoraCliente) {
            this.nombre = nombre;
            this.valorHoraCliente = valorHoraCliente;
        }

        private void sumarHoras(BigDecimal h) {
            this.horas = this.horas.add(h);
        }

        private LineaCuidadorFamilia construir() {
            BigDecimal hNorm = horas.setScale(2, RoundingMode.HALF_UP);
            BigDecimal tot = hNorm.multiply(valorHoraCliente).setScale(2, RoundingMode.HALF_UP);
            return new LineaCuidadorFamilia(nombre, hNorm, valorHoraCliente, tot);
        }
    }
}
