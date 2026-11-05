package com.cooperativa.cuidado.web.cuenta;

import com.cooperativa.cuidado.cuenta.UsuarioCuenta;
import com.cooperativa.cuidado.cuenta.service.CuentaService;
import com.cooperativa.cuidado.web.seguridad.SesionHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {

    private final CuentaService cuentaService;
    private final SesionHelper sesionHelper;

    public LoginController(CuentaService cuentaService, SesionHelper sesionHelper) {
        this.cuentaService = cuentaService;
        this.sesionHelper = sesionHelper;
    }

    @GetMapping("/login")
    public String mostrarLogin(HttpServletRequest request) {
        if (sesionHelper.obtenerUsuarioActual(request).isPresent()) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String iniciarSesion(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String password,
            HttpSession session,
            Model model) {
        Optional<UsuarioCuenta> cuenta = cuentaService.autenticar(username, password);
        if (cuenta.isEmpty()) {
            model.addAttribute("errorLogin", "El usuario o la contraseña son incorrectos.");
            model.addAttribute("usernameIngresado", username);
            return "login";
        }
        sesionHelper.iniciarSesion(session, cuenta.get());
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        sesionHelper.cerrarSesion(session);
        return "redirect:/login?salio=true";
    }
}

