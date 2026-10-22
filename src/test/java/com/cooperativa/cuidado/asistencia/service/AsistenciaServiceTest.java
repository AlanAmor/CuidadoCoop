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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
