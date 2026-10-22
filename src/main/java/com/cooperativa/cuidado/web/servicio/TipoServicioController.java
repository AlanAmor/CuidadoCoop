package com.cooperativa.cuidado.web.servicio;

import com.cooperativa.cuidado.exception.RecursoDuplicadoException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.servicio.service.TipoServicioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/tipos-servicio")
public class TipoServicioController {

    private final TipoServicioService tipoService;

    public TipoServicioController(TipoServicioService tipoService) {
        this.tipoService = tipoService;
    }

    @GetMapping
    public String listar(Model model) {
        Map<Long, Long> usos = new LinkedHashMap<>();
        tipoService.listar().forEach(tipo -> usos.put(tipo.getId(), tipoService.cantidadDeServicios(tipo.getId())));
        model.addAttribute("tipos", tipoService.listar());
        model.addAttribute("usos", usos);
        return "tipos-servicio";
    }

    @PostMapping
    public String crear(@RequestParam(defaultValue = "") String nombre, RedirectAttributes redirect) {
        return ejecutar(redirect, "El tipo de servicio se creó correctamente.", () -> tipoService.crear(nombre));
    }

    @PostMapping("/{id}")
    public String renombrar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String nombre,
            RedirectAttributes redirect) {
        return ejecutar(redirect,
                "El tipo de servicio se renombró. Los servicios y turnos que lo usan muestran el nuevo nombre.",
                () -> tipoService.renombrar(id, nombre));
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        return ejecutar(redirect, "El tipo de servicio se eliminó.", () -> tipoService.eliminar(id));
    }

    private String ejecutar(RedirectAttributes redirect, String exito, Runnable accion) {
        try {
            accion.run();
            redirect.addFlashAttribute("mensajeExito", exito);
        } catch (RecursoDuplicadoException | ReglaNegocioException | RecursoNoEncontradoException ex) {
            redirect.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/tipos-servicio";
    }
}
