package com.cooperativa.cuidado.liquidacion.service;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.TipoServicio;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiquidacionServiceTest {

    @Test
    void calculaRetornoMensualYResumenPorDomicilioConValorHistorico() {
        SocioRepository socioRepository = mock(SocioRepository.class);
        AsistenciaRepository asistenciaRepository = mock(AsistenciaRepository.class);
        Socio socio = new Socio();
        socio.setId(1L);
        socio.setNombre("Ana");
        socio.setApellido("Pérez");

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setDomicilio("Calle 123");
        ServicioDomicilio servicio = new ServicioDomicilio();
        servicio.setId(2L);
        servicio.setTipoServicio(new TipoServicio("Por horas"));

        Asistencia asistencia = new Asistencia();
        asistencia.setCliente(cliente);
        asistencia.setId(3L);
        asistencia.setFecha(LocalDate.of(2026, 10, 5));
        asistencia.setHoraInicio(LocalTime.of(8, 0));
        asistencia.setHoraFin(LocalTime.of(16, 0));
        asistencia.setHorasTotales(new BigDecimal("8.00"));
        asistencia.setValorHoraAplicado(new BigDecimal("2500.00"));
        asistencia.setValorHoraSocioAplicado(new BigDecimal("1500.00"));
        asistencia.setServicioDomicilio(servicio);

        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(asistenciaRepository.findAllBySocioIdAndFechaBetween(
                1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(asistencia));

        var liquidacion = new LiquidacionService(socioRepository, asistenciaRepository)
                .calcularRetornoMensual(1L, YearMonth.of(2026, 10));

        assertEquals(new BigDecimal("8.00"), liquidacion.totalHoras());
        assertEquals(new BigDecimal("12000.00"), liquidacion.totalRetorno());
        assertEquals(1, liquidacion.domicilios().size());
        assertEquals("Calle 123", liquidacion.domicilios().get(0).domicilio());
        assertEquals(new BigDecimal("12000.00"), liquidacion.domicilios().get(0).importe());
    }
}
