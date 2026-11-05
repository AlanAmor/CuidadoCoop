package com.cooperativa.cuidado.web.liquidacion;

import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.liquidacion.dto.LiquidacionMensualSocio;
import com.cooperativa.cuidado.liquidacion.service.LiquidacionService;
import com.cooperativa.cuidado.recibo.ReciboRetribucion;
import com.cooperativa.cuidado.recibo.dto.EmisionReciboForm;
import com.cooperativa.cuidado.recibo.dto.PrevisualizacionReciboDto;
import com.cooperativa.cuidado.recibo.service.ReciboExportService;
import com.cooperativa.cuidado.recibo.service.ReciboRetribucionService;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Controller
public class LiquidacionController {

    private static final MediaType PDF = MediaType.APPLICATION_PDF;
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final LiquidacionService liquidacionService;
    private final SocioRepository socioRepository;
    private final ReciboRetribucionService reciboService;
    private final ReciboExportService reciboExportService;

    public LiquidacionController(
            LiquidacionService liquidacionService,
            SocioRepository socioRepository,
            ReciboRetribucionService reciboService,
            ReciboExportService reciboExportService) {
        this.liquidacionService = liquidacionService;
        this.socioRepository = socioRepository;
        this.reciboService = reciboService;
        this.reciboExportService = reciboExportService;
    }

    @GetMapping("/liquidaciones")
    public String mostrarFormulario(
            @RequestParam(required = false) Long reciboEmitidoId,
            Model model) {
        cargarModeloBase(model, YearMonth.now());
        if (reciboEmitidoId != null) {
            try {
                model.addAttribute("reciboRecienEmitido", reciboService.obtener(reciboEmitidoId));
            } catch (RecursoNoEncontradoException ignored) {
            }
        }
        return "liquidaciones";
    }

    @GetMapping("/liquidaciones/resultado")
    public String consultar(
            @RequestParam Long socioId,
            @RequestParam String periodo,
            Model model) {
        YearMonth base = YearMonth.now();
        try {
            YearMonth yearMonth = YearMonth.parse(periodo);
            base = yearMonth;
            LiquidacionMensualSocio liquidacion =
                    liquidacionService.calcularRetornoMensual(socioId, yearMonth);
            model.addAttribute("liquidacion", liquidacion);
        } catch (DateTimeParseException ex) {
            model.addAttribute("errorConsulta", "El período debe tener el formato AAAA-MM.");
        } catch (RecursoNoEncontradoException ex) {
            model.addAttribute("errorConsulta", ex.getMessage());
        }
        cargarModeloBase(model, base);
        model.addAttribute("periodoActual", periodo);
        return "liquidaciones";
    }

    @GetMapping("/api/liquidaciones/previsualizar")
    @ResponseBody
    public ResponseEntity<?> previsualizar(
            @RequestParam Long socioId,
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            PrevisualizacionReciboDto dto = reciboService.previsualizar(socioId, mes, desde, hasta);
            return ResponseEntity.ok(dto);
        } catch (RecursoNoEncontradoException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (DatosInvalidosException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/liquidaciones/recibos")
    public String emitirRecibo(
            @ModelAttribute EmisionReciboForm form,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            ReciboRetribucion emitido = reciboService.emitirRecibo(form);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Se emitió el Recibo de Retribución " + emitido.getNumeroFormateado()
                            + " para " + emitido.getNombreApellidoSocio() + ".");
            return "redirect:/liquidaciones?reciboEmitidoId=" + emitido.getId();
        } catch (DatosInvalidosException | RecursoNoEncontradoException ex) {
            model.addAttribute("errorConsulta", ex.getMessage());
            cargarModeloBase(model, YearMonth.now());
            return "liquidaciones";
        }
    }

    @GetMapping("/liquidaciones/recibos/{id}/pdf")
    public ResponseEntity<byte[]> descargarReciboPdf(@PathVariable Long id) {
        ReciboRetribucion recibo = reciboService.obtener(id);
        byte[] archivo = reciboExportService.generarPdfDesdeEntidad(recibo);
        return descarga(archivo, PDF, "recibo-retribucion-" + recibo.getNumeroCeros() + ".pdf");
    }

    @GetMapping("/liquidaciones/recibos/{id}/excel")
    public ResponseEntity<byte[]> descargarReciboExcel(@PathVariable Long id) {
        ReciboRetribucion recibo = reciboService.obtener(id);
        byte[] archivo = reciboExportService.generarExcelDesdeEntidad(recibo);
        return descarga(archivo, XLSX, "recibo-retribucion-" + recibo.getNumeroCeros() + ".xlsx");
    }

    private void cargarModeloBase(Model model, YearMonth mesRef) {
        model.addAttribute("socios", socioRepository.findAll(Sort.by("apellido", "nombre")));
        model.addAttribute("periodoActual", mesRef.toString());
        model.addAttribute("hoy", LocalDate.now());
        model.addAttribute("inicioMesActual", mesRef.atDay(1));
        model.addAttribute("finMesActual", mesRef.atEndOfMonth());
        long proximo = reciboService.obtenerProximoNumero();
        model.addAttribute("proximoNumeroFormateado", String.format("N° %05d", proximo));
        model.addAttribute("recibosEmitidos", reciboService.listarEmitidos());
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
