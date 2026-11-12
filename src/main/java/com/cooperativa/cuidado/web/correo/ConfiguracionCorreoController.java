package com.cooperativa.cuidado.web.correo;

import com.cooperativa.cuidado.correo.ConfiguracionCorreo;
import com.cooperativa.cuidado.correo.service.CorreoService;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracion/correo")
public class ConfiguracionCorreoController {

    private final CorreoService correoService;

    public ConfiguracionCorreoController(CorreoService correoService) {
        this.correoService = correoService;
    }

    @GetMapping
    public String mostrarConfiguracion(Model model) {
        ConfiguracionCorreo cfg = correoService.obtenerConfiguracion();
        model.addAttribute("configCorreo", cfg);
        model.addAttribute("correoConfigurado", cfg.tieneCredencialesCompletas());
        return "configuracion-correo";
    }

    @PostMapping
    public String guardarConfiguracion(
            @RequestParam String host,
            @RequestParam Integer puerto,
            @RequestParam String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String remitenteNombre,
            @RequestParam(required = false) String remitenteEmail,
            @RequestParam(defaultValue = "false") boolean usarTls,
            @RequestParam(defaultValue = "false") boolean usarSsl,
            @RequestParam(required = false) String asuntoSocioPlantilla,
            @RequestParam(required = false) String cuerpoSocioPlantilla,
            @RequestParam(required = false) String asuntoClientePlantilla,
            @RequestParam(required = false) String cuerpoClientePlantilla,
            RedirectAttributes redirectAttributes) {
        try {
            correoService.guardarConfiguracion(
                    host,
                    puerto,
                    username,
                    password,
                    remitenteNombre,
                    remitenteEmail,
                    usarTls,
                    usarSsl,
                    asuntoSocioPlantilla,
                    cuerpoSocioPlantilla,
                    asuntoClientePlantilla,
                    cuerpoClientePlantilla);
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "La configuración del correo electrónico se guardó correctamente.");
        } catch (DatosInvalidosException | ReglaNegocioException ex) {
            redirectAttributes.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/configuracion/correo";
    }

    @PostMapping("/probar")
    public String probarEnvio(
            @RequestParam String destinatarioPrueba,
            RedirectAttributes redirectAttributes) {
        try {
            correoService.enviarCorreoPrueba(destinatarioPrueba);
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Correo de prueba enviado correctamente a " + destinatarioPrueba.trim() + ".");
        } catch (DatosInvalidosException | ReglaNegocioException ex) {
            redirectAttributes.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/configuracion/correo";
    }
}
