package com.cooperativa.cuidado.obrasocial;

import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoDuplicadoException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.facturacion.CondicionIva;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/obras-sociales")
public class ObraSocialController {

    private final ObraSocialService obraSocialService;

    public ObraSocialController(ObraSocialService obraSocialService) {
        this.obraSocialService = obraSocialService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("obrasSociales", obraSocialService.listar());
        model.addAttribute("nuevaObraSocial", new ObraSocial());
        model.addAttribute("condicionesIva", CondicionIva.values());
        return "obras-sociales";
    }

    @PostMapping
    public String guardar(@ModelAttribute ObraSocial obraSocial, RedirectAttributes redirectAttributes) {
        try {
            obraSocialService.guardar(obraSocial);
            redirectAttributes.addFlashAttribute("mensajeExito", "Obra Social guardada exitosamente en el padrón.");
        } catch (DatosInvalidosException | RecursoDuplicadoException ex) {
            redirectAttributes.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/obras-sociales";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            obraSocialService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Obra Social eliminada del padrón.");
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/obras-sociales";
    }

    @GetMapping("/api/lista")
    @ResponseBody
    public List<ObraSocial> apiLista() {
        return obraSocialService.listar();
    }
}
