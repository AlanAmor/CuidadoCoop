package com.cooperativa.cuidado.web.calendario;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class CalendarioService {

    private static final int MAXIMO_DIAS_CONSULTA = 62;

    private final AsistenciaRepository asistenciaRepository;
    private final SocioRepository socioRepository;
    private final ClienteRepository clienteRepository;

    public CalendarioService(
            AsistenciaRepository asistenciaRepository,
            SocioRepository socioRepository,
            ClienteRepository clienteRepository) {
        this.asistenciaRepository = asistenciaRepository;
        this.socioRepository = socioRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public CalendarioRespuesta consultar(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new DatosInvalidosException("Seleccioná un rango de fechas válido para consultar el calendario.");
        }
        if (hasta.toEpochDay() - desde.toEpochDay() > MAXIMO_DIAS_CONSULTA) {
            throw new DatosInvalidosException("El calendario permite consultar hasta 63 días por vez.");
        }

        LocalDate hastaExclusivo = hasta.plusDays(1);
        List<Asistencia> asistencias = asistenciaRepository
                .findAllByFechaBetweenOrderByHoraInicioAsc(desde.minusDays(1), hasta);
        List<EventoCalendario> eventos = asistencias.stream()
                .map(this::convertirEvento)
                .filter(evento -> evento.inicio().isBefore(hastaExclusivo.atStartOfDay())
                        && evento.fin().isAfter(desde.atStartOfDay()))
                .toList();

        List<SocioDisponibleCalendario> sociosDisponibles = socioRepository
                .findAllByEstado(EstadoSocio.DISPONIBLE_REEMPLAZO).stream()
                .map(this::convertirSocioDisponible)
                .toList();

        Set<Long> clientesConTurno = eventos.stream()
                .map(EventoCalendario::clienteId)
                .collect(java.util.stream.Collectors.toSet());

        List<ClienteCalendario> clientesSinTurno = clienteRepository
                .findAll(Sort.by("nombrePaciente")).stream()
                .filter(cliente -> !clientesConTurno.contains(cliente.getId()))
                .map(cliente -> new ClienteCalendario(
                        cliente.getId(), cliente.getNombrePaciente(), cliente.getDomicilio()))
                .toList();

        return new CalendarioRespuesta(desde, hasta, eventos, sociosDisponibles, clientesSinTurno);
    }

    private EventoCalendario convertirEvento(Asistencia asistencia) {
        LocalDateTime inicio = LocalDateTime.of(asistencia.getFecha(), asistencia.getHoraInicio());
        LocalDate fechaFin = asistencia.getHoraFin().isBefore(asistencia.getHoraInicio())
                ? asistencia.getFecha().plusDays(1)
                : asistencia.getFecha();
        LocalDateTime fin = LocalDateTime.of(fechaFin, asistencia.getHoraFin());
        return new EventoCalendario(
                asistencia.getId(),
                asistencia.getServicioDomicilio().getId(),
                asistencia.getSocio().getId(),
                asistencia.getCliente().getId(),
                asistencia.getFecha(),
                inicio,
                fin,
                asistencia.getSocio().getApellido() + ", " + asistencia.getSocio().getNombre(),
                asistencia.getSocio().getDni(),
                asistencia.getCliente().getNombrePaciente(),
                asistencia.getCliente().getDomicilio(),
                asistencia.getServicioDomicilio().getTipoServicio().getNombre());
    }

    private SocioDisponibleCalendario convertirSocioDisponible(Socio socio) {
        return new SocioDisponibleCalendario(
                socio.getId(),
                socio.getApellido() + ", " + socio.getNombre(),
                socio.getEspecialidad().name());
    }
}
