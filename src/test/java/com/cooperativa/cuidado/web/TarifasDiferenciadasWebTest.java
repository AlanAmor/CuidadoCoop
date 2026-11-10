package com.cooperativa.cuidado.web;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.feriado.FeriadoNacionalRepository;
import com.cooperativa.cuidado.liquidacion.dto.LiquidacionMensualSocio;
import com.cooperativa.cuidado.liquidacion.service.LiquidacionService;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.servicio.TipoServicio;
import com.cooperativa.cuidado.servicio.TipoServicioRepository;
import com.cooperativa.cuidado.socio.Especialidad;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tarifastest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seguridad.activa=false"
})
@AutoConfigureMockMvc
@Transactional
class TarifasDiferenciadasWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TipoServicioRepository tipoRepository;

    @Autowired
    private ServicioDomicilioRepository servicioRepository;

    @Autowired
    private SocioRepository socioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private FeriadoNacionalRepository feriadoRepository;

    @Autowired
    private LiquidacionService liquidacionService;

    private Socio socio;
    private Cliente cliente;
    private ServicioDomicilio servicio;

    @BeforeEach
    void prepararDatos() throws Exception {
        asistenciaRepository.deleteAll();
        feriadoRepository.deleteAll();
        servicioRepository.deleteAll();
        clienteRepository.deleteAll();
        socioRepository.deleteAll();

        TipoServicio tipo = tipoRepository.findByNombreIgnoreCase("Cuidado integral")
                .orElseGet(() -> tipoRepository.save(new TipoServicio("Cuidado integral")));

        mockMvc.perform(post("/servicios")
                        .param("tipoServicioId", tipo.getId().toString())
                        // Cliente: Semana (<12h / >=12h), Finde (<12h / >=12h), Feriado (<12h / >=12h)
                        .param("valorHora", "5000.00")
                        .param("valorHoraClienteSemanaMas12", "4600.00")
                        .param("valorHoraClienteFindeMenos12", "6000.00")
                        .param("valorHoraClienteFindeMas12", "5500.00")
                        .param("valorHoraClienteFeriadoMenos12", "8000.00")
                        .param("valorHoraClienteFeriadoMas12", "7400.00")
                        // Socio: Semana (<12h / >=12h), Finde (<12h / >=12h), Feriado (<12h / >=12h)
                        .param("valorHoraSocio", "3500.00")
                        .param("valorHoraSocioSemanaMas12", "3200.00")
                        .param("valorHoraSocioFindeMenos12", "4200.00")
                        .param("valorHoraSocioFindeMas12", "3900.00")
                        .param("valorHoraSocioFeriadoMenos12", "6000.00")
                        .param("valorHoraSocioFeriadoMas12", "5500.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/servicios"));

        servicio = servicioRepository.findAll().get(0);

        Socio s = new Socio();
        s.setNombre("Lucía");
        s.setApellido("Martínez");
        s.setDni("34555666");
        s.setDireccion("Av. Freyre 2100");
        s.setTelefono("3425111222");
        s.setCbu("0000003100000034555666");
        s.setEstado(EstadoSocio.DISPONIBLE_REEMPLAZO);
        s.setEspecialidad(Especialidad.CUIDADOR);
        socio = socioRepository.save(s);

        Cliente c = new Cliente();
        c.setNombrePaciente("Marta Sosa");
        c.setNombreFamiliar("Jorge Sosa");
        c.setDomicilio("San Jerónimo 1800");
        c.setTelefono("3424999888");
        cliente = clienteRepository.save(c);
    }

    @Test
    void aplicaLasSeisTarifasDeClienteYSocioSegunDuracionTipoDeDiaYFeriado() throws Exception {
        // Cargar feriados nacionales fijos de 2026 (incluye 01/05 y 25/05)
        mockMvc.perform(post("/feriados/cargar-fijos").param("anio", "2026"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/api/feriados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9));

        // 1) Día de semana < 12 hs: Miércoles 06/05/2026 de 08:00 a 16:00 (8 hs)
        asignarTurnoSimple("2026-05-06", "08:00", "16:00", null, false);

        // 2) Día de semana >= 12 hs (límite exacto 12 hs): Jueves 07/05/2026 de 08:00 a 20:00 (12 hs)
        asignarTurnoSimple("2026-05-07", "08:00", "20:00", null, false);

        // 3) Fin de semana < 12 hs: Sábado 09/05/2026 de 08:00 a 16:00 (8 hs)
        asignarTurnoSimple("2026-05-09", "08:00", "16:00", null, false);

        // 4) Fin de semana >= 12 hs: Domingo 10/05/2026 de 08:00 a 21:00 (13 hs)
        asignarTurnoSimple("2026-05-10", "08:00", "21:00", null, false);

        // 5) Feriado nacional automático < 12 hs: Viernes 01/05/2026 de 08:00 a 16:00 (8 hs)
        asignarTurnoSimple("2026-05-01", "08:00", "16:00", null, false);

        // 6) Feriado nacional automático >= 12 hs: Lunes 25/05/2026 de 08:00 a 20:00 (12 hs)
        asignarTurnoSimple("2026-05-25", "08:00", "20:00", null, false);

        Asistencia tSemM12 = turnoEn(LocalDate.of(2026, 5, 6));
        assertEquals(0, new BigDecimal("5000.00").compareTo(tSemM12.getValorHoraAplicado()));
        assertEquals(0, new BigDecimal("3500.00").compareTo(tSemM12.getValorHoraPagoSocio()));
        assertFalse(tSemM12.isFeriadoNacionalEfectivo());

        Asistencia tSemP12 = turnoEn(LocalDate.of(2026, 5, 7));
        assertEquals(0, new BigDecimal("12.00").compareTo(tSemP12.getHorasTotales()));
        assertEquals(0, new BigDecimal("4600.00").compareTo(tSemP12.getValorHoraAplicado()));
        assertEquals(0, new BigDecimal("3200.00").compareTo(tSemP12.getValorHoraPagoSocio()));

        Asistencia tFinM12 = turnoEn(LocalDate.of(2026, 5, 9));
        assertEquals(0, new BigDecimal("6000.00").compareTo(tFinM12.getValorHoraAplicado()));
        assertEquals(0, new BigDecimal("4200.00").compareTo(tFinM12.getValorHoraPagoSocio()));

        Asistencia tFinP12 = turnoEn(LocalDate.of(2026, 5, 10));
        assertEquals(0, new BigDecimal("5500.00").compareTo(tFinP12.getValorHoraAplicado()));
        assertEquals(0, new BigDecimal("3900.00").compareTo(tFinP12.getValorHoraPagoSocio()));

        Asistencia tFerM12 = turnoEn(LocalDate.of(2026, 5, 1));
        assertTrue(tFerM12.isFeriadoNacionalEfectivo());
        assertEquals(0, new BigDecimal("8000.00").compareTo(tFerM12.getValorHoraAplicado()));
        assertEquals(0, new BigDecimal("6000.00").compareTo(tFerM12.getValorHoraPagoSocio()));

        Asistencia tFerP12 = turnoEn(LocalDate.of(2026, 5, 25));
        assertTrue(tFerP12.isFeriadoNacionalEfectivo());
        assertEquals(0, new BigDecimal("7400.00").compareTo(tFerP12.getValorHoraAplicado()));
        assertEquals(0, new BigDecimal("5500.00").compareTo(tFerP12.getValorHoraPagoSocio()));

        // Verificar impacto en Liquidación Socio y Liquidación Familia
        LiquidacionMensualSocio liqSocio = liquidacionService.calcularRetornoMensual(socio.getId(), YearMonth.of(2026, 5));
        // Horas socio: 8 + 12 + 8 + 13 + 8 + 12 = 61 hs
        // Importe socio: (8*3500) + (12*3200) + (8*4200) + (13*3900) + (8*6000) + (12*5500)
        // = 28000 + 38400 + 33600 + 50700 + 48000 + 66000 = 264700.00
        assertEquals(0, new BigDecimal("61.00").compareTo(liqSocio.totalHoras()));
        assertEquals(0, new BigDecimal("264700.00").compareTo(liqSocio.totalRetorno()));

        mockMvc.perform(get("/api/liquidaciones-familia/previsualizar")
                        .param("clienteId", cliente.getId().toString())
                        .param("periodo", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuidadores.length()").value(6));
    }

    @Test
    void permiteMarcarYDesmarcarFeriadoManualmenteYEnEdicionDeCalendario() throws Exception {
        mockMvc.perform(post("/feriados")
                        .param("fecha", "2026-06-15")
                        .param("descripcion", "Paso a la Inmortalidad de Güemes (trasladable)"))
                .andExpect(status().is3xxRedirection());

        // 1) Desmarcar manualmente una fecha que está en el calendario de feriados (15/06/2026, lunes)
        asignarTurnoSimple("2026-06-15", "08:00", "16:00", false, true);
        Asistencia desmarcado = turnoEn(LocalDate.of(2026, 6, 15));
        assertFalse(desmarcado.isFeriadoNacionalEfectivo());
        assertEquals(0, new BigDecimal("5000.00").compareTo(desmarcado.getValorHoraAplicado()));
        assertEquals(0, new BigDecimal("3500.00").compareTo(desmarcado.getValorHoraPagoSocio()));

        // 2) Marcar manualmente como feriado un día común (martes 16/06/2026)
        asignarTurnoSimple("2026-06-16", "08:00", "16:00", true, true);
        Asistencia marcadoManual = turnoEn(LocalDate.of(2026, 6, 16));
        assertTrue(marcadoManual.isFeriadoNacionalEfectivo());
        assertEquals(0, new BigDecimal("8000.00").compareTo(marcadoManual.getValorHoraAplicado()));
        assertEquals(0, new BigDecimal("6000.00").compareTo(marcadoManual.getValorHoraPagoSocio()));

        // 3) Editar desde el calendario el turno del 15/06 a >= 12 hs y marcarlo como feriado
        String jsonEdicion = """
                {
                  "socioId": %d,
                  "clienteId": %d,
                  "servicioId": %d,
                  "fecha": "2026-06-15",
                  "horaInicio": "08:00",
                  "horaFin": "20:00",
                  "feriadoNacional": true
                }
                """.formatted(socio.getId(), cliente.getId(), servicio.getId());

        mockMvc.perform(post("/api/asistencias/" + desmarcado.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEdicion))
                .andExpect(status().isOk());

        Asistencia editado = asistenciaRepository.findById(desmarcado.getId()).orElseThrow();
        assertTrue(editado.isFeriadoNacionalEfectivo());
        assertEquals(0, new BigDecimal("12.00").compareTo(editado.getHorasTotales()));
        assertEquals(0, new BigDecimal("7400.00").compareTo(editado.getValorHoraAplicado()));
        assertEquals(0, new BigDecimal("5500.00").compareTo(editado.getValorHoraPagoSocio()));
    }

    @Test
    void serieCuidadoFijoAplicaAutomaticamenteSemanaFindeYFeriadoPorCadaFecha() throws Exception {
        // Viernes 08/05 feriado registrado, Sábado 09/05 finde, Lunes 11/05 semana (todos de 12 hs: 08:00 a 20:00)
        mockMvc.perform(post("/feriados")
                        .param("fecha", "2026-05-08")
                        .param("descripcion", "Feriado puente nacional"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/asignaciones")
                        .param("socioId", socio.getId().toString())
                        .param("clienteId", cliente.getId().toString())
                        .param("servicioDomicilioId", servicio.getId().toString())
                        .param("fecha", "2026-05-08")
                        .param("horaInicio", "08:00")
                        .param("horaFin", "20:00")
                        .param("repetir", "true")
                        .param("unidad", "DIAS")
                        .param("cantidad", "4")
                        .param("dias", "FRIDAY", "SATURDAY", "MONDAY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/asignaciones/nueva"));

        List<Asistencia> creados = asistenciaRepository.findAll();
        assertEquals(3, creados.size());

        Asistencia viernesFeriado = turnoEn(LocalDate.of(2026, 5, 8));
        assertTrue(viernesFeriado.isFeriadoNacionalEfectivo());
        assertEquals(0, new BigDecimal("7400.00").compareTo(viernesFeriado.getValorHoraAplicado()));
        assertEquals(0, new BigDecimal("5500.00").compareTo(viernesFeriado.getValorHoraPagoSocio()));

        Asistencia sabadoFinde = turnoEn(LocalDate.of(2026, 5, 9));
        assertFalse(sabadoFinde.isFeriadoNacionalEfectivo());
        assertEquals(0, new BigDecimal("5500.00").compareTo(sabadoFinde.getValorHoraAplicado()));
        assertEquals(0, new BigDecimal("3900.00").compareTo(sabadoFinde.getValorHoraPagoSocio()));

        Asistencia lunesSemana = turnoEn(LocalDate.of(2026, 5, 11));
        assertFalse(lunesSemana.isFeriadoNacionalEfectivo());
        assertEquals(0, new BigDecimal("4600.00").compareTo(lunesSemana.getValorHoraAplicado()));
        assertEquals(0, new BigDecimal("3200.00").compareTo(lunesSemana.getValorHoraPagoSocio()));
    }

    @Test
    void buscadorIndexadoFiltraSociosYClientesCorrectamente() throws Exception {
        mockMvc.perform(get("/socios").param("q", "34555666"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Martínez, Lucía")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Buscador indexado de socios")));

        mockMvc.perform(get("/socios").param("q", "Inexistente9999"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Martínez, Lucía"))));

        mockMvc.perform(get("/clientes").param("q", "San Jerónimo"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Marta Sosa")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Buscador indexado de clientes")));

        mockMvc.perform(get("/clientes").param("q", "PacienteInexistente9999"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Marta Sosa"))));
    }

    @Test
    void historicoPermiteBuscarGeneralPorSocioPorClienteCruceYPeriodoOHistoricoTotal() throws Exception {
        LocalDate fechaFutura = LocalDate.now().plusDays(15);
        asignarTurnoSimple("2026-05-06", "08:00", "16:00", null, false);
        asignarTurnoSimple(fechaFutura.toString(), "09:00", "17:00", null, false);

        // 1) Histórico general por período (incluye turno futuro si el período lo abarca)
        mockMvc.perform(get("/asistencias/historico")
                        .param("desde", "2026-05-01")
                        .param("hasta", fechaFutura.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2026-05-06")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(fechaFutura.toString())));

        // 2) Histórico total: contempla hasta el día de la fecha (NO incluye turnos futuros)
        mockMvc.perform(get("/asistencias/historico")
                        .param("socioId", socio.getId().toString())
                        .param("historicoTotal", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2026-05-06")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<td>" + fechaFutura + "</td>"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Histórico total hasta ")));

        // 3) Histórico de cada cliente en histórico total
        mockMvc.perform(get("/asistencias/historico")
                        .param("clienteId", cliente.getId().toString())
                        .param("historicoTotal", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Histórico de cada cliente por socio")));

        // 4) Histórico de socio por cliente y cliente por socio (PDF y Excel)
        mockMvc.perform(get("/asistencias/historico/pdf")
                        .param("socioId", socio.getId().toString())
                        .param("clienteId", cliente.getId().toString())
                        .param("historicoTotal", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        mockMvc.perform(get("/asistencias/historico/excel")
                        .param("clienteId", cliente.getId().toString())
                        .param("socioId", socio.getId().toString())
                        .param("historicoTotal", "true"))
                .andExpect(status().isOk());
    }

    private void asignarTurnoSimple(
            String fecha,
            String horaInicio,
            String horaFin,
            Boolean feriado,
            boolean desdeFormularioWeb) throws Exception {
        var req = post("/asignaciones")
                .param("socioId", socio.getId().toString())
                .param("clienteId", cliente.getId().toString())
                .param("servicioDomicilioId", servicio.getId().toString())
                .param("fecha", fecha)
                .param("horaInicio", horaInicio)
                .param("horaFin", horaFin);
        if (desdeFormularioWeb) {
            req.param("feriadoNacionalEspecificado", "true");
            if (Boolean.TRUE.equals(feriado)) {
                req.param("feriadoNacional", "true");
            }
        } else if (feriado != null) {
            req.param("feriadoNacional", feriado.toString());
        }
        mockMvc.perform(req)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/asignaciones/nueva"));
    }

    private Asistencia turnoEn(LocalDate fecha) {
        return asistenciaRepository.findAll().stream()
                .filter(a -> fecha.equals(a.getFecha()))
                .findFirst()
                .orElseThrow();
    }
}
