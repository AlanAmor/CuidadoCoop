package com.cooperativa.cuidado.web.seguridad;

import com.cooperativa.cuidado.cuenta.Permiso;
import com.cooperativa.cuidado.cuenta.UsuarioCuenta;
import com.cooperativa.cuidado.cuenta.UsuarioCuentaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("seguridad")
public class SesionHelper {

    public static final String ATRIBUTO_SESION_USUARIO_ID = "USUARIO_CUENTA_ID";
    public static final String ATRIBUTO_REQUEST_USUARIO = "USUARIO_ACTUAL_CACHE";

    private final UsuarioCuentaRepository usuarioRepository;

    public SesionHelper(UsuarioCuentaRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public void iniciarSesion(HttpSession session, UsuarioCuenta usuario) {
        session.setAttribute(ATRIBUTO_SESION_USUARIO_ID, usuario.getId());
    }

    public void cerrarSesion(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    public Optional<UsuarioCuenta> obtenerUsuarioActual(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        Object enRequest = request.getAttribute(ATRIBUTO_REQUEST_USUARIO);
        if (enRequest instanceof UsuarioCuenta u) {
            return Optional.of(u);
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        Object idObj = session.getAttribute(ATRIBUTO_SESION_USUARIO_ID);
        if (!(idObj instanceof Long id)) {
            return Optional.empty();
        }
        Optional<UsuarioCuenta> encontrado = usuarioRepository.findById(id);
        encontrado.ifPresent(u -> request.setAttribute(ATRIBUTO_REQUEST_USUARIO, u));
        return encontrado;
    }

    public boolean tienePermiso(UsuarioCuenta usuario, String nombrePermiso) {
        if (usuario == null) {
            return true;
        }
        try {
            Permiso permiso = Permiso.valueOf(nombrePermiso);
            return usuario.tienePermiso(permiso);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean esSoloTurnosPropios(UsuarioCuenta usuario) {
        return usuario != null && usuario.esSoloTurnosPropios();
    }

    public boolean esAdmin(UsuarioCuenta usuario) {
        return usuario == null || usuario.esAdmin();
    }
}

