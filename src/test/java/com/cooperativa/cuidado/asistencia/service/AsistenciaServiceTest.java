package com.cooperativa.cuidado.asistencia.service;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cooperativa.cuidado.socio.EstadoSocio;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsistenciaServiceTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private final Validator validator = VALIDATOR_FACTORY.getValidator();
    private final AsistenciaRepository asistenciaRepository = mock(AsistenciaRepository.class);
    private final SocioRepository socioRepository = mock(SocioRepository.class);
    private final ServicioDomicilioRepository servicioRepository = mock(ServicioDomicilioRepository.class);
    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final AsistenciaService asistenciaService = new AsistenciaService(
            asistenciaRepository, socioRepository, servicioRepository, clienteRepository, validator);

    @AfterAll
    static void cerrarValidador() {
        VALIDATOR_FACTORY.close();
    }

    @BeforeEach
    void configurarReferencias() {
        Socio socio = new Socio();
        socio.setId(10L);
        ServicioDomicilio servicio = new ServicioDomicilio();
        servicio.setId(20L);
        servicio.setValorHora(new BigDecimal("1500.00"));
        servicio.setValorHoraSocio(new BigDecimal("1000.00"));

        when(socioRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(socio));
        when(servicioRepository.findById(20L)).thenReturn(Optional.of(servicio));
        Cliente cliente = new Cliente();
        cliente.setId(30L);
        when(clienteRepository.findById(30L)).thenReturn(Optional.of(cliente));
    }

    @Test
    void registraTurnoNocturnoYGuardaValorHoraHistorico() {
        when(asistenciaRepository.existeSolapamiento(any(), any(), any(), any())).thenReturn(false);
        when(asistenciaRepository.save(any(Asistencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Asistencia guardada = asistenciaService.registrar(nuevaAsistencia());

        assertEquals(new BigDecimal("7.75"), guardada.getHorasTotales());
        assertEquals(new BigDecimal("1500.00"), guardada.getValorHoraAplicado());
        verify(asistenciaRepository).existeSolapamiento(
                eq(10L),
                eq(LocalDate.of(2026, 10, 21).atTime(22, 30)),
                eq(LocalDate.of(2026, 10, 22).atTime(6, 15)),
                eq(null));
    }

    @Test
    void rechazaYNoGuardaUnTurnoSolapado() {
        when(asistenciaRepository.existeSolapamiento(any(), any(), any(), any())).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> asistenciaService.registrar(nuevaAsistencia()));

        verify(asistenciaRepository, never()).save(any(Asistencia.class));
    }

    @Test
    void listarSociosDisponiblesMarcaTurnoMismoDiaSiNoSeSolapa() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);

        Socio socio2 = new Socio();
        socio2.setId(11L);
        socio2.setNombre("Carlos");
        socio2.setApellido("Lopez");
        socio2.setEstado(EstadoSocio.DISPONIBLE_REEMPLAZO);

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1, socio2));

        Asistencia turnoExistente = new Asistencia();
        turnoExistente.setId(100L);
        turnoExistente.setSocio(socio1);
        turnoExistente.setFecha(LocalDate.of(2026, 10, 21));
        turnoExistente.setHoraInicio(LocalTime.of(8, 0));
        turnoExistente.setHoraFin(LocalTime.of(12, 0));

        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of(turnoExistente));

        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponibles(
                LocalDate.of(2026, 10, 21), LocalTime.of(14, 0), LocalTime.of(18, 0), null);

        assertEquals(2, disponibles.size());
        SocioDisponible d1 = disponibles.stream().filter(d -> d.getId().equals(10L)).findFirst().orElseThrow();
        SocioDisponible d2 = disponibles.stream().filter(d -> d.getId().equals(11L)).findFirst().orElseThrow();

        assertTrue(d1.isTieneTurnoMismoDia(), "El socio con turno previo en el mismo día debe tener tieneTurnoMismoDia=true");
        assertFalse(d2.isTieneTurnoMismoDia(), "El socio sin turnos ese día debe tener tieneTurnoMismoDia=false");
    }

    @Test
    void listarSociosDisponiblesExcluyeSociosConTurnoSolapado() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);

        Socio socio2 = new Socio();
        socio2.setId(11L);
        socio2.setNombre("Carlos");
        socio2.setApellido("Lopez");
        socio2.setEstado(EstadoSocio.DISPONIBLE_REEMPLAZO);

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1, socio2));

        // Turno que se solapa con 14:00 - 18:00
        Asistencia turnoSolapado = new Asistencia();
        turnoSolapado.setId(100L);
        turnoSolapado.setSocio(socio1);
        turnoSolapado.setFecha(LocalDate.of(2026, 10, 21));
        turnoSolapado.setHoraInicio(LocalTime.of(13, 0));
        turnoSolapado.setHoraFin(LocalTime.of(15, 0));

        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of(turnoSolapado));

        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponibles(
                LocalDate.of(2026, 10, 21), LocalTime.of(14, 0), LocalTime.of(18, 0), null);

        assertEquals(1, disponibles.size());
        assertEquals(11L, disponibles.get(0).getId());
        assertFalse(disponibles.get(0).isTieneTurnoMismoDia());
    }

    @Test
    void listarSociosDisponiblesIgnoraElTurnoSiEsExcluirAsistenciaId() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1));

        Asistencia turnoSiendoEditado = new Asistencia();
        turnoSiendoEditado.setId(100L);
        turnoSiendoEditado.setSocio(socio1);
        turnoSiendoEditado.setFecha(LocalDate.of(2026, 10, 21));
        turnoSiendoEditado.setHoraInicio(LocalTime.of(14, 0));
        turnoSiendoEditado.setHoraFin(LocalTime.of(18, 0));

        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of(turnoSiendoEditado));

        // Al excluir el turno que se está editando, no debe excluirse al socio ni marcar tieneTurnoMismoDia
        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponibles(
                LocalDate.of(2026, 10, 21), LocalTime.of(14, 0), LocalTime.of(18, 0), 100L);

        assertEquals(1, disponibles.size());
        assertEquals(10L, disponibles.get(0).getId());
        assertFalse(disponibles.get(0).isTieneTurnoMismoDia());
    }

    @Test
    void listarSociosDisponiblesDetectaTurnoNocturnoQueCruzaElDia() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1));

        // Turno nocturno del día anterior 20/10 22:00 a 21/10 06:00
        Asistencia turnoNocturno = new Asistencia();
        turnoNocturno.setId(100L);
        turnoNocturno.setSocio(socio1);
        turnoNocturno.setFecha(LocalDate.of(2026, 10, 20));
        turnoNocturno.setHoraInicio(LocalTime.of(22, 0));
        turnoNocturno.setHoraFin(LocalTime.of(6, 0));

        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of(turnoNocturno));

        // Consultamos el día 21/10 de 14:00 a 18:00 (no se solapa, pero sí prestó servicio el 21)
        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponibles(
                LocalDate.of(2026, 10, 21), LocalTime.of(14, 0), LocalTime.of(18, 0), null);

        assertEquals(1, disponibles.size());
        assertTrue(disponibles.get(0).isTieneTurnoMismoDia());
    }

    @Test
    void listarSociosDisponiblesEnFechasDetectaTurnoEnCualquieraDeLasFechas() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1));

        // Turno no solapado el 22/10
        Asistencia turno = new Asistencia();
        turno.setId(100L);
        turno.setSocio(socio1);
        turno.setFecha(LocalDate.of(2026, 10, 22));
        turno.setHoraInicio(LocalTime.of(8, 0));
        turno.setHoraFin(LocalTime.of(12, 0));

        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of(turno));

        List<LocalDate> fechasSerie = List.of(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 22));
        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponiblesEnFechas(
                fechasSerie, LocalTime.of(14, 0), LocalTime.of(18, 0));

        assertEquals(1, disponibles.size());
        assertTrue(disponibles.get(0).isTieneTurnoMismoDia());
    }

    @Test
    void listarSociosDisponiblesDetectaTurnoMismoDiaCuandoTurnoSolicitadoEsNocturno() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1));

        // Turno existente durante el día 22/10 de 14:00 a 18:00
        Asistencia turnoDiaSiguiente = new Asistencia();
        turnoDiaSiguiente.setId(100L);
        turnoDiaSiguiente.setSocio(socio1);
        turnoDiaSiguiente.setFecha(LocalDate.of(2026, 10, 22));
        turnoDiaSiguiente.setHoraInicio(LocalTime.of(14, 0));
        turnoDiaSiguiente.setHoraFin(LocalTime.of(18, 0));

        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of(turnoDiaSiguiente));

        // Turno solicitado nocturno del 21/10 22:00 al 22/10 06:00
        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponibles(
                LocalDate.of(2026, 10, 21), LocalTime.of(22, 0), LocalTime.of(6, 0), null);

        assertEquals(1, disponibles.size());
        assertTrue(disponibles.get(0).isTieneTurnoMismoDia(),
                "Debe detectar que el socio ya tiene un turno el 22/10 cuando el turno solicitado termina el 22/10");
    }

    @Test
    void listarSociosDisponiblesPermiteTurnosContiguosEnLimiteExactoConAdvertenciaMismoDia() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1));

        // Turno existente 10:00 a 14:00
        Asistencia turnoPrevio = new Asistencia();
        turnoPrevio.setId(100L);
        turnoPrevio.setSocio(socio1);
        turnoPrevio.setFecha(LocalDate.of(2026, 10, 21));
        turnoPrevio.setHoraInicio(LocalTime.of(10, 0));
        turnoPrevio.setHoraFin(LocalTime.of(14, 0));

        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of(turnoPrevio));

        // Turno solicitado comienza exactamente cuando termina el previo: 14:00 a 18:00
        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponibles(
                LocalDate.of(2026, 10, 21), LocalTime.of(14, 0), LocalTime.of(18, 0), null);

        assertEquals(1, disponibles.size(), "El socio no debe quedar excluido al ser contiguo");
        assertEquals(10L, disponibles.get(0).getId());
        assertTrue(disponibles.get(0).isTieneTurnoMismoDia(), "Debe marcar que ya tiene servicio ese día");
    }

    @Test
    void listarSociosDisponiblesTurnoNocturnoConLimiteExactoSinSolapamiento() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1));

        // Turno nocturno previo del 20/10 22:00 al 21/10 06:00
        Asistencia nocturnoPrevio = new Asistencia();
        nocturnoPrevio.setId(100L);
        nocturnoPrevio.setSocio(socio1);
        nocturnoPrevio.setFecha(LocalDate.of(2026, 10, 20));
        nocturnoPrevio.setHoraInicio(LocalTime.of(22, 0));
        nocturnoPrevio.setHoraFin(LocalTime.of(6, 0));

        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of(nocturnoPrevio));

        // Turno solicitado comienza exactamente a las 06:00 del 21/10 hasta las 10:00
        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponibles(
                LocalDate.of(2026, 10, 21), LocalTime.of(6, 0), LocalTime.of(10, 0), null);

        assertEquals(1, disponibles.size());
        assertTrue(disponibles.get(0).isTieneTurnoMismoDia());
    }

    @Test
    void listarSociosDisponiblesEnFechasExcluyeSocioConSolapamientoEnCualquieraDeLasFechas() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1));

        // Turno que se solapa solo en la segunda fecha de la serie (28/10 de 14:00 a 16:00)
        Asistencia turnoSolapadoEnSegundaFecha = new Asistencia();
        turnoSolapadoEnSegundaFecha.setId(100L);
        turnoSolapadoEnSegundaFecha.setSocio(socio1);
        turnoSolapadoEnSegundaFecha.setFecha(LocalDate.of(2026, 10, 28));
        turnoSolapadoEnSegundaFecha.setHoraInicio(LocalTime.of(14, 0));
        turnoSolapadoEnSegundaFecha.setHoraFin(LocalTime.of(16, 0));

        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of(turnoSolapadoEnSegundaFecha));

        List<LocalDate> fechasSerie = List.of(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 28));
        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponiblesEnFechas(
                fechasSerie, LocalTime.of(14, 0), LocalTime.of(18, 0));

        assertEquals(0, disponibles.size(), "Debe excluir al socio si tiene superposición en cualquier fecha de la serie");
    }

    @Test
    void listarSociosDisponiblesIgnoraElTurnoSiEsExcluirAsistenciaIdPeroDetectaOtroTurnoMismoDia() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1));

        // Turno siendo editado: 21/10 14:00 a 18:00 (id 100)
        Asistencia turnoEditado = new Asistencia();
        turnoEditado.setId(100L);
        turnoEditado.setSocio(socio1);
        turnoEditado.setFecha(LocalDate.of(2026, 10, 21));
        turnoEditado.setHoraInicio(LocalTime.of(14, 0));
        turnoEditado.setHoraFin(LocalTime.of(18, 0));

        // Otro turno no solapado ese mismo día: 21/10 08:00 a 12:00 (id 101)
        Asistencia otroTurnoMismoDia = new Asistencia();
        otroTurnoMismoDia.setId(101L);
        otroTurnoMismoDia.setSocio(socio1);
        otroTurnoMismoDia.setFecha(LocalDate.of(2026, 10, 21));
        otroTurnoMismoDia.setHoraInicio(LocalTime.of(8, 0));
        otroTurnoMismoDia.setHoraFin(LocalTime.of(12, 0));

        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of(turnoEditado, otroTurnoMismoDia));

        // Excluimos turnoEditado (100L): el socio debe estar disponible y marcar tieneTurnoMismoDia=true por turno 101
        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponibles(
                LocalDate.of(2026, 10, 21), LocalTime.of(14, 0), LocalTime.of(18, 0), 100L);

        assertEquals(1, disponibles.size());
        assertEquals(10L, disponibles.get(0).getId());
        assertTrue(disponibles.get(0).isTieneTurnoMismoDia(),
                "Debe marcar que ya tiene servicio ese día por el otro turno no excluido");
    }

    @Test
    void listarSociosDisponiblesEnFechasExcluyeSocioQueNoTieneDisponibleUnoDeLosDias() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);
        // Socio solo trabaja los miércoles
        socio1.setDiasDisponibles(java.util.Set.of(java.time.DayOfWeek.WEDNESDAY));

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1));
        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of());

        // Serie que incluye miércoles (2026-10-21) y jueves (2026-10-22)
        List<LocalDate> fechasSerie = List.of(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 22));
        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponiblesEnFechas(
                fechasSerie, LocalTime.of(14, 0), LocalTime.of(18, 0));

        assertEquals(0, disponibles.size(), "Debe excluir al socio si no tiene disponible todos los días de la serie");
    }

    @Test
    void listarSociosDisponiblesRetornaListaVaciaSiTodosEstanOcupados() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);

        Asistencia turnoOcupado = new Asistencia();
        turnoOcupado.setId(100L);
        turnoOcupado.setSocio(socio1);
        turnoOcupado.setFecha(LocalDate.of(2026, 10, 21));
        turnoOcupado.setHoraInicio(LocalTime.of(14, 0));
        turnoOcupado.setHoraFin(LocalTime.of(18, 0));

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1));
        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of(turnoOcupado));

        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponibles(
                LocalDate.of(2026, 10, 21), LocalTime.of(15, 0), LocalTime.of(17, 0), null);

        assertTrue(disponibles.isEmpty(), "Cuando todos los socios tienen turno solapado la lista debe ser vacía");
    }

    @Test
    void listarSociosDisponiblesEnFechasSerieNocturnaDetectaTurnoMismoDia() {
        Socio socio1 = new Socio();
        socio1.setId(10L);
        socio1.setNombre("Ana");
        socio1.setApellido("Perez");
        socio1.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);

        // Turno existente el 2026-10-22 de 08:00 a 10:00 (mismo día en que termina el turno nocturno del 21)
        Asistencia turnoManana = new Asistencia();
        turnoManana.setId(101L);
        turnoManana.setSocio(socio1);
        turnoManana.setFecha(LocalDate.of(2026, 10, 22));
        turnoManana.setHoraInicio(LocalTime.of(8, 0));
        turnoManana.setHoraFin(LocalTime.of(10, 0));

        when(socioRepository.findAll(any(Sort.class))).thenReturn(List.of(socio1));
        when(asistenciaRepository.findAllByFechaBetweenOrderByHoraInicioAsc(any(), any()))
                .thenReturn(List.of(turnoManana));

        // Solicitud nocturna del 21 al 22 de 22:00 a 06:00
        List<SocioDisponible> disponibles = asistenciaService.listarSociosDisponiblesEnFechas(
                List.of(LocalDate.of(2026, 10, 21)), LocalTime.of(22, 0), LocalTime.of(6, 0));

        assertEquals(1, disponibles.size());
        assertTrue(disponibles.get(0).isTieneTurnoMismoDia(),
                "Debe marcar que ya tiene servicio ese día porque comparte el día 2026-10-22");
    }

    private Asistencia nuevaAsistencia() {
        Asistencia asistencia = new Asistencia();
        Socio socio = new Socio();
        socio.setId(10L);
        ServicioDomicilio servicio = new ServicioDomicilio();
        servicio.setId(20L);
        asistencia.setSocio(socio);
        Cliente cliente = new Cliente();
        cliente.setId(30L);
        asistencia.setCliente(cliente);
        asistencia.setServicioDomicilio(servicio);
        asistencia.setFecha(LocalDate.of(2026, 10, 21));
        asistencia.setHoraInicio(LocalTime.of(22, 30));
        asistencia.setHoraFin(LocalTime.of(6, 15));
        return asistencia;
    }
}
