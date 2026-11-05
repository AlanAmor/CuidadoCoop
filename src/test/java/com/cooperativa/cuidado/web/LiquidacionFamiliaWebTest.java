package com.cooperativa.cuidado.web;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.liquidacionfamilia.LiquidacionFamilia;
import com.cooperativa.cuidado.liquidacionfamilia.LiquidacionFamiliaRepository;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.servicio.TipoServicio;
import com.cooperativa.cuidado.servicio.TipoServicioRepository;
import com.cooperativa.cuidado.socio.Especialidad;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:liqfamtest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seguridad.activa=false"
})
@AutoConfigureMockMvc
@Transactional
class LiquidacionFamiliaWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocioRepository socioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TipoServicioRepository tipoServicioRepository;

    @Autowired
    private ServicioDomicilioRepository servicioRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private LiquidacionFamiliaRepository liquidacionFamiliaRepository;

    private Socio socio1;
    private Socio socio2;
    private Cliente cliente;
    private ServicioDomicilio servicio;

    @BeforeEach
    void prepararDatos() {
        liquidacionFamiliaRepository.deleteAll();
        asistenciaRepository.deleteAll();
        servicioRepository.deleteAll();
        clienteRepository.deleteAll();
        socioRepository.deleteAll();

        socio1 = crearSocio("Romina", "Gómez", "31000111");
        socio2 = crearSocio("María", "Pérez", "32000222");

        Cliente c = new Cliente();
        c.setNombreFamiliar("Hijo Responsable");
        c.setNombrePaciente("Nélida Beatriz Gatti");
        c.setDniPaciente("5432109");
        c.setDomicilio("Urquiza 2450");
        c.setCiudad("Santa Fe");
        c.setLocalidad("Santa Fe");
        c.setCodigoPostal("3000");
        c.setTelefono("3425112233");
        cliente = clienteRepository.save(c);

        TipoServicio tipo = tipoServicioRepository.findByNombreIgnoreCase("Por horas")
                .orElseGet(() -> tipoServicioRepository.save(new TipoServicio("Por horas")));

        ServicioDomicilio srv = new ServicioDomicilio();
        srv.setTipoServicio(tipo);
        srv.setValorHora(new BigDecimal("4000.00"));
        srv.setValorHoraSocio(new BigDecimal("3000.00"));
        servicio = servicioRepository.save(srv);

        // Turnos en Mayo 2026: valorHoraAplicado (cliente) = 4000, valorHoraSocio = 3000
        registrarTurno(socio1, LocalDate.of(2026, 5, 4), LocalTime.of(8, 0), LocalTime.of(16, 0), "8.00");
        registrarTurno(socio1, LocalDate.of(2026, 5, 5), LocalTime.of(8, 0), LocalTime.of(20, 0), "12.00");
        registrarTurno(socio2, LocalDate.of(2026, 5, 6), LocalTime.of(8, 0), LocalTime.of(18, 0), "10.00");
    }

    @Test
    void vistaYPrevisualizacionCarganDatosPacienteYValorHoraCliente() throws Exception {
        mockMvc.perform(get("/liquidaciones-familia"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/liquidaciones-familia/previsualizar")
                        .param("clienteId", cliente.getId().toString())
                        .param("mes", "5")
                        .param("anio", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombrePaciente").value("Nélida Beatriz Gatti"))
                .andExpect(jsonPath("$.dniPaciente").value("5432109"))
                .andExpect(jsonPath("$.direccion").value("Urquiza 2450"))
                .andExpect(jsonPath("$.ciudad").value("Santa Fe"))
                .andExpect(jsonPath("$.localidad").value("Santa Fe"))
                .andExpect(jsonPath("$.codigoPostal").value("3000"))
                .andExpect(jsonPath("$.telefonoContacto").value("3425112233"))
                .andExpect(jsonPath("$.periodo").value("2026-05"))
                .andExpect(jsonPath("$.periodoEtiqueta").value("PERIODO MAYO 2026"))
                .andExpect(jsonPath("$.cuidadores.length()").value(2))
                .andExpect(jsonPath("$.cuidadores[0].nombreCuidador").value("Romina"))
                .andExpect(jsonPath("$.cuidadores[0].horas").value(20.0))
                .andExpect(jsonPath("$.cuidadores[0].valorHoraCliente").value(4000.0))
                .andExpect(jsonPath("$.cuidadores[0].totalLinea").value(80000.0))
                .andExpect(jsonPath("$.cuidadores[1].nombreCuidador").value("María"))
                .andExpect(jsonPath("$.cuidadores[1].horas").value(10.0))
                .andExpect(jsonPath("$.cuidadores[1].valorHoraCliente").value(4000.0))
                .andExpect(jsonPath("$.cuidadores[1].totalLinea").value(40000.0))
                .andExpect(jsonPath("$.subtotal").value(120000.0));
    }

    @Test
    void emisionConDosCuidadoresExportaExcelYPdfRespetandoFormatoYCriteriosVisuales() throws Exception {
        // Subtotal = 20*4000 + 10*4000 = 120000.00
        // IVA 21% = 25200.00 | Impuestos = 5800.00 -> Total Mensual / Saldo = 151000.00
        // Criterios visuales (3.5%, 5%, 4%) NO alteran ningún total monetario
        mockMvc.perform(post("/liquidaciones-familia")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("clienteId", cliente.getId().toString())
                        .param("mes", "5")
                        .param("anio", "2026")
                        .param("nombrePaciente", "Nélida Beatriz Gatti")
                        .param("dniPaciente", "5432109")
                        .param("direccion", "Urquiza 2450")
                        .param("ciudad", "Santa Fe")
                        .param("localidad", "Santa Fe")
                        .param("codigoPostal", "3000")
                        .param("telefonoContacto", "3425112233")
                        .param("cuidadorNombre", "Romina", "María")
                        .param("cuidadorHoras", "20.00", "10.00")
                        .param("cuidadorValorHora", "4000.00", "4000.00")
                        .param("porcentajeIva", "21")
                        .param("importeImpuestos", "5800.00")
                        .param("variacionIpcSalud", "3.5")
                        .param("transportePublico", "5")
                        .param("aumentoParitaria", "4"))
                .andExpect(status().is3xxRedirection());

        List<LiquidacionFamilia> emitidas = liquidacionFamiliaRepository.findAll();
        assertThat(emitidas).hasSize(1);
        LiquidacionFamilia liq = emitidas.get(0);

        assertThat(liq.getSubtotal()).isEqualByComparingTo("120000.00");
        assertThat(liq.getImporteIva()).isEqualByComparingTo("25200.00");
        assertThat(liq.getImporteImpuestos()).isEqualByComparingTo("5800.00");
        assertThat(liq.getTotalMensual()).isEqualByComparingTo("151000.00");
        assertThat(liq.getSaldoATransferir()).isEqualByComparingTo("151000.00");

        byte[] pdfBytes = mockMvc.perform(get("/liquidaciones-familia/{id}/pdf", liq.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        byte[] xlsxBytes = mockMvc.perform(get("/liquidaciones-familia/{id}/excel", liq.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            Sheet sh = wb.getSheetAt(0);
            assertThat(sh.getSheetName()).isEqualTo("LIQUIDACIÓN");
            assertThat(sh.getRow(3).getCell(1).getStringCellValue()).isEqualTo("Nélida Beatriz Gatti");
            assertThat(sh.getRow(4).getCell(1).getStringCellValue()).isEqualTo("DNI 5432109");
            assertThat(sh.getRow(5).getCell(1).getStringCellValue()).isEqualTo("Urquiza 2450");
            assertThat(sh.getRow(6).getCell(1).getStringCellValue()).isEqualTo("Santa Fe, Santa Fe, CP 3000.");
            assertThat(sh.getRow(7).getCell(1).getStringCellValue()).isEqualTo("Contacto: 3425112233");
            assertThat(sh.getRow(7).getCell(3).getStringCellValue()).isEqualTo(" PERIODO MAYO 2026");

            assertThat(sh.getRow(10).getCell(1).getStringCellValue()).isEqualTo("Romina");
            assertThat(sh.getRow(10).getCell(4).getNumericCellValue()).isEqualTo(80000.0);
            assertThat(sh.getRow(11).getCell(1).getStringCellValue()).isEqualTo("María");
            assertThat(sh.getRow(11).getCell(4).getNumericCellValue()).isEqualTo(40000.0);

            // Con 2 filas (delta = -2), SUBTOTAL queda en fila índice 12
            assertThat(sh.getRow(12).getCell(4).getNumericCellValue()).isEqualTo(120000.0);
            assertThat(sh.getRow(13).getCell(4).getNumericCellValue()).isEqualTo(25200.0);
            assertThat(sh.getRow(14).getCell(4).getNumericCellValue()).isEqualTo(5800.0);
            assertThat(sh.getRow(15).getCell(4).getNumericCellValue()).isEqualTo(151000.0);
            assertThat(sh.getRow(16).getCell(4).getNumericCellValue()).isEqualTo(151000.0);

            assertThat(sh.getRow(19).getCell(1).getStringCellValue()).isEqualTo("Variacion de IPC Salud: 3,5%");
            assertThat(sh.getRow(20).getCell(1).getStringCellValue()).isEqualTo("Transporte Publico: 5%");
            assertThat(sh.getRow(21).getCell(1).getStringCellValue()).isEqualTo("Aumento por Paritaria: 4%");
        }
    }

    @Test
    void emisionConSeisCuidadoresDesplazaFilasEnExcelSinErrores() throws Exception {
        mockMvc.perform(post("/liquidaciones-familia")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("clienteId", cliente.getId().toString())
                        .param("mes", "6")
                        .param("anio", "2026")
                        .param("cuidadorNombre", "Ana", "Belen", "Carla", "Diana", "Elena", "Florencia")
                        .param("cuidadorHoras", "10", "10", "10", "10", "10", "10")
                        .param("cuidadorValorHora", "4500", "4500", "4500", "4500", "4500", "4500")
                        .param("porcentajeIva", "10.5")
                        .param("importeImpuestos", "0")
                        .param("variacionIpcSalud", "0")
                        .param("transportePublico", "0")
                        .param("aumentoParitaria", "0"))
                .andExpect(status().is3xxRedirection());

        LiquidacionFamilia liq = liquidacionFamiliaRepository.findAll().get(0);
        // 6 * 45000 = 270000 | IVA 10.5% = 28350 -> Total = 298350
        assertThat(liq.getSubtotal()).isEqualByComparingTo("270000.00");
        assertThat(liq.getImporteIva()).isEqualByComparingTo("28350.00");
        assertThat(liq.getTotalMensual()).isEqualByComparingTo("298350.00");

        byte[] xlsxBytes = mockMvc.perform(get("/liquidaciones-familia/{id}/excel", liq.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            Sheet sh = wb.getSheetAt(0);
            assertThat(sh.getRow(15).getCell(1).getStringCellValue()).isEqualTo("Florencia");
            // Con 6 filas (delta = +2), SUBTOTAL queda en fila índice 16
            assertThat(sh.getRow(16).getCell(4).getNumericCellValue()).isEqualTo(270000.0);
            assertThat(sh.getRow(20).getCell(4).getNumericCellValue()).isEqualTo(298350.0);
        }

        mockMvc.perform(get("/liquidaciones-familia/{id}/pdf", liq.getId()))
                .andExpect(status().isOk());
    }

    private Socio crearSocio(String nombre, String apellido, String dni) {
        Socio s = new Socio();
        s.setNombre(nombre);
        s.setApellido(apellido);
        s.setDni(dni);
        s.setCuil("27-" + dni + "-4");
        s.setDireccion("Calle 123");
        s.setLocalidad("Santa Fe");
        s.setCodigoPostal("3000");
        s.setTelefono("3425000000");
        s.setCorreo(dni + "@coop.com");
        s.setCbu("0000003100000000000000");
        s.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);
        s.setEspecialidad(Especialidad.CUIDADOR);
        return socioRepository.save(s);
    }

    private void registrarTurno(Socio socio, LocalDate fecha, LocalTime inicio, LocalTime fin, String horas) {
        Asistencia a = new Asistencia();
        a.setSocio(socio);
        a.setCliente(cliente);
        a.setServicioDomicilio(servicio);
        a.setFecha(fecha);
        a.setHoraInicio(inicio);
        a.setHoraFin(fin);
        a.setHorasTotales(new BigDecimal(horas));
        a.setValorHoraAplicado(new BigDecimal("4000.00"));
        a.setValorHoraSocioAplicado(new BigDecimal("3000.00"));
        asistenciaRepository.save(a);
    }
}