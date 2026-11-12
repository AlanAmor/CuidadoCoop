package com.cooperativa.cuidado.web;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.cuenta.RolUsuario;
import com.cooperativa.cuidado.cuenta.RolUsuarioRepository;
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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cuentatest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seguridad.activa=true"
})
@AutoConfigureMockMvc
@Transactional
class CuentaRolesWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioCuentaRepository usuarioRepository;

    @Autowired
    private RolUsuarioRepository rolRepository;

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

    @Test
    void redirigeALoginCuandoNoHaySesionActiva() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/api/calendario")
                        .param("desde", "2026-10-01")
                        .param("hasta", "2026-10-07"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void iniciaSesionConCredencialesDeEjemploYAdministraCuentasYRoles() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "clave-incorrecta"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(Matchers.containsString("incorrectos")));

        MockHttpSession sesionAdmin = new MockHttpSession();
        mockMvc.perform(post("/login")
                        .session(sesionAdmin)
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/cuentas").session(sesionAdmin))
                .andExpect(status().isOk())
                .andExpect(view().name("cuentas"))
                .andExpect(content().string(Matchers.containsString("admin")))
                .andExpect(content().string(Matchers.containsString("ayudante")))
                .andExpect(content().string(Matchers.containsString("socio")));

        mockMvc.perform(get("/cuentas/roles").session(sesionAdmin))
                .andExpect(status().isOk())
                .andExpect(view().name("roles"))
                .andExpect(content().string(Matchers.containsString("GESTIONAR_RETENCIONES")))
                .andExpect(content().string(Matchers.containsString("REALIZAR_LIQUIDACIONES_FAMILIA")))
                .andExpect(content().string(Matchers.containsString("GESTIONAR_FERIADOS")))
                .andExpect(content().string(Matchers.containsString("GESTIONAR_MARCACIONES_TURNOS")))
                .andExpect(content().string(Matchers.containsString("CONFIGURAR_CORREO")));

        // Configurar cuenta de correo desde /configuracion/correo
        mockMvc.perform(get("/configuracion/correo").session(sesionAdmin))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracion-correo"));

        mockMvc.perform(post("/configuracion/correo")
                        .session(sesionAdmin)
                        .param("host", "smtp.gmail.com")
                        .param("puerto", "587")
                        .param("username", "grupoazul.ucd@gmail.com")
                        .param("password", "abcd efgh ijkl mnop")
                        .param("remitenteNombre", "Cooperativa Grupo Azul")
                        .param("usarTls", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion/correo"));

        // Crear un nuevo rol personalizado con casillas de funciones
        mockMvc.perform(post("/cuentas/roles")
                        .session(sesionAdmin)
                        .param("nombre", "AUDITOR")
                        .param("soloTurnosPropios", "false")
                        .param("permisos", "VER_ESTADISTICAS", "EXPORTAR_HISTORICOS", "GESTIONAR_RETENCIONES", "REALIZAR_LIQUIDACIONES_FAMILIA", "GESTIONAR_FERIADOS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cuentas/roles"));

        RolUsuario rolAuditor = rolRepository.findByNombreIgnoreCase("AUDITOR").orElseThrow();
        assertEquals(5, rolAuditor.getPermisos().size());

        // Crear una cuenta asignada al nuevo rol con nombre, apellido y DNI
        mockMvc.perform(post("/cuentas")
                        .session(sesionAdmin)
                        .param("username", "auditor1")
                        .param("password", "auditor123")
                        .param("nombre", "Pedro")
                        .param("apellido", "Auditor")
                        .param("dni", "33444555")
                        .param("rolId", rolAuditor.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cuentas"));

        assertTrue(usuarioRepository.existsByUsernameIgnoreCase("auditor1"));
    }

    @Test
    void rolAyudanteRespetaPermisosExactosSolicitados() throws Exception {
        UsuarioCuenta ayudante = usuarioRepository.findByUsernameIgnoreCase("ayudante").orElseThrow();
        MockHttpSession sesionAyudante = new MockHttpSession();
        sesionAyudante.setAttribute(SesionHelper.ATRIBUTO_SESION_USUARIO_ID, ayudante.getId());

        // Puede acceder a asignar turnos, socios, clientes y presupuestos
        mockMvc.perform(get("/asignaciones/nueva").session(sesionAyudante))
                .andExpect(status().isOk());
        mockMvc.perform(get("/socios").session(sesionAyudante))
                .andExpect(status().isOk());
        mockMvc.perform(get("/clientes").session(sesionAyudante))
                .andExpect(status().isOk());
        mockMvc.perform(get("/presupuestos").session(sesionAyudante))
                .andExpect(status().isOk());

        // No puede acceder a estadísticas, liquidaciones, retenciones, liquidación familia, feriados ni cuentas
        mockMvc.perform(get("/estadisticas").session(sesionAyudante))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/liquidaciones").session(sesionAyudante))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/retenciones").session(sesionAyudante))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/liquidaciones-familia").session(sesionAyudante))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/feriados").session(sesionAyudante))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/cuentas").session(sesionAyudante))
                .andExpect(status().isForbidden());

        // No puede eliminar socios ni clientes
        mockMvc.perform(post("/socios/1/eliminar").session(sesionAyudante))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/clientes/1/eliminar").session(sesionAyudante))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolSocioSoloVeElCalendarioConSusTurnosVinculadosPorDni() throws Exception {
        UsuarioCuenta cuentaSocio = usuarioRepository.findByUsernameIgnoreCase("socio").orElseThrow();
        Socio socioPropio = socioRepository.findByDni(cuentaSocio.getDni()).orElseThrow();

        Socio otroSocio = new Socio();
        otroSocio.setNombre("Carlos");
        otroSocio.setApellido("Otro");
        otroSocio.setDni("29888777");
        otroSocio.setDireccion("Calle 2");
        otroSocio.setTelefono("1140009999");
        otroSocio.setCbu("2850590900000000999999");
        otroSocio.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);
        otroSocio.setEspecialidad(Especialidad.CUIDADOR);
        otroSocio = socioRepository.save(otroSocio);

        Cliente cliente = new Cliente();
        cliente.setNombreFamiliar("Familiar");
        cliente.setTelefono("1140001111");
        cliente.setNombrePaciente("Paciente Prueba");
        cliente.setDomicilio("Domicilio 100");
        cliente = clienteRepository.save(cliente);

        TipoServicio tipo = tipoRepository.findAll().get(0);
        ServicioDomicilio servicio = new ServicioDomicilio();
        servicio.setTipoServicio(tipo);
        servicio.setValorHora(new BigDecimal("2000.00"));
        servicio.setValorHoraSocio(new BigDecimal("1500.00"));
        servicio = servicioRepository.save(servicio);

        LocalDate hoy = LocalDate.now();

        Asistencia turnoPropio = new Asistencia();
        turnoPropio.setSocio(socioPropio);
        turnoPropio.setCliente(cliente);
        turnoPropio.setServicioDomicilio(servicio);
        turnoPropio.setFecha(hoy);
        turnoPropio.setHoraInicio(LocalTime.of(8, 0));
        turnoPropio.setHoraFin(LocalTime.of(12, 0));
        asistenciaRepository.save(turnoPropio);

        Asistencia turnoAjeno = new Asistencia();
        turnoAjeno.setSocio(otroSocio);
        turnoAjeno.setCliente(cliente);
        turnoAjeno.setServicioDomicilio(servicio);
        turnoAjeno.setFecha(hoy);
        turnoAjeno.setHoraInicio(LocalTime.of(14, 0));
        turnoAjeno.setHoraFin(LocalTime.of(18, 0));
        asistenciaRepository.save(turnoAjeno);

        MockHttpSession sesionSocio = new MockHttpSession();
        sesionSocio.setAttribute(SesionHelper.ATRIBUTO_SESION_USUARIO_ID, cuentaSocio.getId());

        // Antes de tener turnos propios, muestra el aviso de que no tiene turnos asignados (y no un error 404)
        asistenciaRepository.delete(turnoPropio);
        asistenciaRepository.flush();
        mockMvc.perform(get("/").session(sesionSocio))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("No tenés turnos asignados.")));

        Asistencia nuevoTurnoPropio = new Asistencia();
        nuevoTurnoPropio.setSocio(socioPropio);
        nuevoTurnoPropio.setCliente(cliente);
        nuevoTurnoPropio.setServicioDomicilio(servicio);
        nuevoTurnoPropio.setFecha(hoy);
        nuevoTurnoPropio.setHoraInicio(LocalTime.of(8, 0));
        nuevoTurnoPropio.setHoraFin(LocalTime.of(12, 0));
        asistenciaRepository.save(nuevoTurnoPropio);

        mockMvc.perform(get("/").session(sesionSocio))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Mis turnos asignados")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Socios disponibles"))));

        mockMvc.perform(get("/api/calendario").session(sesionSocio)
                        .param("desde", hoy.toString())
                        .param("hasta", hoy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos.length()").value(1))
                .andExpect(jsonPath("$.eventos[0].dniSocio").value(cuentaSocio.getDni()));

        // El socio no puede navegar a otras secciones
        mockMvc.perform(get("/socios").session(sesionSocio))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/asignaciones/nueva").session(sesionSocio))
                .andExpect(status().isForbidden());
    }
}

