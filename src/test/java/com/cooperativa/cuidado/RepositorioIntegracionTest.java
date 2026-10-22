package com.cooperativa.cuidado;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.servicio.TipoServicio;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.Especialidad;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class RepositorioIntegracionTest {

    @Autowired
    private SocioRepository socioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ServicioDomicilioRepository servicioRepository;

    @Autowired
    private com.cooperativa.cuidado.servicio.TipoServicioRepository tipoRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Test
    void persisteRelacionesCalculaHorasYDetectaSolapamientoIncluyendoMedianoche() {
        Socio socio = socioRepository.save(crearSocio());
        Cliente cliente = clienteRepository.save(crearCliente());
        ServicioDomicilio servicio = new ServicioDomicilio();
        servicio.setValorHora(new BigDecimal("1500.00"));
        servicio.setValorHoraSocio(new BigDecimal("1000.00"));
        servicio.setTipoServicio(tipoRepository.findByNombreIgnoreCase("Por horas")
                .orElseGet(() -> tipoRepository.save(new TipoServicio("Por horas"))));
        servicio = servicioRepository.save(servicio);

        Asistencia asistencia = new Asistencia();
        asistencia.setSocio(socio);
        asistencia.setCliente(cliente);
        asistencia.setServicioDomicilio(servicio);
        asistencia.setFecha(LocalDate.of(2026, 10, 21));
        asistencia.setHoraInicio(LocalTime.of(22, 0));
        asistencia.setHoraFin(LocalTime.of(6, 0));
        asistencia = asistenciaRepository.saveAndFlush(asistencia);

        assertEquals(1, socioRepository.findAllByEstado(EstadoSocio.DISPONIBLE_REEMPLAZO).size());
        assertEquals(new BigDecimal("8.00"), asistencia.getHorasTotales());
        assertEquals(new BigDecimal("1500.00"), asistencia.getValorHoraAplicado());
        assertTrue(asistenciaRepository.existeSolapamiento(
                socio.getId(),
                LocalDate.of(2026, 10, 22).atTime(5, 30),
                LocalDate.of(2026, 10, 22).atTime(7, 0),
                null));
        assertFalse(asistenciaRepository.existeSolapamiento(
                socio.getId(),
                LocalDate.of(2026, 10, 22).atTime(6, 0),
                LocalDate.of(2026, 10, 22).atTime(7, 0),
                null));
        assertEquals(new BigDecimal("8.00"), socioRepository.sumarHorasTrabajadas(
                socio.getId(),
                LocalDate.of(2026, 10, 21),
                LocalDate.of(2026, 10, 22)));
    }

    private Socio crearSocio() {
        Socio socio = new Socio();
        socio.setNombre("Ana");
        socio.setApellido("Pérez");
        socio.setDni("12345678");
        socio.setDireccion("Calle 1");
        socio.setTelefono("1123456789");
        socio.setCbu("1234567890123456789012");
        socio.setEstado(EstadoSocio.DISPONIBLE_REEMPLAZO);
        socio.setEspecialidad(Especialidad.CUIDADOR);
        return socio;
    }

    private Cliente crearCliente() {
        Cliente cliente = new Cliente();
        cliente.setNombreFamiliar("Juan Pérez");
        cliente.setTelefono("1123456789");
        cliente.setNombrePaciente("María Pérez");
        cliente.setDomicilio("Calle 123");
        return cliente;
    }
}
