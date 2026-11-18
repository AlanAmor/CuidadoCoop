package com.cooperativa.cuidado.web;

import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.facturacion.CondicionIva;
import com.cooperativa.cuidado.facturacion.Factura;
import com.cooperativa.cuidado.facturacion.FacturaRepository;
import com.cooperativa.cuidado.facturacion.TipoComprobante;
import com.cooperativa.cuidado.facturacion.TipoReceptor;
import com.cooperativa.cuidado.facturacion.dto.EmisionFacturaForm;
import com.cooperativa.cuidado.facturacion.dto.ItemLineaFacturaForm;
import com.cooperativa.cuidado.facturacion.service.ArcaFacturacionService;
import com.cooperativa.cuidado.liquidacionfamilia.LineaCuidadorFamilia;
import com.cooperativa.cuidado.liquidacionfamilia.LiquidacionFamilia;
import com.cooperativa.cuidado.liquidacionfamilia.LiquidacionFamiliaRepository;
import org.hamcrest.Matchers;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:facturawebtest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seguridad.activa=false"
})
@AutoConfigureMockMvc
@Transactional
class FacturacionWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArcaFacturacionService arcaService;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private LiquidacionFamiliaRepository liquidacionFamiliaRepository;

    private Cliente cliente;
    private LiquidacionFamilia liquidacion;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setNombrePaciente("Roberto Sánchez");
        cliente.setDniPaciente("12987654");
        cliente.setNombreFamiliar("Susana Sánchez");
        cliente.setTelefono("3424001122");
        cliente.setDomicilio("Belgrano 2500, Santa Fe");
        cliente = clienteRepository.save(cliente);

        liquidacion = new LiquidacionFamilia();
        liquidacion.setCliente(cliente);
        liquidacion.setNombrePaciente(cliente.getNombrePaciente());
        liquidacion.setDniPaciente(cliente.getDniPaciente());
        liquidacion.setDireccion(cliente.getDomicilio());
        liquidacion.setPeriodo("2026-11");
        liquidacion.setFechaEmision(LocalDate.now());

        LineaCuidadorFamilia linea = new LineaCuidadorFamilia(
                "Marta Cuidadora",
                BigDecimal.valueOf(40),
                BigDecimal.valueOf(4500),
                BigDecimal.valueOf(180000)
        );
        liquidacion.getLineas().add(linea);
        liquidacion.setSubtotal(BigDecimal.valueOf(180000));
        liquidacion.setTotalMensual(BigDecimal.valueOf(180000));
        liquidacion.setSaldoATransferir(BigDecimal.valueOf(180000));
        liquidacion = liquidacionFamiliaRepository.save(liquidacion);
    }

    @Test
    void muestraListadoDeFacturasYFormularioDeEmision() throws Exception {
        mockMvc.perform(get("/facturas"))
                .andExpect(status().isOk())
                .andExpect(view().name("facturas"))
                .andExpect(content().string(Matchers.containsString("Facturador ARCA")));

        mockMvc.perform(get("/facturas/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("factura-form"))
                .andExpect(content().string(Matchers.containsString("Emitir Factura ARCA")))
                .andExpect(content().string(Matchers.containsString("Facturar a Obra Social / Prepaga")));
    }

    @Test
    void precargaFormularioDesdeLiquidacionFamilia() throws Exception {
        mockMvc.perform(get("/facturas/nueva")
                        .param("liquidacionFamiliaId", liquidacion.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("factura-form"))
                .andExpect(content().string(Matchers.containsString("Roberto Sánchez")))
                .andExpect(content().string(Matchers.containsString("Marta Cuidadora")));
    }

    @Test
    void emiteFacturaHaciaObraSocialYDescargaPdf() throws Exception {
        mockMvc.perform(post("/facturas")
                        .param("puntoVenta", "0001")
                        .param("tipoComprobante", "FACTURA_C")
                        .param("concepto", "SERVICIOS")
                        .param("fechaEmision", "2026-11-18")
                        .param("periodoDesde", "2026-11-01")
                        .param("periodoHasta", "2026-11-30")
                        .param("fechaVencimientoPago", "2026-12-10")
                        .param("tipoReceptor", "OBRA_SOCIAL")
                        .param("obraSocialNombre", "PAMI - INSSJP")
                        .param("obraSocialCuit", "30-52625211-9")
                        .param("condicionIvaReceptor", "IVA_EXENTO")
                        .param("nombrePacienteAsociado", "Roberto Sánchez")
                        .param("numeroAfiliado", "150938472800")
                        .param("lineaDescripcion", "Servicio integral de cuidados domiciliarios")
                        .param("lineaCantidad", "40")
                        .param("lineaUnidadMedida", "horas")
                        .param("lineaPrecioUnitario", "4500"))
                .andExpect(status().is3xxRedirection());

        List<Factura> facturas = facturaRepository.findAll();
        assertFalse(facturas.isEmpty());
        Factura f = facturas.get(0);
        assertEquals("PAMI - INSSJP", f.getDenominacionReceptor());
        assertEquals(TipoReceptor.OBRA_SOCIAL, f.getTipoReceptor());
        assertEquals("150938472800", f.getNumeroAfiliado());

        // Ver detalle
        mockMvc.perform(get("/facturas/" + f.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("factura-detalle"))
                .andExpect(content().string(Matchers.containsString("C.A.E.")))
                .andExpect(content().string(Matchers.containsString("Comprobante Autorizado por ARCA")))
                .andExpect(content().string(Matchers.containsString("PAMI - INSSJP")));

        // Descargar PDF
        mockMvc.perform(get("/facturas/" + f.getId() + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("Factura-C-0001-")));

        // Anular factura
        mockMvc.perform(post("/facturas/" + f.getId() + "/anular")
                        .param("motivo", "Error de facturación"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas/" + f.getId()));
    }
}
