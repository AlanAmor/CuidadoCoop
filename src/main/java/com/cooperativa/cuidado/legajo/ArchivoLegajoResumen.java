package com.cooperativa.cuidado.legajo;

import java.time.LocalDateTime;

/** Datos de un archivo del legajo sin su contenido, para listarlo con rapidez. */
public record ArchivoLegajoResumen(
        Long id, String nombre, String tipoContenido, long tamano, LocalDateTime fechaSubida) {
}
