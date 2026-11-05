package com.cooperativa.cuidado.web;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.cuenta.UsuarioCuenta;
import com.cooperativa.cuidado.cuenta.UsuarioCuentaRepository;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.servicio.TipoServicio;
import com.cooperativa.cuidado.servicio.TipoServicioRepository;
import com.cooperativa.cuidado.socio.Especialidad;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import com.cooperativa.cuidado.web.seguridad.SesionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:marcaciontest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seguridad.activa=true"
})
@AutoConfigureMockMvc
@Transactional
class MarcacionTurnoWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioCuentaRepository usuarioRepository;

    @Autowired
    private SocioRepository socioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ServicioDomicilioRepository servicioRepository;

    @Autowired
    private TipoServicioRepository tipoRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    private MockHttpSession sesionAdmin;
    private MockHttpSession sesionAyudante;
    private MockHttpSession sesionSocio;
    private Socio socioDemo;
    private Socio otroSocio;
    private Cliente cliente;
    private ServicioDomicilio servicio;

    @BeforeEach
    void prepararEscenario() {
        UsuarioCuenta admin = usuarioRepository.findByUsernameIgnoreCase("admin").orElseThrow();
        UsuarioCuenta ayudante = usuarioRepository.findByUsernameIgnoreCase("ayudante").orElseThrow();
        UsuarioCuenta socioCuenta = usuarioRepository.findByUsernameIgnoreCase("socio").orElseThrow();

        sesionAdmin = sesionPara(admin);
        sesionAyudante = sesionPara(ayudante);
        sesionSocio = sesionPara(socioCuenta);

        socioDemo = socioRepository.findByDni(socioCuenta.getDni()).orElseThrow();

        Socio s2 = new Socio();
        s2.setNombre("Pedro");
        s2.setApellido("Ramírez");
        s2.setDni("33444555");
        s2.setDireccion("Belgrano 500");
        s2.setTelefono("3425998877");
        s2.setCorreo("pedro@coop.com");
        s2.setCbu("2850590900000033444555");
        s2.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);
        s2.setEspecialidad(Especialidad.CUIDADOR);
        otroSocio = socioRepository.save(s2);

        Cliente c = new Cliente();
        c.setNombreFamiliar("Laura Familiar");
        c.setNombrePaciente("Marta Paciente");
        c.setDomicilio("San Jerónimo 1800");
        c.setTelefono("3424001122");
        cliente = clienteRepository.save(c);

        TipoServicio tipo = tipoRepository.findByNombreIgnoreCase("Por horas")
                .orElseGet(() -> tipoRepository.save(new TipoServicio("Por horas")));
        ServicioDomicilio srv = new ServicioDomicilio();
        srv.setTipoServicio(tipo);
        srv.setValorHora(new BigDecimal("4500.00"));
        srv.setValorHoraSocio(new BigDecimal("3200.00"));
        servicio = servicioRepository.save(srv);
    }

    @Test
    void socioPuedeMarcarEntradaSalidaYAclaracionElDiaDelTurnoPeroNoReescribir() throws Exception {
        LocalDate hoy = LocalDate.now();
        Asistencia turnoHoy = crearTurno(socioDemo, hoy, LocalTime.of(8, 0), LocalTime.of(16, 0));

        // Panel personal muestra el bloque de guardia del día
        mockMvc.perform(get("/").session(sesionSocio))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Marcar entrada, salida o aclaración de hoy")))
                .andExpect(content().string(containsString("Marta Paciente")));

        // 1. Marcar entrada con aclaración de llegada tarde
        mockMvc.perform(post("/api/asistencias/{id}/marcacion", turnoHoy.getId())
                        .session(sesionSocio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\":\"ENTRADA\",\"aclaracionTurno\":\"Llegué 10 minutos tarde por demora del colectivo.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marcacionEntrada").isNotEmpty())
                .andExpect(jsonPath("$.marcacionSalida").isEmpty())
                .andExpect(jsonPath("$.aclaracionTurno").value("Llegué 10 minutos tarde por demora del colectivo."));

        // Intentar marcar entrada por segunda vez es rechazado para el socio
        mockMvc.perform(post("/api/asistencias/{id}/marcacion", turnoHoy.getId())
                        .session(sesionSocio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\":\"ENTRADA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        // 2. Actualizar aclaración y marcar salida
        mockMvc.perform(post("/api/asistencias/{id}/marcacion", turnoHoy.getId())
                        .session(sesionSocio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\":\"SALIDA\",\"aclaracionTurno\":\"Llegué 10 min tarde. Guardia sin novedades médicas.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marcacionEntrada").isNotEmpty())
                .andExpect(jsonPath("$.marcacionSalida").isNotEmpty())
                .andExpect(jsonPath("$.aclaracionTurno").value("Llegué 10 min tarde. Guardia sin novedades médicas."));

        // El calendario devuelve las marcas y la aclaración
        mockMvc.perform(get("/api/calendario")
                        .session(sesionSocio)
                        .param("desde", hoy.toString())
                        .param("hasta", hoy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos[0].marcacionEntrada").isNotEmpty())
                .andExpect(jsonPath("$.eventos[0].marcacionSalida").isNotEmpty())
                .andExpect(jsonPath("$.eventos[0].aclaracionTurno").value("Llegué 10 min tarde. Guardia sin novedades médicas."))
                .andExpect(jsonPath("$.eventos[0].diaDelTurnoHoy").value(true));
    }

    @Test
    void socioNoPuedeMarcarFueraDelDiaDelTurnoNiEnTurnosDeOtroSocioYPeroTurnoNocturnoSiPermiteAlCerrar() throws Exception {
        LocalDate hoy = LocalDate.now();
        Asistencia turnoAyer = crearTurno(socioDemo, hoy.minusDays(2), LocalTime.of(8, 0), LocalTime.of(16, 0));
        Asistencia turnoManiana = crearTurno(socioDemo, hoy.plusDays(1), LocalTime.of(8, 0), LocalTime.of(16, 0));
        Asistencia turnoAjenoHoy = crearTurno(otroSocio, hoy, LocalTime.of(8, 0), LocalTime.of(16, 0));
        Asistencia turnoNocturnoCierraHoy = crearTurno(socioDemo, hoy.minusDays(1), LocalTime.of(22, 0), LocalTime.of(6, 0));

        // Fuera de fecha (pasado y futuro) -> 409 Conflict
        mockMvc.perform(post("/api/asistencias/{id}/marcacion", turnoAyer.getId())
                        .session(sesionSocio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\":\"ENTRADA\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/asistencias/{id}/marcacion", turnoManiana.getId())
                        .session(sesionSocio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\":\"ENTRADA\"}"))
                .andExpect(status().isConflict());

        // Turno de otro socio -> 403 Forbidden
        mockMvc.perform(post("/api/asistencias/{id}/marcacion", turnoAjenoHoy.getId())
                        .session(sesionSocio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\":\"ENTRADA\"}"))
                .andExpect(status().isForbidden());

        // Ayudante tampoco puede marcar entradas/salidas de socios en cualquier momento -> 403 Forbidden
        mockMvc.perform(post("/api/asistencias/{id}/marcacion", turnoAyer.getId())
                        .session(sesionAyudante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\":\"ENTRADA\"}"))
                .andExpect(status().isForbidden());

        // Turno nocturno que empezó ayer a las 22:00 y cierra hoy a las 06:00 -> permitido marcar salida hoy
        mockMvc.perform(post("/api/asistencias/{id}/marcacion", turnoNocturnoCierraHoy.getId())
                        .session(sesionSocio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\":\"SALIDA\",\"aclaracionTurno\":\"Salida a las 6am sin inconvenientes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marcacionSalida").isNotEmpty());
    }

    @Test
    void soloAdministradorPuedeMarcarYModificarEntradasSalidasEnCualquierMomento() throws Exception {
        LocalDate fechaPasada = LocalDate.now().minusDays(10);
        Asistencia turnoPasado = crearTurno(socioDemo, fechaPasada, LocalTime.of(8, 0), LocalTime.of(20, 0));

        String entradaIso = fechaPasada + "T08:05:00";
        String salidaIso = fechaPasada + "T20:02:00";

        mockMvc.perform(post("/api/asistencias/{id}/marcacion", turnoPasado.getId())
                        .session(sesionAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accion": "ADMIN_GUARDAR",
                                  "marcacionEntrada": "%s",
                                  "marcacionSalida": "%s",
                                  "aclaracionTurno": "Cargado por administración según parte telefónico."
                                }
                                """.formatted(entradaIso, salidaIso)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aclaracionTurno").value("Cargado por administración según parte telefónico."));

        Asistencia actualizada = asistenciaRepository.findById(turnoPasado.getId()).orElseThrow();
        assertThat(actualizada.getMarcacionEntrada()).isEqualTo(LocalDateTime.of(fechaPasada, LocalTime.of(8, 5)));
        assertThat(actualizada.getMarcacionSalida()).isEqualTo(LocalDateTime.of(fechaPasada, LocalTime.of(20, 2)));
        assertThat(actualizada.getHorasTotales()).isEqualByComparingTo("12.00");
    }

    private Asistencia crearTurno(Socio socio, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        Asistencia a = new Asistencia();
        a.setSocio(socio);
        a.setCliente(cliente);
        a.setServicioDomicilio(servicio);
        a.setFecha(fecha);
        a.setHoraInicio(inicio);
        a.setHoraFin(fin);
        return asistenciaRepository.save(a);
    }

    private static MockHttpSession sesionPara(UsuarioCuenta cuenta) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SesionHelper.ATRIBUTO_SESION_USUARIO_ID, cuenta.getId());
        return session;
    }
}

