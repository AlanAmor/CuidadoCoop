package com.cooperativa.cuidado.web.facturacion;

import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.facturacion.CondicionIva;
import com.cooperativa.cuidado.facturacion.Factura;
import com.cooperativa.cuidado.facturacion.TipoComprobante;
import com.cooperativa.cuidado.facturacion.TipoDocumentoReceptor;
import com.cooperativa.cuidado.facturacion.TipoReceptor;
import com.cooperativa.cuidado.facturacion.dto.EmisionFacturaForm;
import com.cooperativa.cuidado.facturacion.dto.ItemLineaFacturaForm;
import com.cooperativa.cuidado.facturacion.dto.ResumenTurnosPeriodoDto;
import com.cooperativa.cuidado.facturacion.service.ArcaFacturacionService;
import com.cooperativa.cuidado.facturacion.service.FacturacionExportService;
import com.cooperativa.cuidado.obrasocial.ObraSocial;
import com.cooperativa.cuidado.obrasocial.ObraSocialRepository;
import com.cooperativa.cuidado.servicio.TipoServicio;
import com.cooperativa.cuidado.servicio.TipoServicioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
public class FacturacionController {

    private final ArcaFacturacionService arcaService;
    private final FacturacionExportService exportService;
    private final ClienteRepository clienteRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final ObraSocialRepository obraSocialRepository;

    private static final List<String> OBRAS_SOCIALES_PRESET = List.of(
            "I.A.P.O.S. (Instituto Autárquico Provincial de Obra Social)",
            "PAMI - INSSJP",
            "OSDE Binario",
            "OSECAC (Empleados de Comercio)",
            "Swiss Medical",
            "Medicus",
            "Galeno Argentina",
            "OSPE (Petroleros)",
            "UPCN / Unión Personal",
            "OSUTHGRA (Gastronómicos)"
    );

    public FacturacionController(
            ArcaFacturacionService arcaService,
            FacturacionExportService exportService,
            ClienteRepository clienteRepository,
            TipoServicioRepository tipoServicioRepository,
            ObraSocialRepository obraSocialRepository) {
        this.arcaService = arcaService;
        this.exportService = exportService;
        this.clienteRepository = clienteRepository;
        this.tipoServicioRepository = tipoServicioRepository;
        this.obraSocialRepository = obraSocialRepository;
    }

    @GetMapping("/facturas")
    public String listarFacturas(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) TipoReceptor tipoReceptor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {

        List<Factura> facturas = arcaService.filtrar(buscar, tipoReceptor, desde, hasta);
        BigDecimal totalFacturado = facturas.stream()
                .map(Factura::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("facturas", facturas);
        model.addAttribute("totalFacturado", totalFacturado);
        model.addAttribute("cantidadFacturas", facturas.size());
        model.addAttribute("buscar", buscar);
        model.addAttribute("tipoReceptor", tipoReceptor);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "facturas";
    }

    @GetMapping("/facturas/nueva")
    public String nuevaFactura(
            @RequestParam(required = false) Long liquidacionFamiliaId,
            @RequestParam(required = false) Long clienteId,
            Model model) {

        EmisionFacturaForm form;
        if (liquidacionFamiliaId != null) {
            try {
                form = arcaService.prepararFormularioDesdeLiquidacionFamilia(liquidacionFamiliaId);
                model.addAttribute("origenLiquidacion", true);
            } catch (RecursoNoEncontradoException ex) {
                form = new EmisionFacturaForm();
                model.addAttribute("errorOperacion", ex.getMessage());
            }
        } else {
            EmisionFacturaForm f = new EmisionFacturaForm();
            if (clienteId != null) {
                clienteRepository.findById(clienteId).ifPresent(c -> {
                    f.setClienteId(c.getId());
                    f.setDenominacionReceptor(c.getNombrePaciente());
                    f.setDocumentoReceptor(c.getDniPaciente());
                    f.setDomicilioReceptor(c.getDomicilio());
                    f.setEmailReceptor(c.getCorreo());
                    f.setNombrePacienteAsociado(c.getNombrePaciente());
                });
            }
            if (f.getLineas().isEmpty()) {
                f.getLineas().add(new ItemLineaFacturaForm("Servicio de cuidado domiciliario", BigDecimal.valueOf(8), "horas", BigDecimal.valueOf(4500), BigDecimal.ZERO, BigDecimal.valueOf(36000)));
            }
            form = f;
        }

        cargarListasModelo(model);
        model.addAttribute("form", form);
        return "factura-form";
    }

    @PostMapping("/facturas")
    public String emitir(
            @ModelAttribute EmisionFacturaForm form,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            Factura emitida = arcaService.emitirFactura(form);
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Se emitió exitosamente la " + emitida.getComprobanteFormateado()
                            + " autorizada por ARCA con CAE " + emitida.getCae() + ".");
            return "redirect:/facturas/" + emitida.getId();
        } catch (DatosInvalidosException | RecursoNoEncontradoException ex) {
            model.addAttribute("errorOperacion", ex.getMessage());
            model.addAttribute("form", form);
            cargarListasModelo(model);
            return "factura-form";
        }
    }

    @GetMapping("/facturas/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        Factura factura = arcaService.obtener(id);
        model.addAttribute("factura", factura);
        return "factura-detalle";
    }

    @GetMapping("/facturas/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        Factura factura = arcaService.obtener(id);
        byte[] pdfBytes = exportService.generarPdf(factura);

        String nombreArchivo = "Factura-" + factura.getTipoComprobante().getLetra() + "-"
                + factura.getPuntoVenta() + "-" + factura.getNumeroComprobante() + ".pdf";

        ContentDisposition disposicion = ContentDisposition.attachment()
                .filename(nombreArchivo, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .body(pdfBytes);
    }

    @PostMapping("/facturas/{id}/anular")
    public String anular(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            RedirectAttributes redirectAttributes) {
        try {
            arcaService.anularFactura(id, motivo);
            redirectAttributes.addFlashAttribute("mensajeExito", "La factura ha sido anulada correctamente.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/facturas/" + id;
    }

    @GetMapping("/facturas/resumen-turnos")
    @ResponseBody
    public ResponseEntity<?> obtenerResumenTurnos(
            @RequestParam Long clienteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            ResumenTurnosPeriodoDto resumen = arcaService.cargarResumenTurnosPeriodo(clienteId, desde, hasta);
            return ResponseEntity.ok(resumen);
        } catch (DatosInvalidosException | RecursoNoEncontradoException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        }
    }

    private void cargarListasModelo(Model model) {
        model.addAttribute("clientes", clienteRepository.findAll(Sort.by("nombrePaciente")));
        model.addAttribute("tiposServicio", tipoServicioRepository.findAll(Sort.by("nombre")));
        model.addAttribute("obrasSocialesPadron", obraSocialRepository.findAllByOrderByNombreAsc());
        model.addAttribute("obrasSocialesPreset", OBRAS_SOCIALES_PRESET);
        model.addAttribute("tiposComprobante", TipoComprobante.values());
        model.addAttribute("tiposDocumento", TipoDocumentoReceptor.values());
        model.addAttribute("condicionesIva", CondicionIva.values());
        model.addAttribute("cuitEmisor", arcaService.getCuitEmisor());
        model.addAttribute("razonSocialEmisor", arcaService.getRazonSocialEmisor());
    }
}
