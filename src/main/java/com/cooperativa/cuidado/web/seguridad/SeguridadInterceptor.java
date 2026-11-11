package com.cooperativa.cuidado.web.seguridad;

import com.cooperativa.cuidado.cuenta.Permiso;
import com.cooperativa.cuidado.cuenta.UsuarioCuenta;
import com.cooperativa.cuidado.exception.AccesoDenegadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class SeguridadInterceptor implements HandlerInterceptor {

    private final SesionHelper sesionHelper;
    private final boolean seguridadActiva;

    public SeguridadInterceptor(
            SesionHelper sesionHelper,
            @Value("${app.seguridad.activa:true}") boolean seguridadActiva) {
        this.sesionHelper = sesionHelper;
        this.seguridadActiva = seguridadActiva;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        Optional<UsuarioCuenta> usuarioOpt = sesionHelper.obtenerUsuarioActual(request);
        usuarioOpt.ifPresent(u -> request.setAttribute("usuarioActual", u));

        if (esRutaPublica(path)) {
            return true;
        }

        if (usuarioOpt.isEmpty()) {
            if (!seguridadActiva) {
                return true;
            }
            if (esSolicitudJson(request, path)) {
                escribirErrorJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Tu sesión expiró. Iniciá sesión nuevamente.");
                return false;
            }
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        UsuarioCuenta usuario = usuarioOpt.get();
        Permiso permisoRequerido = permisoParaRuta(path, request.getMethod());
        if (permisoRequerido != null && !usuario.tienePermiso(permisoRequerido)) {
            String msg = "Tu cuenta (" + usuario.getRol().getNombre()
                    + ") no tiene permiso para realizar esta acción (" + permisoRequerido.getEtiqueta() + ").";
            if (esSolicitudJson(request, path)) {
                escribirErrorJson(response, HttpServletResponse.SC_FORBIDDEN, msg);
                return false;
            }
            throw new AccesoDenegadoException(msg);
        }

        return true;
    }

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView) {
        if (modelAndView != null && modelAndView.getViewName() != null
                && !modelAndView.getViewName().startsWith("redirect:")) {
            sesionHelper.obtenerUsuarioActual(request)
                    .ifPresent(u -> modelAndView.addObject("usuarioActual", u));
        }
    }

    private boolean esRutaPublica(String path) {
        return path.equals("/login")
                || path.equals("/logout")
                || path.equals("/error")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.equals("/favicon.ico");
    }

    private boolean esSolicitudJson(HttpServletRequest request, String path) {
        if (path.startsWith("/api/")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)
                && !accept.contains(MediaType.TEXT_HTML_VALUE);
    }

    private void escribirErrorJson(HttpServletResponse response, int status, String mensaje) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String escapado = mensaje.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"error\":\"" + escapado + "\"}");
    }

    private Permiso permisoParaRuta(String path, String metodo) {
        if (path.equals("/") || path.equals("/api/calendario")
                || path.equals("/api/feriados")
                || path.matches("^/api/asistencias/\\d+/marcacion$")) {
            return null;
        }
        if (path.startsWith("/cuentas")) {
            return Permiso.GESTIONAR_CUENTAS;
        }
        if (path.startsWith("/estadisticas")) {
            return Permiso.VER_ESTADISTICAS;
        }
        if (path.startsWith("/liquidaciones-familia")) {
            return Permiso.REALIZAR_LIQUIDACIONES_FAMILIA;
        }
        if (path.startsWith("/retenciones")) {
            return Permiso.GESTIONAR_RETENCIONES;
        }
        if (path.startsWith("/liquidaciones")
                || path.startsWith("/api/liquidaciones")) {
            return Permiso.REALIZAR_LIQUIDACIONES;
        }
        if (path.startsWith("/presupuestos")) {
            return Permiso.REALIZAR_PRESUPUESTOS;
        }
        if (path.startsWith("/asignaciones")) {
            return Permiso.ASIGNAR_TURNOS;
        }
        if (path.matches("^/asistencias/\\d+/eliminar$")) {
            return Permiso.ELIMINAR_TURNOS;
        }
        if (path.startsWith("/asistencias/historico")) {
            return Permiso.EXPORTAR_HISTORICOS;
        }
        if (path.matches("^/api/asistencias/\\d+$")
                || path.matches("^/api/asistencias/\\d+/adicionales$")) {
            return Permiso.MODIFICAR_TURNOS;
        }
        if (path.equals("/api/socios/disponibles")
                || path.equals("/api/clientes/opciones")
                || path.equals("/api/servicios/opciones")) {
            return Permiso.MODIFICAR_TURNOS;
        }
        if (path.matches("^/socios/\\d+/eliminar$")) {
            return Permiso.ELIMINAR_SOCIOS;
        }
        if (path.matches("^/socios/\\d+/historico/.*$")) {
            return Permiso.EXPORTAR_HISTORICOS;
        }
        if (path.startsWith("/socios")) {
            return Permiso.AGREGAR_SOCIOS;
        }
        if (path.matches("^/clientes/\\d+/eliminar$")) {
            return Permiso.ELIMINAR_CLIENTES;
        }
        if (path.matches("^/clientes/\\d+/historico/.*$")) {
            return Permiso.EXPORTAR_HISTORICOS;
        }
        if (path.startsWith("/clientes")) {
            return Permiso.AGREGAR_CLIENTES;
        }
        if (path.startsWith("/feriados")) {
            return Permiso.GESTIONAR_FERIADOS;
        }
        if (path.startsWith("/servicios")
                || path.startsWith("/tipos-servicio")) {
            return Permiso.GESTIONAR_SERVICIOS;
        }
        if (path.startsWith("/api/legajos/archivos/") && "DELETE".equalsIgnoreCase(metodo)) {
            return Permiso.ELIMINAR_LEGAJOS;
        }
        if (path.startsWith("/api/legajos") || path.startsWith("/legajos/archivos")) {
            return Permiso.AGREGAR_LEGAJOS;
        }
        return null;
    }
}
