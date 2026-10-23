package com.cooperativa.cuidado.asistencia.service;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final SocioRepository socioRepository;
    private final ServicioDomicilioRepository servicioDomicilioRepository;
    private final ClienteRepository clienteRepository;
    private final Validator validator;

    public AsistenciaService(
            AsistenciaRepository asistenciaRepository,
            SocioRepository socioRepository,
            ServicioDomicilioRepository servicioDomicilioRepository,
            ClienteRepository clienteRepository,
            Validator validator) {
        this.asistenciaRepository = asistenciaRepository;
        this.socioRepository = socioRepository;
        this.servicioDomicilioRepository = servicioDomicilioRepository;
        this.clienteRepository = clienteRepository;
        this.validator = validator;
    }

    @Transactional
    public Asistencia registrar(Asistencia asistencia) {
        asistencia.setId(null);
        cargarReferenciasBloqueadas(asistencia);
        asistencia.calcularHorasTotales();
        asistencia.capturarValorHora();
        validarDatos(asistencia);
        validarDiaDisponible(asistencia);
        validarSolapamiento(asistencia, null);
        return asistenciaRepository.save(asistencia);
    }

    @Transactional
    public void eliminar(Long id) {
        Asistencia asistencia = asistenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El turno que querés eliminar ya no existe. Actualizá el calendario."));
        asistenciaRepository.delete(asistencia);
    }

    @Transactional(readOnly = true)
    public List<SocioDisponible> listarSociosDisponibles(
            LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, Long excluirAsistenciaId) {
        if (fecha == null) {
            throw new DatosInvalidosException("Indicá el día y el horario del servicio para ver los socios disponibles.");
        }
        return disponiblesEn(List.of(fecha), horaInicio, horaFin, excluirAsistenciaId);
    }

    /** Socios libres en TODAS las fechas indicadas (para cuidados fijos que se repiten). */
    @Transactional(readOnly = true)
    public List<SocioDisponible> listarSociosDisponiblesEnFechas(
            List<LocalDate> fechas, LocalTime horaInicio, LocalTime horaFin) {
        if (fechas == null || fechas.isEmpty()) {
            throw new DatosInvalidosException("Indicá el día y el horario del servicio para ver los socios disponibles.");
        }
        return disponiblesEn(fechas, horaInicio, horaFin, null);
    }

    private List<SocioDisponible> disponiblesEn(
            List<LocalDate> fechas, LocalTime horaInicio, LocalTime horaFin, Long excluirAsistenciaId) {
        if (horaInicio == null || horaFin == null) {
            throw new DatosInvalidosException("Indicá el día y el horario del servicio para ver los socios disponibles.");
        }
        if (horaInicio.equals(horaFin)) {
            throw new DatosInvalidosException("La hora de fin debe ser distinta de la hora de inicio.");
        }
        LocalDate primera = fechas.stream().min(Comparator.naturalOrder()).orElseThrow();
        LocalDate ultima = fechas.stream().max(Comparator.naturalOrder()).orElseThrow();
        List<Asistencia> existentes = asistenciaRepository
                .findAllByFechaBetweenOrderByHoraInicioAsc(primera.minusDays(1), ultima.plusDays(1)).stream()
                .filter(a -> !Objects.equals(a.getId(), excluirAsistenciaId))
                .toList();

        Set<Long> ocupados = new HashSet<>();
        Set<Long> conTurnoMismoDia = new HashSet<>();
        for (LocalDate fecha : fechas) {
            LocalDateTime inicio = inicioDe(fecha, horaInicio);
            LocalDateTime fin = finDe(fecha, horaInicio, horaFin);
            for (Asistencia a : existentes) {
                LocalDateTime inicioA = inicioDe(a.getFecha(), a.getHoraInicio());
                LocalDateTime finA = finDe(a.getFecha(), a.getHoraInicio(), a.getHoraFin());
                if (inicio.isBefore(finA) && fin.isAfter(inicioA)) {
                    ocupados.add(a.getSocio().getId());
                } else if (compartenDia(inicio, fin, inicioA, finA)) {
                    conTurnoMismoDia.add(a.getSocio().getId());
                }
            }
        }

        return socioRepository.findAll(Sort.by("apellido", "nombre")).stream()
                .filter(socio -> socio.getEstado() != EstadoSocio.INACTIVO)
                .filter(socio -> fechas.stream().allMatch(f -> socio.trabajaEl(f.getDayOfWeek())))
                .filter(socio -> !ocupados.contains(socio.getId()))
                .map(socio -> new SocioDisponible(socio, conTurnoMismoDia.contains(socio.getId())))
                .toList();
    }

    private static boolean compartenDia(
            LocalDateTime inicio1, LocalDateTime fin1,
            LocalDateTime inicio2, LocalDateTime fin2) {
        LocalDate d1 = inicio1.toLocalDate();
        if (tocaDia(inicio2, fin2, d1)) {
            return true;
        }
        LocalDate d2 = fin1.toLocalDate();
        return !d2.equals(d1) && fin1.isAfter(d2.atStartOfDay()) && tocaDia(inicio2, fin2, d2);
    }

    private static boolean tocaDia(LocalDateTime inicio, LocalDateTime fin, LocalDate dia) {
        return inicio.isBefore(dia.plusDays(1).atStartOfDay()) && fin.isAfter(dia.atStartOfDay());
    }

    @Transactional
    public Asistencia actualizar(Long id, Asistencia datos) {
        Asistencia existente = asistenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El turno que querés modificar ya no existe. Actualizá el calendario."));
        cargarReferenciasBloqueadas(datos);

        boolean cambiaServicio = !Objects.equals(
                existente.getServicioDomicilio().getId(), datos.getServicioDomicilio().getId());
        boolean cambiaSocioOFecha = !Objects.equals(existente.getSocio().getId(), datos.getSocio().getId())
                || !Objects.equals(existente.getFecha(), datos.getFecha());
        existente.setSocio(datos.getSocio());
        existente.setCliente(datos.getCliente());
        existente.setServicioDomicilio(datos.getServicioDomicilio());
        existente.setFecha(datos.getFecha());
        existente.setHoraInicio(datos.getHoraInicio());
        existente.setHoraFin(datos.getHoraFin());
        if (cambiaServicio) {
            existente.setValorHoraAplicado(null);
            existente.setValorHoraSocioAplicado(null);
        }
        existente.calcularHorasTotales();
        validarDatos(existente);
        if (cambiaSocioOFecha) {
            validarDiaDisponible(existente);
        }
        validarSolapamiento(existente, id);
        return asistenciaRepository.save(existente);
    }

    private void cargarReferenciasBloqueadas(Asistencia asistencia) {
        if (asistencia.getSocio() == null || asistencia.getSocio().getId() == null) {
            throw new RecursoNoEncontradoException("El socio asociado a la asistencia no existe");
        }
        Socio socio = socioRepository.findByIdForUpdate(asistencia.getSocio().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El socio asociado a la asistencia no existe"));

        if (asistencia.getServicioDomicilio() == null || asistencia.getServicioDomicilio().getId() == null) {
            throw new RecursoNoEncontradoException("El servicio domiciliario asociado no existe");
        }
        ServicioDomicilio servicio = servicioDomicilioRepository.findById(
                        asistencia.getServicioDomicilio().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El servicio domiciliario asociado no existe"));
        asistencia.setSocio(socio);
        asistencia.setServicioDomicilio(servicio);

        if (asistencia.getCliente() == null || asistencia.getCliente().getId() == null) {
            throw new RecursoNoEncontradoException("Seleccioná el cliente al que se le presta el servicio.");
        }
        asistencia.setCliente(clienteRepository.findById(asistencia.getCliente().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El cliente seleccionado ya no existe. Actualizá la lista e intentá nuevamente.")));
    }

    private void validarDatos(Asistencia asistencia) {
        Set<ConstraintViolation<Asistencia>> violaciones = validator.validate(asistencia);
        if (!violaciones.isEmpty()) {
            String errores = violaciones.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new DatosInvalidosException(errores);
        }
    }

    private static LocalDateTime inicioDe(LocalDate fecha, LocalTime horaInicio) {
        return LocalDateTime.of(fecha, horaInicio);
    }

    private static LocalDateTime finDe(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        return LocalDateTime.of(horaFin.isBefore(horaInicio) ? fecha.plusDays(1) : fecha, horaFin);
    }

    private void validarDiaDisponible(Asistencia asistencia) {
        DayOfWeek dia = asistencia.getFecha().getDayOfWeek();
        if (!asistencia.getSocio().trabajaEl(dia)) {
            throw new ReglaNegocioException("El socio " + asistencia.getSocio().getApellido() + ", "
                    + asistencia.getSocio().getNombre() + " no tiene marcado como disponible el día "
                    + dia.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"))
                    + ". Podés cambiar sus días disponibles desde la pestaña Socios");
        }
    }

    private void validarSolapamiento(Asistencia asistencia, Long excluirId) {
        LocalDate fecha = asistencia.getFecha();
        LocalDateTime inicio = inicioDe(fecha, asistencia.getHoraInicio());
        LocalDateTime fin = finDe(fecha, asistencia.getHoraInicio(), asistencia.getHoraFin());

        if (asistenciaRepository.existeSolapamiento(
                asistencia.getSocio().getId(), inicio, fin, excluirId)) {
            throw new ReglaNegocioException("El socio ya tiene una asistencia asignada en ese horario");
        }
    }
}
