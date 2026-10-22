package com.cooperativa.cuidado.web.error;

import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoDuplicadoException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashSet;
import java.util.Set;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class
    })
    public ModelAndView manejarErroresDeFormulario(Exception excepcion) {
        BindException errores = (BindException) excepcion;
        Set<String> mensajes = errores.getBindingResult().getFieldErrors().stream()
                .map(this::describirErrorCampo)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String detalle = mensajes.isEmpty()
                ? "Revisá los datos ingresados e intentá nuevamente."
                : String.join(" ", mensajes);
        return vistaError(HttpStatus.BAD_REQUEST, "Revisá los datos ingresados", detalle);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ModelAndView manejarValidacion(ConstraintViolationException excepcion) {
        String detalle = excepcion.getConstraintViolations().stream()
                .map(violacion -> violacion.getPropertyPath() + ": " + violacion.getMessage())
                .collect(Collectors.joining(" "));
        return vistaError(HttpStatus.BAD_REQUEST, "Hay datos que no son válidos", detalle);
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ModelAndView manejarDatosInvalidos(DatosInvalidosException excepcion) {
        return vistaError(HttpStatus.BAD_REQUEST, "No se pudieron validar los datos", excepcion.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ModelAndView manejarRecursoNoEncontrado(RecursoNoEncontradoException excepcion) {
        return vistaError(HttpStatus.NOT_FOUND, "No encontramos ese registro", excepcion.getMessage());
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ModelAndView manejarRecursoDuplicado(RecursoDuplicadoException excepcion) {
        return vistaError(HttpStatus.CONFLICT, "Ese dato ya está registrado", excepcion.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ModelAndView manejarReglaNegocio(ReglaNegocioException excepcion) {
        return vistaError(HttpStatus.CONFLICT, "No se pudo completar la operación", excepcion.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView manejarIntegridadDeDatos(DataIntegrityViolationException excepcion) {
        LOGGER.warn("Se rechazó una operación por una restricción de integridad de datos");
        return vistaError(
                HttpStatus.CONFLICT,
                "No se pudo guardar la información",
                "Puede existir un DNI, CBU u otro dato relacionado duplicado, o un registro asociado que impide el cambio. Revisá los datos e intentá nuevamente.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ModelAndView manejarPersistencia(DataAccessException excepcion) {
        LOGGER.error("Falló el acceso a la base de datos durante una solicitud web: {}",
                excepcion.getClass().getSimpleName());
        return vistaError(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No pudimos acceder a los datos",
                "La base de datos no respondió correctamente. Verificá la conexión y volvé a intentar en unos minutos.");
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            DateTimeParseException.class,
            IllegalArgumentException.class
    })
    public ModelAndView manejarParametrosInvalidos(Exception excepcion) {
        return vistaError(
                HttpStatus.BAD_REQUEST,
                "Falta un dato o tiene un formato incorrecto",
                "Revisá los campos requeridos, las fechas y los valores seleccionados. " +
                        "Por ejemplo, el período debe tener formato AAAA-MM y las fechas AAAA-MM-DD.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView manejarRecursoWebNoEncontrado(NoResourceFoundException excepcion) {
        return vistaError(
                HttpStatus.NOT_FOUND,
                "No encontramos esa página",
                "La dirección puede haber cambiado o estar escrita incorrectamente. Volvé al inicio y elegí una opción del menú.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView manejarMetodoNoPermitido(HttpRequestMethodNotSupportedException excepcion) {
        return vistaError(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Esta acción no está disponible de esa manera",
                "Volvé a la pantalla anterior y usá el botón o formulario correspondiente.");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ModelAndView manejarRutaNoEncontrada(NoHandlerFoundException excepcion) {
        return vistaError(
                HttpStatus.NOT_FOUND,
                "No encontramos esa página",
                "La dirección puede haber cambiado o estar escrita incorrectamente. Volvé al inicio y elegí una opción del menú.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView manejarEstadoHttp(ResponseStatusException excepcion) {
        HttpStatus estado = HttpStatus.valueOf(excepcion.getStatusCode().value());
        String titulo = estado.is4xxClientError()
                ? "No se pudo completar la solicitud"
                : "Ocurrió un problema con el servicio";
        String mensaje = estado.is4xxClientError()
                ? "Revisá la dirección y los datos enviados. Si el problema continúa, volvé al inicio."
                : "El servicio no pudo responder. Intentá nuevamente en unos minutos.";
        return vistaError(estado, titulo, mensaje);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView manejarErrorInesperado(Exception excepcion) {
        LOGGER.error("Error inesperado al procesar una solicitud web: {}",
                excepcion.getClass().getSimpleName());
        return vistaError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un problema inesperado",
                "No se completó la operación. Tus datos podrían no haberse guardado; revisá el resultado antes de repetirla. Si vuelve a ocurrir, contactá a soporte.");
    }

    private String describirErrorCampo(FieldError error) {
        String campo = error.getField();
        String etiqueta = switch (campo) {
            case "nombre" -> "El nombre";
            case "apellido" -> "El apellido";
            case "dni" -> "El DNI";
            case "telefono" -> "El teléfono";
            case "correo" -> "El correo";
            case "cbu" -> "El CBU";
            case "socioId" -> "El socio";
            case "clienteId" -> "El cliente";
            case "servicioDomicilioId" -> "El servicio";
            case "fecha" -> "La fecha";
            case "horaInicio" -> "La hora de inicio";
            case "horaFin" -> "La hora de fin";
            default -> "El campo " + campo;
        };
        return etiqueta + " " + error.getDefaultMessage() + ".";
    }

    private ModelAndView vistaError(HttpStatus estado, String titulo, String mensaje) {
        ModelAndView vista = new ModelAndView("error");
        vista.setStatus(estado);
        vista.addObject("codigoEstado", estado.value());
        vista.addObject("tituloError", titulo);
        vista.addObject("mensajeError", mensaje);
        return vista;
    }
}
