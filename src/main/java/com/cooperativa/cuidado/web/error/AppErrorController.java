package com.cooperativa.cuidado.web.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ModelAndView mostrarError(HttpServletRequest request) {
        Object atributoEstado = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int codigo = atributoEstado instanceof Integer valor ? valor : HttpStatus.INTERNAL_SERVER_ERROR.value();
        HttpStatus estado = HttpStatus.resolve(codigo);
        if (estado == null) {
            estado = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String titulo;
        String mensaje;
        if (estado == HttpStatus.NOT_FOUND) {
            titulo = "No encontramos esa página";
            mensaje = "La dirección puede haber cambiado o estar escrita incorrectamente. Volvé al inicio y elegí una opción del menú.";
        } else if (estado == HttpStatus.METHOD_NOT_ALLOWED) {
            titulo = "Esta acción no está disponible de esa manera";
            mensaje = "Volvé a la pantalla anterior y usá el botón o formulario correspondiente.";
        } else if (estado.is4xxClientError()) {
            titulo = "No se pudo procesar la solicitud";
            mensaje = "Revisá los datos enviados e intentá nuevamente. Si el problema continúa, volvé al inicio.";
        } else {
            titulo = "Ocurrió un problema inesperado";
            mensaje = "No se completó la operación. Revisá el resultado antes de repetirla y, si vuelve a ocurrir, contactá a soporte.";
        }

        ModelAndView vista = new ModelAndView("error");
        vista.setStatus(estado);
        vista.addObject("codigoEstado", estado.value());
        vista.addObject("tituloError", titulo);
        vista.addObject("mensajeError", mensaje);
        return vista;
    }
}
