package com.cooperativa.cuidado.web.socio;

import com.cooperativa.cuidado.exception.RecursoDuplicadoException;
import com.cooperativa.cuidado.socio.Especialidad;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.service.SocioService;
import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/socios")
public class SocioController {

    private final SocioService socioService;

    public SocioController(SocioService socioService) {
        this.socioService = socioService;
    }

    @InitBinder
    void configurarBinder(WebDataBinder binder) {
        binder.setDisallowedFields("id");
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("socios", socioService.listar());
        return "socios";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        Socio socio = new Socio();
        socio.setEstado(EstadoSocio.DISPONIBLE_REEMPLAZO);
        return formulario(model, socio, "Nuevo socio", "/socios");
    }

    @PostMapping
    public String crear(
            @Valid @ModelAttribute("socio") Socio socio,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return formulario(model, null, "Nuevo socio", "/socios");
        }
        try {
            Socio guardado = socioService.registrar(socio);
            redirect.addFlashAttribute("mensajeExito",
                    "Se registró al socio " + guardado.getApellido() + ", " + guardado.getNombre() + ".");
            return "redirect:/socios";
        } catch (RecursoDuplicadoException ex) {
            model.addAttribute("errorOperacion", ex.getMessage()
                    + ". Revisá que el DNI y el CBU no pertenezcan a otro socio.");
            return formulario(model, null, "Nuevo socio", "/socios");
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        return formulario(model, socioService.obtener(id), "Editar socio", "/socios/" + id, id);
    }

    @PostMapping("/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("socio") Socio socio,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {
        String accion = "/socios/" + id;
        if (resultado.hasErrors()) {
            return formulario(model, null, "Editar socio", accion, id);
        }
        try {
            socioService.actualizar(id, socio);
            redirect.addFlashAttribute("mensajeExito", "Los datos del socio se actualizaron correctamente.");
            return "redirect:/socios";
        } catch (RecursoDuplicadoException ex) {
            model.addAttribute("errorOperacion", ex.getMessage()
                    + ". Revisá que el DNI y el CBU no pertenezcan a otro socio.");
            return formulario(model, null, "Editar socio", accion, id);
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        socioService.eliminar(id);
        redirect.addFlashAttribute("mensajeExito", "El socio y sus turnos registrados se eliminaron.");
        return "redirect:/socios";
    }

    private String formulario(Model model, Socio socio, String titulo, String accion) {
        return formulario(model, socio, titulo, accion, null);
    }

    private String formulario(Model model, Socio socio, String titulo, String accion, Long legajoId) {
        if (socio != null) {
            model.addAttribute("socio", socio);
        }
        model.addAttribute("legajoId", legajoId);
        model.addAttribute("legajoTipo", "socios");
        model.addAttribute("titulo", titulo);
        model.addAttribute("accion", accion);
        model.addAttribute("estados", EstadoSocio.values());
        model.addAttribute("especialidades", Especialidad.values());
        return "socio-form";
    }
}
