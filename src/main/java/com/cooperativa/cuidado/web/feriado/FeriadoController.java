package com.cooperativa.cuidado.web.feriado;

import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.feriado.FeriadoNacional;
import com.cooperativa.cuidado.feriado.service.FeriadoNacionalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class FeriadoController {

    private final FeriadoNacionalService feriadoService;

    public FeriadoController(FeriadoNacionalService feriadoService) {
        this.feriadoService = feriadoService;
    }

    @GetMapping("/feriados")
    public String listar(
            @RequestParam(required = false) Integer anio,
            Model model) {
        int anioActual = LocalDate.now().getYear();
        List<FeriadoNacional> feriados = anio != null
                ? feriadoService.listarPorAnio(anio)
                : feriadoService.listar();
        model.addAttribute("feriados", feriados);
        model.addAttribute("anioFiltro", anio);
        model.addAttribute("anioSugerido", anio != null ? anio : anioActual);
        return "feriados";
    }

    @PostMapping("/feriados")
    public String registrar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String descripcion,
            RedirectAttributes redirect) {
        try {
            FeriadoNacional guardado = feriadoService.registrar(fecha, descripcion);
            redirect.addFlashAttribute("mensajeExito",
                    "Se registró el feriado nacional (" + guardado.getDescripcion() + ").");
        } catch (DatosInvalidosException | ReglaNegocioException ex) {
            redirect.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/feriados";
    }

    @PostMapping("/feriados/cargar-fijos")
    public String cargarFijos(
            @RequestParam int anio,
            RedirectAttributes redirect) {
        try {
            int agregados = feriadoService.cargarFeriadosFijosAnio(anio);
            if (agregados > 0) {
                redirect.addFlashAttribute("mensajeExito",
                        "Se cargaron " + agregados + " feriados nacionales inamovibles para el año " + anio + ".");
            } else {
                redirect.addFlashAttribute("mensajeExito",
                        "Todos los feriados nacionales inamovibles de " + anio + " ya estaban registrados.");
            }
        } catch (DatosInvalidosException ex) {
            redirect.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/feriados?anio=" + anio;
    }

    @PostMapping("/feriados/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            feriadoService.eliminar(id);
            redirect.addFlashAttribute("mensajeExito", "El feriado nacional se eliminó correctamente.");
        } catch (RecursoNoEncontradoException ex) {
            redirect.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/feriados";
    }

    @GetMapping("/api/feriados")
    @ResponseBody
    public List<FeriadoDto> listarApi() {
        return feriadoService.listar().stream()
                .map(f -> new FeriadoDto(f.getId(), f.getFecha(), f.getDescripcion()))
                .toList();
    }

    public record FeriadoDto(Long id, LocalDate fecha, String descripcion) {
    }
}

