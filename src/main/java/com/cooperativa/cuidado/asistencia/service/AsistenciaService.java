package com.cooperativa.cuidado.asistencia.service;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.exception.AccesoDenegadoException;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.feriado.FeriadoNacionalRepository;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
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
    private final FeriadoNacionalRepository feriadoRepository;

    public AsistenciaService(
            AsistenciaRepository asistenciaRepository,
            SocioRepository socioRepository,
            ServicioDomicilioRepository servicioDomicilioRepository,
            ClienteRepository clienteRepository,
            Validator validator) {
        this(asistenciaRepository, socioRepository, servicioDomicilioRepository, clienteRepository, validator, null);
    }

    @Autowired
    public AsistenciaService(
            AsistenciaRepository asistenciaRepository,
            SocioRepository socioRepository,
            ServicioDomicilioRepository servicioDomicilioRepository,
            ClienteRepository clienteRepository,
            Validator validator,
            FeriadoNacionalRepository feriadoRepository) {
        this.asistenciaRepository = asistenciaRepository;
        this.socioRepository = socioRepository;
        this.servicioDomicilioRepository = servicioDomicilioRepository;
        this.clienteRepository = clienteRepository;
        this.validator = validator;
        this.feriadoRepository = feriadoRepository;
    }

    @Transactional
    public Asistencia registrar(Asistencia asistencia) {
        asistencia.setId(null);
        cargarReferenciasBloqueadas(asistencia);
        if (asistencia.getFeriadoNacional() == null) {
            asistencia.setFeriadoNacional(esFeriadoRegistrado(asistencia.getFecha()));
        }
        asistencia.setImporteViatico(normalizarAdicional(asistencia.getImporteViatico(), "viático"));
        asistencia.setImporteSanatorio(normalizarAdicional(asistencia.getImporteSanatorio(), "sanatorio"));
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
        boolean cambiaFecha = !Objects.equals(existente.getFecha(), datos.getFecha());
        boolean cambiaHorario = !Objects.equals(existente.getHoraInicio(), datos.getHoraInicio())
                || !Objects.equals(existente.getHoraFin(), datos.getHoraFin());
        boolean cambiaSocioOFecha = !Objects.equals(existente.getSocio().getId(), datos.getSocio().getId())
                || cambiaFecha;
        boolean nuevoFeriado = datos.getFeriadoNacional() != null
                ? datos.getFeriadoNacional()
                : (cambiaFecha ? esFeriadoRegistrado(datos.getFecha()) : existente.isFeriadoNacionalEfectivo());
        boolean cambiaFeriado = existente.isFeriadoNacionalEfectivo() != nuevoFeriado;

        existente.setSocio(datos.getSocio());
        existente.setCliente(datos.getCliente());
        existente.setServicioDomicilio(datos.getServicioDomicilio());
        existente.setFecha(datos.getFecha());
        existente.setHoraInicio(datos.getHoraInicio());
        existente.setHoraFin(datos.getHoraFin());
        existente.setFeriadoNacional(nuevoFeriado);
        if (datos.getImporteViatico() != null) {
            existente.setImporteViatico(normalizarAdicional(datos.getImporteViatico(), "viático"));
        }
        if (datos.getImporteSanatorio() != null) {
            existente.setImporteSanatorio(normalizarAdicional(datos.getImporteSanatorio(), "sanatorio"));
        }
        if (cambiaServicio || cambiaFecha || cambiaHorario || cambiaFeriado) {
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

    /** Permite cargar o editar viáticos y sanatorio de cualquier turno (incluso si ya finalizó). */
    @Transactional
    public Asistencia actualizarAdicionales(Long id, BigDecimal importeViatico, BigDecimal importeSanatorio) {
        Asistencia existente = asistenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El turno seleccionado ya no existe. Actualizá el calendario."));
        existente.setImporteViatico(normalizarAdicional(importeViatico, "viático"));
        existente.setImporteSanatorio(normalizarAdicional(importeSanatorio, "sanatorio"));
        return asistenciaRepository.save(existente);
    }

    /**
     * Registra la entrada, salida o aclaración por parte del propio socio asignado al turno.
     * Solo se permite el día operativo del turno.
     */
    @Transactional
    public Asistencia registrarMarcacionSocio(
            Long id,
            String dniSocio,
            String accion,
            String aclaracion,
            LocalDateTime momentoActual) {
        Asistencia existente = asistenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El turno seleccionado ya no existe. Actualizá el calendario."));

        if (dniSocio == null || !dniSocio.equals(existente.getSocio().getDni())) {
            throw new AccesoDenegadoException(
                    "Solo el socio asignado al turno o un administrador pueden registrar entrada, salida o aclaraciones.");
        }

        LocalDateTime ahora = (momentoActual != null ? momentoActual : LocalDateTime.now())
                .truncatedTo(ChronoUnit.MINUTES);
        if (!existente.esDiaDelTurno(ahora.toLocalDate())) {
            throw new ReglaNegocioException(
                    "Las entradas, salidas y aclaraciones solo se pueden marcar el día del turno. Solo el administrador puede registrarlas en cualquier momento.");
        }

        String tipoAccion = accion == null ? "" : accion.trim().toUpperCase(Locale.ROOT);
        switch (tipoAccion) {
            case "ENTRADA" -> {
                if (existente.getMarcacionEntrada() != null) {
                    throw new ReglaNegocioException(
                            "La entrada de este turno ya fue registrada. Si necesitás corregirla, contactá al administrador.");
                }
                existente.setMarcacionEntrada(ahora);
                if (aclaracion != null) {
                    existente.setAclaracionTurno(normalizarAclaracion(aclaracion));
                }
            }
            case "SALIDA" -> {
                if (existente.getMarcacionSalida() != null) {
                    throw new ReglaNegocioException(
                            "La salida de este turno ya fue registrada. Si necesitás corregirla, contactá al administrador.");
                }
                LocalDateTime salida = ahora;
                if (existente.getMarcacionEntrada() != null && salida.isBefore(existente.getMarcacionEntrada())) {
                    salida = existente.getMarcacionEntrada();
                }
                existente.setMarcacionSalida(salida);
                if (aclaracion != null) {
                    existente.setAclaracionTurno(normalizarAclaracion(aclaracion));
                }
            }
            case "ACLARACION" -> existente.setAclaracionTurno(normalizarAclaracion(aclaracion));
            default -> throw new DatosInvalidosException(
                    "Seleccioná una acción válida (entrada, salida o aclaración).");
        }

        validarDatos(existente);
        return asistenciaRepository.save(existente);
    }

    /**
     * Permite al administrador registrar, corregir o limpiar entradas, salidas y aclaraciones
     * en cualquier turno y en cualquier momento.
     */
    @Transactional
    public Asistencia registrarMarcacionAdmin(
            Long id,
            String accion,
            LocalDateTime marcacionEntrada,
            LocalDateTime marcacionSalida,
            String aclaracion,
            LocalDateTime momentoActual) {
        Asistencia existente = asistenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El turno seleccionado ya no existe. Actualizá el calendario."));

        LocalDateTime ahora = (momentoActual != null ? momentoActual : LocalDateTime.now())
                .truncatedTo(ChronoUnit.MINUTES);
        String tipoAccion = accion == null || accion.isBlank()
                ? "ADMIN_GUARDAR"
                : accion.trim().toUpperCase(Locale.ROOT);

        switch (tipoAccion) {
            case "ENTRADA" -> {
                existente.setMarcacionEntrada(
                        marcacionEntrada != null ? marcacionEntrada.truncatedTo(ChronoUnit.MINUTES) : ahora);
                if (aclaracion != null) {
                    existente.setAclaracionTurno(normalizarAclaracion(aclaracion));
                }
            }
            case "SALIDA" -> {
                existente.setMarcacionSalida(
                        marcacionSalida != null ? marcacionSalida.truncatedTo(ChronoUnit.MINUTES) : ahora);
                if (aclaracion != null) {
                    existente.setAclaracionTurno(normalizarAclaracion(aclaracion));
                }
            }
            case "ACLARACION" -> existente.setAclaracionTurno(normalizarAclaracion(aclaracion));
            case "ADMIN_GUARDAR" -> {
                existente.setMarcacionEntrada(
                        marcacionEntrada != null ? marcacionEntrada.truncatedTo(ChronoUnit.MINUTES) : null);
                existente.setMarcacionSalida(
                        marcacionSalida != null ? marcacionSalida.truncatedTo(ChronoUnit.MINUTES) : null);
                existente.setAclaracionTurno(normalizarAclaracion(aclaracion));
            }
            default -> throw new DatosInvalidosException(
                    "Seleccioná una acción válida para el registro de asistencia.");
        }

        if (existente.getMarcacionEntrada() != null
                && existente.getMarcacionSalida() != null
                && existente.getMarcacionSalida().isBefore(existente.getMarcacionEntrada())) {
            throw new DatosInvalidosException(
                    "La fecha y hora de salida no puede ser anterior a la de entrada.");
        }

        validarDatos(existente);
        return asistenciaRepository.save(existente);
    }

    private boolean esFeriadoRegistrado(LocalDate fecha) {
        return feriadoRepository != null && fecha != null && feriadoRepository.existsByFecha(fecha);
    }

    private String normalizarAclaracion(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.length() > 1000) {
            throw new DatosInvalidosException("La aclaración del turno no puede superar los 1000 caracteres.");
        }
        return limpio;
    }

    private BigDecimal normalizarAdicional(BigDecimal monto, String etiqueta) {
        if (monto == null) {
            return null;
        }
        if (monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new DatosInvalidosException("El importe de " + etiqueta + " no puede ser negativo.");
        }
        if (monto.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return monto.setScale(2, RoundingMode.HALF_UP);
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
