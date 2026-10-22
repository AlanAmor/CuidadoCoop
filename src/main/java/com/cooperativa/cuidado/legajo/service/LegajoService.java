package com.cooperativa.cuidado.legajo.service;

import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.legajo.ArchivoLegajo;
import com.cooperativa.cuidado.legajo.ArchivoLegajoRepository;
import com.cooperativa.cuidado.legajo.ArchivoLegajoResumen;
import com.cooperativa.cuidado.legajo.TipoLegajo;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Gestiona los archivos de los legajos. El contenido se guarda en disco, una carpeta por persona
 * ({@code <directorio>/socios/<id>} y {@code <directorio>/clientes/<id>}); la base solo guarda los datos del archivo.
 */
@Service
public class LegajoService {

    public static final long TAMANO_MAXIMO_BYTES = 10L * 1024 * 1024;
    private static final int NOMBRE_MAXIMO = 200;

    private final ArchivoLegajoRepository archivoRepository;
    private final SocioRepository socioRepository;
    private final ClienteRepository clienteRepository;
    private final Path raiz;

    public LegajoService(
            ArchivoLegajoRepository archivoRepository,
            SocioRepository socioRepository,
            ClienteRepository clienteRepository,
            @Value("${app.legajos.directorio:legajos}") String directorio) {
        this.archivoRepository = archivoRepository;
        this.socioRepository = socioRepository;
        this.clienteRepository = clienteRepository;
        this.raiz = Path.of(directorio).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<ArchivoLegajoResumen> listar(TipoLegajo tipo, Long propietarioId) {
        verificarPropietario(tipo, propietarioId);
        return archivoRepository.listarResumenes(tipo, propietarioId);
    }

    /** Valida todos los archivos antes de guardar: o se suben todos o no se sube ninguno. */
    @Transactional
    public List<ArchivoLegajoResumen> subir(TipoLegajo tipo, Long propietarioId, List<MultipartFile> archivos) {
        verificarPropietario(tipo, propietarioId);
        List<MultipartFile> recibidos = archivos == null ? List.of()
                : archivos.stream().filter(a -> a != null && !(a.isEmpty() && vacio(a.getOriginalFilename()))).toList();
        if (recibidos.isEmpty()) {
            throw new DatosInvalidosException("No se recibió ningún archivo. Arrastrá o elegí al menos un PDF o imagen.");
        }

        List<byte[]> contenidos = new ArrayList<>();
        List<ArchivoLegajo> nuevos = new ArrayList<>();
        for (MultipartFile archivo : recibidos) {
            byte[] contenido = leer(archivo);
            contenidos.add(contenido);
            nuevos.add(construir(tipo, propietarioId, archivo, contenido));
        }

        List<Path> escritos = new ArrayList<>();
        try {
            Path carpeta = carpeta(tipo, propietarioId);
            Files.createDirectories(carpeta);
            for (int i = 0; i < nuevos.size(); i++) {
                Path destino = resolver(carpeta, nuevos.get(i).getNombreAlmacenado());
                Files.write(destino, contenidos.get(i));
                escritos.add(destino);
            }
            return archivoRepository.saveAllAndFlush(nuevos).stream()
                    .map(a -> new ArchivoLegajoResumen(
                            a.getId(), a.getNombre(), a.getTipoContenido(), a.getTamano(), a.getFechaSubida()))
                    .toList();
        } catch (IOException ex) {
            borrarSilencioso(escritos);
            throw new UncheckedIOException(ex);
        } catch (RuntimeException ex) {
            borrarSilencioso(escritos);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ArchivoLegajo obtener(Long archivoId) {
        return archivoRepository.findById(archivoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el archivo con ID " + archivoId + ". Puede que ya haya sido eliminado."));
    }

    public byte[] leerContenido(ArchivoLegajo archivo) {
        Path ruta = resolver(carpeta(archivo.getTipo(), archivo.getPropietarioId()), archivo.getNombreAlmacenado());
        try {
            return Files.readAllBytes(ruta);
        } catch (IOException ex) {
            throw new RecursoNoEncontradoException("No se encontró el archivo \"" + archivo.getNombre()
                    + "\" en la carpeta del legajo. Puede que haya sido movido o borrado fuera de la aplicación.");
        }
    }

    @Transactional
    public void eliminar(Long archivoId) {
        ArchivoLegajo archivo = obtener(archivoId);
        Path ruta = resolver(carpeta(archivo.getTipo(), archivo.getPropietarioId()), archivo.getNombreAlmacenado());
        archivoRepository.delete(archivo);
        archivoRepository.flush();
        borrarSilencioso(List.of(ruta));
    }

    @Transactional
    public void eliminarLegajo(TipoLegajo tipo, Long propietarioId) {
        archivoRepository.eliminarDelPropietario(tipo, propietarioId);
        borrarCarpeta(carpeta(tipo, propietarioId));
    }

    private byte[] leer(MultipartFile archivo) {
        String nombre = limpiarNombre(archivo.getOriginalFilename());
        if (archivo.isEmpty()) {
            throw new DatosInvalidosException("El archivo \"" + nombre + "\" está vacío.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new DatosInvalidosException("El archivo \"" + nombre + "\" supera el máximo de 10 MB.");
        }
        try {
            return archivo.getBytes();
        } catch (IOException ex) {
            throw new DatosInvalidosException("No se pudo leer el archivo \"" + nombre + "\". Intentá subirlo de nuevo.");
        }
    }

    private ArchivoLegajo construir(TipoLegajo tipo, Long propietarioId, MultipartFile archivo, byte[] contenido) {
        String nombre = limpiarNombre(archivo.getOriginalFilename());
        String tipoContenido = detectarTipo(nombre, contenido);

        ArchivoLegajo nuevo = new ArchivoLegajo();
        nuevo.setTipo(tipo);
        nuevo.setPropietarioId(propietarioId);
        nuevo.setNombre(nombre);
        nuevo.setNombreAlmacenado(UUID.randomUUID().toString().substring(0, 8) + "_" + nombreSeguro(nombre));
        nuevo.setTipoContenido(tipoContenido);
        nuevo.setTamano(contenido.length);
        nuevo.setFechaSubida(LocalDateTime.now());
        return nuevo;
    }

    /** Solo se aceptan PDF y JPG/PNG; el tipo se confirma por el contenido real, no solo por la extensión. */
    private String detectarTipo(String nombre, byte[] datos) {
        String minusculas = nombre.toLowerCase(Locale.ROOT);
        if (minusculas.endsWith(".pdf") && empiezaCon(datos, '%', 'P', 'D', 'F', '-')) {
            return "application/pdf";
        }
        if (minusculas.endsWith(".png") && empiezaCon(datos, 0x89, 'P', 'N', 'G')) {
            return "image/png";
        }
        if ((minusculas.endsWith(".jpg") || minusculas.endsWith(".jpeg")) && empiezaCon(datos, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        throw new DatosInvalidosException("El archivo \"" + nombre
                + "\" no es válido. Solo se admiten PDF, JPG y PNG que no estén dañados.");
    }

    private boolean empiezaCon(byte[] datos, int... esperado) {
        if (datos.length < esperado.length) {
            return false;
        }
        for (int i = 0; i < esperado.length; i++) {
            if ((datos[i] & 0xFF) != esperado[i]) {
                return false;
            }
        }
        return true;
    }

    private String limpiarNombre(String original) {
        if (vacio(original)) {
            return "archivo";
        }
        String nombre = original.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (nombre.isEmpty()) {
            return "archivo";
        }
        if (nombre.length() > NOMBRE_MAXIMO) {
            int punto = nombre.lastIndexOf('.');
            String extension = punto >= 0 && nombre.length() - punto <= 10 ? nombre.substring(punto) : "";
            nombre = nombre.substring(0, NOMBRE_MAXIMO - extension.length()) + extension;
        }
        return nombre;
    }

    /** Nombre apto para el sistema de archivos de cualquier sistema operativo. */
    private String nombreSeguro(String nombre) {
        String seguro = nombre.replaceAll("[^\\p{L}\\p{N}._ -]", "_").replaceAll("^\\.+", "_");
        return seguro.isBlank() ? "archivo" : seguro;
    }

    private Path carpeta(TipoLegajo tipo, Long propietarioId) {
        return raiz.resolve(tipo == TipoLegajo.SOCIO ? "socios" : "clientes").resolve(String.valueOf(propietarioId));
    }

    private Path resolver(Path carpeta, String nombreAlmacenado) {
        Path ruta = carpeta.resolve(nombreAlmacenado).normalize();
        if (!ruta.startsWith(carpeta)) {
            throw new DatosInvalidosException("El nombre del archivo no es válido.");
        }
        return ruta;
    }

    private void borrarSilencioso(List<Path> rutas) {
        for (Path ruta : rutas) {
            try {
                Files.deleteIfExists(ruta);
            } catch (IOException ignorado) {
                // Un archivo huérfano en disco no afecta el funcionamiento; no se interrumpe la operación.
            }
        }
    }

    private void borrarCarpeta(Path carpeta) {
        if (!Files.isDirectory(carpeta)) {
            return;
        }
        try (Stream<Path> rutas = Files.walk(carpeta)) {
            rutas.sorted(Comparator.reverseOrder()).forEach(ruta -> borrarSilencioso(List.of(ruta)));
        } catch (IOException ignorado) {
            // Si no se puede limpiar la carpeta, los datos de la base ya fueron eliminados.
        }
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private void verificarPropietario(TipoLegajo tipo, Long id) {
        if (tipo == null || id == null) {
            throw new DatosInvalidosException("No se indicó de quién es el legajo.");
        }
        boolean existe = tipo == TipoLegajo.SOCIO ? socioRepository.existsById(id) : clienteRepository.existsById(id);
        if (!existe) {
            throw new RecursoNoEncontradoException("No existe el "
                    + (tipo == TipoLegajo.SOCIO ? "socio" : "cliente") + " con ID " + id);
        }
    }
}
