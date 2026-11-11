package com.cooperativa.cuidado.web.asignacion;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.service.AsistenciaService;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.cuenta.Permiso;
import com.cooperativa.cuidado.cuenta.UsuarioCuenta;
import com.cooperativa.cuidado.exception.AccesoDenegadoException;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.web.calendario.ClienteCalendario;
import com.cooperativa.cuidado.web.calendario.ServicioOpcionCalendario;
import com.cooperativa.cuidado.web.calendario.SocioDisponibleCalendario;
import com.cooperativa.cuidado.web.seguridad.SesionHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Endpoints JSON que usa el panel de edición de turnos del calendario. */
@RestController
public class TurnoApiController {

    private final AsistenciaService asistenciaService;
    private final AsignacionService asignacionService;
    private final ServicioDomicilioRepository servicioRepository;
    private final ClienteRepository clienteRepository;
    private final SesionHelper sesionHelper;
    private final boolean seguridadActiva;

    public TurnoApiController(
            AsistenciaService asistenciaService,
            AsignacionService asignacionService,
            ServicioDomicilioRepository servicioRepository,
            ClienteRepository clienteRepository,
            SesionHelper sesionHelper,
            @Value("${app.seguridad.activa:true}") boolean seguridadActiva) {
        this.asistenciaService = asistenciaService;
        this.asignacionService = asignacionService;
        this.servicioRepository = servicioRepository;
        this.clienteRepository = clienteRepository;
        this.sesionHelper = sesionHelper;
        this.seguridadActiva = seguridadActiva;
    }

    @GetMapping("/api/socios/disponibles")
    public ResponseEntity<?> sociosDisponibles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            @RequestParam(required = false) Long excluirAsistenciaId) {
        try {
            List<SocioDisponibleCalendario> socios = asistenciaService
                    .listarSociosDisponibles(fecha, horaInicio, horaFin, excluirAsistenciaId).stream()
                    .map(socio -> new SocioDisponibleCalendario(
                            socio.getId(),
                            socio.getApellido() + ", " + socio.getNombre(),
                            socio.getEspecialidad() != null ? socio.getEspecialidad().name() : null,
                            socio.isTieneTurnoMismoDia()))
                    .toList();
            return ResponseEntity.ok(socios);
        } catch (DatosInvalidosException ex) {
            return error(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/api/servicios/opciones")
    public List<ServicioOpcionCalendario> servicios() {
        return servicioRepository.findAllByOrderByIdAsc().stream()
                .map(servicio -> new ServicioOpcionCalendario(
                        servicio.getId(),
                        servicio.getTipoServicio().getNombre(),
                        servicio.getValorHora(),
                        servicio.getEfectivoSocioSemanaMenos12(),
                        servicio.getEfectivoClienteSemanaMas12(),
                        servicio.getEfectivoClienteFindeMenos12(),
                        servicio.getEfectivoClienteFindeMas12(),
                        servicio.getEfectivoClienteFeriadoMenos12(),
                        servicio.getEfectivoClienteFeriadoMas12(),
                        servicio.getEfectivoSocioSemanaMas12(),
                        servicio.getEfectivoSocioFindeMenos12(),
                        servicio.getEfectivoSocioFindeMas12(),
                        servicio.getEfectivoSocioFeriadoMenos12(),
                        servicio.getEfectivoSocioFeriadoMas12()))
                .toList();
    }

    @GetMapping("/api/clientes/opciones")
    public List<ClienteCalendario> clientes() {
        return clienteRepository.findAll(Sort.by("nombrePaciente")).stream()
                .map(cliente -> new ClienteCalendario(
                        cliente.getId(), cliente.getNombrePaciente(), cliente.getDomicilio()))
                .toList();
    }

    @PostMapping("/api/asistencias/{id}")
    public ResponseEntity<Map<String, String>> modificar(
            @PathVariable Long id, @RequestBody CambioTurno cambio) {
        if (cambio == null || cambio.socioId() == null || cambio.servicioId() == null
                || cambio.clienteId() == null
                || cambio.fecha() == null || cambio.horaInicio() == null || cambio.horaFin() == null) {
            return error(HttpStatus.BAD_REQUEST,
                    "Completá el socio, el cliente, el servicio, la fecha y el horario del turno antes de guardar.");
        }
        try {
            Socio socio = new Socio();
            socio.setId(cambio.socioId());
            ServicioDomicilio servicio = new ServicioDomicilio();
            servicio.setId(cambio.servicioId());
            Cliente cliente = new Cliente();
            cliente.setId(cambio.clienteId());
            Asistencia datos = new Asistencia();
            datos.setSocio(socio);
            datos.setCliente(cliente);
            datos.setServicioDomicilio(servicio);
            datos.setFecha(cambio.fecha());
            datos.setHoraInicio(cambio.horaInicio());
            datos.setHoraFin(cambio.horaFin());
            datos.setFeriadoNacional(cambio.feriadoNacional());
            datos.setImporteViatico(cambio.importeViatico());
            datos.setImporteSanatorio(cambio.importeSanatorio());
            asignacionService.modificar(id, datos);
            return ResponseEntity.ok(Map.of("mensaje", "El turno se actualizó correctamente."));
        } catch (RecursoNoEncontradoException ex) {
            return error(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (ReglaNegocioException ex) {
            return error(HttpStatus.CONFLICT, ex.getMessage());
        } catch (DatosInvalidosException ex) {
            return error(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/api/asistencias/{id}/adicionales")
    public ResponseEntity<Map<String, String>> actualizarAdicionales(
            @PathVariable Long id, @RequestBody(required = false) AdicionalesTurno adicionales) {
        try {
            BigDecimal viatico = adicionales != null ? adicionales.importeViatico() : BigDecimal.ZERO;
            BigDecimal sanatorio = adicionales != null ? adicionales.importeSanatorio() : BigDecimal.ZERO;
            asistenciaService.actualizarAdicionales(id, viatico, sanatorio);
            return ResponseEntity.ok(Map.of("mensaje", "Viático y sanatorio del turno actualizados correctamente."));
        } catch (RecursoNoEncontradoException ex) {
            return error(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (DatosInvalidosException ex) {
            return error(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/api/asistencias/{id}/marcacion")
    public ResponseEntity<Map<String, Object>> registrarMarcacion(
            @PathVariable Long id,
            @RequestBody(required = false) MarcacionTurnoRequest solicitud,
            HttpServletRequest request) {
        Optional<UsuarioCuenta> usuarioOpt = sesionHelper.obtenerUsuarioActual(request);
        if (usuarioOpt.isEmpty() && seguridadActiva) {
            return error(HttpStatus.UNAUTHORIZED, "Tu sesión expiró. Iniciá sesión nuevamente.");
        }
        try {
            boolean puedeGestionarMarcaciones = usuarioOpt
                    .map(u -> u.esAdmin() || u.tienePermiso(Permiso.GESTIONAR_MARCACIONES_TURNOS))
                    .orElse(!seguridadActiva);
            String accion = solicitud != null ? solicitud.accion() : null;
            String aclaracion = solicitud != null ? solicitud.aclaracionTurno() : null;
            Asistencia actualizada;
            if (puedeGestionarMarcaciones) {
                actualizada = asistenciaService.registrarMarcacionAdmin(
                        id,
                        accion,
                        solicitud != null ? solicitud.marcacionEntrada() : null,
                        solicitud != null ? solicitud.marcacionSalida() : null,
                        aclaracion,
                        LocalDateTime.now());
            } else {
                actualizada = asistenciaService.registrarMarcacionSocio(
                        id,
                        usuarioOpt.get().getDni(),
                        accion,
                        aclaracion,
                        LocalDateTime.now());
            }

            String msg = switch (accion != null ? accion.trim().toUpperCase() : "") {
                case "ENTRADA" -> "Entrada del turno registrada correctamente.";
                case "SALIDA" -> "Salida del turno registrada correctamente.";
                case "ACLARACION" -> "Aclaración del turno guardada correctamente.";
                default -> "Registro de entrada, salida y aclaración actualizado correctamente.";
            };
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("mensaje", msg);
            cuerpo.put("marcacionEntrada", actualizada.getMarcacionEntrada());
            cuerpo.put("marcacionSalida", actualizada.getMarcacionSalida());
            cuerpo.put("aclaracionTurno", actualizada.getAclaracionTurno());
            return ResponseEntity.ok(cuerpo);
        } catch (AccesoDenegadoException ex) {
            return error(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (RecursoNoEncontradoException ex) {
            return error(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (ReglaNegocioException ex) {
            return error(HttpStatus.CONFLICT, ex.getMessage());
        } catch (DatosInvalidosException ex) {
            return error(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private static <T> ResponseEntity<T> error(HttpStatus estado, String mensaje) {
        @SuppressWarnings("unchecked")
        T cuerpo = (T) Map.of("error", mensaje);
        return ResponseEntity.status(estado).body(cuerpo);
    }

    public record CambioTurno(
            Long socioId,
            Long clienteId,
            Long servicioId,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            BigDecimal importeViatico,
            BigDecimal importeSanatorio,
            Boolean feriadoNacional) {
    }

    public record AdicionalesTurno(BigDecimal importeViatico, BigDecimal importeSanatorio) {
    }

    public record MarcacionTurnoRequest(
            String accion,
            LocalDateTime marcacionEntrada,
            LocalDateTime marcacionSalida,
            String aclaracionTurno) {
    }
}
