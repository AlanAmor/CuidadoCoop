package com.cooperativa.cuidado.facturacion;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "facturas",
        indexes = {
                @Index(name = "idx_facturas_numero", columnList = "puntoVenta,tipoComprobante,numeroComprobante"),
                @Index(name = "idx_facturas_cae", columnList = "cae"),
                @Index(name = "idx_facturas_fecha", columnList = "fechaEmision"),
                @Index(name = "idx_facturas_cliente", columnList = "clienteId"),
                @Index(name = "idx_facturas_liq_familia", columnList = "liquidacionFamiliaId")
        }
)
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5)
    private String puntoVenta = "0001";

    @Column(nullable = false, length = 8)
    private String numeroComprobante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoComprobante tipoComprobante = TipoComprobante.FACTURA_C;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConceptoFactura concepto = ConceptoFactura.SERVICIOS;

    @Column(nullable = false)
    private LocalDate fechaEmision = LocalDate.now();

    private LocalDate periodoDesde;

    private LocalDate periodoHasta;

    private LocalDate fechaVencimientoPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoReceptor tipoReceptor = TipoReceptor.PACIENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoDocumentoReceptor tipoDocumentoReceptor = TipoDocumentoReceptor.DNI;

    @Column(nullable = false, length = 30)
    private String documentoReceptor = "";

    @Column(nullable = false, length = 150)
    private String denominacionReceptor = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CondicionIva condicionIvaReceptor = CondicionIva.CONSUMIDOR_FINAL;

    @Column(length = 200)
    private String domicilioReceptor = "";

    @Column(length = 150)
    private String emailReceptor = "";

    @Column(length = 150)
    private String obraSocialNombre = "";

    @Column(length = 20)
    private String obraSocialCuit = "";

    @Column(length = 60)
    private String numeroAfiliado = "";

    @Column(length = 150)
    private String nombrePacienteAsociado = "";

    private Long clienteId;

    private Long liquidacionFamiliaId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "factura_lineas", joinColumns = @JoinColumn(name = "factura_id"))
    @OrderColumn(name = "orden")
    private List<LineaFactura> lineas = new ArrayList<>();

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeIva = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal importeIva = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal importeOtrosTributos = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false, length = 14)
    private String cae = "";

    @Column(nullable = false)
    private LocalDate fechaVencimientoCae;

    @Column(length = 60)
    private String codigoBarras = "";

    @Column(length = 2000)
    private String qrData = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoFactura estado = EstadoFactura.EMITIDA;

    @Column(length = 1000)
    private String observaciones;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public Factura() {
    }

    public String getComprobanteFormateado() {
        return (tipoComprobante != null ? tipoComprobante.getDescripcion() : "Factura")
                + " " + getPuntoVentaYNumero();
    }

    public String getPuntoVentaYNumero() {
        return (puntoVenta != null ? puntoVenta : "0001") + "-" + (numeroComprobante != null ? numeroComprobante : "00000000");
    }

    public String getPeriodoFormateado() {
        if (periodoDesde == null || periodoHasta == null) {
            return "-";
        }
        return periodoDesde + " al " + periodoHasta;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPuntoVenta() {
        return puntoVenta;
    }

    public void setPuntoVenta(String puntoVenta) {
        this.puntoVenta = puntoVenta;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public TipoComprobante getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(TipoComprobante tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public ConceptoFactura getConcepto() {
        return concepto;
    }

    public void setConcepto(ConceptoFactura concepto) {
        this.concepto = concepto;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getPeriodoDesde() {
        return periodoDesde;
    }

    public void setPeriodoDesde(LocalDate periodoDesde) {
        this.periodoDesde = periodoDesde;
    }

    public LocalDate getPeriodoHasta() {
        return periodoHasta;
    }

    public void setPeriodoHasta(LocalDate periodoHasta) {
        this.periodoHasta = periodoHasta;
    }

    public LocalDate getFechaVencimientoPago() {
        return fechaVencimientoPago;
    }

    public void setFechaVencimientoPago(LocalDate fechaVencimientoPago) {
        this.fechaVencimientoPago = fechaVencimientoPago;
    }

    public TipoReceptor getTipoReceptor() {
        return tipoReceptor;
    }

    public void setTipoReceptor(TipoReceptor tipoReceptor) {
        this.tipoReceptor = tipoReceptor;
    }

    public TipoDocumentoReceptor getTipoDocumentoReceptor() {
        return tipoDocumentoReceptor;
    }

    public void setTipoDocumentoReceptor(TipoDocumentoReceptor tipoDocumentoReceptor) {
        this.tipoDocumentoReceptor = tipoDocumentoReceptor;
    }

    public String getDocumentoReceptor() {
        return documentoReceptor;
    }

    public void setDocumentoReceptor(String documentoReceptor) {
        this.documentoReceptor = documentoReceptor;
    }

    public String getDenominacionReceptor() {
        return denominacionReceptor;
    }

    public void setDenominacionReceptor(String denominacionReceptor) {
        this.denominacionReceptor = denominacionReceptor;
    }

    public CondicionIva getCondicionIvaReceptor() {
        return condicionIvaReceptor;
    }

    public void setCondicionIvaReceptor(CondicionIva condicionIvaReceptor) {
        this.condicionIvaReceptor = condicionIvaReceptor;
    }

    public String getDomicilioReceptor() {
        return domicilioReceptor;
    }

    public void setDomicilioReceptor(String domicilioReceptor) {
        this.domicilioReceptor = domicilioReceptor;
    }

    public String getEmailReceptor() {
        return emailReceptor;
    }

    public void setEmailReceptor(String emailReceptor) {
        this.emailReceptor = emailReceptor;
    }

    public String getObraSocialNombre() {
        return obraSocialNombre;
    }

    public void setObraSocialNombre(String obraSocialNombre) {
        this.obraSocialNombre = obraSocialNombre;
    }

    public String getObraSocialCuit() {
        return obraSocialCuit;
    }

    public void setObraSocialCuit(String obraSocialCuit) {
        this.obraSocialCuit = obraSocialCuit;
    }

    public String getNumeroAfiliado() {
        return numeroAfiliado;
    }

    public void setNumeroAfiliado(String numeroAfiliado) {
        this.numeroAfiliado = numeroAfiliado;
    }

    public String getNombrePacienteAsociado() {
        return nombrePacienteAsociado;
    }

    public void setNombrePacienteAsociado(String nombrePacienteAsociado) {
        this.nombrePacienteAsociado = nombrePacienteAsociado;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getLiquidacionFamiliaId() {
        return liquidacionFamiliaId;
    }

    public void setLiquidacionFamiliaId(Long liquidacionFamiliaId) {
        this.liquidacionFamiliaId = liquidacionFamiliaId;
    }

    public List<LineaFactura> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaFactura> lineas) {
        this.lineas = lineas;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getPorcentajeIva() {
        return porcentajeIva;
    }

    public void setPorcentajeIva(BigDecimal porcentajeIva) {
        this.porcentajeIva = porcentajeIva;
    }

    public BigDecimal getImporteIva() {
        return importeIva;
    }

    public void setImporteIva(BigDecimal importeIva) {
        this.importeIva = importeIva;
    }

    public BigDecimal getImporteOtrosTributos() {
        return importeOtrosTributos;
    }

    public void setImporteOtrosTributos(BigDecimal importeOtrosTributos) {
        this.importeOtrosTributos = importeOtrosTributos;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getCae() {
        return cae;
    }

    public void setCae(String cae) {
        this.cae = cae;
    }

    public LocalDate getFechaVencimientoCae() {
        return fechaVencimientoCae;
    }

    public void setFechaVencimientoCae(LocalDate fechaVencimientoCae) {
        this.fechaVencimientoCae = fechaVencimientoCae;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getQrData() {
        return qrData;
    }

    public void setQrData(String qrData) {
        this.qrData = qrData;
    }

    public EstadoFactura getEstado() {
        return estado;
    }

    public void setEstado(EstadoFactura estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
