package com.cooperativa.cuidado.facturacion.dto;

import com.cooperativa.cuidado.facturacion.ConceptoFactura;
import com.cooperativa.cuidado.facturacion.CondicionIva;
import com.cooperativa.cuidado.facturacion.TipoComprobante;
import com.cooperativa.cuidado.facturacion.TipoDocumentoReceptor;
import com.cooperativa.cuidado.facturacion.TipoReceptor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmisionFacturaForm {

    private String puntoVenta = "0001";
    private TipoComprobante tipoComprobante = TipoComprobante.FACTURA_C;
    private ConceptoFactura concepto = ConceptoFactura.SERVICIOS;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaEmision = LocalDate.now();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodoDesde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodoHasta;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVencimientoPago;

    private TipoReceptor tipoReceptor = TipoReceptor.PACIENTE;
    private TipoDocumentoReceptor tipoDocumentoReceptor = TipoDocumentoReceptor.DNI;
    private String documentoReceptor;
    private String denominacionReceptor;
    private CondicionIva condicionIvaReceptor = CondicionIva.CONSUMIDOR_FINAL;
    private String domicilioReceptor;
    private String emailReceptor;

    // Campos para Obra Social
    private String obraSocialNombre;
    private String obraSocialCuit;
    private String numeroAfiliado;
    private String nombrePacienteAsociado;

    private Long clienteId;
    private Long liquidacionFamiliaId;

    private BigDecimal porcentajeIva = BigDecimal.ZERO;
    private BigDecimal importeOtrosTributos = BigDecimal.ZERO;
    private String observaciones;

    private List<ItemLineaFacturaForm> lineas = new ArrayList<>();

    // Parámetros de formulario para enlace directo de listas HTML
    private List<String> lineaDescripcion = new ArrayList<>();
    private List<BigDecimal> lineaCantidad = new ArrayList<>();
    private List<BigDecimal> lineaPrecioUnitario = new ArrayList<>();
    private List<String> lineaUnidadMedida = new ArrayList<>();

    public EmisionFacturaForm() {
    }

    public List<ItemLineaFacturaForm> consolidarLineas() {
        if (lineas != null && !lineas.isEmpty()) {
            return lineas;
        }
        List<ItemLineaFacturaForm> resultado = new ArrayList<>();
        if (lineaDescripcion != null) {
            for (int i = 0; i < lineaDescripcion.size(); i++) {
                String desc = lineaDescripcion.get(i);
                if (desc == null || desc.isBlank()) {
                    continue;
                }
                BigDecimal cant = (lineaCantidad != null && i < lineaCantidad.size() && lineaCantidad.get(i) != null)
                        ? lineaCantidad.get(i) : BigDecimal.ONE;
                BigDecimal precio = (lineaPrecioUnitario != null && i < lineaPrecioUnitario.size() && lineaPrecioUnitario.get(i) != null)
                        ? lineaPrecioUnitario.get(i) : BigDecimal.ZERO;
                String um = (lineaUnidadMedida != null && i < lineaUnidadMedida.size() && lineaUnidadMedida.get(i) != null)
                        ? lineaUnidadMedida.get(i) : "horas";
                BigDecimal sub = cant.multiply(precio);
                resultado.add(new ItemLineaFacturaForm(desc, cant, um, precio, BigDecimal.ZERO, sub));
            }
        }
        return resultado;
    }

    public String getPuntoVenta() {
        return puntoVenta;
    }

    public void setPuntoVenta(String puntoVenta) {
        this.puntoVenta = puntoVenta;
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

    public BigDecimal getPorcentajeIva() {
        return porcentajeIva;
    }

    public void setPorcentajeIva(BigDecimal porcentajeIva) {
        this.porcentajeIva = porcentajeIva;
    }

    public BigDecimal getImporteOtrosTributos() {
        return importeOtrosTributos;
    }

    public void setImporteOtrosTributos(BigDecimal importeOtrosTributos) {
        this.importeOtrosTributos = importeOtrosTributos;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<ItemLineaFacturaForm> getLineas() {
        return lineas;
    }

    public void setLineas(List<ItemLineaFacturaForm> lineas) {
        this.lineas = lineas;
    }

    public List<String> getLineaDescripcion() {
        return lineaDescripcion;
    }

    public void setLineaDescripcion(List<String> lineaDescripcion) {
        this.lineaDescripcion = lineaDescripcion;
    }

    public List<BigDecimal> getLineaCantidad() {
        return lineaCantidad;
    }

    public void setLineaCantidad(List<BigDecimal> lineaCantidad) {
        this.lineaCantidad = lineaCantidad;
    }

    public List<BigDecimal> getLineaPrecioUnitario() {
        return lineaPrecioUnitario;
    }

    public void setLineaPrecioUnitario(List<BigDecimal> lineaPrecioUnitario) {
        this.lineaPrecioUnitario = lineaPrecioUnitario;
    }

    public List<String> getLineaUnidadMedida() {
        return lineaUnidadMedida;
    }

    public void setLineaUnidadMedida(List<String> lineaUnidadMedida) {
        this.lineaUnidadMedida = lineaUnidadMedida;
    }
}
