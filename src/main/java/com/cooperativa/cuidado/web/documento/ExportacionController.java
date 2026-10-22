package com.cooperativa.cuidado.web.documento;

import com.cooperativa.cuidado.documento.AlicuotaIva;
import com.cooperativa.cuidado.documento.service.PdfExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;

@Controller
public class ExportacionController {

    private static final MediaType PDF = MediaType.APPLICATION_PDF;
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final PdfExportService pdfExportService;

    public ExportacionController(PdfExportService pdfExportService) {
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("/liquidaciones/{socioId}/{periodo}/pdf")
    public ResponseEntity<byte[]> descargarRetorno(
            @PathVariable Long socioId,
            @PathVariable String periodo) {
        YearMonth mes = YearMonth.parse(periodo);
        byte[] archivo = pdfExportService.generarComprobanteRetorno(socioId, mes);
        return descarga(archivo, PDF, "retorno-" + socioId + "-" + mes + ".pdf");
    }

    @GetMapping("/presupuestos/pdf")
    public ResponseEntity<byte[]> descargarPresupuesto(
            @RequestParam Long clienteId,
            @RequestParam Long servicioId,
            @RequestParam BigDecimal horas,
            @RequestParam(defaultValue = "SIN_IVA") AlicuotaIva alicuotaIva,
            @RequestParam(defaultValue = "false") boolean pagoEfectivo) {
        byte[] archivo = pdfExportService.generarPresupuesto(clienteId, servicioId, horas, alicuotaIva, pagoEfectivo);
        return descarga(archivo, PDF, "presupuesto-" + clienteId + "-" + servicioId + ".pdf");
    }

    @GetMapping("/asistencias/historico/pdf")
    public ResponseEntity<byte[]> descargarHistoricoPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        byte[] archivo = pdfExportService.exportarHistoricoAsistenciasPdf(desde, hasta);
        return descarga(archivo, PDF, "historico-asistencias-" + desde + "-a-" + hasta + ".pdf");
    }

    @GetMapping("/asistencias/historico/excel")
    public ResponseEntity<byte[]> descargarHistoricoExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        byte[] archivo = pdfExportService.exportarHistoricoAsistenciasExcel(desde, hasta);
        return descarga(archivo, XLSX, "historico-asistencias-" + desde + "-a-" + hasta + ".xlsx");
    }

    @GetMapping("/socios/{id}/historico/pdf")
    public ResponseEntity<byte[]> historicoSocioPdf(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descarga(pdfExportService.exportarHistoricoSocioPdf(id, desde, hasta), PDF,
                "historico-socio-" + id + ".pdf");
    }

    @GetMapping("/socios/{id}/historico/excel")
    public ResponseEntity<byte[]> historicoSocioExcel(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descarga(pdfExportService.exportarHistoricoSocioExcel(id, desde, hasta), XLSX,
                "historico-socio-" + id + ".xlsx");
    }

    @GetMapping("/clientes/{id}/historico/pdf")
    public ResponseEntity<byte[]> historicoClientePdf(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descarga(pdfExportService.exportarHistoricoClientePdf(id, desde, hasta), PDF,
                "historico-cliente-" + id + ".pdf");
    }

    @GetMapping("/clientes/{id}/historico/excel")
    public ResponseEntity<byte[]> historicoClienteExcel(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descarga(pdfExportService.exportarHistoricoClienteExcel(id, desde, hasta), XLSX,
                "historico-cliente-" + id + ".xlsx");
    }

    private ResponseEntity<byte[]> descarga(byte[] archivo, MediaType tipo, String nombre) {
        ContentDisposition disposicion = ContentDisposition.attachment()
                .filename(nombre, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .body(archivo);
    }
}
