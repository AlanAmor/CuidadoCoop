package com.cooperativa.cuidado.web.asignacion;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.service.AsistenciaService;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.service.ClienteService;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.socio.Socio;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class AsignacionController {

    private final ClienteService clienteService;
    private final ServicioDomicilioRepository servicioRepository;
    private final AsignacionService asignacionService;
    private final AsistenciaService asistenciaService;

    public AsignacionController(
            ClienteService clienteService,
            ServicioDomicilioRepository servicioRepository,
            AsignacionService asignacionService,
            AsistenciaService asistenciaService) {
        this.clienteService = clienteService;
        this.servicioRepository = servicioRepository;
        this.asignacionService = asignacionService;
        this.asistenciaService = asistenciaService;
    }

    @PostMapping("/asistencias/{id}/eliminar")
    public String eliminarTurno(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        asistenciaService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "El turno se eliminó correctamente.");
        return "redirect:/";
    }

    @GetMapping("/asignaciones/nueva")
    public String mostrarFormulario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            @RequestParam(defaultValue = "false") boolean repetir,
            @RequestParam(required = false) UnidadDuracion unidad,
            @RequestParam(required = false) Integer cantidad,
            @RequestParam(required = false) List<DayOfWeek> dias,
            Model model) {
        if (!model.containsAttribute("asignacion")) {
            AsignacionForm form = new AsignacionForm();
            form.setFecha(fecha != null ? fecha : LocalDate.now());
            form.setHoraInicio(horaInicio);
            form.setHoraFin(horaFin);
            form.setRepetir(repetir);
            if (unidad != null) {
                form.setUnidad(unidad);
            }
            if (cantidad != null) {
                form.setCantidad(cantidad);
            }
            form.setDias(dias != null ? dias : new ArrayList<>(List.of(DayOfWeek.values())));
            model.addAttribute("asignacion", form);
        }
        AsignacionForm form = (AsignacionForm) model.getAttribute("asignacion");
        if (fecha != null && horaInicio != null && horaFin != null) {
            prepararOpciones(model, form);
        } else {
            prepararServicios(model);
        }
        return "asignaciones";
    }

    @PostMapping("/asignaciones")
    public String asignar(
            @Valid @ModelAttribute("asignacion") AsignacionForm formulario,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepararOpciones(model, formulario);
            return "asignaciones";
        }

        try {
            Socio socio = new Socio();
            socio.setId(formulario.getSocioId());
            ServicioDomicilio servicio = new ServicioDomicilio();
            servicio.setId(formulario.getServicioDomicilioId());
            Cliente cliente = new Cliente();
            cliente.setId(formulario.getClienteId());

            Asistencia asistencia = new Asistencia();
            asistencia.setSocio(socio);
            asistencia.setCliente(cliente);
            asistencia.setServicioDomicilio(servicio);
            asistencia.setFecha(formulario.getFecha());
            asistencia.setHoraInicio(formulario.getHoraInicio());
            asistencia.setHoraFin(formulario.getHoraFin());
            if (formulario.isRepetir()) {
                List<LocalDate> fechas = CalculoRepeticion.fechas(
                        formulario.getFecha(), formulario.getUnidad(),
                        formulario.getCantidad(), formulario.getDias());
                int creados = asignacionService.asignarSerie(asistencia, fechas);
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "Se asignaron " + creados + " turnos del cuidado fijo. Podés editar o eliminar cada uno desde el calendario.");
            } else {
                asignacionService.asignar(asistencia);
                redirectAttributes.addFlashAttribute("mensajeExito", "El turno se asignó correctamente.");
            }
            return "redirect:/asignaciones/nueva";
        } catch (ReglaNegocioException | DatosInvalidosException | RecursoNoEncontradoException ex) {
            model.addAttribute("errorOperacion", ex.getMessage());
            prepararOpciones(model, formulario);
            return "asignaciones";
        }
    }

    private void prepararServicios(Model model) {
        model.addAttribute("servicios", servicioRepository.findAllByOrderByIdAsc());
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("unidades", UnidadDuracion.values());
        Map<DayOfWeek, String> diasSemana = new LinkedHashMap<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            diasSemana.put(dia, dia.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es")));
        }
        model.addAttribute("diasSemana", diasSemana);
    }

    /** Con día y horario definidos, calcula los socios libres en ese rango (o en toda la serie) y muestra el paso 2. */
    private void prepararOpciones(Model model, AsignacionForm form) {
        prepararServicios(model);
        if (form.getFecha() == null || form.getHoraInicio() == null || form.getHoraFin() == null) {
            return;
        }
        try {
            if (form.isRepetir()) {
                List<LocalDate> fechas = CalculoRepeticion.fechas(
                        form.getFecha(), form.getUnidad(), form.getCantidad(), form.getDias());
                model.addAttribute("fechasSerie", fechas);
                model.addAttribute("sociosDisponibles", asistenciaService.listarSociosDisponiblesEnFechas(
                        fechas, form.getHoraInicio(), form.getHoraFin()));
            } else {
                model.addAttribute("sociosDisponibles", asistenciaService.listarSociosDisponibles(
                        form.getFecha(), form.getHoraInicio(), form.getHoraFin(), null));
            }
        } catch (DatosInvalidosException ex) {
            model.addAttribute("errorOperacion", ex.getMessage());
        }
    }
}
