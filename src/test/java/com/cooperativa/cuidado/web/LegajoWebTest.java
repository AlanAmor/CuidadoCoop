package com.cooperativa.cuidado.web;

import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.legajo.ArchivoLegajoRepository;
import com.cooperativa.cuidado.socio.Especialidad;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:legajotest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.legajos.directorio=target/legajos-test"
})
@AutoConfigureMockMvc
@Transactional
class LegajoWebTest {

    private static final byte[] PDF = "%PDF-1.4\n1 0 obj<<>>endobj\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocioRepository socioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ArchivoLegajoRepository archivoRepository;

    @Test
    void subeListaVeYEliminaArchivosDelLegajoDeUnSocio() throws Exception {
        Socio socio = socioRepository.save(crearSocio());
        MockMultipartFile factura = new MockMultipartFile("archivos", "factura.pdf", "application/pdf", PDF);

        mockMvc.perform(multipart("/api/legajos/socios/" + socio.getId()).file(factura))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivos.length()").value(1))
                .andExpect(jsonPath("$.mensaje").value("Se agregó 1 archivo al legajo."));

        mockMvc.perform(get("/api/legajos/socios/" + socio.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("factura.pdf"))
                .andExpect(jsonPath("$[0].tipoContenido").value("application/pdf"));

        Long archivoId = archivoRepository.findAll().get(0).getId();
        byte[] devuelto = mockMvc.perform(get("/legajos/archivos/" + archivoId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.startsWith("inline")))
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals(PDF.length, devuelto.length);

        mockMvc.perform(delete("/api/legajos/archivos/" + archivoId)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/legajos/archivos/" + archivoId)).andExpect(status().isNotFound());
        assertEquals(0, archivoRepository.count());
    }

    @Test
    void guardaPdfGrandesYVariosArchivosEnLaCarpetaDeCadaPersona() throws Exception {
        Socio socio = socioRepository.save(crearSocio());
        Cliente cliente = clienteRepository.save(crearCliente());
        byte[] grande = new byte[3 * 1024 * 1024];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);

        mockMvc.perform(multipart("/api/legajos/socios/" + socio.getId())
                        .file(new MockMultipartFile("archivos", "grande.pdf", "application/pdf", grande))
                        .file(new MockMultipartFile("archivos", "otra factura.pdf", "application/pdf", PDF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivos.length()").value(2));
        mockMvc.perform(multipart("/api/legajos/clientes/" + cliente.getId())
                        .file(new MockMultipartFile("archivos", "presupuesto.pdf", "application/pdf", PDF)))
                .andExpect(status().isOk());

        java.nio.file.Path carpetaSocio = java.nio.file.Path.of("target/legajos-test/socios/" + socio.getId());
        java.nio.file.Path carpetaCliente = java.nio.file.Path.of("target/legajos-test/clientes/" + cliente.getId());
        try (var archivos = java.nio.file.Files.list(carpetaSocio)) {
            assertEquals(2, archivos.count());
        }
        assertEquals(true, java.nio.file.Files.isDirectory(carpetaCliente));

        Long grandeId = archivoRepository.findAll().stream()
                .filter(a -> a.getNombre().equals("grande.pdf")).findFirst().orElseThrow().getId();
        byte[] devuelto = mockMvc.perform(get("/legajos/archivos/" + grandeId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertEquals(grande.length, devuelto.length);

        mockMvc.perform(post("/socios/" + socio.getId() + "/eliminar")).andExpect(status().is3xxRedirection());
        assertEquals(false, java.nio.file.Files.exists(carpetaSocio));
        mockMvc.perform(post("/clientes/" + cliente.getId() + "/eliminar")).andExpect(status().is3xxRedirection());
        assertEquals(false, java.nio.file.Files.exists(carpetaCliente));
    }

    @Test
    void rechazaArchivosNoAdmitidosOFalsificados() throws Exception {
        Socio socio = socioRepository.save(crearSocio());
        String url = "/api/legajos/socios/" + socio.getId();

        mockMvc.perform(multipart(url).file(new MockMultipartFile(
                        "archivos", "virus.exe", "application/octet-stream", "MZ".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("PDF, JPG y PNG")));

        mockMvc.perform(multipart(url).file(new MockMultipartFile(
                        "archivos", "falso.pdf", "application/pdf", "no soy un pdf".getBytes())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart(url).file(new MockMultipartFile(
                        "archivos", "vacio.pdf", "application/pdf", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("vacío")));

        mockMvc.perform(multipart(url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("ningún archivo")));

        assertEquals(0, archivoRepository.count());
    }

    @Test
    void noGuardaNadaSiUnoDeLosArchivosEsInvalido() throws Exception {
        Cliente cliente = clienteRepository.save(crearCliente());

        mockMvc.perform(multipart("/api/legajos/clientes/" + cliente.getId())
                        .file(new MockMultipartFile("archivos", "ok.pdf", "application/pdf", PDF))
                        .file(new MockMultipartFile("archivos", "mal.pdf", "application/pdf", "xx".getBytes())))
                .andExpect(status().isBadRequest());

        assertEquals(0, archivoRepository.count());
    }

    @Test
    void respondeConOrientacionSiElLegajoOElArchivoNoExisten() throws Exception {
        mockMvc.perform(get("/api/legajos/socios/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(get("/api/legajos/otros/1")).andExpect(status().isNotFound());
        mockMvc.perform(get("/legajos/archivos/999999")).andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnSocioOUnClienteBorraTambienSuLegajo() throws Exception {
        Socio socio = socioRepository.save(crearSocio());
        Cliente cliente = clienteRepository.save(crearCliente());
        MockMultipartFile pdf = new MockMultipartFile("archivos", "doc.pdf", "application/pdf", PDF);
        mockMvc.perform(multipart("/api/legajos/socios/" + socio.getId()).file(pdf)).andExpect(status().isOk());
        mockMvc.perform(multipart("/api/legajos/clientes/" + cliente.getId()).file(pdf)).andExpect(status().isOk());
        assertEquals(2, archivoRepository.count());

        mockMvc.perform(post("/socios/" + socio.getId() + "/eliminar")).andExpect(status().is3xxRedirection());
        assertEquals(1, archivoRepository.count());
        mockMvc.perform(post("/clientes/" + cliente.getId() + "/eliminar")).andExpect(status().is3xxRedirection());
        assertEquals(0, archivoRepository.count());
    }

    @Test
    void losFormulariosDeEdicionMuestranElPanelDeLegajoYElListadoElBoton() throws Exception {
        Socio socio = socioRepository.save(crearSocio());
        Cliente cliente = clienteRepository.save(crearCliente());

        mockMvc.perform(get("/socios/" + socio.getId() + "/editar"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-legajo-panel")));
        mockMvc.perform(get("/clientes/" + cliente.getId() + "/editar"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-legajo-panel")));
        mockMvc.perform(get("/socios/nuevo"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("data-legajo-panel"))));
        mockMvc.perform(get("/socios"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-legajo-tipo=\"socios\"")));
        mockMvc.perform(get("/clientes"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-legajo-tipo=\"clientes\"")));
    }

    private Socio crearSocio() {
        Socio socio = new Socio();
        socio.setNombre("Ana");
        socio.setApellido("Socio");
        socio.setDni("12345678");
        socio.setDireccion("Calle 1");
        socio.setTelefono("1123456789");
        socio.setCbu("1234567812345678901234");
        socio.setEstado(EstadoSocio.DISPONIBLE_REEMPLAZO);
        socio.setEspecialidad(Especialidad.CUIDADOR);
        return socio;
    }

    private Cliente crearCliente() {
        Cliente cliente = new Cliente();
        cliente.setNombreFamiliar("Juan Cliente");
        cliente.setTelefono("1123456789");
        cliente.setNombrePaciente("Paciente");
        cliente.setDomicilio("Calle 123");
        return cliente;
    }
}
