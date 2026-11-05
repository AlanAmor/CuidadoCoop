package com.cooperativa.cuidado.web.liquidacionfamilia;

import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.liquidacionfamilia.LiquidacionFamilia;
import com.cooperativa.cuidado.liquidacionfamilia.dto.EmisionLiquidacionFamiliaForm;
import com.cooperativa.cuidado.liquidacionfamilia.dto.PrevisualizacionFamiliaDto;
import com.cooperativa.cuidado.liquidacionfamilia.service.LiquidacionFamiliaExportService;
import com.cooperativa.cuidado.liquidacionfamilia.service.LiquidacionFamiliaService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

@Controller
public class LiquidacionFamiliaController {

    private static final MediaType PDF = MediaType.APPLICATION_PDF;
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private static final String[] NOMBRES_MESES = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private final LiquidacionFamiliaService liquidacionService;
    private final LiquidacionFamiliaExportService exportService;
    private final ClienteRepository clienteRepository;

    public LiquidacionFamiliaController(
            LiquidacionFamiliaService liquidacionService,
            LiquidacionFamiliaExportService exportService,
            ClienteRepository clienteRepository) {
        this.liquidacionService = liquidacionService;
        this.exportService = exportService;
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/liquidaciones-familia")
    public String mostrarFormulario(
            @RequestParam(required = false) Long emitidaId,
            Model model) {
        cargarModeloBase(model);
        if (emitidaId != null) {
            try {
                model.addAttribute("recienEmitida", liquidacionService.obtener(emitidaId));
            } catch (RecursoNoEncontradoException ignored) {
            }
        }
        return "liquidaciones-familia";
    }

    @GetMapping("/api/liquidaciones-familia/previsualizar")
    @ResponseBody
    public ResponseEntity<?> previsualizar(
            @RequestParam Long clienteId,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) String periodo) {
        try {
            PrevisualizacionFamiliaDto dto = liquidacionService.previsualizar(clienteId, mes, anio, periodo);
            return ResponseEntity.ok(dto);
        } catch (RecursoNoEncontradoException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (DatosInvalidosException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/liquidaciones-familia")
    public String emitir(
            @ModelAttribute EmisionLiquidacionFamiliaForm form,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            LiquidacionFamilia emitida = liquidacionService.emitir(form);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Se generó la Liquidación Familia de " + emitida.getNombrePaciente()
                            + " (" + emitida.getPeriodoEtiqueta() + ").");
            return "redirect:/liquidaciones-familia?emitidaId=" + emitida.getId();
        } catch (DatosInvalidosException | RecursoNoEncontradoException ex) {
            model.addAttribute("errorOperacion", ex.getMessage());
            cargarModeloBase(model);
            return "liquidaciones-familia";
        }
    }

    @GetMapping("/liquidaciones-familia/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        LiquidacionFamilia liq = liquidacionService.obtener(id);
        byte[] archivo = exportService.generarPdfDesdeEntidad(liq);
        return descarga(archivo, PDF, nombreArchivo(liq, "pdf"));
    }

    @GetMapping("/liquidaciones-familia/{id}/excel")
    public ResponseEntity<byte[]> descargarExcel(@PathVariable Long id) {
        LiquidacionFamilia liq = liquidacionService.obtener(id);
        byte[] archivo = exportService.generarExcelDesdeEntidad(liq);
        return descarga(archivo, XLSX, nombreArchivo(liq, "xlsx"));
    }

    private void cargarModeloBase(Model model) {
        YearMonth ahora = YearMonth.now();
        Map<Integer, String> meses = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            meses.put(i, NOMBRES_MESES[i - 1]);
        }
        List<Integer> anios = IntStream.rangeClosed(ahora.getYear() - 3, ahora.getYear() + 3)
                .boxed()
                .toList();

        model.addAttribute("clientes", clienteRepository.findAll(Sort.by("nombrePaciente")));
        model.addAttribute("meses", meses);
        model.addAttribute("anios", anios);
        model.addAttribute("mesActual", ahora.getMonthValue());
        model.addAttribute("anioActual", ahora.getYear());
        model.addAttribute("liquidacionesEmitidas", liquidacionService.listarEmitidas());
    }

    private static String nombreArchivo(LiquidacionFamilia liq, String ext) {
        String basePaciente = liq.getNombrePaciente() != null ? liq.getNombrePaciente() : "paciente";
        String limpio = Normalizer.normalize(basePaciente, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (limpio.isEmpty()) {
            limpio = "paciente";
        }
        return "liquidacion-familia-" + limpio + "-" + liq.getPeriodo() + "." + ext;
    }

    private static ResponseEntity<byte[]> descarga(byte[] archivo, MediaType tipo, String nombre) {
        ContentDisposition disposicion = ContentDisposition.attachment()
                .filename(nombre, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .body(archivo);
    }
}

