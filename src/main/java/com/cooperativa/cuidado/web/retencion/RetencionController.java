package com.cooperativa.cuidado.web.retencion;

import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.retencion.TipoRetencion;
import com.cooperativa.cuidado.retencion.service.RetencionService;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.YearMonth;

@Controller
public class RetencionController {

    private final RetencionService retencionService;
    private final SocioRepository socioRepository;

    public RetencionController(RetencionService retencionService, SocioRepository socioRepository) {
        this.retencionService = retencionService;
        this.socioRepository = socioRepository;
    }

    @GetMapping("/retenciones")
    public String listar(
            @RequestParam(required = false) Long socioFiltro,
            @RequestParam(required = false) String periodoFiltro,
            Model model) {
        cargarModelo(model, socioFiltro, periodoFiltro);
        return "retenciones";
    }

    @PostMapping("/retenciones")
    public String crear(
            @RequestParam(required = false) Long socioId,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) TipoRetencion tipo,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) BigDecimal unidad,
            @RequestParam(required = false) BigDecimal valorUnidad,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            retencionService.registrar(socioId, periodo, tipo, motivo, unidad, valorUnidad);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "La retención se registró correctamente y se cargará automáticamente al emitir el recibo del período.");
            return "redirect:/retenciones?periodoFiltro=" + (periodo != null ? periodo : "");
        } catch (DatosInvalidosException | RecursoNoEncontradoException ex) {
            model.addAttribute("errorOperacion", ex.getMessage());
            cargarModelo(model, socioId, periodo);
            model.addAttribute("formSocioId", socioId);
            model.addAttribute("formPeriodo", periodo);
            model.addAttribute("formTipo", tipo);
            model.addAttribute("formMotivo", motivo);
            model.addAttribute("formUnidad", unidad);
            model.addAttribute("formValorUnidad", valorUnidad);
            return "retenciones";
        }
    }

    @PostMapping("/retenciones/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            retencionService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "La retención se eliminó correctamente.");
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/retenciones";
    }

    private void cargarModelo(Model model, Long socioFiltro, String periodoFiltro) {
        model.addAttribute("socios", socioRepository.findAll(Sort.by("apellido", "nombre")));
        model.addAttribute("tiposRetencion", TipoRetencion.values());
        model.addAttribute("periodoActual", YearMonth.now().toString());
        model.addAttribute("socioFiltro", socioFiltro);
        model.addAttribute("periodoFiltro", periodoFiltro);
        model.addAttribute("retenciones", retencionService.listar(socioFiltro, periodoFiltro));
    }
}

