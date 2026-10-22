package com.cooperativa.cuidado.web.legajo;

import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.legajo.ArchivoLegajo;
import com.cooperativa.cuidado.legajo.ArchivoLegajoResumen;
import com.cooperativa.cuidado.legajo.TipoLegajo;
import com.cooperativa.cuidado.legajo.service.LegajoService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/** Endpoints JSON del legajo de socios y clientes (listar, subir, ver y eliminar archivos). */
@RestController
public class LegajoApiController {

    private final LegajoService legajoService;

    public LegajoApiController(LegajoService legajoService) {
        this.legajoService = legajoService;
    }

    @GetMapping("/api/legajos/{tipo}/{id}")
    public ResponseEntity<?> listar(@PathVariable String tipo, @PathVariable Long id) {
        TipoLegajo tipoLegajo = TipoLegajo.desdeRuta(tipo);
        if (tipoLegajo == null) {
            return tipoInvalido();
        }
        List<ArchivoLegajoResumen> archivos = legajoService.listar(tipoLegajo, id);
        return ResponseEntity.ok(archivos);
    }

    @PostMapping("/api/legajos/{tipo}/{id}")
    public ResponseEntity<?> subir(
            @PathVariable String tipo,
            @PathVariable Long id,
            @RequestParam(name = "archivos", required = false) List<MultipartFile> archivos) {
        TipoLegajo tipoLegajo = TipoLegajo.desdeRuta(tipo);
        if (tipoLegajo == null) {
            return tipoInvalido();
        }
        List<ArchivoLegajoResumen> subidos = legajoService.subir(tipoLegajo, id, archivos);
        String mensaje = subidos.size() == 1
                ? "Se agregó 1 archivo al legajo."
                : "Se agregaron " + subidos.size() + " archivos al legajo.";
        return ResponseEntity.ok(Map.of("mensaje", mensaje, "archivos", subidos));
    }

    @GetMapping("/legajos/archivos/{archivoId}")
    public ResponseEntity<byte[]> ver(
            @PathVariable Long archivoId, @RequestParam(defaultValue = "false") boolean descargar) {
        ArchivoLegajo archivo = legajoService.obtener(archivoId);
        byte[] contenido = legajoService.leerContenido(archivo);
        ContentDisposition disposicion = (descargar ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(archivo.getNombre(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.getTipoContenido()))
                .contentLength(contenido.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(contenido);
    }

    @DeleteMapping("/api/legajos/archivos/{archivoId}")
    public ResponseEntity<?> eliminar(@PathVariable Long archivoId) {
        legajoService.eliminar(archivoId);
        return ResponseEntity.ok(Map.of("mensaje", "El archivo se eliminó del legajo."));
    }

    @ExceptionHandler(DatosInvalidosException.class)
    ResponseEntity<Map<String, String>> datosInvalidos(DatosInvalidosException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<Map<String, String>> noEncontrado(RecursoNoEncontradoException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> demasiadoGrande(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo es demasiado grande. Cada archivo puede pesar hasta 10 MB.");
    }

    @ExceptionHandler(UncheckedIOException.class)
    ResponseEntity<Map<String, String>> errorDeDisco(UncheckedIOException ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo guardar el archivo en la carpeta del legajo. Verificá que haya espacio en disco y permisos de escritura, e intentá de nuevo.");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String, String>> errorDeBase(DataAccessException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo registrar el archivo en la base de datos. Intentá de nuevo en unos instantes.");
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<Map<String, String>> subidaInvalida(MultipartException ex) {
        return error(HttpStatus.BAD_REQUEST,
                "No se pudo recibir el archivo. Verificá tu conexión e intentá subirlo nuevamente.");
    }

    private ResponseEntity<?> tipoInvalido() {
        return error(HttpStatus.NOT_FOUND, "El legajo solicitado no existe.");
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }
}
