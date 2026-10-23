package com.cooperativa.cuidado.web;

import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.documento.AlicuotaIva;
import com.cooperativa.cuidado.documento.CalculoPresupuesto;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.servicio.TipoServicio;
import com.cooperativa.cuidado.servicio.TipoServicioRepository;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.Especialidad;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.RequestDispatcher;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:webtest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class InterfazWebTest {

    @Autowired
    private MockMvc mockMvc;

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

    @BeforeEach
    void limpiarBase() {
        asistenciaRepository.deleteAll();
        servicioRepository.deleteAll();
        clienteRepository.deleteAll();
        socioRepository.deleteAll();
    }

    @Test
    void muestraDashboardConMetricas() throws Exception {
        Socio socioServicio = socioRepository.save(crearSocio("12345678", EstadoSocio.ACTIVO_EN_SERVICIO));
        socioRepository.save(crearSocio("12345679", EstadoSocio.DISPONIBLE_REEMPLAZO));
        ServicioDomicilio servicio = servicioRepository.save(crearServicio());
        Asistencia asistencia = new Asistencia();
        asistencia.setSocio(socioServicio);
        asistencia.setCliente(clientePorDefecto());
        asistencia.setServicioDomicilio(servicio);
        asistencia.setFecha(LocalDate.now());
        asistencia.setHoraInicio(LocalTime.of(8, 0));
        asistencia.setHoraFin(LocalTime.of(9, 0));
        asistenciaRepository.save(asistencia);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Resumen de la cooperativa")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Calendario de cobertura")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">1<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">1,00<")));
    }

    @Test
    void muestraFormularioDeAsignacionPidiendoPrimeroDiaYHorario() throws Exception {
        socioRepository.save(crearSocio("12345678", EstadoSocio.DISPONIBLE_REEMPLAZO));
        servicioRepository.save(crearServicio());
        clientePorDefecto();

        mockMvc.perform(get("/asignaciones/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("asignaciones"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Día y horario del servicio")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Socio, Ana"))));

        mockMvc.perform(get("/asignaciones/nueva")
                        .param("fecha", "2026-10-21").param("horaInicio", "08:00").param("horaFin", "12:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Socio, Ana")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Paciente · Calle 123")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cliente $ 1500.00/h")));
    }

    @Test
    void ofreceSoloSociosLibresEnElDiaYHorarioElegidos() throws Exception {
        Socio ocupado = socioRepository.save(crearSocio("30000001", EstadoSocio.ACTIVO_EN_SERVICIO));
        ocupado.setApellido("Ocupado");
        Socio libreOtroHorario = socioRepository.save(crearSocio("30000002", EstadoSocio.ACTIVO_EN_SERVICIO));
        libreOtroHorario.setApellido("Libre");
        Socio inactivo = socioRepository.save(crearSocio("30000003", EstadoSocio.INACTIVO));
        inactivo.setApellido("Inactivo");
        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());
        guardarAsistencia(ocupado, servicio, LocalDate.of(2026, 10, 21));

        mockMvc.perform(get("/api/socios/disponibles")
                        .param("fecha", "2026-10-21").param("horaInicio", "09:00").param("horaFin", "11:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Libre, Ana"));

        mockMvc.perform(get("/api/socios/disponibles")
                        .param("fecha", "2026-10-21").param("horaInicio", "14:00").param("horaFin", "16:00"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.nombre == 'Ocupado, Ana')].tieneTurnoMismoDia").value(true))
                .andExpect(jsonPath("$[?(@.nombre == 'Libre, Ana')].tieneTurnoMismoDia").value(false));

        Long turnoId = asistenciaRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/socios/disponibles")
                        .param("fecha", "2026-10-21").param("horaInicio", "09:00").param("horaFin", "11:00")
                        .param("excluirAsistenciaId", turnoId.toString()))
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/socios/disponibles")
                        .param("fecha", "2026-10-21").param("horaInicio", "09:00").param("horaFin", "09:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void apiSociosDisponiblesDetectaTurnoMismoDiaYExcluyeSolapados() throws Exception {
        Socio socioConTurno = socioRepository.save(crearSocio("30000010", EstadoSocio.ACTIVO_EN_SERVICIO));
        socioConTurno.setApellido("ConTurno");
        socioRepository.save(socioConTurno);

        Socio socioLibre = socioRepository.save(crearSocio("30000020", EstadoSocio.ACTIVO_EN_SERVICIO));
        socioLibre.setApellido("TotalmenteLibre");
        socioRepository.save(socioLibre);

        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());
        // Turno existente de 08:00 a 10:00 el 2026-10-21
        guardarAsistencia(socioConTurno, servicio, LocalDate.of(2026, 10, 21));

        // 1. Horario solapado (09:00 a 11:00): socioConTurno NO debe aparecer
        mockMvc.perform(get("/api/socios/disponibles")
                        .param("fecha", "2026-10-21").param("horaInicio", "09:00").param("horaFin", "11:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("TotalmenteLibre, Ana"))
                .andExpect(jsonPath("$[0].tieneTurnoMismoDia").value(false));

        // 2. Horario no solapado en el mismo día (14:00 a 18:00): ambos aparecen, y socioConTurno tiene tieneTurnoMismoDia=true
        mockMvc.perform(get("/api/socios/disponibles")
                        .param("fecha", "2026-10-21").param("horaInicio", "14:00").param("horaFin", "18:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.nombre == 'ConTurno, Ana')].tieneTurnoMismoDia").value(true))
                .andExpect(jsonPath("$[?(@.nombre == 'TotalmenteLibre, Ana')].tieneTurnoMismoDia").value(false));

        // 3. Turno contiguo en límite exacto (10:00 a 14:00): no se solapa con el turno previo 08:00-10:00 y marca mismo día
        mockMvc.perform(get("/api/socios/disponibles")
                        .param("fecha", "2026-10-21").param("horaInicio", "10:00").param("horaFin", "14:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.nombre == 'ConTurno, Ana')].tieneTurnoMismoDia").value(true));
    }

    @Test
    void apiSociosDisponiblesDetectaTurnoMismoDiaConTurnoSolicitadoNocturno() throws Exception {
        Socio socioConTurno = socioRepository.save(crearSocio("30000055", EstadoSocio.ACTIVO_EN_SERVICIO));
        socioConTurno.setApellido("Nocturno");
        socioRepository.save(socioConTurno);

        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());
        // Turno existente el 2026-10-22 de 14:00 a 18:00
        guardarAsistencia(socioConTurno, servicio, LocalDate.of(2026, 10, 22));

        // Turno solicitado nocturno del 2026-10-21 22:00 al 2026-10-22 06:00
        // Debe detectar que el socio ya trabaja el día 22 y marcar tieneTurnoMismoDia=true
        mockMvc.perform(get("/api/socios/disponibles")
                        .param("fecha", "2026-10-21").param("horaInicio", "22:00").param("horaFin", "06:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Nocturno, Ana')].tieneTurnoMismoDia").value(true));
    }

    @Test
    void formularioAsignacionMuestraEtiquetaAdvertenciaMismoDiaYPermiteAsignar() throws Exception {
        Socio socioConTurno = socioRepository.save(crearSocio("30000030", EstadoSocio.ACTIVO_EN_SERVICIO));
        socioConTurno.setApellido("ConTurno");
        socioConTurno.setNombre("Belen");
        socioRepository.save(socioConTurno);

        Socio socioLibre = socioRepository.save(crearSocio("30000040", EstadoSocio.ACTIVO_EN_SERVICIO));
        socioLibre.setApellido("Libre");
        socioLibre.setNombre("Carlos");
        socioRepository.save(socioLibre);

        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());
        Cliente cliente = clientePorDefecto();

        // Turno existente de 08:00 a 10:00 el 2026-10-21 para socioConTurno
        guardarAsistencia(socioConTurno, servicio, LocalDate.of(2026, 10, 21));

        // Ver socios disponibles para 2026-10-21 de 14:00 a 18:00 (no solapado)
        mockMvc.perform(get("/asignaciones/nueva")
                        .param("fecha", "2026-10-21").param("horaInicio", "14:00").param("horaFin", "18:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ConTurno, Belen")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Libre, Carlos")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ya tiene un servicio ese día")));

        // Ver socios disponibles para 2026-10-21 de 09:00 a 11:00 (solapado con el turno de socioConTurno)
        mockMvc.perform(get("/asignaciones/nueva")
                        .param("fecha", "2026-10-21").param("horaInicio", "09:00").param("horaFin", "11:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Libre, Carlos")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("ConTurno, Belen"))));

        // Asignar el turno para socioConTurno en horario no solapado a pesar de tener otro turno ese día
        mockMvc.perform(post("/asignaciones")
                        .param("socioId", socioConTurno.getId().toString())
                        .param("clienteId", cliente.getId().toString())
                        .param("servicioDomicilioId", servicio.getId().toString())
                        .param("fecha", "2026-10-21")
                        .param("horaInicio", "14:00")
                        .param("horaFin", "18:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/asignaciones/nueva"));

        // Se deben haber registrado los 2 turnos para socioConTurno en el mismo día
        org.junit.jupiter.api.Assertions.assertEquals(2, asistenciaRepository.findAllBySocioIdAndFechaBetween(
                socioConTurno.getId(), LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 21)).size());
    }

    @Test
    void cuidadoFijoPaso2MuestraAdvertenciaMismoDiaParaTurnoNoSolapadoYExcluyeSolapado() throws Exception {
        Socio socioConTurno = socioRepository.save(crearSocio("30000050", EstadoSocio.ACTIVO_EN_SERVICIO));
        socioConTurno.setApellido("SerieA");
        socioConTurno.setNombre("Alba");
        socioRepository.save(socioConTurno);

        Socio socioSolapado = socioRepository.save(crearSocio("30000060", EstadoSocio.ACTIVO_EN_SERVICIO));
        socioSolapado.setApellido("SerieB");
        socioSolapado.setNombre("Bernardo");
        socioRepository.save(socioSolapado);

        Socio socioLibre = socioRepository.save(crearSocio("30000070", EstadoSocio.DISPONIBLE_REEMPLAZO));
        socioLibre.setApellido("SerieC");
        socioLibre.setNombre("Cecilia");
        socioRepository.save(socioLibre);

        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());

        // socioConTurno tiene turno no solapado el 2026-10-12 de 08:00 a 10:00
        guardarAsistencia(socioConTurno, servicio, LocalDate.of(2026, 10, 12));
        // socioSolapado tiene turno solapado el 2026-10-19 de 14:00 a 18:00
        Asistencia turnoSolapado = new Asistencia();
        turnoSolapado.setSocio(socioSolapado);
        turnoSolapado.setCliente(clientePorDefecto());
        turnoSolapado.setServicioDomicilio(servicio);
        turnoSolapado.setFecha(LocalDate.of(2026, 10, 19));
        turnoSolapado.setHoraInicio(LocalTime.of(14, 0));
        turnoSolapado.setHoraFin(LocalTime.of(18, 0));
        asistenciaRepository.saveAndFlush(turnoSolapado);

        // Paso 2 para cuidado fijo los lunes de 14:00 a 18:00 durante 3 semanas (12, 19 y 26 de octubre)
        mockMvc.perform(get("/asignaciones/nueva")
                        .param("fecha", "2026-10-12").param("horaInicio", "14:00").param("horaFin", "18:00")
                        .param("repetir", "true").param("unidad", "SEMANAS").param("cantidad", "3")
                        .param("dias", "MONDAY"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SerieA, Alba")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SerieC, Cecilia")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SerieB, Bernardo"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ya tiene un servicio ese día")));

        // Confirmar asignación de la serie para socioConTurno
        mockMvc.perform(post("/asignaciones")
                        .param("socioId", socioConTurno.getId().toString())
                        .param("clienteId", clientePorDefecto().getId().toString())
                        .param("servicioDomicilioId", servicio.getId().toString())
                        .param("fecha", "2026-10-12")
                        .param("horaInicio", "14:00")
                        .param("horaFin", "18:00")
                        .param("repetir", "true")
                        .param("unidad", "SEMANAS")
                        .param("cantidad", "3")
                        .param("dias", "MONDAY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/asignaciones/nueva"));

        // Se deben haber creado 3 turnos de la serie + 1 previo = 4 turnos para socioConTurno
        org.junit.jupiter.api.Assertions.assertEquals(4, asistenciaRepository.findAllBySocioIdAndFechaBetween(
                socioConTurno.getId(), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 26)).size());
    }

    @Test
    void apiSociosDisponiblesExcluyeAsistenciaIdYDetectaOtroTurnoMismoDia() throws Exception {
        Socio socio = socioRepository.save(crearSocio("30000080", EstadoSocio.ACTIVO_EN_SERVICIO));
        socio.setApellido("Editado");
        socio.setNombre("Daniel");
        socioRepository.save(socio);

        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());

        // Turno 1 siendo editado: 14:00 a 18:00
        Asistencia turno1 = new Asistencia();
        turno1.setSocio(socio);
        turno1.setCliente(clientePorDefecto());
        turno1.setServicioDomicilio(servicio);
        turno1.setFecha(LocalDate.of(2026, 10, 21));
        turno1.setHoraInicio(LocalTime.of(14, 0));
        turno1.setHoraFin(LocalTime.of(18, 0));
        turno1 = asistenciaRepository.saveAndFlush(turno1);

        // Sin otro turno: al excluir turno1, el socio está libre y tieneTurnoMismoDia=false
        mockMvc.perform(get("/api/socios/disponibles")
                        .param("fecha", "2026-10-21").param("horaInicio", "14:00").param("horaFin", "18:00")
                        .param("excluirAsistenciaId", turno1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Editado, Daniel')].tieneTurnoMismoDia").value(false));

        // Turno 2 no solapado el mismo día: 08:00 a 10:00
        guardarAsistencia(socio, servicio, LocalDate.of(2026, 10, 21));

        // Con turno 2: al excluir turno1, el socio está libre y tieneTurnoMismoDia=true
        mockMvc.perform(get("/api/socios/disponibles")
                        .param("fecha", "2026-10-21").param("horaInicio", "14:00").param("horaFin", "18:00")
                        .param("excluirAsistenciaId", turno1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Editado, Daniel')].tieneTurnoMismoDia").value(true));
    }

    @Test
    void formularioAsignacionRenderizaAtributosAriaParaAccesibilidad() throws Exception {
        Socio socio = socioRepository.save(crearSocio("30000090", EstadoSocio.ACTIVO_EN_SERVICIO));
        socio.setApellido("Accesible");
        socio.setNombre("Elena");
        socioRepository.save(socio);

        mockMvc.perform(get("/asignaciones/nueva")
                        .param("fecha", "2026-10-21").param("horaInicio", "14:00").param("horaFin", "18:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("role=\"combobox\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-controls=\"socio-dropdown-menu\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-haspopup=\"listbox\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("role=\"listbox\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("role=\"option\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-selected=")));
    }

    @Test
    void submisionPaso2SinSeleccionarSocioDisparaValidacion() throws Exception {
        Socio socio = socioRepository.save(crearSocio("30000095", EstadoSocio.DISPONIBLE_REEMPLAZO));
        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());
        Cliente cliente = clientePorDefecto();

        // Enviar formulario sin socioId
        mockMvc.perform(post("/asignaciones")
                        .param("clienteId", cliente.getId().toString())
                        .param("servicioDomicilioId", servicio.getId().toString())
                        .param("fecha", "2026-10-21")
                        .param("horaInicio", "14:00")
                        .param("horaFin", "18:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Seleccioná un socio disponible")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-invalid=\"true\"")));

        // Ningún turno debió haberse persistido
        org.junit.jupiter.api.Assertions.assertEquals(0, asistenciaRepository.count());
    }

    @Test
    void paso2YApiManejanCeroSociosDisponiblesCuandoTodosEstanOcupados() throws Exception {
        Socio socio = socioRepository.save(crearSocio("30000096", EstadoSocio.ACTIVO_EN_SERVICIO));
        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());

        // Ocupamos al único socio en el horario 14:00 a 18:00
        Asistencia turnoOcupado = new Asistencia();
        turnoOcupado.setSocio(socio);
        turnoOcupado.setCliente(clientePorDefecto());
        turnoOcupado.setServicioDomicilio(servicio);
        turnoOcupado.setFecha(LocalDate.of(2026, 10, 21));
        turnoOcupado.setHoraInicio(LocalTime.of(14, 0));
        turnoOcupado.setHoraFin(LocalTime.of(18, 0));
        asistenciaRepository.saveAndFlush(turnoOcupado);

        // Paso 2 en asignaciones: dropdown deshabilitado y mensaje de ocupados
        mockMvc.perform(get("/asignaciones/nueva")
                        .param("fecha", "2026-10-21").param("horaInicio", "14:00").param("horaFin", "18:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Todos los socios tienen un turno en ese horario")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("disabled=\"disabled\"")));

        // API de socios disponibles: retorna lista vacía
        mockMvc.perform(get("/api/socios/disponibles")
                        .param("fecha", "2026-10-21").param("horaInicio", "14:00").param("horaFin", "18:00"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    void dropdownManejaCaracteresEspecialesAcentosYEspecialidadLarga() throws Exception {
        Socio socio = crearSocio("30000097", EstadoSocio.DISPONIBLE_REEMPLAZO);
        socio.setNombre("María Inés");
        socio.setApellido("Pérez Ñandú");
        socio.setEspecialidad(com.cooperativa.cuidado.socio.Especialidad.ENFERMERO);
        socio = socioRepository.save(socio);

        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());
        // Turno no solapado el mismo día de 08:00 a 10:00
        guardarAsistencia(socio, servicio, LocalDate.of(2026, 10, 21));

        // Vista HTML: verifica nombres con acentos/ñ, etiqueta de especialidad y badge de advertencia
        mockMvc.perform(get("/asignaciones/nueva")
                        .param("fecha", "2026-10-21").param("horaInicio", "14:00").param("horaFin", "18:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pérez Ñandú, María Inés")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Enfermero/a")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ya tiene un servicio ese día")));

        // Endpoint JSON para calendario: verifica estructura y booleano
        mockMvc.perform(get("/api/socios/disponibles")
                        .param("fecha", "2026-10-21").param("horaInicio", "14:00").param("horaFin", "18:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Pérez Ñandú, María Inés')].especialidad").value("ENFERMERO"))
                .andExpect(jsonPath("$[?(@.nombre == 'Pérez Ñandú, María Inés')].tieneTurnoMismoDia").value(true));
    }

    @Test
    void modificaCuidadorHorarioYClienteDeUnTurnoExistente() throws Exception {
        Socio original = socioRepository.save(crearSocio("30000001", EstadoSocio.DISPONIBLE_REEMPLAZO));
        Socio reemplazo = socioRepository.save(crearSocio("30000002", EstadoSocio.DISPONIBLE_REEMPLAZO));
        ServicioDomicilio servicioA = servicioRepository.saveAndFlush(crearServicio());
        ServicioDomicilio servicioB = crearServicio();
        servicioB.setTipoServicio(tipo("Enfermería express"));
        servicioB.setValorHora(new BigDecimal("3000.00"));
        servicioB.setValorHoraSocio(new BigDecimal("2100.00"));
        servicioRepository.saveAndFlush(servicioB);
        Cliente clienteA = crearCliente("Paciente A", "Calle A");
        Cliente clienteB = crearCliente("Paciente B", "Calle B");
        guardarAsistencia(original, clienteA, servicioA, LocalDate.of(2026, 10, 21));
        Long id = asistenciaRepository.findAll().get(0).getId();

        String cuerpo = """
                {"socioId": %d, "clienteId": %d, "servicioId": %d, "fecha": "2026-10-22", "horaInicio": "10:00", "horaFin": "15:30"}
                """.formatted(reemplazo.getId(), clienteB.getId(), servicioB.getId());
        mockMvc.perform(post("/api/asistencias/" + id)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());

        Asistencia actualizada = asistenciaRepository.findById(id).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(clienteB.getId(), actualizada.getCliente().getId());
        org.junit.jupiter.api.Assertions.assertEquals(reemplazo.getId(), actualizada.getSocio().getId());
        org.junit.jupiter.api.Assertions.assertEquals(servicioB.getId(), actualizada.getServicioDomicilio().getId());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 10, 22), actualizada.getFecha());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(15, 30), actualizada.getHoraFin());
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("5.50").compareTo(actualizada.getHorasTotales()));
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("3000").compareTo(actualizada.getValorHoraAplicado()));
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("2100").compareTo(actualizada.getValorHoraPagoSocio()));
    }

    @Test
    void rechazaModificarUnTurnoSiElSocioYaTieneOtroEnEseHorario() throws Exception {
        Socio ana = socioRepository.save(crearSocio("30000001", EstadoSocio.DISPONIBLE_REEMPLAZO));
        Socio luis = socioRepository.save(crearSocio("30000002", EstadoSocio.DISPONIBLE_REEMPLAZO));
        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());
        guardarAsistencia(ana, servicio, LocalDate.of(2026, 10, 21));
        guardarAsistencia(luis, servicio, LocalDate.of(2026, 10, 21));
        Long turnoDeLuis = asistenciaRepository.findAll().stream()
                .filter(a -> a.getSocio().getId().equals(luis.getId())).findFirst().orElseThrow().getId();

        String cuerpo = """
                {"socioId": %d, "clienteId": %d, "servicioId": %d, "fecha": "2026-10-21", "horaInicio": "09:00", "horaFin": "11:00"}
                """.formatted(ana.getId(), clientePorDefecto().getId(), servicio.getId());
        mockMvc.perform(post("/api/asistencias/" + turnoDeLuis)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("ya tiene")));

        mockMvc.perform(post("/api/asistencias/999999")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/asistencias/" + turnoDeLuis)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void asignaTurnoDesdeFormularioYActualizaEstadoDelSocio() throws Exception {
        Socio socio = socioRepository.save(crearSocio("12345678", EstadoSocio.DISPONIBLE_REEMPLAZO));
        ServicioDomicilio servicio = servicioRepository.save(crearServicio());

        mockMvc.perform(post("/asignaciones")
                        .param("socioId", socio.getId().toString())
                        .param("clienteId", clientePorDefecto().getId().toString())
                        .param("servicioDomicilioId", servicio.getId().toString())
                        .param("fecha", "2026-10-21")
                        .param("horaInicio", "22:00")
                        .param("horaFin", "06:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/asignaciones/nueva"));

        org.junit.jupiter.api.Assertions.assertEquals(1, asistenciaRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(
                EstadoSocio.ACTIVO_EN_SERVICIO,
                socioRepository.findById(socio.getId()).orElseThrow().getEstado());
    }

    @Test
    void asignaUnCuidadoFijoCreandoUnTurnoPorCadaDiaElegido() throws Exception {
        Socio socio = socioRepository.save(crearSocio("12345678", EstadoSocio.DISPONIBLE_REEMPLAZO));
        ServicioDomicilio servicio = servicioRepository.save(crearServicio());

        mockMvc.perform(post("/asignaciones")
                        .param("socioId", socio.getId().toString())
                        .param("clienteId", clientePorDefecto().getId().toString())
                        .param("servicioDomicilioId", servicio.getId().toString())
                        .param("fecha", "2026-10-05")
                        .param("horaInicio", "22:00")
                        .param("horaFin", "06:00")
                        .param("repetir", "true")
                        .param("unidad", "SEMANAS")
                        .param("cantidad", "2")
                        .param("dias", "MONDAY", "WEDNESDAY", "FRIDAY"))
                .andExpect(status().is3xxRedirection());

        org.junit.jupiter.api.Assertions.assertEquals(6, asistenciaRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(
                EstadoSocio.ACTIVO_EN_SERVICIO,
                socioRepository.findById(socio.getId()).orElseThrow().getEstado());
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void elCuidadoFijoEsTodoONadaSiUnaFechaChoca() throws Exception {
        Socio socio = socioRepository.save(crearSocio("12345678", EstadoSocio.DISPONIBLE_REEMPLAZO));
        ServicioDomicilio servicio = servicioRepository.save(crearServicio());
        guardarAsistencia(socio, servicio, LocalDate.of(2026, 10, 7));
        long antes = asistenciaRepository.count();

        mockMvc.perform(post("/asignaciones")
                        .param("socioId", socio.getId().toString())
                        .param("clienteId", clientePorDefecto().getId().toString())
                        .param("servicioDomicilioId", servicio.getId().toString())
                        .param("fecha", "2026-10-05")
                        .param("horaInicio", "08:00")
                        .param("horaFin", "12:00")
                        .param("repetir", "true")
                        .param("unidad", "DIAS")
                        .param("cantidad", "5")
                        .param("dias", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No se creó ningún turno")));

        org.junit.jupiter.api.Assertions.assertEquals(antes, asistenciaRepository.count());
        limpiarBase();
    }

    @Test
    void elPaso2ExcluyeSociosOcupadosEnCualquierFechaDeLaSerie() throws Exception {
        Socio ocupado = socioRepository.save(crearSocio("11111111", EstadoSocio.DISPONIBLE_REEMPLAZO));
        socioRepository.save(crearSocio("22222222", EstadoSocio.DISPONIBLE_REEMPLAZO));
        ServicioDomicilio servicio = servicioRepository.save(crearServicio());
        guardarAsistencia(ocupado, servicio, LocalDate.of(2026, 10, 12));

        mockMvc.perform(get("/asignaciones/nueva")
                        .param("fecha", "2026-10-05").param("horaInicio", "08:00").param("horaFin", "12:00")
                        .param("repetir", "true").param("unidad", "SEMANAS").param("cantidad", "3")
                        .param("dias", "MONDAY"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DNI 22222222")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("DNI 11111111"))));
    }

    @Test
    void rechazaUnCuidadoFijoMasLargoQueUnAnio() throws Exception {
        mockMvc.perform(get("/asignaciones/nueva")
                        .param("fecha", "2026-10-05").param("horaInicio", "08:00").param("horaFin", "12:00")
                        .param("repetir", "true").param("unidad", "MESES").param("cantidad", "13")
                        .param("dias", "MONDAY"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("duración máxima")));
    }

    @Test
    void descargaElHistoricoDeUnSocioYDeUnCliente() throws Exception {
        Socio socio = socioRepository.save(crearSocio("12345678", EstadoSocio.ACTIVO_EN_SERVICIO));
        ServicioDomicilio servicio = servicioRepository.save(crearServicio());
        Cliente cliente = clientePorDefecto();
        guardarAsistencia(socio, cliente, servicio, LocalDate.now());

        for (String ruta : new String[]{"/socios/" + socio.getId() + "/historico/pdf",
                "/clientes/" + cliente.getId() + "/historico/pdf"}) {
            byte[] pdf = mockMvc.perform(get(ruta)).andExpect(status().isOk())
                    .andExpect(content().contentType("application/pdf"))
                    .andReturn().getResponse().getContentAsByteArray();
            org.junit.jupiter.api.Assertions.assertArrayEquals(
                    "%PDF-".getBytes(), java.util.Arrays.copyOf(pdf, 5));
        }
        for (String ruta : new String[]{"/socios/" + socio.getId() + "/historico/excel",
                "/clientes/" + cliente.getId() + "/historico/excel"}) {
            byte[] xlsx = mockMvc.perform(get(ruta)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsByteArray();
            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
                org.junit.jupiter.api.Assertions.assertEquals(1, workbook.getSheetAt(0).getLastRowNum() - 3);
            }
        }
    }

    @Test
    void elHistoricoDeUnSocioInexistenteMuestraUnMensajeClaro() throws Exception {
        mockMvc.perform(get("/socios/999999/historico/pdf"))
                .andExpect(status().isNotFound());
    }

    @Test
    void muestraFormularioDeLiquidaciones() throws Exception {
        socioRepository.save(crearSocio("12345678", EstadoSocio.DISPONIBLE_REEMPLAZO));

        mockMvc.perform(get("/liquidaciones"))
                .andExpect(status().isOk())
                .andExpect(view().name("liquidaciones"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Retornos mensuales")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Socio, Ana")));
    }

    @Test
    void generaPdfDeRetornoYPresupuesto() throws Exception {
        Socio socio = socioRepository.save(crearSocio("12345678", EstadoSocio.ACTIVO_EN_SERVICIO));
        ServicioDomicilio servicio = servicioRepository.save(crearServicio());
        guardarAsistencia(socio, servicio, LocalDate.now());

        byte[] pdfRetorno = mockMvc.perform(get("/liquidaciones/{socioId}/{periodo}/pdf",
                        socio.getId(), YearMonth.now()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("retorno-" + socio.getId())))
                .andReturn().getResponse().getContentAsByteArray();
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                "%PDF-".getBytes(), java.util.Arrays.copyOf(pdfRetorno, 5));

        byte[] pdfPresupuesto = mockMvc.perform(get("/presupuestos/pdf")
                        .param("clienteId", clientePorDefecto().getId().toString())
                        .param("servicioId", servicio.getId().toString())
                        .param("horas", "10.5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                "%PDF-".getBytes(), java.util.Arrays.copyOf(pdfPresupuesto, 5));
    }

    @Test
    void exportaHistoricoEnPdfYExcel() throws Exception {
        Socio socio = socioRepository.save(crearSocio("12345678", EstadoSocio.ACTIVO_EN_SERVICIO));
        ServicioDomicilio servicio = servicioRepository.save(crearServicio());
        LocalDate fecha = LocalDate.now();
        guardarAsistencia(socio, servicio, fecha);

        byte[] pdf = mockMvc.perform(get("/asistencias/historico/pdf")
                        .param("desde", fecha.toString())
                        .param("hasta", fecha.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                "%PDF-".getBytes(), java.util.Arrays.copyOf(pdf, 5));

        byte[] excel = mockMvc.perform(get("/asistencias/historico/excel")
                        .param("desde", fecha.toString())
                        .param("hasta", fecha.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new byte[]{'P', 'K'}, java.util.Arrays.copyOf(excel, 2));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            org.junit.jupiter.api.Assertions.assertEquals(
                    "Histórico de asistencias", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals(
                    "Socio", workbook.getSheetAt(0).getRow(3).getCell(1).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals(1,
                    workbook.getSheetAt(0).getLastRowNum() - 3);
        }
    }

    @Test
    void presentaEstadisticasAnualesAgrupadas() throws Exception {
        Socio socio = socioRepository.save(crearSocio("12345678", EstadoSocio.ACTIVO_EN_SERVICIO));
        ServicioDomicilio servicio = servicioRepository.save(crearServicio());
        LocalDate fecha = LocalDate.now();
        guardarAsistencia(socio, servicio, fecha);

        mockMvc.perform(get("/estadisticas").param("anio", String.valueOf(fecha.getYear())))
                .andExpect(status().isOk())
                .andExpect(view().name("estadisticas"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Horas por socio")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Top socios de")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Horas por semana")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("100,00% de las asistencias")));
    }

    @Test
    void estadisticasCalculanComparativasPorSemanaMesYAnio() throws Exception {
        Socio socio = socioRepository.save(crearSocio("12345678", EstadoSocio.ACTIVO_EN_SERVICIO));
        ServicioDomicilio servicio = servicioRepository.save(crearServicio());
        LocalDate fecha = LocalDate.of(2024, 5, 15);
        guardarAsistencia(socio, servicio, fecha);
        guardarAsistencia(socio, servicio, fecha.minusYears(1));

        mockMvc.perform(get("/estadisticas")
                        .param("anio", "2024").param("mes", "5").param("semana", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Top socios de Mayo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("vs. año anterior")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Todos los socios activos tuvieron")));

        mockMvc.perform(get("/estadisticas").param("anio", "2024").param("mes", "13"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void estadisticasFuncionanSinNingunDato() throws Exception {
        mockMvc.perform(get("/estadisticas"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ningún socio registra horas")));
    }

    @Test
    void muestraFormularioParaExportarHistorico() throws Exception {
        mockMvc.perform(get("/asistencias/historico"))
                .andExpect(status().isOk())
                .andExpect(view().name("historico"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Histórico de asistencias")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Descargar Excel")));
    }

    @Test
    void muestraEventosYDisponibilidadEnLaApiDelCalendario() throws Exception {
        Socio asignado = socioRepository.save(crearSocio("12345678", EstadoSocio.ACTIVO_EN_SERVICIO));
        socioRepository.save(crearSocio("12345679", EstadoSocio.DISPONIBLE_REEMPLAZO));
        ServicioDomicilio servicioAsignado = servicioRepository.save(crearServicio());
        crearCliente("Paciente Sin Turno", "Calle 456");
        LocalDate fecha = LocalDate.now();
        guardarAsistencia(asignado, servicioAsignado, fecha);

        mockMvc.perform(get("/api/calendario")
                        .param("desde", fecha.toString())
                        .param("hasta", fecha.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.eventos.length()").value(1))
                .andExpect(jsonPath("$.eventos[0].nombreSocio").value("Socio, Ana"))
                .andExpect(jsonPath("$.eventos[0].paciente").value("Paciente"))
                .andExpect(jsonPath("$.eventos[0].domicilio").value("Calle 123"))
                .andExpect(jsonPath("$.sociosDisponibles.length()").value(1))
                .andExpect(jsonPath("$.sociosDisponibles[0].nombre").value("Socio, Ana"))
                .andExpect(jsonPath("$.clientesSinTurno.length()").value(1))
                .andExpect(jsonPath("$.clientesSinTurno[0].paciente").value("Paciente Sin Turno"));
    }

    @Test
    void incluyeEnElCalendarioUnTurnoNocturnoQueCruzaAlDiaConsultado() throws Exception {
        Socio socio = socioRepository.save(crearSocio("12345678", EstadoSocio.ACTIVO_EN_SERVICIO));
        ServicioDomicilio servicio = servicioRepository.save(crearServicio());
        LocalDate fechaConsulta = LocalDate.now();
        Asistencia nocturna = new Asistencia();
        nocturna.setSocio(socio);
        nocturna.setCliente(clientePorDefecto());
        nocturna.setServicioDomicilio(servicio);
        nocturna.setFecha(fechaConsulta.minusDays(1));
        nocturna.setHoraInicio(LocalTime.of(22, 0));
        nocturna.setHoraFin(LocalTime.of(2, 0));
        asistenciaRepository.saveAndFlush(nocturna);

        mockMvc.perform(get("/api/calendario")
                        .param("desde", fechaConsulta.toString())
                        .param("hasta", fechaConsulta.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos.length()").value(1))
                .andExpect(jsonPath("$.eventos[0].fecha").value(fechaConsulta.minusDays(1).toString()))
                .andExpect(jsonPath("$.eventos[0].inicio").value(fechaConsulta.minusDays(1) + "T22:00:00"))
                .andExpect(jsonPath("$.eventos[0].fin").value(fechaConsulta + "T02:00:00"));
    }

    @Test
    void rechazaRangoInvertidoAlExportarHistorico() throws Exception {
        mockMvc.perform(get("/asistencias/historico/pdf")
                        .param("desde", "2026-10-22")
                        .param("hasta", "2026-10-21"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("formato incorrecto")));
    }

    @Test
    void muestraOrientacionParaParametrosIncorrectos() throws Exception {
        mockMvc.perform(get("/presupuestos/pdf")
                        .param("clienteId", "1")
                        .param("servicioId", "no-es-numero")
                        .param("horas", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("formato incorrecto")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("MethodArgumentTypeMismatchException"))));
    }

    @Test
    void rechazaUnPeriodoInvalidoConUnaExplicacionAmigable() throws Exception {
        mockMvc.perform(get("/liquidaciones/1/2026-99/pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("formato incorrecto")));
    }

    @Test
    void muestraPaginaAmigableParaUnaRutaInexistente() throws Exception {
        mockMvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No encontramos esa página")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Volvé al inicio")));
    }

    @Test
    void noExponeDetallesTecnicosEnErroresInternos() throws Exception {
        mockMvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ocurrió un problema inesperado")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("contactá a soporte")));
    }

    @Test
    void creaEditaYEliminaUnSocioDesdeLaInterfaz() throws Exception {
        mockMvc.perform(post("/socios")
                        .param("nombre", "Lucia").param("apellido", "Gomez").param("dni", "30111222")
                        .param("direccion", "Calle 9").param("telefono", "1155555555")
                        .param("correo", "lucia@mail.com").param("cbu", "1234567890123456789012")
                        .param("estado", "DISPONIBLE_REEMPLAZO").param("especialidad", "CUIDADOR"))
                .andExpect(redirectedUrl("/socios"));
        Socio socio = socioRepository.findAll().get(0);

        mockMvc.perform(post("/socios/" + socio.getId())
                        .param("nombre", "Lucía").param("apellido", "Gomez").param("dni", "30111222")
                        .param("direccion", "Calle 9").param("telefono", "1155555555")
                        .param("correo", "lucia@mail.com").param("cbu", "1234567890123456789012")
                        .param("estado", "INACTIVO").param("especialidad", "ENFERMERO"))
                .andExpect(redirectedUrl("/socios"));
        org.junit.jupiter.api.Assertions.assertEquals(EstadoSocio.INACTIVO,
                socioRepository.findById(socio.getId()).orElseThrow().getEstado());

        mockMvc.perform(post("/socios/" + socio.getId() + "/eliminar"))
                .andExpect(redirectedUrl("/socios"));
        org.junit.jupiter.api.Assertions.assertEquals(0, socioRepository.count());
    }

    @Test
    void eligeYModificaLosDiasDisponiblesDeUnSocio() throws Exception {
        mockMvc.perform(post("/socios")
                        .param("nombre", "Lucia").param("apellido", "Gomez").param("dni", "30111222")
                        .param("direccion", "Calle 9").param("telefono", "1155555555")
                        .param("cbu", "1234567890123456789012")
                        .param("estado", "DISPONIBLE_REEMPLAZO").param("especialidad", "CUIDADOR")
                        .param("diasDisponibles", "MONDAY", "TUESDAY"))
                .andExpect(redirectedUrl("/socios"));
        Socio socio = socioRepository.findAll().get(0);
        org.junit.jupiter.api.Assertions.assertEquals(
                java.util.Set.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY),
                socio.getDiasDisponibles());

        mockMvc.perform(post("/socios/" + socio.getId())
                        .param("nombre", "Lucia").param("apellido", "Gomez").param("dni", "30111222")
                        .param("direccion", "Calle 9").param("telefono", "1155555555")
                        .param("cbu", "1234567890123456789012")
                        .param("estado", "DISPONIBLE_REEMPLAZO").param("especialidad", "CUIDADOR")
                        .param("diasDisponibles", "SATURDAY"))
                .andExpect(redirectedUrl("/socios"));
        org.junit.jupiter.api.Assertions.assertEquals(
                java.util.Set.of(java.time.DayOfWeek.SATURDAY),
                socioRepository.findById(socio.getId()).orElseThrow().getDiasDisponibles());

        mockMvc.perform(post("/socios/" + socio.getId())
                        .param("nombre", "Lucia").param("apellido", "Gomez").param("dni", "30111222")
                        .param("direccion", "Calle 9").param("telefono", "1155555555")
                        .param("cbu", "1234567890123456789012")
                        .param("estado", "DISPONIBLE_REEMPLAZO").param("especialidad", "CUIDADOR")
                        .param("_diasDisponibles", "on"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("al menos un día")));
    }

    @Test
    void soloOfreceYAsignaSociosEnSusDiasDisponibles() throws Exception {
        Socio lunes = crearSocio("11111111", EstadoSocio.DISPONIBLE_REEMPLAZO);
        lunes.setDiasDisponibles(java.util.Set.of(java.time.DayOfWeek.MONDAY));
        lunes = socioRepository.save(lunes);
        socioRepository.save(crearSocio("22222222", EstadoSocio.DISPONIBLE_REEMPLAZO));
        ServicioDomicilio servicio = servicioRepository.save(crearServicio());

        // 2026-10-06 es martes
        mockMvc.perform(get("/asignaciones/nueva")
                        .param("fecha", "2026-10-06").param("horaInicio", "08:00").param("horaFin", "12:00"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DNI 22222222")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("DNI 11111111"))));

        mockMvc.perform(post("/asignaciones")
                        .param("socioId", lunes.getId().toString())
                        .param("clienteId", clientePorDefecto().getId().toString())
                        .param("servicioDomicilioId", servicio.getId().toString())
                        .param("fecha", "2026-10-06").param("horaInicio", "08:00").param("horaFin", "12:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("martes")));
        org.junit.jupiter.api.Assertions.assertEquals(0, asistenciaRepository.count());
    }

    @Test
    void rechazaUnSocioConDniDuplicadoConUnMensajeClaro() throws Exception {
        socioRepository.saveAndFlush(crearSocio("30111222", EstadoSocio.DISPONIBLE_REEMPLAZO));
        mockMvc.perform(post("/socios")
                        .param("nombre", "Otra").param("apellido", "Persona").param("dni", "30111222")
                        .param("direccion", "Calle 9").param("telefono", "1155555555")
                        .param("cbu", "9999999999999999999999")
                        .param("estado", "DISPONIBLE_REEMPLAZO").param("especialidad", "CUIDADOR"))
                .andExpect(status().isOk())
                .andExpect(view().name("socio-form"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .model().attributeExists("errorOperacion"));
        org.junit.jupiter.api.Assertions.assertEquals(1, socioRepository.count());
    }

    @Test
    void validaCamposObligatoriosDelFormularioDeCliente() throws Exception {
        mockMvc.perform(post("/clientes").param("nombreFamiliar", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente-form"));
        org.junit.jupiter.api.Assertions.assertEquals(0, clienteRepository.count());
    }

    @Test
    void eliminarUnClienteBorraSusTurnosPeroNoLosServicios() throws Exception {
        Socio socio = socioRepository.saveAndFlush(crearSocio("30111222", EstadoSocio.DISPONIBLE_REEMPLAZO));
        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());
        guardarAsistencia(socio, servicio, LocalDate.now());

        mockMvc.perform(post("/clientes/" + clientePorDefecto().getId() + "/eliminar"))
                .andExpect(redirectedUrl("/clientes"));

        org.junit.jupiter.api.Assertions.assertEquals(0, clienteRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(1, servicioRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(0, asistenciaRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(1, socioRepository.count());
    }

    @Test
    void creaYAjustaElPrecioPorHoraDeUnServicio() throws Exception {
        TipoServicio enfermeria = tipo("Enfermería express");
        mockMvc.perform(post("/servicios")
                        .param("tipoServicioId", String.valueOf(enfermeria.getId())).param("valorHora", "3200.50")
                        .param("valorHoraSocio", "2000"))
                .andExpect(redirectedUrl("/servicios"));
        ServicioDomicilio nuevo = servicioRepository.findAll().stream()
                .filter(s -> s.getTipoServicio().getId().equals(enfermeria.getId())).findFirst().orElseThrow();

        mockMvc.perform(post("/servicios/" + nuevo.getId())
                        .param("tipoServicioId", String.valueOf(enfermeria.getId())).param("valorHora", "4000")
                        .param("valorHoraSocio", "2500"))
                .andExpect(redirectedUrl("/servicios"));
        ServicioDomicilio editado = servicioRepository.findById(nuevo.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("2500").compareTo(editado.getValorHoraSocio()));
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("4000").compareTo(editado.getValorHora()));
        mockMvc.perform(post("/servicios")
                        .param("tipoServicioId", String.valueOf(enfermeria.getId())).param("valorHora", "100"))
                .andExpect(view().name("servicio-form"));
        mockMvc.perform(post("/servicios")
                        .param("tipoServicioId", String.valueOf(enfermeria.getId())).param("valorHora", "0"))
                .andExpect(view().name("servicio-form"));
        mockMvc.perform(post("/servicios")
                        .param("valorHora", "100").param("valorHoraSocio", "50"))
                .andExpect(view().name("servicio-form"));
    }

    @Test
    void permiteCrearRenombrarYEliminarTiposDeServicio() throws Exception {
        mockMvc.perform(post("/tipos-servicio").param("nombre", "  Kinesiología  "))
                .andExpect(redirectedUrl("/tipos-servicio"));
        TipoServicio kine = tipoRepository.findByNombreIgnoreCase("kinesiología").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Kinesiología", kine.getNombre());

        mockMvc.perform(post("/tipos-servicio").param("nombre", "KINESIOLOGÍA"))
                .andExpect(redirectedUrl("/tipos-servicio"))
                .andExpect(flash().attribute("errorOperacion",
                        org.hamcrest.Matchers.containsString("Ya existe")));
        mockMvc.perform(post("/tipos-servicio").param("nombre", "   "))
                .andExpect(flash().attributeExists("errorOperacion"));

        mockMvc.perform(post("/tipos-servicio/" + kine.getId()).param("nombre", "Fisioterapia"))
                .andExpect(flash().attributeExists("mensajeExito"));
        org.junit.jupiter.api.Assertions.assertEquals("Fisioterapia",
                tipoRepository.findById(kine.getId()).orElseThrow().getNombre());

        ServicioDomicilio servicio = crearServicio();
        servicio.setTipoServicio(kine);
        servicioRepository.save(servicio);
        mockMvc.perform(get("/tipos-servicio"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fisioterapia")));
        mockMvc.perform(get("/servicios"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fisioterapia")));
        mockMvc.perform(post("/tipos-servicio/" + kine.getId() + "/eliminar"))
                .andExpect(flash().attribute("errorOperacion",
                        org.hamcrest.Matchers.containsString("lo usan 1 servicio")));
        org.junit.jupiter.api.Assertions.assertTrue(tipoRepository.existsById(kine.getId()));

        servicioRepository.deleteAll();
        mockMvc.perform(post("/tipos-servicio/" + kine.getId() + "/eliminar"))
                .andExpect(flash().attributeExists("mensajeExito"));
        org.junit.jupiter.api.Assertions.assertFalse(tipoRepository.existsById(kine.getId()));
    }

    @Test
    void elFormularioDeServiciosNoPideDatosDelCliente() throws Exception {
        mockMvc.perform(get("/servicios/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pago por hora al socio")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("clienteId"))));
    }

    @Test
    void eliminaUnTurnoDesdeElCalendario() throws Exception {
        Socio socio = socioRepository.saveAndFlush(crearSocio("30111222", EstadoSocio.DISPONIBLE_REEMPLAZO));
        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());
        guardarAsistencia(socio, servicio, LocalDate.now());
        Long id = asistenciaRepository.findAll().get(0).getId();

        mockMvc.perform(post("/asistencias/" + id + "/eliminar")).andExpect(redirectedUrl("/"));
        org.junit.jupiter.api.Assertions.assertEquals(0, asistenciaRepository.count());

        mockMvc.perform(post("/asistencias/" + id + "/eliminar")).andExpect(status().isNotFound());
    }

    @Test
    void calculaPresupuestoConDescuentoEnEfectivoEIva() throws Exception {
        ServicioDomicilio servicio = servicioRepository.saveAndFlush(crearServicio());
        mockMvc.perform(get("/presupuestos/calculo")
                        .param("servicioId", servicio.getId().toString()).param("horas", "10")
                        .param("alicuotaIva", "IVA_21").param("pagoEfectivo", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("presupuestos"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .model().attributeExists("calculo"));
        CalculoPresupuesto calculo = CalculoPresupuesto.calcular(
                new BigDecimal("10"), new BigDecimal("1500.00"), AlicuotaIva.IVA_21, true);
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("15000.00"), calculo.subtotal());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("1500.00"), calculo.descuento());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("2835.00"), calculo.iva());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("16335.00"), calculo.total());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> CalculoPresupuesto.calcular(BigDecimal.ZERO, BigDecimal.TEN, AlicuotaIva.SIN_IVA, false));

        mockMvc.perform(get("/presupuestos/pdf")
                        .param("clienteId", clientePorDefecto().getId().toString())
                        .param("servicioId", servicio.getId().toString()).param("horas", "10")
                        .param("alicuotaIva", "IVA_21").param("pagoEfectivo", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    private Socio crearSocio(String dni, EstadoSocio estado) {
        Socio socio = new Socio();
        socio.setNombre("Ana");
        socio.setApellido("Socio");
        socio.setDni(dni);
        socio.setDireccion("Calle 1");
        socio.setTelefono("1123456789");
        socio.setCbu(dni + "12345678901234");
        socio.setEstado(estado);
        socio.setEspecialidad(Especialidad.CUIDADOR);
        return socio;
    }

    private ServicioDomicilio crearServicio() {
        ServicioDomicilio servicio = new ServicioDomicilio();
        servicio.setValorHora(new BigDecimal("1500.00"));
        servicio.setValorHoraSocio(new BigDecimal("1000.00"));
        servicio.setTipoServicio(tipo("Por horas"));
        return servicio;
    }

    private TipoServicio tipo(String nombre) {
        return tipoRepository.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> tipoRepository.save(new TipoServicio(nombre)));
    }

    private Cliente crearCliente(String nombrePaciente, String domicilio) {
        Cliente cliente = new Cliente();
        cliente.setNombreFamiliar("Juan Cliente");
        cliente.setTelefono("1123456789");
        cliente.setNombrePaciente(nombrePaciente);
        cliente.setDomicilio(domicilio);
        return clienteRepository.saveAndFlush(cliente);
    }

    private Cliente clientePorDefecto() {
        return clienteRepository.findAll().stream()
                .filter(c -> "Paciente".equals(c.getNombrePaciente())).findFirst()
                .orElseGet(() -> crearCliente("Paciente", "Calle 123"));
    }

    private void guardarAsistencia(Socio socio, ServicioDomicilio servicio, LocalDate fecha) {
        guardarAsistencia(socio, clientePorDefecto(), servicio, fecha);
    }

    private void guardarAsistencia(Socio socio, Cliente cliente, ServicioDomicilio servicio, LocalDate fecha) {
        Asistencia asistencia = new Asistencia();
        asistencia.setSocio(socio);
        asistencia.setCliente(cliente);
        asistencia.setServicioDomicilio(servicio);
        asistencia.setFecha(fecha);
        asistencia.setHoraInicio(LocalTime.of(8, 0));
        asistencia.setHoraFin(LocalTime.of(10, 0));
        asistenciaRepository.saveAndFlush(asistencia);
    }
}
