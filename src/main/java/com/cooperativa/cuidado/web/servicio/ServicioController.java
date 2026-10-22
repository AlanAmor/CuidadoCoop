package com.cooperativa.cuidado.web.servicio;

import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.service.ServicioDomicilioService;
import com.cooperativa.cuidado.servicio.service.TipoServicioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/servicios")
public class ServicioController {

    private final ServicioDomicilioService servicioService;
    private final TipoServicioService tipoService;

    public ServicioController(ServicioDomicilioService servicioService, TipoServicioService tipoService) {
        this.servicioService = servicioService;
        this.tipoService = tipoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("servicios", servicioService.listar());
        return "servicios";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return formulario(model, new ServicioForm(), "Nuevo servicio", "/servicios");
    }

    @PostMapping
    public String crear(
            @Valid @ModelAttribute("servicio") ServicioForm formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return formulario(model, null, "Nuevo servicio", "/servicios");
        }
        try {
            servicioService.registrar(aEntidad(formulario));
            redirect.addFlashAttribute("mensajeExito", "El servicio se creó correctamente.");
            return "redirect:/servicios";
        } catch (RecursoNoEncontradoException ex) {
            model.addAttribute("errorOperacion", ex.getMessage());
            return formulario(model, null, "Nuevo servicio", "/servicios");
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        ServicioDomicilio servicio = servicioService.obtener(id);
        ServicioForm formulario = new ServicioForm();
        formulario.setTipoServicioId(servicio.getTipoServicio().getId());
        formulario.setValorHora(servicio.getValorHora());
        formulario.setValorHoraSocio(servicio.getValorHoraSocio());
        return formulario(model, formulario, "Editar servicio", "/servicios/" + id);
    }

    @PostMapping("/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("servicio") ServicioForm formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {
        String accion = "/servicios/" + id;
        if (resultado.hasErrors()) {
            return formulario(model, null, "Editar servicio", accion);
        }
        try {
            servicioService.actualizar(id, aEntidad(formulario));
            redirect.addFlashAttribute("mensajeExito",
                    "El servicio se actualizó. Los turnos ya cargados conservan el valor hora con el que se registraron.");
            return "redirect:/servicios";
        } catch (RecursoNoEncontradoException ex) {
            model.addAttribute("errorOperacion", ex.getMessage());
            return formulario(model, null, "Editar servicio", accion);
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        servicioService.eliminar(id);
        redirect.addFlashAttribute("mensajeExito", "El servicio y sus turnos registrados se eliminaron.");
        return "redirect:/servicios";
    }

    private ServicioDomicilio aEntidad(ServicioForm formulario) {
        ServicioDomicilio servicio = new ServicioDomicilio();
        servicio.setTipoServicio(tipoService.obtener(formulario.getTipoServicioId()));
        servicio.setValorHora(formulario.getValorHora());
        servicio.setValorHoraSocio(formulario.getValorHoraSocio());
        return servicio;
    }

    private String formulario(Model model, ServicioForm formulario, String titulo, String accion) {
        if (formulario != null) {
            model.addAttribute("servicio", formulario);
        }
        model.addAttribute("titulo", titulo);
        model.addAttribute("accion", accion);
        model.addAttribute("tipos", tipoService.listar());
        return "servicio-form";
    }
}
