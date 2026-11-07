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
        formulario.setValorHoraClienteSemanaMas12(servicio.getEfectivoClienteSemanaMas12());
        formulario.setValorHoraClienteFindeMenos12(servicio.getEfectivoClienteFindeMenos12());
        formulario.setValorHoraClienteFindeMas12(servicio.getEfectivoClienteFindeMas12());
        formulario.setValorHoraClienteFeriadoMenos12(servicio.getEfectivoClienteFeriadoMenos12());
        formulario.setValorHoraClienteFeriadoMas12(servicio.getEfectivoClienteFeriadoMas12());

        formulario.setValorHoraSocio(servicio.getEfectivoSocioSemanaMenos12());
        formulario.setValorHoraSocioSemanaMas12(servicio.getEfectivoSocioSemanaMas12());
        formulario.setValorHoraSocioFindeMenos12(servicio.getEfectivoSocioFindeMenos12());
        formulario.setValorHoraSocioFindeMas12(servicio.getEfectivoSocioFindeMas12());
        formulario.setValorHoraSocioFeriadoMenos12(servicio.getEfectivoSocioFeriadoMenos12());
        formulario.setValorHoraSocioFeriadoMas12(servicio.getEfectivoSocioFeriadoMas12());
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
        ServicioDomicilio borrador = new ServicioDomicilio();
        borrador.setTipoServicio(tipoService.obtener(formulario.getTipoServicioId()));
        borrador.setValorHora(formulario.getValorHora());
        borrador.setValorHoraClienteSemanaMas12(formulario.getValorHoraClienteSemanaMas12());
        borrador.setValorHoraClienteFindeMenos12(formulario.getValorHoraClienteFindeMenos12());
        borrador.setValorHoraClienteFindeMas12(formulario.getValorHoraClienteFindeMas12());
        borrador.setValorHoraClienteFeriadoMenos12(formulario.getValorHoraClienteFeriadoMenos12());
        borrador.setValorHoraClienteFeriadoMas12(formulario.getValorHoraClienteFeriadoMas12());

        borrador.setValorHoraSocio(formulario.getValorHoraSocio());
        borrador.setValorHoraSocioSemanaMas12(formulario.getValorHoraSocioSemanaMas12());
        borrador.setValorHoraSocioFindeMenos12(formulario.getValorHoraSocioFindeMenos12());
        borrador.setValorHoraSocioFindeMas12(formulario.getValorHoraSocioFindeMas12());
        borrador.setValorHoraSocioFeriadoMenos12(formulario.getValorHoraSocioFeriadoMenos12());
        borrador.setValorHoraSocioFeriadoMas12(formulario.getValorHoraSocioFeriadoMas12());

        // Persistir las 6 tarifas resueltas tanto para cliente como para socio
        ServicioDomicilio servicio = new ServicioDomicilio();
        servicio.setTipoServicio(borrador.getTipoServicio());
        servicio.setValorHora(borrador.getValorHora());
        servicio.setValorHoraClienteSemanaMas12(borrador.getEfectivoClienteSemanaMas12());
        servicio.setValorHoraClienteFindeMenos12(borrador.getEfectivoClienteFindeMenos12());
        servicio.setValorHoraClienteFindeMas12(borrador.getEfectivoClienteFindeMas12());
        servicio.setValorHoraClienteFeriadoMenos12(borrador.getEfectivoClienteFeriadoMenos12());
        servicio.setValorHoraClienteFeriadoMas12(borrador.getEfectivoClienteFeriadoMas12());

        servicio.setValorHoraSocio(borrador.getValorHoraSocio());
        servicio.setValorHoraSocioSemanaMas12(borrador.getEfectivoSocioSemanaMas12());
        servicio.setValorHoraSocioFindeMenos12(borrador.getEfectivoSocioFindeMenos12());
        servicio.setValorHoraSocioFindeMas12(borrador.getEfectivoSocioFindeMas12());
        servicio.setValorHoraSocioFeriadoMenos12(borrador.getEfectivoSocioFeriadoMenos12());
        servicio.setValorHoraSocioFeriadoMas12(borrador.getEfectivoSocioFeriadoMas12());
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
