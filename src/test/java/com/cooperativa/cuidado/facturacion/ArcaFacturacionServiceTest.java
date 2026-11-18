package com.cooperativa.cuidado.facturacion;

import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.facturacion.dto.EmisionFacturaForm;
import com.cooperativa.cuidado.facturacion.dto.ItemLineaFacturaForm;
import com.cooperativa.cuidado.facturacion.service.ArcaFacturacionService;
import com.cooperativa.cuidado.liquidacionfamilia.LineaCuidadorFamilia;
import com.cooperativa.cuidado.liquidacionfamilia.LiquidacionFamilia;
import com.cooperativa.cuidado.liquidacionfamilia.LiquidacionFamiliaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:facturatest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seguridad.activa=false"
})
@Transactional
class ArcaFacturacionServiceTest {

    @Autowired
    private ArcaFacturacionService arcaService;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private LiquidacionFamiliaRepository liquidacionFamiliaRepository;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setNombrePaciente("María López");
        cliente.setDniPaciente("18234567");
        cliente.setNombreFamiliar("Carlos López");
        cliente.setTelefono("3425112233");
        cliente.setDomicilio("San Martín 1820, Santa Fe");
        cliente.setCorreo("familia.lopez@ejemplo.com");
        cliente = clienteRepository.save(cliente);
    }

    @Test
    void emiteFacturaCConCaeYCodigoBarrasHaciaPaciente() {
        EmisionFacturaForm form = new EmisionFacturaForm();
        form.setTipoComprobante(TipoComprobante.FACTURA_C);
        form.setPuntoVenta("0001");
        form.setConcepto(ConceptoFactura.SERVICIOS);
        form.setFechaEmision(LocalDate.of(2026, 11, 1));
        form.setPeriodoDesde(LocalDate.of(2026, 11, 1));
        form.setPeriodoHasta(LocalDate.of(2026, 11, 30));
        form.setFechaVencimientoPago(LocalDate.of(2026, 12, 10));

        form.setTipoReceptor(TipoReceptor.PACIENTE);
        form.setDenominacionReceptor("María López");
        form.setDocumentoReceptor("18234567");
        form.setTipoDocumentoReceptor(TipoDocumentoReceptor.DNI);
        form.setDomicilioReceptor("San Martín 1820, Santa Fe");

        form.getLineas().add(new ItemLineaFacturaForm(
                "Servicio de atención y acompañamiento domiciliario",
                BigDecimal.valueOf(40),
                "horas",
                BigDecimal.valueOf(4500),
                BigDecimal.ZERO,
                BigDecimal.valueOf(180000)
        ));

        Factura emitida = arcaService.emitirFactura(form);

        assertNotNull(emitida.getId());
        assertEquals("0001", emitida.getPuntoVenta());
        assertEquals("00000001", emitida.getNumeroComprobante());
        assertEquals(TipoComprobante.FACTURA_C, emitida.getTipoComprobante());
        assertEquals(new BigDecimal("180000.00"), emitida.getSubtotal());
        assertEquals(new BigDecimal("180000.00"), emitida.getTotal());

        // Verificaciones ARCA
        assertNotNull(emitida.getCae());
        assertEquals(14, emitida.getCae().length(), "El CAE debe contener exactamente 14 dígitos");
        assertTrue(emitida.getCae().startsWith("7"), "El CAE debe comenzar con prefijo 7");
        assertEquals(LocalDate.of(2026, 11, 11), emitida.getFechaVencimientoCae(), "El CAE vence a los 10 días");

        assertNotNull(emitida.getCodigoBarras());
        assertEquals(42, emitida.getCodigoBarras().length(), "El código de barras oficial ARCA tiene 42 dígitos");

        assertNotNull(emitida.getQrData());
        assertTrue(emitida.getQrData().startsWith("https://www.afip.gob.ar/fe/qr/?p="));

        assertEquals(EstadoFactura.EMITIDA, emitida.getEstado());
    }

    @Test
    void emiteFacturaHaciaObraSocialConDatosDeAfiliadoYPaciente() {
        EmisionFacturaForm form = new EmisionFacturaForm();
        form.setTipoComprobante(TipoComprobante.FACTURA_C);
        form.setPuntoVenta("0001");
        form.setFechaEmision(LocalDate.now());

        form.setTipoReceptor(TipoReceptor.OBRA_SOCIAL);
        form.setObraSocialNombre("I.A.P.O.S. (Instituto Autárquico Provincial de Obra Social)");
        form.setObraSocialCuit("30-65481234-8");
        form.setNombrePacienteAsociado("María López");
        form.setNumeroAfiliado("02-18234567-01");
        form.setDomicilioReceptor("San Jerónimo 3050, Santa Fe");
        form.setCondicionIvaReceptor(CondicionIva.IVA_EXENTO);

        form.getLineas().add(new ItemLineaFacturaForm(
                "Módulo mensual de enfermería y cuidados domiciliarios",
                BigDecimal.ONE,
                "módulo",
                BigDecimal.valueOf(250000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(250000)
        ));

        Factura emitida = arcaService.emitirFactura(form);

        assertEquals(TipoReceptor.OBRA_SOCIAL, emitida.getTipoReceptor());
        assertEquals("I.A.P.O.S. (Instituto Autárquico Provincial de Obra Social)", emitida.getDenominacionReceptor());
        assertEquals("30654812348", emitida.getDocumentoReceptor());
        assertEquals(TipoDocumentoReceptor.CUIT, emitida.getTipoDocumentoReceptor());
        assertEquals("02-18234567-01", emitida.getNumeroAfiliado());
        assertEquals("María López", emitida.getNombrePacienteAsociado());
        assertEquals(new BigDecimal("250000.00"), emitida.getTotal());
    }

    @Test
    void preparaFormularioDesdeLiquidacionFamiliaConLineasPreexistentes() {
        LiquidacionFamilia liq = new LiquidacionFamilia();
        liq.setCliente(cliente);
        liq.setNombrePaciente(cliente.getNombrePaciente());
        liq.setDniPaciente(cliente.getDniPaciente());
        liq.setDireccion(cliente.getDomicilio());
        liq.setPeriodo("2026-11");
        liq.setFechaEmision(LocalDate.now());

        LineaCuidadorFamilia l1 = new LineaCuidadorFamilia("Gómez, Ana", BigDecimal.valueOf(30), BigDecimal.valueOf(4000), BigDecimal.valueOf(120000));
        LineaCuidadorFamilia l2 = new LineaCuidadorFamilia("Pérez, Laura", BigDecimal.valueOf(20), BigDecimal.valueOf(4200), BigDecimal.valueOf(84000));
        liq.getLineas().addAll(List.of(l1, l2));
        liq.setSubtotal(BigDecimal.valueOf(204000));
        liq.setTotalMensual(BigDecimal.valueOf(204000));
        liq.setSaldoATransferir(BigDecimal.valueOf(204000));
        liq = liquidacionFamiliaRepository.save(liq);

        EmisionFacturaForm form = arcaService.prepararFormularioDesdeLiquidacionFamilia(liq.getId());

        assertEquals(liq.getId(), form.getLiquidacionFamiliaId());
        assertEquals(cliente.getId(), form.getClienteId());
        assertEquals("María López", form.getDenominacionReceptor());
        assertEquals("18234567", form.getDocumentoReceptor());
        assertEquals(2, form.getLineas().size());
        assertEquals(BigDecimal.valueOf(30), form.getLineas().get(0).getCantidad());
        assertEquals(BigDecimal.valueOf(4000), form.getLineas().get(0).getPrecioUnitario());

        // Emitir factura con este formulario pero cambiando destinatario a Obra Social
        form.setTipoReceptor(TipoReceptor.OBRA_SOCIAL);
        form.setObraSocialNombre("PAMI - INSSJP");
        form.setObraSocialCuit("30-52625211-9");
        form.setNumeroAfiliado("150938472800");

        Factura emitida = arcaService.emitirFactura(form);
        assertNotNull(emitida.getId());
        assertEquals("PAMI - INSSJP", emitida.getDenominacionReceptor());
        assertEquals("150938472800", emitida.getNumeroAfiliado());
        assertEquals(new BigDecimal("204000.00"), emitida.getTotal());
        assertEquals(liq.getId(), emitida.getLiquidacionFamiliaId());
    }

    @Test
    void rechazaEmisionSiObraSocialNoTieneCuit() {
        EmisionFacturaForm form = new EmisionFacturaForm();
        form.setTipoReceptor(TipoReceptor.OBRA_SOCIAL);
        form.setObraSocialNombre("OSDE");
        form.setObraSocialCuit(""); // Vacío
        form.getLineas().add(new ItemLineaFacturaForm("Cuidado", BigDecimal.ONE, "hs", BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.valueOf(1000)));

        assertThrows(DatosInvalidosException.class, () -> arcaService.emitirFactura(form));
    }

    @Test
    void anulaFacturaRegistrandoMotivo() {
        EmisionFacturaForm form = new EmisionFacturaForm();
        form.setDenominacionReceptor("Juan Pérez");
        form.setDocumentoReceptor("20334455");
        form.getLineas().add(new ItemLineaFacturaForm("Servicio", BigDecimal.ONE, "hs", BigDecimal.valueOf(5000), BigDecimal.ZERO, BigDecimal.valueOf(5000)));

        Factura emitida = arcaService.emitirFactura(form);
        assertEquals(EstadoFactura.EMITIDA, emitida.getEstado());

        arcaService.anularFactura(emitida.getId(), "Error en el importe convenido");
        Factura anulada = arcaService.obtener(emitida.getId());
        assertEquals(EstadoFactura.ANULADA, anulada.getEstado());
        assertTrue(anulada.getObservaciones().contains("Error en el importe convenido"));
    }
}
