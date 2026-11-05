package com.cooperativa.cuidado.web;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.recibo.ReciboRetribucion;
import com.cooperativa.cuidado.recibo.ReciboRetribucionRepository;
import com.cooperativa.cuidado.retencion.RetencionSocioRepository;
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
        "spring.datasource.url=jdbc:h2:mem:recibotest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seguridad.activa=false"
})
@AutoConfigureMockMvc
@Transactional
class ReciboRetribucionWebTest {

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
    private RetencionSocioRepository retencionRepository;

    @Autowired
    private ReciboRetribucionRepository reciboRepository;

    private Socio socio;
    private Cliente cliente;
    private ServicioDomicilio servicio;

    @BeforeEach
    void prepararDatos() {
        reciboRepository.deleteAll();
        retencionRepository.deleteAll();
        asistenciaRepository.deleteAll();
        servicioRepository.deleteAll();
        clienteRepository.deleteAll();
        socioRepository.deleteAll();

        Socio s = new Socio();
        s.setNombre("Carolina Andrea");
        s.setApellido("Barquin");
        s.setDni("30961836");
        s.setCuil("27-30961836-4");
        s.setDireccion("Córdoba 1234");
        s.setLocalidad("Arroyo Aguiar");
        s.setCodigoPostal("3014");
        s.setTelefono("3425001122");
        s.setCorreo("carolina@coop.com");
        s.setCbu("0000003100012345678901");
        s.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);
        s.setEspecialidad(Especialidad.CUIDADOR);
        socio = socioRepository.save(s);

        Cliente c = new Cliente();
        c.setNombreFamiliar("Familiar Test");
        c.setNombrePaciente("Paciente Test");
        c.setDomicilio("San Martín 2000");
        c.setTelefono("3424112233");
        cliente = clienteRepository.save(c);

        TipoServicio tipo = tipoServicioRepository.findByNombreIgnoreCase("Por horas")
                .orElseGet(() -> tipoServicioRepository.save(new TipoServicio("Por horas")));

        ServicioDomicilio srv = new ServicioDomicilio();
        srv.setTipoServicio(tipo);
        srv.setValorHora(new BigDecimal("5000.00"));
        srv.setValorHoraSocio(new BigDecimal("3500.00"));
        servicio = servicioRepository.save(srv);
    }

    @Test
    void permiteCargarViaticosYSanatoriosEnTurnoFinalizadoYPrevisualizarRecibo() throws Exception {
        Asistencia turnoPasado = new Asistencia();
        turnoPasado.setSocio(socio);
        turnoPasado.setCliente(cliente);
        turnoPasado.setServicioDomicilio(servicio);
        turnoPasado.setFecha(LocalDate.of(2026, 4, 10));
        turnoPasado.setHoraInicio(LocalTime.of(8, 0));
        turnoPasado.setHoraFin(LocalTime.of(20, 0));
        turnoPasado = asistenciaRepository.save(turnoPasado);

        mockMvc.perform(post("/api/asistencias/" + turnoPasado.getId() + "/adicionales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"importeViatico\": 3440.00, \"importeSanatorio\": 5200.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());

        mockMvc.perform(post("/retenciones")
                        .param("socioId", socio.getId().toString())
                        .param("periodo", "2026-04")
                        .param("tipo", "OTRAS_RETENCIONES")
                        .param("motivo", "Adelanto farmacia")
                        .param("unidad", "1")
                        .param("valorUnidad", "4000.00"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/api/liquidaciones/previsualizar")
                        .param("socioId", socio.getId().toString())
                        .param("mes", "2026-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreApellidoSocio").value("CAROLINA ANDREA BARQUIN"))
                .andExpect(jsonPath("$.cpSocio").value("3014"))
                .andExpect(jsonPath("$.localidadSocio").value("Arroyo Aguiar"))
                .andExpect(jsonPath("$.cbuSocio").value("0000003100012345678901"))
                .andExpect(jsonPath("$.horasCuidados").value(12.0))
                .andExpect(jsonPath("$.subtotalHoras").value(42000.0))
                .andExpect(jsonPath("$.unidadesViaticos").value(1))
                .andExpect(jsonPath("$.subtotalViaticos").value(3440.0))
                .andExpect(jsonPath("$.unidadesSanatorio").value(1))
                .andExpect(jsonPath("$.subtotalSanatorio").value(5200.0))
                .andExpect(jsonPath("$.subtotalCuidados").value(50640.0))
                .andExpect(jsonPath("$.retencionesPeriodo[0].motivo").value("Adelanto farmacia"));
    }

    @Test
    void emiteRecibosCorrelativosRespetaValoresPorDefectoYExportaPdfYExcelConMultiplesRetenciones() throws Exception {
        Asistencia turno = new Asistencia();
        turno.setSocio(socio);
        turno.setCliente(cliente);
        turno.setServicioDomicilio(servicio);
        turno.setFecha(LocalDate.of(2026, 4, 15));
        turno.setHoraInicio(LocalTime.of(8, 0));
        turno.setHoraFin(LocalTime.of(16, 0));
        turno.setImporteViatico(new BigDecimal("3000.00"));
        asistenciaRepository.save(turno);

        // Recibo 1: sin modificar opcionales -> queda tal cual la planilla por defecto y número N° 00001
        mockMvc.perform(post("/liquidaciones/recibos")
                        .param("socioId", socio.getId().toString())
                        .param("mesSeleccionado", "2026-04")
                        .param("fechaEmision", "2026-05-02")
                        .param("periodoDesde", "2026-04-01")
                        .param("periodoHasta", "2026-04-30")
                        .param("fechaPago", "2026-05-02")
                        .param("comprobanteTransferencia", "TRF-998877"))
                .andExpect(status().is3xxRedirection());

        // Recibo 2: con excedente anual + monotributo + 2 filas de Otras Retenciones -> sube a N° 00002
        mockMvc.perform(post("/liquidaciones/recibos")
                        .param("socioId", socio.getId().toString())
                        .param("mesSeleccionado", "2026-04")
                        .param("fechaEmision", "2026-05-03")
                        .param("periodoDesde", "2026-04-01")
                        .param("periodoHasta", "2026-04-30")
                        .param("fechaPago", "2026-05-03")
                        .param("comprobanteTransferencia", "TRF-998878")
                        .param("incluirExcedenteAnual", "true")
                        .param("excedenteUnidad", "1")
                        .param("excedenteValorUnidad", "10000.00")
                        .param("totalExcedente", "10000.00")
                        .param("incluirMonotributo", "true")
                        .param("monotributoDetalle", "Retencion Pago Monotributo")
                        .param("monotributoUnidad", "1")
                        .param("monotributoValorUnidad", "5000.00")
                        .param("incluirOtrasRetenciones", "true")
                        .param("otrasRetencionesDetalle", "Descuento ambo", "Incumplimiento Art. 5")
                        .param("otrasRetencionesUnidad", "1", "2")
                        .param("otrasRetencionesValorUnidad", "2000.00", "1500.00"))
                .andExpect(status().is3xxRedirection());

        List<ReciboRetribucion> emitidos = reciboRepository.findAllByOrderByNumeroReciboDesc();
        assertThat(emitidos).hasSize(2);

        ReciboRetribucion primero = emitidos.get(1);
        assertThat(primero.getNumeroFormateado()).isEqualTo("N° 00001");
        assertThat(primero.getOtrosConcepto()).isEqualTo("-");
        assertThat(primero.getExcedenteUnidad()).isEqualByComparingTo("1.00");
        assertThat(primero.getTotalExcedente()).isEqualByComparingTo("0.00");
        assertThat(primero.getItemsRetencion()).hasSize(3);
        assertThat(primero.getItemsRetencion().get(2).getDetalle())
                .isEqualTo("Retención por Incumplimiento Articulo 5 Reglamento Interno");
        // 8h * 3500 = 28000 + 3000 viatico = 31000
        assertThat(primero.getRetribucionNeta()).isEqualByComparingTo("31000.00");

        ReciboRetribucion segundo = emitidos.get(0);
        assertThat(segundo.getNumeroFormateado()).isEqualTo("N° 00002");
        assertThat(segundo.getItemsRetencion()).hasSize(4); // Monotributo + Préstamo + 2 Otras Retenciones
        // Total retribucion 31000 + Excedente 10000 - Retenciones (5000 + 2000 + 3000 = 10000) = 31000
        assertThat(segundo.getTotalRetenciones()).isEqualByComparingTo("10000.00");
        assertThat(segundo.getRetribucionNeta()).isEqualByComparingTo("31000.00");

        byte[] pdfBytes = mockMvc.perform(get("/liquidaciones/recibos/" + segundo.getId() + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("recibo-retribucion-00002.pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");

        byte[] xlsxBytes = mockMvc.perform(get("/liquidaciones/recibos/" + segundo.getId() + "/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("recibo-retribucion-00002.xlsx")))
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            Sheet sh = wb.getSheetAt(0);
            assertThat(sh.getRow(7).getCell(9).getStringCellValue()).isEqualTo("N° 00002");
            assertThat(sh.getRow(14).getCell(1).getStringCellValue()).isEqualTo("CAROLINA ANDREA BARQUIN");
            assertThat(sh.getRow(17).getCell(4).getStringCellValue()).isEqualTo("CP: 3014");
            assertThat(sh.getRow(17).getCell(8).getStringCellValue()).isEqualTo("TRF-998878");
            assertThat(sh.getRow(37).getCell(2).getStringCellValue()).isEqualTo("Descuento ambo");
            assertThat(sh.getRow(38).getCell(2).getStringCellValue()).isEqualTo("Incumplimiento Art. 5");
            // Con 1 fila extra de Otras Retenciones, RETRIBUCIÓN NETA pasa de fila 40 (r=41) a fila 41 (r=42)
            assertThat(sh.getRow(41).getCell(8).getNumericCellValue()).isEqualTo(31000.0);
        }
    }
}
