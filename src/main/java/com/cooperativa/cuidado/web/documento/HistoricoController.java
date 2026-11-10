package com.cooperativa.cuidado.web.documento;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.service.ClienteService;
import com.cooperativa.cuidado.documento.service.PdfExportService;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.service.SocioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class HistoricoController {

    private final SocioService socioService;
    private final ClienteService clienteService;
    private final PdfExportService pdfExportService;

    public HistoricoController(
            SocioService socioService,
            ClienteService clienteService,
            PdfExportService pdfExportService) {
        this.socioService = socioService;
        this.clienteService = clienteService;
        this.pdfExportService = pdfExportService;
    }

    @Transactional(readOnly = true)
    @GetMapping("/asistencias/historico")
    public String mostrarFormulario(
            @RequestParam(name = "modo", defaultValue = "GENERAL") String modo,
            @RequestParam(name = "socioId", required = false) Long socioId,
            @RequestParam(name = "clienteId", required = false) Long clienteId,
            @RequestParam(name = "historicoTotal", defaultValue = "false") boolean historicoTotal,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDate desdeEfectivo = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate hastaEfectivo = hasta != null ? hasta : hoy;
        if (!historicoTotal && hastaEfectivo.isBefore(desdeEfectivo)) {
            hastaEfectivo = desdeEfectivo;
        }

        String modoNormalizado = normalizarModo(modo);
        Long socioEfectivo = socioId;
        Long clienteEfectivo = clienteId;

        List<Socio> socios = socioService.listar();
        List<Cliente> clientes = clienteService.listar();

        List<Asistencia> asistencias = pdfExportService.consultarHistorico(
                socioEfectivo, clienteEfectivo, desdeEfectivo, hastaEfectivo, historicoTotal);

        BigDecimal totalHoras = BigDecimal.ZERO;
        BigDecimal totalImporteCliente = BigDecimal.ZERO;
        BigDecimal totalImporteSocio = BigDecimal.ZERO;

        for (Asistencia a : asistencias) {
            BigDecimal h = a.getHorasTotales() != null ? a.getHorasTotales() : BigDecimal.ZERO;
            BigDecimal vhCliente = a.getValorHoraAplicado() != null ? a.getValorHoraAplicado() : BigDecimal.ZERO;
            BigDecimal vhSocio = resolverValorHoraSocio(a);
            totalHoras = totalHoras.add(h);
            totalImporteCliente = totalImporteCliente.add(h.multiply(vhCliente).setScale(2, RoundingMode.HALF_UP));
            totalImporteSocio = totalImporteSocio.add(h.multiply(vhSocio).setScale(2, RoundingMode.HALF_UP));
        }

        List<ResumenRelacionHistorico> resumenSocioPorCliente = agruparRelacion(asistencias, true);
        List<ResumenRelacionHistorico> resumenClientePorSocio = agruparRelacion(asistencias, false);
        List<ResumenSocioHistorico> resumenPorSocio = agruparPorSocio(asistencias);
        List<ResumenClienteHistorico> resumenPorCliente = agruparPorCliente(asistencias);

        Socio socioSeleccionado = socioEfectivo != null
                ? socios.stream().filter(s -> s.getId().equals(socioEfectivo)).findFirst().orElse(null)
                : null;
        Cliente clienteSeleccionado = clienteEfectivo != null
                ? clientes.stream().filter(c -> c.getId().equals(clienteEfectivo)).findFirst().orElse(null)
                : null;

        model.addAttribute("modo", modoNormalizado);
        model.addAttribute("socioId", socioEfectivo);
        model.addAttribute("clienteId", clienteEfectivo);
        model.addAttribute("socioSeleccionado", socioSeleccionado);
        model.addAttribute("clienteSeleccionado", clienteSeleccionado);
        model.addAttribute("historicoTotal", historicoTotal);
        model.addAttribute("hoyConsulta", hoy);
        model.addAttribute("desde", desdeEfectivo);
        model.addAttribute("hasta", hastaEfectivo);
        model.addAttribute("socios", socios);
        model.addAttribute("clientes", clientes);
        model.addAttribute("asistencias", asistencias);
        model.addAttribute("totalTurnos", asistencias.size());
        model.addAttribute("totalHoras", totalHoras);
        model.addAttribute("totalImporteCliente", totalImporteCliente);
        model.addAttribute("totalImporteSocio", totalImporteSocio);
        model.addAttribute("resumenSocioPorCliente", resumenSocioPorCliente);
        model.addAttribute("resumenClientePorSocio", resumenClientePorSocio);
        model.addAttribute("resumenPorSocio", resumenPorSocio);
        model.addAttribute("resumenPorCliente", resumenPorCliente);
        return "historico";
    }

    private static String normalizarModo(String modo) {
        if (modo == null) {
            return "GENERAL";
        }
        return switch (modo.trim().toUpperCase()) {
            case "SOCIO", "CLIENTE", "SOCIO_POR_CLIENTE", "CLIENTE_POR_SOCIO" -> modo.trim().toUpperCase();
            default -> "GENERAL";
        };
    }

    private static BigDecimal resolverValorHoraSocio(Asistencia a) {
        if (a.getValorHoraPagoSocio() != null) {
            return a.getValorHoraPagoSocio();
        }
        if (a.getValorHoraAplicado() != null) {
            return a.getValorHoraAplicado();
        }
        return BigDecimal.ZERO;
    }

    private static List<ResumenRelacionHistorico> agruparRelacion(List<Asistencia> asistencias, boolean ordenarPorSocioPrimero) {
        Map<String, AcumuladorRelacion> mapa = new LinkedHashMap<>();
        for (Asistencia a : asistencias) {
            String clave = a.getSocio().getId() + "-" + a.getCliente().getId();
            AcumuladorRelacion acc = mapa.computeIfAbsent(clave, k -> new AcumuladorRelacion(a.getSocio(), a.getCliente()));
            acc.acumular(a);
        }
        List<ResumenRelacionHistorico> lista = new ArrayList<>();
        for (AcumuladorRelacion acc : mapa.values()) {
            lista.add(acc.toRecord());
        }
        Comparator<ResumenRelacionHistorico> cmpSocio = Comparator.comparing(
                r -> (r.socio().getApellido() + " " + r.socio().getNombre()).toLowerCase());
        Comparator<ResumenRelacionHistorico> cmpCliente = Comparator.comparing(
                r -> r.cliente().getNombrePaciente().toLowerCase());
        lista.sort(ordenarPorSocioPrimero ? cmpSocio.thenComparing(cmpCliente) : cmpCliente.thenComparing(cmpSocio));
        return lista;
    }

    private static List<ResumenSocioHistorico> agruparPorSocio(List<Asistencia> asistencias) {
        Map<Long, AcumuladorSocio> mapa = new LinkedHashMap<>();
        for (Asistencia a : asistencias) {
            AcumuladorSocio acc = mapa.computeIfAbsent(a.getSocio().getId(), id -> new AcumuladorSocio(a.getSocio()));
            acc.acumular(a);
        }
        List<ResumenSocioHistorico> lista = new ArrayList<>();
        for (AcumuladorSocio acc : mapa.values()) {
            lista.add(acc.toRecord());
        }
        lista.sort(Comparator.comparing(r -> (r.socio().getApellido() + " " + r.socio().getNombre()).toLowerCase()));
        return lista;
    }

    private static List<ResumenClienteHistorico> agruparPorCliente(List<Asistencia> asistencias) {
        Map<Long, AcumuladorCliente> mapa = new LinkedHashMap<>();
        for (Asistencia a : asistencias) {
            AcumuladorCliente acc = mapa.computeIfAbsent(a.getCliente().getId(), id -> new AcumuladorCliente(a.getCliente()));
            acc.acumular(a);
        }
        List<ResumenClienteHistorico> lista = new ArrayList<>();
        for (AcumuladorCliente acc : mapa.values()) {
            lista.add(acc.toRecord());
        }
        lista.sort(Comparator.comparing(r -> r.cliente().getNombrePaciente().toLowerCase()));
        return lista;
    }

    public record ResumenRelacionHistorico(
            Socio socio,
            Cliente cliente,
            int turnos,
            BigDecimal horas,
            BigDecimal importeCliente,
            BigDecimal importeSocio) {
    }

    public record ResumenSocioHistorico(
            Socio socio,
            int turnos,
            int cantidadClientes,
            BigDecimal horas,
            BigDecimal importeSocio,
            BigDecimal importeCliente) {
    }

    public record ResumenClienteHistorico(
            Cliente cliente,
            int turnos,
            int cantidadSocios,
            BigDecimal horas,
            BigDecimal importeCliente,
            BigDecimal importeSocio) {
    }

    private static final class AcumuladorRelacion {
        private final Socio socio;
        private final Cliente cliente;
        private int turnos;
        private BigDecimal horas = BigDecimal.ZERO;
        private BigDecimal importeCliente = BigDecimal.ZERO;
        private BigDecimal importeSocio = BigDecimal.ZERO;

        private AcumuladorRelacion(Socio socio, Cliente cliente) {
            this.socio = socio;
            this.cliente = cliente;
        }

        private void acumular(Asistencia a) {
            turnos++;
            BigDecimal h = a.getHorasTotales() != null ? a.getHorasTotales() : BigDecimal.ZERO;
            BigDecimal vCli = a.getValorHoraAplicado() != null ? a.getValorHoraAplicado() : BigDecimal.ZERO;
            BigDecimal vSoc = resolverValorHoraSocio(a);
            horas = horas.add(h);
            importeCliente = importeCliente.add(h.multiply(vCli).setScale(2, RoundingMode.HALF_UP));
            importeSocio = importeSocio.add(h.multiply(vSoc).setScale(2, RoundingMode.HALF_UP));
        }

        private ResumenRelacionHistorico toRecord() {
            return new ResumenRelacionHistorico(socio, cliente, turnos, horas, importeCliente, importeSocio);
        }
    }

    private static final class AcumuladorSocio {
        private final Socio socio;
        private int turnos;
        private final Set<Long> clientesIds = new LinkedHashSet<>();
        private BigDecimal horas = BigDecimal.ZERO;
        private BigDecimal importeSocio = BigDecimal.ZERO;
        private BigDecimal importeCliente = BigDecimal.ZERO;

        private AcumuladorSocio(Socio socio) {
            this.socio = socio;
        }

        private void acumular(Asistencia a) {
            turnos++;
            clientesIds.add(a.getCliente().getId());
            BigDecimal h = a.getHorasTotales() != null ? a.getHorasTotales() : BigDecimal.ZERO;
            BigDecimal vCli = a.getValorHoraAplicado() != null ? a.getValorHoraAplicado() : BigDecimal.ZERO;
            BigDecimal vSoc = resolverValorHoraSocio(a);
            horas = horas.add(h);
            importeCliente = importeCliente.add(h.multiply(vCli).setScale(2, RoundingMode.HALF_UP));
            importeSocio = importeSocio.add(h.multiply(vSoc).setScale(2, RoundingMode.HALF_UP));
        }

        private ResumenSocioHistorico toRecord() {
            return new ResumenSocioHistorico(socio, turnos, clientesIds.size(), horas, importeSocio, importeCliente);
        }
    }

    private static final class AcumuladorCliente {
        private final Cliente cliente;
        private int turnos;
        private final Set<Long> sociosIds = new LinkedHashSet<>();
        private BigDecimal horas = BigDecimal.ZERO;
        private BigDecimal importeCliente = BigDecimal.ZERO;
        private BigDecimal importeSocio = BigDecimal.ZERO;

        private AcumuladorCliente(Cliente cliente) {
            this.cliente = cliente;
        }

        private void acumular(Asistencia a) {
            turnos++;
            sociosIds.add(a.getSocio().getId());
            BigDecimal h = a.getHorasTotales() != null ? a.getHorasTotales() : BigDecimal.ZERO;
            BigDecimal vCli = a.getValorHoraAplicado() != null ? a.getValorHoraAplicado() : BigDecimal.ZERO;
            BigDecimal vSoc = resolverValorHoraSocio(a);
            horas = horas.add(h);
            importeCliente = importeCliente.add(h.multiply(vCli).setScale(2, RoundingMode.HALF_UP));
            importeSocio = importeSocio.add(h.multiply(vSoc).setScale(2, RoundingMode.HALF_UP));
        }

        private ResumenClienteHistorico toRecord() {
            return new ResumenClienteHistorico(cliente, turnos, sociosIds.size(), horas, importeCliente, importeSocio);
        }
    }
}
