package com.cooperativa.cuidado.web.asignacion;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.service.AsistenciaService;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.web.calendario.ClienteCalendario;
import com.cooperativa.cuidado.web.calendario.ServicioOpcionCalendario;
import com.cooperativa.cuidado.web.calendario.SocioDisponibleCalendario;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** Endpoints JSON que usa el panel de edición de turnos del calendario. */
@RestController
public class TurnoApiController {

    private final AsistenciaService asistenciaService;
    private final AsignacionService asignacionService;
    private final ServicioDomicilioRepository servicioRepository;
    private final ClienteRepository clienteRepository;

    public TurnoApiController(
            AsistenciaService asistenciaService,
            AsignacionService asignacionService,
            ServicioDomicilioRepository servicioRepository,
            ClienteRepository clienteRepository) {
        this.asistenciaService = asistenciaService;
        this.asignacionService = asignacionService;
        this.servicioRepository = servicioRepository;
        this.clienteRepository = clienteRepository;
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
                            socio.getEspecialidad().name()))
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
                        servicio.getValorHoraSocio()))
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

    private static <T> ResponseEntity<T> error(HttpStatus estado, String mensaje) {
        @SuppressWarnings("unchecked")
        T cuerpo = (T) Map.of("error", mensaje);
        return ResponseEntity.status(estado).body(cuerpo);
    }

    public record CambioTurno(
            Long socioId, Long clienteId, Long servicioId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
    }
}
