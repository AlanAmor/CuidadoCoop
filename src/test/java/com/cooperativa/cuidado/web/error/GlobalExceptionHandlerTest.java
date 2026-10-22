package com.cooperativa.cuidado.web.error;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void informaCuandoLaBaseDeDatosNoRespondeSinExponerDetallesTecnicos() {
        ModelAndView respuesta = handler.manejarPersistencia(
                new DataAccessResourceFailureException("detalle técnico confidencial"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, respuesta.getStatus());
        assertEquals("No pudimos acceder a los datos", respuesta.getModel().get("tituloError"));
        String mensaje = (String) respuesta.getModel().get("mensajeError");
        assertTrue(mensaje.contains("Verificá la conexión"));
        assertFalse(mensaje.contains("detalle técnico confidencial"));
    }

    @Test
    void informaConflictosDeIntegridadComoDatosDuplicadosORelacionados() {
        ModelAndView respuesta = handler.manejarIntegridadDeDatos(
                new DataIntegrityViolationException("detalle técnico confidencial"));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatus());
        assertEquals("No se pudo guardar la información", respuesta.getModel().get("tituloError"));
        String mensaje = (String) respuesta.getModel().get("mensajeError");
        assertTrue(mensaje.contains("dato relacionado duplicado"));
        assertFalse(mensaje.contains("detalle técnico confidencial"));
    }

    @Test
    void errorInesperadoIndicaQueLaOperacionPuedeNoHaberseGuardado() {
        ModelAndView respuesta = handler.manejarErrorInesperado(
                new IllegalStateException("detalle técnico confidencial"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatus());
        assertEquals("Ocurrió un problema inesperado", respuesta.getModel().get("tituloError"));
        String mensaje = (String) respuesta.getModel().get("mensajeError");
        assertTrue(mensaje.contains("podrían no haberse guardado"));
        assertFalse(mensaje.contains("detalle técnico confidencial"));
    }
}
