package com.cooperativa.cuidado.web.cuenta;

import com.cooperativa.cuidado.cuenta.Permiso;
import com.cooperativa.cuidado.cuenta.RolUsuario;
import com.cooperativa.cuidado.cuenta.UsuarioCuenta;
import com.cooperativa.cuidado.cuenta.service.CuentaService;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoDuplicadoException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.socio.SocioRepository;
import com.cooperativa.cuidado.web.seguridad.SesionHelper;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;
    private final SocioRepository socioRepository;
    private final SesionHelper sesionHelper;

    public CuentaController(
            CuentaService cuentaService,
            SocioRepository socioRepository,
            SesionHelper sesionHelper) {
        this.cuentaService = cuentaService;
        this.socioRepository = socioRepository;
        this.sesionHelper = sesionHelper;
    }

    @InitBinder
    void configurarBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String listar(Model model) {
        List<UsuarioCuenta> usuarios = cuentaService.listarUsuarios();
        Map<String, Boolean> dniSocioRegistrado = new LinkedHashMap<>();
        for (UsuarioCuenta u : usuarios) {
            dniSocioRegistrado.put(u.getDni(), socioRepository.findByDni(u.getDni()).isPresent());
        }
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("dniSocioRegistrado", dniSocioRegistrado);
        return "cuentas";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        return formularioCuenta(model, new CuentaForm(), "Nueva cuenta de usuario", "/cuentas", false);
    }

    @PostMapping
    public String crear(
            @Valid @ModelAttribute("cuenta") CuentaForm formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {
        if (formulario.getPassword() == null || formulario.getPassword().trim().length() < 4) {
            resultado.rejectValue("password", "password.corto", "Ingresá una contraseña de al menos 4 caracteres");
        }
        if (resultado.hasErrors()) {
            return formularioCuenta(model, null, "Nueva cuenta de usuario", "/cuentas", false);
        }
        try {
            UsuarioCuenta creada = cuentaService.crearUsuario(
                    formulario.getUsername(),
                    formulario.getPassword(),
                    formulario.getNombre(),
                    formulario.getApellido(),
                    formulario.getDni(),
                    formulario.getRolId());
            redirect.addFlashAttribute("mensajeExito",
                    "Se creó la cuenta \"" + creada.getUsername() + "\" (" + creada.getRol().getNombre() + ").");
            return "redirect:/cuentas";
        } catch (RecursoDuplicadoException | DatosInvalidosException | RecursoNoEncontradoException ex) {
            model.addAttribute("errorOperacion", ex.getMessage());
            return formularioCuenta(model, null, "Nueva cuenta de usuario", "/cuentas", false);
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        UsuarioCuenta existente = cuentaService.obtenerUsuario(id);
        CuentaForm form = new CuentaForm();
        form.setUsername(existente.getUsername());
        form.setNombre(existente.getNombre());
        form.setApellido(existente.getApellido());
        form.setDni(existente.getDni());
        form.setRolId(existente.getRol().getId());
        return formularioCuenta(model, form, "Editar cuenta de usuario", "/cuentas/" + id, true);
    }

    @PostMapping("/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("cuenta") CuentaForm formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {
        String accion = "/cuentas/" + id;
        if (formulario.getPassword() != null && !formulario.getPassword().isBlank()
                && formulario.getPassword().trim().length() < 4) {
            resultado.rejectValue("password", "password.corto", "La nueva contraseña debe tener al menos 4 caracteres");
        }
        if (resultado.hasErrors()) {
            return formularioCuenta(model, null, "Editar cuenta de usuario", accion, true);
        }
        try {
            UsuarioCuenta actualizada = cuentaService.actualizarUsuario(
                    id,
                    formulario.getUsername(),
                    formulario.getPassword(),
                    formulario.getNombre(),
                    formulario.getApellido(),
                    formulario.getDni(),
                    formulario.getRolId());
            redirect.addFlashAttribute("mensajeExito",
                    "Los datos de la cuenta \"" + actualizada.getUsername() + "\" se actualizaron correctamente.");
            return "redirect:/cuentas";
        } catch (RecursoDuplicadoException | DatosInvalidosException | RecursoNoEncontradoException ex) {
            model.addAttribute("errorOperacion", ex.getMessage());
            return formularioCuenta(model, null, "Editar cuenta de usuario", accion, true);
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(
            @PathVariable Long id,
            HttpServletRequest request,
            RedirectAttributes redirect) {
        try {
            Long actualId = sesionHelper.obtenerUsuarioActual(request)
                    .map(UsuarioCuenta::getId)
                    .orElse(null);
            cuentaService.eliminarUsuario(id, actualId);
            redirect.addFlashAttribute("mensajeExito", "La cuenta de usuario se eliminó.");
        } catch (ReglaNegocioException | RecursoNoEncontradoException ex) {
            redirect.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/cuentas";
    }

    @GetMapping("/roles")
    public String listarRoles(Model model) {
        List<RolUsuario> roles = cuentaService.listarRoles();
        Map<Long, Long> usos = new LinkedHashMap<>();
        for (RolUsuario r : roles) {
            usos.put(r.getId(), cuentaService.cantidadUsuariosPorRol(r.getId()));
        }
        model.addAttribute("roles", roles);
        model.addAttribute("usos", usos);
        model.addAttribute("permisosDisponibles", Permiso.values());
        return "roles";
    }

    @PostMapping("/roles")
    public String crearRol(
            @RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "false") boolean soloTurnosPropios,
            @RequestParam(required = false) List<Permiso> permisos,
            RedirectAttributes redirect) {
        try {
            Set<Permiso> conjunto = permisos == null || permisos.isEmpty()
                    ? EnumSet.noneOf(Permiso.class)
                    : EnumSet.copyOf(permisos);
            RolUsuario creado = cuentaService.crearRol(nombre, soloTurnosPropios, conjunto);
            redirect.addFlashAttribute("mensajeExito",
                    "Se creó el rol \"" + creado.getNombre() + "\" con sus permisos configurados.");
        } catch (DatosInvalidosException | RecursoDuplicadoException ex) {
            redirect.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/cuentas/roles";
    }

    @PostMapping("/roles/{id}")
    public String actualizarRol(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "false") boolean soloTurnosPropios,
            @RequestParam(required = false) List<Permiso> permisos,
            RedirectAttributes redirect) {
        try {
            Set<Permiso> conjunto = permisos == null || permisos.isEmpty()
                    ? EnumSet.noneOf(Permiso.class)
                    : EnumSet.copyOf(permisos);
            RolUsuario actualizado = cuentaService.actualizarRol(id, nombre, soloTurnosPropios, conjunto);
            redirect.addFlashAttribute("mensajeExito",
                    "Se guardaron los permisos del rol \"" + actualizado.getNombre() + "\".");
        } catch (DatosInvalidosException | RecursoDuplicadoException | RecursoNoEncontradoException ex) {
            redirect.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/cuentas/roles";
    }

    @PostMapping("/roles/{id}/eliminar")
    public String eliminarRol(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            cuentaService.eliminarRol(id);
            redirect.addFlashAttribute("mensajeExito", "El rol se eliminó correctamente.");
        } catch (ReglaNegocioException | RecursoNoEncontradoException ex) {
            redirect.addFlashAttribute("errorOperacion", ex.getMessage());
        }
        return "redirect:/cuentas/roles";
    }

    private String formularioCuenta(
            Model model,
            CuentaForm form,
            String titulo,
            String accion,
            boolean edicion) {
        if (form != null) {
            model.addAttribute("cuenta", form);
        }
        model.addAttribute("titulo", titulo);
        model.addAttribute("accion", accion);
        model.addAttribute("edicion", edicion);
        model.addAttribute("roles", cuentaService.listarRoles());
        model.addAttribute("sociosRegistrados", socioRepository.findAll());
        return "cuenta-form";
    }
}

